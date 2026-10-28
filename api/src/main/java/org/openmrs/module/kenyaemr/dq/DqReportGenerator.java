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
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.util.OpenmrsClassLoader;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

		Date start = DqPeriods.monthStart(periodStart);
		Date end = DqPeriods.lastDayOfMonth(periodStart);
		ReportData reportData = evaluate(descriptor, start, end);

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
	 * Evaluates some columns of a report for any period, e.g. a week, for checks that are not
	 * monthly. Only those columns are counted, so a weekly check of two indicators does not run
	 * every row of the report. Nothing is saved, and there is no payload.
	 *
	 * @param columns column names by dataset name; datasets not listed are not evaluated
	 * @return the cell values, attached to an unsaved run that names the report and period
	 */
	public List<DqValue> evaluateValues(String reportName, Map<String, ? extends Collection<String>> columns, Date start,
	                                    Date end) {
		ReportDescriptor descriptor = getDescriptor(reportName);
		ReportDefinition definition = subset(getOrBuildDefinition(descriptor), columns);
		ReportData reportData = evaluate(definition, descriptor, start, end);
		AdxConfiguration config = new ConfigurableAdxGenerationStrategy().getConfigurationForReport(reportName);
		DqReportRun run = new DqReportRun();
		run.setReportName(reportName);
		run.setReportUuid(descriptor.getTargetUuid());
		run.setPeriodStart(start);
		run.setPeriodEnd(end);
		return toValues(run, ReportValueExtractor.extractAll(reportData, config));
	}

	/**
	 * An unsaved copy of a report definition with only the given datasets and, in cohort indicator
	 * datasets, only the given columns. Any other kind of dataset that is listed is kept whole.
	 *
	 * @param columns column names by dataset name (the dataset definition's name, or failing that
	 * its key in the report, as the stored values name it)
	 */
	static ReportDefinition subset(ReportDefinition definition, Map<String, ? extends Collection<String>> columns) {
		ReportDefinition subset = new ReportDefinition();
		subset.setName(definition.getName());
		subset.setUuid(definition.getUuid());
		subset.setParameters(definition.getParameters());
		subset.setBaseCohortDefinition(definition.getBaseCohortDefinition());
		for (Map.Entry<String, Mapped<? extends DataSetDefinition>> entry : definition.getDataSetDefinitions().entrySet()) {
			DataSetDefinition dataset = entry.getValue().getParameterizable();
			String name = dataset != null && dataset.getName() != null ? dataset.getName() : entry.getKey();
			Collection<String> wanted = columns.get(name);
			if (wanted == null) {
				continue;
			}
			if (dataset instanceof CohortIndicatorDataSetDefinition) {
				CohortIndicatorDataSetDefinition source = (CohortIndicatorDataSetDefinition) dataset;
				CohortIndicatorDataSetDefinition copy = new CohortIndicatorDataSetDefinition();
				copy.setName(source.getName());
				copy.setDescription(source.getDescription());
				copy.setParameters(source.getParameters());
				copy.setDimensions(source.getDimensions());
				for (CohortIndicatorDataSetDefinition.CohortIndicatorAndDimensionColumn column : source.getColumns()) {
					if (wanted.contains(column.getName())) {
						copy.addColumn(column.getName(), column.getLabel(), column.getIndicator(), column.getDimensionOptions());
					}
				}
				dataset = copy;
			}
			subset.addDataSetDefinition(entry.getKey(),
					new Mapped<DataSetDefinition>(dataset, entry.getValue().getParameterMappings()));
		}
		return subset;
	}

	private ReportData evaluate(ReportDescriptor descriptor, Date start, Date end) {
		return evaluate(getOrBuildDefinition(descriptor), descriptor, start, end);
	}

	private ReportData evaluate(ReportDefinition definition, ReportDescriptor descriptor, Date start, Date end) {
		EvaluationContext context = new EvaluationContext();
		context.addParameterValue("startDate", start);
		context.addParameterValue("endDate", end);
		try {
			return Context.getService(ReportDefinitionService.class).evaluate(definition, context);
		}
		catch (EvaluationException e) {
			throw new APIException("Could not evaluate " + descriptor.getName() + " for " + start, e);
		}
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
