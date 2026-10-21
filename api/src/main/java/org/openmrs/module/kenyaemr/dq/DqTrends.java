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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleEngine;
import org.openmrs.module.kenyaemr.dq.rule.IndicatorRatioRule;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Month-by-month series for the trend charts. Each month is checked with the rule itself, as of
 * the values stored now, so the expected range drawn is exactly the range the rule applies.
 */
public final class DqTrends {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private DqTrends() {
	}

	/**
	 * An indicator's value for each month with its T22-03 expected ranges
	 *
	 * @param context any context; it supplies the configuration and stored values
	 * @param indicator the indicator, tracked or not
	 * @param first the first month
	 * @param last the last month
	 */
	public static List<IndicatorPoint> indicatorTrend(DqRuleContext context, DqConfig.Indicator indicator, Date first,
	                                                  Date last) {
		ConsistencyOverTimeRule rule = new ConsistencyOverTimeRule();
		List<IndicatorPoint> points = new ArrayList<IndicatorPoint>();
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			DqRuleContext monthContext = context.forMonth(month);
			DqIndicatorCalculator.IndicatorValue value = monthContext.getIndicator(indicator.getCode(), month);
			IndicatorPoint point = new IndicatorPoint(month, value.getValue(), value.isComplete());
			for (DqResult result : rule.evaluateIndicator(monthContext, indicator)) {
				Map<String, Object> detail = detail(result);
				Object baseline = detail.get("baseline");
				if (ConsistencyOverTimeRule.BASELINE_ROLLING.equals(baseline)) {
					point.rolling = new Baseline(result, number(detail.get("average")),
							number(detail.get("monthsOfHistory")), detail);
				} else if (ConsistencyOverTimeRule.BASELINE_LAST_YEAR.equals(baseline)) {
					point.lastYear = new Baseline(result, number(detail.get("previous")), null, detail);
				}
			}
			points.add(point);
		}
		return points;
	}

	/**
	 * A ratio rule's numerator, denominator and ratio for each month
	 *
	 * @param context any context; it supplies the configuration and stored values
	 * @param rule a rule from {@link #ratioRule(String)}
	 */
	public static List<RatioPoint> ratioTrend(DqRuleContext context, IndicatorRatioRule rule, Date first, Date last) {
		List<RatioPoint> points = new ArrayList<RatioPoint>();
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			DqResult result = rule.evaluate(context.forMonth(month)).get(0);
			points.add(new RatioPoint(month, result));
		}
		return points;
	}

	/**
	 * @return the monthly rule that bounds a ratio of two indicators (T22-04, X-RPT-01), or null
	 */
	public static IndicatorRatioRule ratioRule(String ruleId) {
		for (DqRule rule : DqRuleEngine.monthlyRules()) {
			if (rule instanceof IndicatorRatioRule && rule.getId().equals(ruleId)) {
				return (IndicatorRatioRule) rule;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> detail(DqResult result) {
		if (result.getDetail() == null) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(result.getDetail(), Map.class);
		}
		catch (IOException e) {
			return Collections.emptyMap();
		}
	}

	private static Double number(Object value) {
		return value instanceof Number ? Double.valueOf(((Number) value).doubleValue()) : null;
	}

	/**
	 * One month of an indicator trend
	 */
	public static class IndicatorPoint {

		private final Date month;

		private final Double value;

		private final boolean complete;

		private Baseline rolling;

		private Baseline lastYear;

		IndicatorPoint(Date month, Double value, boolean complete) {
			this.month = month;
			this.value = value;
			this.complete = complete;
		}

		public Date getMonth() {
			return month;
		}

		/**
		 * @return the indicator, or null when none of its reports was generated for the month
		 */
		public Double getValue() {
			return value;
		}

		/**
		 * @return false when some of the indicator's report columns are missing, so the value is partial
		 */
		public boolean isComplete() {
			return complete;
		}

		/**
		 * @return the check against the previous 12 months, or null when the value is incomplete
		 */
		public Baseline getRolling() {
			return rolling;
		}

		/**
		 * @return the check against the same month last year, or null when the value is incomplete
		 */
		public Baseline getLastYear() {
			return lastYear;
		}
	}

	/**
	 * One T22-03 baseline for one month
	 */
	public static class Baseline {

		private final DqOutcome outcome;

		private final Double reference;

		private final Integer monthsOfHistory;

		private final Double expectedMin;

		private final Double expectedMax;

		private final String message;

		Baseline(DqResult result, Double reference, Double monthsOfHistory, Map<String, Object> detail) {
			this.outcome = result.getOutcome();
			this.reference = reference;
			this.monthsOfHistory = monthsOfHistory != null ? Integer.valueOf(monthsOfHistory.intValue()) : null;
			this.expectedMin = number(detail.get("expectedMin"));
			this.expectedMax = number(detail.get("expectedMax"));
			this.message = result.getMessage();
		}

		public DqOutcome getOutcome() {
			return outcome;
		}

		/**
		 * @return the 12-month average or last year's value, or null when there is not enough history
		 */
		public Double getReference() {
			return reference;
		}

		/**
		 * @return for the rolling baseline, how many of the previous 12 months had a complete value
		 */
		public Integer getMonthsOfHistory() {
			return monthsOfHistory;
		}

		public Double getExpectedMin() {
			return expectedMin;
		}

		public Double getExpectedMax() {
			return expectedMax;
		}

		public String getMessage() {
			return message;
		}
	}

	/**
	 * One month of a ratio rule
	 */
	public static class RatioPoint {

		private final Date month;

		private final Double numerator;

		private final Double denominator;

		private final Double ratio;

		private final DqOutcome outcome;

		private final String message;

		RatioPoint(Date month, DqResult result) {
			this.month = month;
			this.numerator = result.getNumerator();
			this.denominator = result.getDenominator();
			this.ratio = result.getMeasureValue();
			this.outcome = result.getOutcome();
			this.message = result.getMessage();
		}

		public Date getMonth() {
			return month;
		}

		public Double getNumerator() {
			return numerator;
		}

		public Double getDenominator() {
			return denominator;
		}

		/**
		 * @return the ratio, or null when it could not be calculated or the denominator is 0
		 */
		public Double getRatio() {
			return ratio;
		}

		public DqOutcome getOutcome() {
			return outcome;
		}

		public String getMessage() {
			return message;
		}
	}
}
