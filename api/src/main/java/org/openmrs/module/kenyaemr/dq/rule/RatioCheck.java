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
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that the ratio of two indicators for the period lies within bounds
 */
class RatioCheck {

	private final AbstractDqRule rule;

	RatioCheck(AbstractDqRule rule) {
		this.rule = rule;
	}

	/**
	 * @param min lowest acceptable ratio, or null for none
	 * @param max highest acceptable ratio, or null for none
	 * @param severity outcome when the ratio is out of bounds
	 */
	DqResult check(DqRuleContext context, String numeratorCode, String denominatorCode, Double min, Double max,
	               DqOutcome severity) {
		String code = numeratorCode + "/" + denominatorCode;
		DqConfig.Indicator numerator = context.getConfig().getIndicator(numeratorCode);
		DqConfig.Indicator denominator = context.getConfig().getIndicator(denominatorCode);
		if (numerator == null || denominator == null) {
			return rule.result(context, code, DqOutcome.NOT_APPLICABLE,
					"Indicator " + (numerator == null ? numeratorCode : denominatorCode) + " is not configured.");
		}

		List<String> notGenerated = new ArrayList<String>();
		for (DqConfig.Indicator indicator : new DqConfig.Indicator[] { numerator, denominator }) {
			for (String report : indicator.getReports()) {
				if (!notGenerated.contains(report) && !context.isGenerated(report, context.getPeriodStart())) {
					notGenerated.add(report);
				}
			}
		}
		if (!notGenerated.isEmpty()) {
			return rule.result(context, code, DqOutcome.NOT_APPLICABLE, join(notGenerated) + " has not been generated for "
					+ new SimpleDateFormat("MMMM yyyy").format(context.getPeriodStart()) + ", so this check cannot run yet.");
		}

		DqIndicatorCalculator.IndicatorValue num = context.getIndicator(numeratorCode, context.getPeriodStart());
		DqIndicatorCalculator.IndicatorValue den = context.getIndicator(denominatorCode, context.getPeriodStart());

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("numerator", numeratorCode);
		detail.put("denominator", denominatorCode);
		List<String> missing = new ArrayList<String>(num.getMissingColumns());
		missing.addAll(den.getMissingColumns());
		if (!missing.isEmpty()) {
			detail.put("missingColumns", missing);
		}

		if (num.getValue() == null || den.getValue() == null) {
			DqResult result = rule.result(context, code, DqOutcome.NOT_APPLICABLE,
					"The report columns for " + (num.getValue() == null ? numerator.getName() : denominator.getName())
							+ " were not found. Check the indicator definition.");
			result.setDetail(AbstractDqRule.json(detail));
			return result;
		}

		String threshold = threshold(min, max);
		String label = numerator.getName() + " ÷ " + denominator.getName();
		DqResult result;
		if (den.getValue() == 0) {
			if (num.getValue() == 0) {
				result = rule.result(context, code, DqOutcome.PASS, "No cases this period: " + label + " is 0 ÷ 0.");
			} else {
				result = rule.result(context, code, severity, numerator.getName() + " is "
						+ AbstractDqRule.format(num.getValue()) + " but " + denominator.getName() + " is 0.");
			}
		} else {
			double ratio = num.getValue() / den.getValue();
			boolean outside = (min != null && ratio < min - AbstractDqRule.EPSILON)
					|| (max != null && ratio > max + AbstractDqRule.EPSILON);
			result = rule.result(context, code, outside ? severity : DqOutcome.PASS, label + " = "
					+ AbstractDqRule.format(num.getValue()) + " ÷ " + AbstractDqRule.format(den.getValue()) + " = "
					+ AbstractDqRule.format(ratio) + (outside ? "; it must be " : "; within ") + threshold + ".");
			result.setMeasureValue(ratio);
		}
		result.setNumerator(num.getValue());
		result.setDenominator(den.getValue());
		result.setThreshold(threshold);
		result.setDetail(AbstractDqRule.json(detail));
		return result;
	}

	private static String threshold(Double min, Double max) {
		if (min != null && max != null) {
			return AbstractDqRule.format(min) + " to " + AbstractDqRule.format(max);
		}
		if (max != null) {
			return "≤ " + AbstractDqRule.format(max);
		}
		return min != null ? "≥ " + AbstractDqRule.format(min) : "any";
	}

	private static String join(List<String> items) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				text.append(i == items.size() - 1 ? " and " : ", ");
			}
			text.append(items.get(i));
		}
		return text.toString();
	}
}
