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

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule type CHANGE (malaria T22-08): a rate (numerator ÷ denominator) for the week compared with
 * the week before. A rise or a fall above changeThreshold, relative to the week before (20% to 31%
 * positive is a 55% rise, 36% to 15% a 58% fall), opens a ticket, by default for the
 * SCHRIO. Every week with tests is compared with the week before, however few. A week with no
 * tests has no rate, so it is not compared; but a week after one with no tests (or with 0%
 * positive) is flagged when it has positives, as there is no rate to measure the rise against.
 * <p>
 * For malaria the rate is test positivity: malaria tests positive ÷ malaria tests performed, all
 * methods, from MOH 706 (MAL_POSITIVE_706 ÷ MAL_TESTS_706, the tested and confirmed steps of the
 * dashboard's cascade; X-RPT-01 reads the same tests). Both count laboratory tests, so the rate
 * cannot pass 100%: MOH 705's confirmed malaria counts diagnoses, with or without a test, and is
 * not a share of the patients it counts as tested. The weeks are Monday to Sunday, counted with
 * the report's own queries.
 * <p>
 * Settings: numerator, denominator (indicator codes), changeThreshold (default 0.5), severity
 * (default TICKET).
 */
public class PositivityChangeRule extends AbstractDqRule {

	public static final String ID = "T22-08";

	public PositivityChangeRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.WEEK;
	}

	/**
	 * The week's and the week before's values of the reports of its indicators, so a week already
	 * checked is checked again once a newer version of one of them is stored
	 */
	@Override
	public boolean readsReportValues() {
		return true;
	}

	/**
	 * Checked for each week by the daily task, and for a weekly version of a report (MOH 505) so
	 * that the week is held from KHIS until it is reviewed
	 */
	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null
				|| DqPeriods.isWholeWeek(context.getRun().getPeriodStart(), context.getRun().getPeriodEnd());
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		String numeratorCode = settings.getString("numerator", null);
		String denominatorCode = settings.getString("denominator", null);
		double changeThreshold = settings.getDouble("changeThreshold", 0.5);
		String code = numeratorCode + "/" + denominatorCode;
		DqConfig.Indicator numerator = context.getConfig().getIndicator(numeratorCode);
		DqConfig.Indicator denominator = context.getConfig().getIndicator(denominatorCode);
		if (numerator == null || denominator == null) {
			return Collections.singletonList(result(context, code, DqOutcome.NOT_APPLICABLE, "Indicator "
					+ (numerator == null ? numeratorCode : denominatorCode) + " is not configured."));
		}

		Date previousStart = DqPeriods.addDays(context.getPeriodStart(), -7);
		Date previousEnd = DqPeriods.weekEnd(previousStart);
		DqIndicatorCalculator.IndicatorValue num = context.getIndicator(numeratorCode, context.getPeriodStart(), context.getPeriodEnd());
		DqIndicatorCalculator.IndicatorValue den = context.getIndicator(denominatorCode, context.getPeriodStart(), context.getPeriodEnd());
		DqIndicatorCalculator.IndicatorValue previousNum = context.getIndicator(numeratorCode, previousStart, previousEnd);
		DqIndicatorCalculator.IndicatorValue previousDen = context.getIndicator(denominatorCode, previousStart, previousEnd);
		for (DqIndicatorCalculator.IndicatorValue value : new DqIndicatorCalculator.IndicatorValue[] { num, den, previousNum, previousDen }) {
			if (!value.isComplete()) {
				return Collections.singletonList(result(context, code, DqOutcome.NOT_APPLICABLE, "The weekly values of "
						+ value.getCode() + " could not be counted: " + value.getMissingColumns()));
			}
		}

		double current = num.getValue();
		double tested = den.getValue();
		double previous = previousNum.getValue();
		double previousTested = previousDen.getValue();
		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("numerator", numeratorCode);
		detail.put("denominator", denominatorCode);
		detail.put("tested", tested);
		detail.put("positive", current);
		detail.put("previousTested", previousTested);
		detail.put("previousPositive", previous);
		String threshold = "change ≤ " + percent(changeThreshold);

		DqOutcome severity = settings.getOutcome("severity", DqOutcome.TICKET);
		DqResult result;
		if (tested > 0 && previousTested == 0 && current > 0) {
			// Positives after a week with no tests: no rate the week before, so no change to measure
			double rate = current / tested;
			detail.put("positivity", rate);
			result = result(context, code, severity, numerator.getName() + " ÷ " + denominator.getName() + " was "
					+ percent(rate) + " (" + format(current) + " of " + format(tested) + ") this week, after a week with "
					+ "no tests; positives after a week with no tests are flagged.");
		} else if (tested == 0 || previousTested == 0) {
			result = result(context, code, DqOutcome.NOT_APPLICABLE, "Not compared: " + denominator.getName() + " was "
					+ format(tested) + " this week and " + format(previousTested) + " the week before. Positivity can "
					+ "only be compared when both weeks have tests.");
		} else {
			double rate = current / tested;
			double previousRate = previous / previousTested;
			detail.put("positivity", rate);
			detail.put("previousPositivity", previousRate);
			String rates = numerator.getName() + " ÷ " + denominator.getName() + " was " + percent(rate) + " ("
					+ format(current) + " of " + format(tested) + ") against " + percent(previousRate) + " ("
					+ format(previous) + " of " + format(previousTested) + ") the week before";
			if (previousRate == 0) {
				result = result(context, code, rate > 0 ? severity : DqOutcome.PASS, rates + ".");
			} else {
				double change = (rate - previousRate) / previousRate;
				// A rise or a fall
				boolean flagged = Math.abs(change) > changeThreshold + EPSILON;
				result = result(context, code, flagged ? severity : DqOutcome.PASS, rates + ", "
						+ (change < 0 ? "a fall of " : "a rise of ") + percent(Math.abs(change))
						+ (flagged ? "; above the " + percent(changeThreshold) + " threshold." : "."));
				result.setMeasureValue(change);
			}
		}
		result.setNumerator(current);
		result.setDenominator(tested);
		result.setThreshold(threshold);
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}
}
