/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq.rule;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule type KHIS_PARITY (malaria G-RPT-02): once a sent report has been read back from KHIS (from
 * the 10th of the following month), the EMR values are compared with the values KHIS holds. A
 * variance above tolerance (default 5%) opens a reconciliation ticket with the EMR value, the KHIS
 * value and the variance.
 * <p>
 * "compare" sets what is compared:
 * <ul>
 * <li>INDICATORS (default): each indicator the rule watches, only the part of it in the report
 * being compared</li>
 * <li>ALL_VALUES: every data element of the report sent to KHIS, added up over its category option
 * combos (e.g. age and sex)</li>
 * </ul>
 * Only cells with a KHIS mapping (data element and category option combo) can be compared. Cells
 * sharing a data element and category option combo are added up on the EMR side and the KHIS value
 * is counted once.
 * <p>
 * Settings: compare, indicators (INDICATORS; default every indicator the program watches), reports
 * (required for ALL_VALUES; otherwise limits the reports compared), tolerance (default 0.05),
 * severity (default TICKET).
 */
public class KhisParityRule extends AbstractDqRule {

	public static final String ID = "G-RPT-02";

	public static final String COMPARE_INDICATORS = "INDICATORS";

	public static final String COMPARE_ALL_VALUES = "ALL_VALUES";

	public KhisParityRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.KHIS_READBACK;
	}

	private boolean allValues() {
		return COMPARE_ALL_VALUES.equalsIgnoreCase(settings().getString("compare", COMPARE_INDICATORS));
	}

	@Override
	public Set<String> getReports(DqConfig config) {
		List<String> compared = settings().getStrings("reports");
		if (allValues()) {
			return new LinkedHashSet<String>(compared);
		}
		Set<String> reports = new LinkedHashSet<String>();
		for (DqConfig.Indicator indicator : indicators(config)) {
			reports.addAll(indicator.getReports());
		}
		if (!compared.isEmpty()) {
			reports.retainAll(compared);
		}
		return reports;
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		List<String> compared = settings().getStrings("reports");
		if (!compared.isEmpty() && (context.getRun() == null || !compared.contains(context.getRun().getReportName()))) {
			return false;
		}
		if (allValues()) {
			return context.getRun() != null && !compared.isEmpty();
		}
		for (DqConfig.Indicator indicator : indicators(context.getConfig())) {
			if (readsRun(context, indicator)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		return allValues() ? evaluateAllValues(context) : evaluateIndicators(context);
	}

	private List<DqResult> evaluateIndicators(DqRuleContext context) {
		String report = context.getRun().getReportName();
		Map<String, DqValue> byCell = new HashMap<String, DqValue>();
		for (DqValue value : context.getValues(context.getRun())) {
			byCell.put(value.getDatasetName() + "\u0000" + value.getColumnName(), value);
		}

		List<DqResult> results = new ArrayList<DqResult>();
		for (DqConfig.Indicator indicator : indicators(context.getConfig())) {
			if (!readsRun(context, indicator)) {
				continue;
			}
			Cells cells = new Cells();
			Set<String> unmapped = new HashSet<String>();
			for (DqConfig.Part part : indicator.getParts()) {
				if (!report.equals(part.getReport())) {
					continue;
				}
				for (String column : part.getColumns()) {
					DqValue value = byCell.get(part.getDataset() + "\u0000" + column);
					if (value == null || value.getDataElement() == null) {
						unmapped.add(part.getDataset() + " / " + column);
					} else {
						cells.add(value);
					}
				}
			}
			Map<String, Object> detail = new LinkedHashMap<String, Object>();
			if (!unmapped.isEmpty()) {
				detail.put("notMapped", new ArrayList<String>(unmapped));
			}
			results.add(compare(context, indicator.getCode(), indicator.getName(), cells, detail));
		}
		return results;
	}

	/**
	 * One result per data element of the report, named by the report columns that carry it
	 */
	private List<DqResult> evaluateAllValues(DqRuleContext context) {
		Map<String, Cells> byDataElement = new LinkedHashMap<String, Cells>();
		Map<String, Set<String>> columns = new HashMap<String, Set<String>>();
		for (DqValue value : context.getValues(context.getRun())) {
			if (value.getDataElement() == null) {
				continue;
			}
			Cells cells = byDataElement.get(value.getDataElement());
			if (cells == null) {
				cells = new Cells();
				byDataElement.put(value.getDataElement(), cells);
				columns.put(value.getDataElement(), new LinkedHashSet<String>());
			}
			cells.add(value);
			columns.get(value.getDataElement()).add(value.getColumnName());
		}

		List<DqResult> results = new ArrayList<DqResult>();
		for (Map.Entry<String, Cells> entry : byDataElement.entrySet()) {
			String name = StringUtils.abbreviate(StringUtils.join(columns.get(entry.getKey()), ", "), 200);
			Map<String, Object> detail = new LinkedHashMap<String, Object>();
			detail.put("dataElement", entry.getKey());
			detail.put("columns", new ArrayList<String>(columns.get(entry.getKey())));
			results.add(compare(context, entry.getKey(), name, entry.getValue(), detail));
		}
		if (results.isEmpty()) {
			DqResult result = result(context, null, DqOutcome.NOT_APPLICABLE, context.getRun().getReportName()
					+ " has no value with a KHIS mapping, so it cannot be compared with KHIS.");
			result.setThreshold(threshold());
			results.add(result);
		}
		return results;
	}

	private DqResult compare(DqRuleContext context, String code, String name, Cells cells, Map<String, Object> detail) {
		DqRuleSettings settings = settings();
		double tolerance = settings.getDouble("tolerance", 0.05);
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.TICKET);
		String report = context.getRun().getReportName();

		Map<String, Object> all = new LinkedHashMap<String, Object>();
		all.put("report", report);
		all.put("cells", cells.emr.size());
		all.putAll(detail);
		DqResult result;
		if (cells.emr.isEmpty()) {
			result = result(context, code, DqOutcome.NOT_APPLICABLE, name + ": none of its " + report
					+ " cells has a KHIS mapping, so it cannot be compared with KHIS.");
		} else {
			double emrTotal = 0;
			double khisTotal = 0;
			for (Map.Entry<String, Double> entry : cells.emr.entrySet()) {
				emrTotal += entry.getValue();
				// A cell KHIS has no value for: the EMR sent nothing, or KHIS lost it
				Double held = cells.khis.get(entry.getKey());
				khisTotal += held != null ? held : 0;
			}
			double variance = emrTotal == 0 ? (khisTotal == 0 ? 0 : 1) : (khisTotal - emrTotal) / emrTotal;
			boolean outside = Math.abs(variance) > tolerance + EPSILON;
			result = result(context, code, outside ? severity : DqOutcome.PASS, name + " in " + report + ": EMR "
					+ format(emrTotal) + ", KHIS " + format(khisTotal) + ", a variance of " + percent(Math.abs(variance))
					+ (outside ? "; allowed ±" + percent(tolerance) + "." : "."));
			result.setNumerator(emrTotal);
			result.setDenominator(khisTotal);
			result.setMeasureValue(variance);
		}
		result.setThreshold(threshold());
		result.setDetail(json(all));
		return result;
	}

	private String threshold() {
		return "within ±" + percent(settings().getDouble("tolerance", 0.05)) + " of the EMR";
	}

	/**
	 * The EMR and KHIS values of a set of cells, by data element and category option combo
	 */
	private static class Cells {

		final Map<String, Double> emr = new LinkedHashMap<String, Double>();

		final Map<String, Double> khis = new HashMap<String, Double>();

		void add(DqValue value) {
			String key = value.getDataElement() + "|" + value.getCategoryOptionCombo();
			Double sum = emr.get(key);
			emr.put(key, (sum != null ? sum : 0) + (value.getValueNumeric() != null ? value.getValueNumeric() : 0));
			if (value.getKhisValue() != null) {
				khis.put(key, value.getKhisValue());
			}
		}
	}

	/**
	 * @return the indicators in the "indicators" setting, else every indicator the program watches
	 */
	List<DqConfig.Indicator> indicators(DqConfig config) {
		List<String> codes = settings().getStrings("indicators");
		if (codes.isEmpty() && getDefinition().getProgram() != null) {
			codes = getDefinition().getProgram().getIndicators();
		}
		List<DqConfig.Indicator> indicators = new ArrayList<DqConfig.Indicator>();
		for (String code : codes) {
			DqConfig.Indicator indicator = config.getIndicator(code);
			if (indicator != null) {
				indicators.add(indicator);
			}
		}
		return indicators;
	}
}
