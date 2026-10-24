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
 * the 10th of the following month), each indicator's EMR value is compared with the value KHIS
 * holds. A variance above tolerance (default 5%) opens a reconciliation ticket with the EMR value,
 * the KHIS value and the variance.
 * <p>
 * Only the part of an indicator in the report being compared counts, and only cells with a KHIS
 * mapping (data element and category option combo) can be compared. Cells sharing a data element
 * are added up on the EMR side and the KHIS value is counted once.
 * <p>
 * Settings: indicators (default: every indicator the program watches), tolerance (default 0.05),
 * severity (default TICKET).
 */
public class KhisParityRule extends AbstractDqRule {

	public static final String ID = "G-RPT-02";

	public KhisParityRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.KHIS_READBACK;
	}

	@Override
	public Set<String> getReports(DqConfig config) {
		Set<String> reports = new LinkedHashSet<String>();
		for (DqConfig.Indicator indicator : indicators(config)) {
			reports.addAll(indicator.getReports());
		}
		return reports;
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		for (DqConfig.Indicator indicator : indicators(context.getConfig())) {
			if (readsRun(context, indicator)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		double tolerance = settings.getDouble("tolerance", 0.05);
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.TICKET);
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
			Map<String, Double> emr = new LinkedHashMap<String, Double>();
			Map<String, Double> khis = new HashMap<String, Double>();
			Set<String> unmapped = new HashSet<String>();
			for (DqConfig.Part part : indicator.getParts()) {
				if (!report.equals(part.getReport())) {
					continue;
				}
				for (String column : part.getColumns()) {
					DqValue value = byCell.get(part.getDataset() + "\u0000" + column);
					if (value == null || value.getDataElement() == null) {
						unmapped.add(part.getDataset() + " / " + column);
						continue;
					}
					String key = value.getDataElement() + "|" + value.getCategoryOptionCombo();
					Double sum = emr.get(key);
					emr.put(key, (sum != null ? sum : 0) + (value.getValueNumeric() != null ? value.getValueNumeric() : 0));
					if (value.getKhisValue() != null) {
						khis.put(key, value.getKhisValue());
					}
				}
			}

			Map<String, Object> detail = new LinkedHashMap<String, Object>();
			detail.put("report", report);
			detail.put("dataElements", emr.size());
			if (!unmapped.isEmpty()) {
				detail.put("notMapped", new ArrayList<String>(unmapped));
			}
			String threshold = "within ±" + percent(tolerance) + " of the EMR";
			DqResult result;
			if (emr.isEmpty()) {
				result = result(context, indicator.getCode(), DqOutcome.NOT_APPLICABLE, indicator.getName() + ": none of its "
						+ report + " cells has a KHIS mapping, so it cannot be compared with KHIS.");
			} else {
				double emrTotal = 0;
				double khisTotal = 0;
				for (Map.Entry<String, Double> entry : emr.entrySet()) {
					emrTotal += entry.getValue();
					// A data element KHIS has no value for: the EMR sent nothing, or KHIS lost it
					Double held = khis.get(entry.getKey());
					khisTotal += held != null ? held : 0;
				}
				double variance = emrTotal == 0 ? (khisTotal == 0 ? 0 : 1) : (khisTotal - emrTotal) / emrTotal;
				boolean outside = Math.abs(variance) > tolerance + EPSILON;
				result = result(context, indicator.getCode(), outside ? severity : DqOutcome.PASS, indicator.getName()
						+ " in " + report + ": EMR " + format(emrTotal) + ", KHIS " + format(khisTotal) + ", a variance of "
						+ percent(Math.abs(variance)) + (outside ? "; allowed ±" + percent(tolerance) + "." : "."));
				result.setNumerator(emrTotal);
				result.setDenominator(khisTotal);
				result.setMeasureValue(variance);
			}
			result.setThreshold(threshold);
			result.setDetail(json(detail));
			results.add(result);
		}
		return results;
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
