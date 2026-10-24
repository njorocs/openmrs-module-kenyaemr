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
 * the week before. A relative change above changeThreshold opens a ticket, by default for the
 * SCHRIO. Weeks whose denominator is below minDenominator are not compared, so small numbers do
 * not raise alarms.
 * <p>
 * For malaria the rate is test positivity: tested positive (CMC in MOH 705A, COA in MOH 705B) ÷
 * tested (TMC in both), counted for each Monday-to-Sunday week with the reports' own queries.
 * <p>
 * Settings: numerator, denominator (indicator codes), changeThreshold (default 0.5), minDenominator
 * (default 20; minTests is read too), severity (default TICKET).
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

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		String numeratorCode = settings.getString("numerator", null);
		String denominatorCode = settings.getString("denominator", null);
		double changeThreshold = settings.getDouble("changeThreshold", 0.5);
		int minDenominator = settings.getInteger("minDenominator", settings.getInteger("minTests", 20));
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
		String threshold = "change ≤ " + percent(changeThreshold) + ", with at least " + minDenominator + " a week";

		DqResult result;
		if (tested < minDenominator || previousTested < minDenominator) {
			result = result(context, code, DqOutcome.NOT_APPLICABLE, "Too few to compare: " + format(tested) + " "
					+ denominator.getName() + " this week and " + format(previousTested) + " the week before; at least "
					+ minDenominator + " are needed in each.");
		} else {
			double rate = current / tested;
			double previousRate = previous / previousTested;
			detail.put("positivity", rate);
			detail.put("previousPositivity", previousRate);
			String rates = numerator.getName() + " ÷ " + denominator.getName() + " was " + percent(rate) + " ("
					+ format(current) + " of " + format(tested) + ") against " + percent(previousRate) + " ("
					+ format(previous) + " of " + format(previousTested) + ") the week before";
			DqOutcome severity = settings.getOutcome("severity", DqOutcome.TICKET);
			if (previousRate == 0) {
				result = result(context, code, rate > 0 ? severity : DqOutcome.PASS, rates + ".");
			} else {
				double change = (rate - previousRate) / previousRate;
				boolean flagged = Math.abs(change) > changeThreshold + EPSILON;
				result = result(context, code, flagged ? severity : DqOutcome.PASS, rates + ", a change of "
						+ percent(Math.abs(change)) + (flagged ? "; above the " + percent(changeThreshold) + " threshold." : "."));
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
