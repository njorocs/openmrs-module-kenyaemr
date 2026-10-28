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
import org.openmrs.module.kenyaemr.dq.DqIndicatorCalculator;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule type VERIFICATION (mRDQA II, verification factors): each indicator in "indicators" is
 * counted again from the EMR (the recount) and divided by the value in the report as generated
 * (VF monthly report) and by the value KHIS holds, read back (VF HMIS). In an EMR the recount
 * differs from the report mostly through records entered or changed after the report was made.
 * Checked once KHIS has been read back, after checkAfterDay (default 10).
 * <p>
 * One result per indicator: numerator the recount, denominator the reported value, measure the VF
 * monthly report, with the KHIS value and VF HMIS in the detail. A VF outside 1 ± margin (default
 * 0.1) is a warning. Settings: indicators, margin, checkAfterDay, severity (default WARN).
 */
public class VerificationRule extends AbstractDqRule {

	public static final String ID = "VF-01";

	static final int DEFAULT_CHECK_AFTER_DAY = 10;

	public VerificationRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.MONTH;
	}

	@Override
	public Date checkAfter(DqConfig config, Date month) {
		return DqPeriods.addDays(DqPeriods.deadline(month, settings().getInteger("checkAfterDay", DEFAULT_CHECK_AFTER_DAY)), 1);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null && !settings().getStrings("indicators").isEmpty();
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		double margin = settings.getDouble("margin", 0.1);
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.WARN);
		Date month = context.getPeriodStart();
		List<DqResult> results = new ArrayList<DqResult>();
		for (String code : settings.getStrings("indicators")) {
			DqConfig.Indicator indicator = context.getConfig().getIndicator(code);
			String name = indicator != null ? indicator.getName() : code;
			DqIndicatorCalculator.IndicatorValue reported = context.getIndicator(code, month);
			Map<String, Object> detail = new LinkedHashMap<String, Object>();
			DqResult result;
			if (reported.getValue() == null) {
				result = result(context, code, DqOutcome.NOT_APPLICABLE, name + ": the report was not generated for the month.");
			} else {
				DqIndicatorCalculator.IndicatorValue recount = context.recountIndicator(code, month);
				DqIndicatorCalculator.IndicatorValue khis = context.getKhisIndicator(code, month);
				Double vfReport = vf(recount.getValue(), reported.getValue());
				Double vfKhis = vf(recount.getValue(), khis.getValue());
				detail.put("reported", reported.getValue());
				detail.put("recount", recount.getValue());
				detail.put("khis", khis.getValue());
				detail.put("vfReport", vfReport);
				detail.put("vfKhis", vfKhis);
				if (!khis.isComplete()) {
					detail.put("khisMissing", khis.getMissingColumns());
				}
				if (recount.getValue() == null || !recount.isComplete()) {
					result = result(context, code, DqOutcome.NOT_APPLICABLE, name + ": could not be counted again: "
							+ recount.getMissingColumns());
				} else {
					// A recount above a value of 0 has no ratio, but is still a discrepancy
					boolean off = outside(vfReport, margin) || outside(vfKhis, margin) || vfReport == null
							|| (khis.getValue() != null && vfKhis == null);
					result = result(context, code, off ? severity : DqOutcome.PASS, name + ": recount " + format(recount.getValue())
							+ ", report " + format(reported.getValue()) + " (VF " + format(vfReport) + "), KHIS "
							+ (khis.getValue() != null ? format(khis.getValue()) + " (VF " + format(vfKhis) + ")" : "not read back")
							+ (off ? "; outside " + format(1 - margin) + " to " + format(1 + margin) + "." : "."));
					result.setMeasureValue(vfReport);
				}
				result.setNumerator(recount.getValue());
				result.setDenominator(reported.getValue());
			}
			result.setThreshold("VF " + format(1 - margin) + " to " + format(1 + margin));
			result.setDetail(json(detail));
			results.add(result);
		}
		return results;
	}

	/**
	 * @return recount ÷ value; 1 when both are 0, null when only the value is 0 or either is unknown
	 */
	static Double vf(Double recount, Double value) {
		if (recount == null || value == null) {
			return null;
		}
		if (value == 0) {
			return recount == 0 ? Double.valueOf(1) : null;
		}
		return recount / value;
	}

	private static boolean outside(Double vf, double margin) {
		return vf != null && (vf < 1 - margin - EPSILON || vf > 1 + margin + EPSILON);
	}
}
