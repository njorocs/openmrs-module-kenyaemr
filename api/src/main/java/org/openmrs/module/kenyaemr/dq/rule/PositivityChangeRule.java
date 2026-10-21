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

import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T22-08: weekly malaria test positivity (positive ÷ tested) compared with the previous week. A
 * relative change above changeThreshold opens a ticket for the SCHRIO. Weeks with fewer than
 * minTests tests are not compared, so small numbers do not raise alarms.
 * <p>
 * This rule is weekly and not tied to a report run; the context period is the Monday-to-Sunday
 * week being checked.
 */
public class PositivityChangeRule extends AbstractDqRule {

	public static final String ID = "T22-08";

	public PositivityChangeRule() {
		super(ID);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null && settings(context, null).isEnabled();
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		double changeThreshold = settings.getDouble("changeThreshold", 0.5);
		int minTests = settings.getInteger("minTests", 20);

		Date previousStart = DqPeriods.addDays(context.getPeriodStart(), -7);
		long[] current = counts(context, settings, context.getPeriodStart(), context.getPeriodEnd());
		long[] previous = counts(context, settings, previousStart, DqPeriods.weekEnd(previousStart));

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("tested", current[0]);
		detail.put("positive", current[1]);
		detail.put("previousTested", previous[0]);
		detail.put("previousPositive", previous[1]);
		String threshold = "change ≤ " + percent(changeThreshold) + ", with at least " + minTests + " tests a week";

		DqResult result;
		if (current[0] < minTests || previous[0] < minTests) {
			result = result(context, null, DqOutcome.NOT_APPLICABLE, "Too few malaria tests to compare: " + current[0]
					+ " this week and " + previous[0] + " the week before; at least " + minTests + " are needed in each.");
		} else {
			double rate = (double) current[1] / current[0];
			double previousRate = (double) previous[1] / previous[0];
			detail.put("positivity", rate);
			detail.put("previousPositivity", previousRate);
			String rates = "Test positivity was " + percent(rate) + " (" + current[1] + " of " + current[0]
					+ ") against " + percent(previousRate) + " (" + previous[1] + " of " + previous[0] + ") the week before";
			DqOutcome severity = settings.getOutcome("severity", DqOutcome.TICKET);
			if (previousRate == 0) {
				result = result(context, null, rate > 0 ? severity : DqOutcome.PASS, rates + ".");
			} else {
				double change = (rate - previousRate) / previousRate;
				boolean flagged = Math.abs(change) > changeThreshold + EPSILON;
				result = result(context, null, flagged ? severity : DqOutcome.PASS, rates + ", a change of "
						+ percent(Math.abs(change)) + (flagged ? "; above the " + percent(changeThreshold) + " threshold." : "."));
				result.setMeasureValue(change);
			}
		}
		result.setNumerator((double) current[1]);
		result.setDenominator((double) current[0]);
		result.setThreshold(threshold);
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	/**
	 * @return {tests with a result, positive results} between the dates
	 */
	private static long[] counts(DqRuleContext context, DqRuleSettings settings, Date start, Date end) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("labTests", MalariaLabSettings.labTests(settings));
		parameters.put("positiveResults", MalariaLabSettings.positiveResults(settings));
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		List<Object[]> rows = context.query("select count(*), "
				+ "coalesce(sum(case when lower(x.result_name) in (:positiveResults) then 1 else 0 end), 0) "
				+ "from " + context.getEtlDatabase() + ".etl_laboratory_extract x "
				+ "where x.lab_test in (:labTests) and x.result_name is not null "
				+ "and x.date_test_requested between :startDate and :endDate", parameters);
		if (rows.isEmpty()) {
			return new long[] { 0, 0 };
		}
		Object[] row = rows.get(0);
		return new long[] { toLong(row[0]), toLong(row[1]) };
	}

	private static long toLong(Object value) {
		return value instanceof Number ? ((Number) value).longValue() : 0;
	}
}
