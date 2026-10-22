/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportManager;
import org.openmrs.module.kenyaemr.model.dq.DqReportPayload;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.module.kenyaemr.reporting.air.AdxConfiguration;
import org.openmrs.module.kenyaemr.reporting.air.AdxMetadata;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableAdxGenerationStrategy;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableAdxReportRenderer;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableJsonReportRenderer;
import org.openmrs.module.kenyaemr.reporting.air.ReportCellValue;
import org.openmrs.module.kenyaemr.reporting.air.ReportValueExtractor;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.util.OpenmrsClassLoader;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Evaluates a report for a month and turns it into a new {@link DqReportRun} version with one
 * {@link DqValue} per report cell
 */
public class DqReportGenerator {

	private static final Log log = LogFactory.getLog(DqReportGenerator.class);

	/**
	 * Evaluates the report. Nothing is saved.
	 *
	 * @param reportName the report descriptor name, e.g. "MOH-743 Report"
	 * @param periodStart any date in the month to report on
	 * @return a run (version not yet assigned) holding its values
	 */
	public GeneratedRun generate(String reportName, Date periodStart) {
		ReportDescriptor descriptor = getDescriptor(reportName);
		ReportDefinition definition = getOrBuildDefinition(descriptor);

		Date start = DqPeriods.monthStart(periodStart);
		Date end = DqPeriods.lastDayOfMonth(periodStart);

		EvaluationContext context = new EvaluationContext();
		context.addParameterValue("startDate", start);
		context.addParameterValue("endDate", end);

		ReportData reportData;
		try {
			reportData = Context.getService(ReportDefinitionService.class).evaluate(definition, context);
		}
		catch (EvaluationException e) {
			throw new APIException("Could not evaluate " + reportName + " for " + start, e);
		}

		AdxConfiguration config = new ConfigurableAdxGenerationStrategy().getConfigurationForReport(reportName);
		List<ReportCellValue> cells = ReportValueExtractor.extractAll(reportData, config);

		DqReportRun run = new DqReportRun();
		run.setReportName(reportName);
		run.setReportUuid(descriptor.getTargetUuid());
		run.setPeriodStart(start);
		run.setPeriodEnd(end);
		run.setStatus(DqRunStatus.GENERATED);
		run.setGeneratedAt(new Date());

		List<DqValue> values = toValues(run, cells);
		run.setValuesHash(hash(values));
		return new GeneratedRun(run, values, renderPayload(run, reportData, config));
	}

	/**
	 * Renders the payload the existing send button would post, so the approved version can be sent
	 * exactly as it was checked. A report without a KHIS mapping, or one that fails to render, gets
	 * none and cannot be sent through the pipeline; the run itself is still saved and checked.
	 */
	static DqReportPayload renderPayload(DqReportRun run, ReportData reportData, AdxConfiguration config) {
		if (config == null) {
			return null;
		}
		boolean json = "json".equalsIgnoreCase(config.getOutputFormat());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			if (json) {
				new ConfigurableJsonReportRenderer().render(reportData, null, out);
			} else {
				new ConfigurableAdxReportRenderer().render(reportData, null, out);
			}
		}
		catch (Exception e) {
			log.warn("Could not render the KHIS payload of " + run.getReportName() + " for " + run.getPeriodStart(), e);
			return null;
		}

		DqReportPayload payload = new DqReportPayload();
		payload.setReportRun(run);
		payload.setFormat(json ? DqReportPayload.FORMAT_JSON : DqReportPayload.FORMAT_ADX);
		payload.setContent(new String(out.toByteArray(), StandardCharsets.UTF_8));
		payload.setContentHash(sha256(payload.getContent()));
		payload.setEndpointPath(endpointPath(new ConfigurableAdxGenerationStrategy().getAdxMetadata(reportData)));
		payload.setCreatedAt(new Date());
		return payload;
	}

	/**
	 * @return the part of the configured endpoint from "api/" on; the server address is read when
	 * sending so that it is never baked into a stored payload
	 */
	static String endpointPath(AdxMetadata metadata) {
		String url = metadata != null ? metadata.getEndpointUrl() : null;
		int api = url != null ? url.indexOf("/api/") : -1;
		return api >= 0 ? url.substring(api + 1) : null;
	}

	static List<DqValue> toValues(DqReportRun run, List<ReportCellValue> cells) {
		List<DqValue> values = new ArrayList<DqValue>();
		for (ReportCellValue cell : cells) {
			DqValue value = new DqValue();
			value.setReportRun(run);
			value.setDatasetName(cell.getDatasetName());
			value.setColumnName(cell.getColumnName());
			value.setMappingKey(cell.getMappingKey());
			if (cell.isMapped()) {
				value.setDataElement(cell.getDataElement());
				value.setCategoryOptionCombo(cell.getCategoryOptionCombo());
			}
			value.setValueText(cell.getValue());
			value.setValueNumeric(DqIndicatorCalculator.parseNumber(cell.getValue()));
			value.setSource(DqValue.SOURCE_REPORT);
			values.add(value);
		}
		return values;
	}

	/**
	 * @return a SHA-256 over every cell, so an unchanged regeneration can be recognised
	 */
	static String hash(List<DqValue> values) {
		StringBuilder lines = new StringBuilder();
		for (DqValue value : values) {
			lines.append(value.getDatasetName()).append('\u0000').append(value.getColumnName()).append('\u0000')
					.append(value.getValueText()).append('\n');
		}
		return sha256(lines.toString());
	}

	static String sha256(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			StringBuilder hex = new StringBuilder();
			for (byte b : digest.digest(text.getBytes(StandardCharsets.UTF_8))) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private ReportDescriptor getDescriptor(String reportName) {
		ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
		for (ReportDescriptor descriptor : reportManager.getAllReportDescriptors()) {
			if (reportName.equals(descriptor.getName())) {
				return descriptor;
			}
		}
		throw new APIException("No report named " + reportName);
	}

	/**
	 * Same lookup the reporting REST controller uses, so the DQ run evaluates the same definition
	 * the HRIO runs by hand
	 */
	private ReportDefinition getOrBuildDefinition(ReportDescriptor descriptor) {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(OpenmrsClassLoader.getInstance());
		try {
			ReportDefinitionService definitionService = Context.getService(ReportDefinitionService.class);
			ReportDefinition definition = definitionService.getDefinitionByUuid(descriptor.getTargetUuid());
			if (definition == null) {
				ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
				definition = reportManager.buildReportDefinition(descriptor);
				if (definition == null) {
					throw new APIException("Could not build report definition for " + descriptor.getName());
				}
				definition = definitionService.saveDefinition(definition);
			}
			return definition;
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * A generated run and its values, not yet saved
	 */
	public static class GeneratedRun {

		private final DqReportRun run;

		private final List<DqValue> values;

		private final DqReportPayload payload;

		public GeneratedRun(DqReportRun run, List<DqValue> values, DqReportPayload payload) {
			this.run = run;
			this.values = values;
			this.payload = payload;
		}

		public DqReportRun getRun() {
			return run;
		}

		public List<DqValue> getValues() {
			return values;
		}

		/**
		 * @return the KHIS payload, or null when the report has none
		 */
		public DqReportPayload getPayload() {
			return payload;
		}
	}
}
