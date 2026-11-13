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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rule type EXPECTED_RANGE (malaria T22-03): consistency over time for the indicators in the
 * "indicators" setting, or for every indicator marked tracked when the setting is absent. Malaria is seasonal, so the two
 * baselines are separate results, labelled in the detail:
 * <ul>
 * <li>ROLLING_12_MONTHS: within ±band of the average of the previous 12 months (needs at least
 * minHistoryMonths of them)</li>
 * <li>SAME_MONTH_LAST_YEAR: ratio to the same month last year between ratioMin and ratioMax</li>
 * </ul>
 * The "baseline" setting names the one the rule judges, or BOTH (the default). A month outside it
 * gets the rule's severity. The other baseline is still worked out for the trend charts: it never
 * flags and is not saved with the report.
 * <p>
 * Messages leave out the indicator's name: every screen that shows one shows the indicator beside it.
 */
public class ConsistencyOverTimeRule extends AbstractDqRule {

	public static final String ID = "T22-03";

	public static final String BASELINE_ROLLING = "ROLLING_12_MONTHS";

	public static final String BASELINE_LAST_YEAR = "SAME_MONTH_LAST_YEAR";

	public static final String BASELINE_BOTH = "BOTH";

	public ConsistencyOverTimeRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return !trackedIndicators(context).isEmpty();
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		List<DqResult> results = new ArrayList<DqResult>();
		for (DqConfig.Indicator indicator : trackedIndicators(context)) {
			List<DqResult> baselines = evaluateIndicator(context, indicator);
			if (baselines.size() == 1) {
				// The indicator could not be calculated
				results.addAll(baselines);
				continue;
			}
			// The baseline that is not judged is only for the trend charts
			if (isJudged(settings(indicator), BASELINE_ROLLING)) {
				results.add(baselines.get(0));
			}
			if (isJudged(settings(indicator), BASELINE_LAST_YEAR)) {
				results.add(baselines.get(1));
			}
		}
		return results;
	}

	/**
	 * Checks one indicator for the context's month, whether or not it is tracked. The trend charts
	 * use this to draw the same expected range the rule checks against.
	 *
	 * @return a result for each baseline, rolling then last year, with the expected range in the detail as expectedMin and
	 * expectedMax; or a single NOT_APPLICABLE result when the indicator cannot be calculated
	 */
	public List<DqResult> evaluateIndicator(DqRuleContext context, DqConfig.Indicator indicator) {
		DqRuleSettings settings = settings(indicator);
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.WARN);
		DqIndicatorCalculator.IndicatorValue current = context.getIndicator(indicator.getCode(), context.getPeriodStart());
		if (!current.isComplete()) {
			return Collections.singletonList(result(context, indicator.getCode(), DqOutcome.NOT_APPLICABLE,
					indicator.getName() + " could not be calculated: " + current.getMissingColumns().size()
							+ " of its report columns are missing. Generate all of its reports for the period, or check "
							+ "the indicator definition."));
		}
		return Arrays.asList(
				rolling(context, indicator, settings, isJudged(settings, BASELINE_ROLLING) ? severity : null,
						current.getValue()),
				lastYear(context, indicator, settings, isJudged(settings, BASELINE_LAST_YEAR) ? severity : null,
						current.getValue()));
	}

	/**
	 * @return true when the rule judges a month against this baseline, per the "baseline" setting
	 */
	static boolean isJudged(DqRuleSettings settings, String baseline) {
		String judged = settings.getString("baseline", BASELINE_BOTH);
		return BASELINE_BOTH.equals(judged) || judged.equals(baseline);
	}

	/**
	 * @return the outcome of a month outside the range: the rule's severity, or NOT_APPLICABLE for
	 * a baseline shown for information only
	 */
	private static DqOutcome outside(DqOutcome severity) {
		return severity != null ? severity : DqOutcome.NOT_APPLICABLE;
	}

	/**
	 * @return the outcome of a month within the range; a baseline shown for information is not
	 * checked, so it does not pass either
	 */
	private static DqOutcome within(DqOutcome severity) {
		return severity != null ? DqOutcome.PASS : DqOutcome.NOT_APPLICABLE;
	}

	/**
	 * @return the month as it is written in the messages, e.g. Oct 2025
	 */
	private static String month(Date periodStart) {
		return new SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(periodStart);
	}

	private DqResult rolling(DqRuleContext context, DqConfig.Indicator indicator, DqRuleSettings settings,
	                         DqOutcome severity, double current) {
		double band = settings.getDouble("band", 0.20);
		int minHistory = settings.getInteger("minHistoryMonths", 3);

		List<Double> history = new ArrayList<Double>();
		for (int i = 1; i <= 12; i++) {
			DqIndicatorCalculator.IndicatorValue value = context.getIndicator(indicator.getCode(),
					DqPeriods.addMonths(context.getPeriodStart(), -i));
			// A month with some of the indicator's reports missing would skew the average
			if (value.isComplete()) {
				history.add(value.getValue());
			}
		}

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("baseline", BASELINE_ROLLING);
		detail.put("monthsOfHistory", history.size());
		String threshold = "within ±" + percent(band) + " of the 12-month average";

		DqResult result;
		if (history.size() < minHistory) {
			result = result(context, indicator.getCode(), DqOutcome.NOT_APPLICABLE, "Only "
					+ history.size() + " earlier month(s) are stored; at least " + minHistory
					+ " are needed for the 12-month average. Generate earlier months to backfill.");
		} else {
			double sum = 0;
			for (Double value : history) {
				sum += value;
			}
			double average = sum / history.size();
			detail.put("average", average);
			detail.put("expectedMin", average * (1 - band));
			detail.put("expectedMax", average * (1 + band));
			if (average == 0) {
				boolean change = current != 0;
				result = result(context, indicator.getCode(), change ? outside(severity) : within(severity),
						format(current) + " against a 12-month average of 0.");
			} else {
				double deviation = (current - average) / average;
				boolean outside = Math.abs(deviation) > band + EPSILON;
				result = result(context, indicator.getCode(), outside ? outside(severity) : within(severity),
						format(current) + " is " + percent(Math.abs(deviation))
								+ (deviation >= 0 ? " above" : " below") + " the 12-month average of " + format(average)
								+ (outside ? "; allowed ±" + percent(band) + "." : "."));
				result.setMeasureValue(deviation);
			}
			result.setDenominator(average);
		}
		result.setNumerator(current);
		result.setThreshold(threshold);
		result.setDetail(json(detail));
		return result;
	}

	private DqResult lastYear(DqRuleContext context, DqConfig.Indicator indicator, DqRuleSettings settings,
	                          DqOutcome severity, double current) {
		double ratioMin = settings.getDouble("ratioMin", 0.8);
		double ratioMax = settings.getDouble("ratioMax", 1.2);
		DqIndicatorCalculator.IndicatorValue lastYear = context.getIndicator(indicator.getCode(),
				DqPeriods.addMonths(context.getPeriodStart(), -12));
		Double previous = lastYear.isComplete() ? lastYear.getValue() : null;
		String previousMonth = month(DqPeriods.addMonths(context.getPeriodStart(), -12));

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("baseline", BASELINE_LAST_YEAR);
		String threshold = "ratio " + format(ratioMin) + " to " + format(ratioMax) + " of the same month of the previous year";
		if (previous != null) {
			detail.put("previous", previous);
			detail.put("expectedMin", previous * ratioMin);
			detail.put("expectedMax", previous * ratioMax);
		}

		DqResult result;
		if (previous == null) {
			result = result(context, indicator.getCode(), DqOutcome.NOT_APPLICABLE, previousMonth
					+ ", the same month of the previous year, has not been generated.");
		} else if (previous == 0) {
			result = result(context, indicator.getCode(), current != 0 ? outside(severity) : within(severity),
					format(current) + " against 0 in " + previousMonth + ".");
		} else {
			double ratio = current / previous;
			boolean outside = ratio < ratioMin - EPSILON || ratio > ratioMax + EPSILON;
			double change = ratio - 1;
			String allowed = Math.abs((1 - ratioMin) - (ratioMax - 1)) < EPSILON ? "±" + percent(ratioMax - 1)
					: percent(ratioMin - 1) + " to +" + percent(ratioMax - 1);
			result = result(context, indicator.getCode(), outside ? outside(severity) : within(severity),
					format(current) + " against " + format(previous) + " in " + previousMonth + ", "
							+ (Math.abs(change) < EPSILON ? "the same" : percent(Math.abs(change)) + (change > 0 ? " more" : " less"))
							+ (outside ? "; allowed " + allowed + "." : "."));
			result.setMeasureValue(ratio);
		}
		result.setNumerator(current);
		result.setDenominator(previous);
		result.setThreshold(threshold);
		result.setDetail(json(detail));
		return result;
	}

	private List<DqConfig.Indicator> trackedIndicators(DqRuleContext context) {
		List<String> codes = settings().getStrings("indicators");
		List<DqConfig.Indicator> tracked = new ArrayList<DqConfig.Indicator>();
		for (DqConfig.Indicator indicator : context.getConfig().getIndicators()) {
			boolean checked = codes.isEmpty() ? indicator.isTracked() : codes.contains(indicator.getCode());
			if (checked && readsRun(context, indicator) && settings(indicator).isEnabled()) {
				tracked.add(indicator);
			}
		}
		return tracked;
	}
}
