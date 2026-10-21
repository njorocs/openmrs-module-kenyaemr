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

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportManager;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.module.kenyaemr.reporting.air.AdxConfiguration;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableAdxGenerationStrategy;
import org.openmrs.module.kenyaemr.reporting.air.ReportCellValue;
import org.openmrs.module.kenyaemr.reporting.air.ReportValueExtractor;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.util.OpenmrsClassLoader;

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
		return new GeneratedRun(run, values);
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
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (DqValue value : values) {
				String line = value.getDatasetName() + "\u0000" + value.getColumnName() + "\u0000" + value.getValueText() + "\n";
				digest.update(line.getBytes(StandardCharsets.UTF_8));
			}
			StringBuilder hex = new StringBuilder();
			for (byte b : digest.digest()) {
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

		public GeneratedRun(DqReportRun run, List<DqValue> values) {
			this.run = run;
			this.values = values;
		}

		public DqReportRun getRun() {
			return run;
		}

		public List<DqValue> getValues() {
			return values;
		}
	}
}
