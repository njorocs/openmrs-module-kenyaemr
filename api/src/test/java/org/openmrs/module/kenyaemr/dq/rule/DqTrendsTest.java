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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqTrends;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the trend chart series
 */
public class DqTrendsTest {

	private static final String MOH_743 = "MOH-743 Report";

	private static final String TREATED = "MAL_TREATED_ACT_743";

	private static final String CONFIRMED = "MAL_CONFIRMED_743";

	private static final String CONFIRMED_705 = "MAL_CONFIRMED_705";

	private static final String TESTED_705 = "MAL_TESTED_705";

	private final Date march = TestUtils.date(2026, 3, 1);

	private final Date february = DqPeriods.addMonths(march, -1);

	@Test
	public void indicatorTrend_shouldGiveTheExpectedRangeTheRuleChecks() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march).indicator(CONFIRMED, march, 130.0);
		for (int i = 1; i <= 12; i++) {
			context.indicator(CONFIRMED, DqPeriods.addMonths(march, -i), 100.0);
		}

		List<DqTrends.IndicatorPoint> points = DqTrends.indicatorTrend(context,
				context.getConfig().getIndicator(CONFIRMED), february, march);

		Assert.assertEquals(2, points.size());
		DqTrends.IndicatorPoint point = points.get(1);
		Assert.assertEquals(march, point.getMonth());
		Assert.assertEquals(130.0, point.getValue(), 0);
		Assert.assertEquals(DqOutcome.WARN, point.getRolling().getOutcome());
		Assert.assertEquals(100.0, point.getRolling().getReference(), 1e-9);
		Assert.assertEquals(80.0, point.getRolling().getExpectedMin(), 1e-9);
		Assert.assertEquals(120.0, point.getRolling().getExpectedMax(), 1e-9);
		Assert.assertEquals(Integer.valueOf(12), point.getRolling().getMonthsOfHistory());
		Assert.assertEquals(DqOutcome.WARN, point.getLastYear().getOutcome());
		Assert.assertEquals(100.0, point.getLastYear().getReference(), 1e-9);
		Assert.assertEquals(120.0, point.getLastYear().getExpectedMax(), 1e-9);

		// February has 11 earlier months and no value for the year before
		DqTrends.IndicatorPoint previous = points.get(0);
		Assert.assertEquals(DqOutcome.PASS, previous.getRolling().getOutcome());
		Assert.assertEquals(Integer.valueOf(11), previous.getRolling().getMonthsOfHistory());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, previous.getLastYear().getOutcome());
		Assert.assertNull(previous.getLastYear().getExpectedMin());
	}

	@Test
	public void indicatorTrend_shouldLeaveMonthsWithoutValuesUnchecked() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march);

		DqTrends.IndicatorPoint point = DqTrends.indicatorTrend(context, context.getConfig().getIndicator(CONFIRMED),
				march, march).get(0);

		Assert.assertNull(point.getValue());
		Assert.assertFalse(point.isComplete());
		Assert.assertNull(point.getRolling());
		Assert.assertNull(point.getLastYear());
	}

	@Test
	public void ratioTrend_shouldCheckEachMonth() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march)
				.indicator("MAL_TREATED_743", march, 130.0).indicator("MAL_CONFIRMED_705", march, 120.0);

		List<DqTrends.RatioPoint> points = DqTrends.ratioTrend(context, DqTrends.ratioRule(context.getConfig(), DqRuleEngine.T22_04),
				february, march);

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, points.get(0).getOutcome());
		Assert.assertNull(points.get(0).getRatio());
		Assert.assertEquals(DqOutcome.BLOCK, points.get(1).getOutcome());
		Assert.assertEquals(130.0 / 120.0, points.get(1).getRatio(), 1e-9);
		Assert.assertEquals(130.0, points.get(1).getNumerator(), 0);
		Assert.assertEquals(120.0, points.get(1).getDenominator(), 0);
	}

	@Test
	public void ratioTrend_shouldGiveEachPairOfARule() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march).indicator("MAL_SUSPECTED_705", march, 100.0)
				.indicator("MAL_TESTS_706", march, 100.0).indicator("MAL_TREATED_743", march, 50.0)
				.indicator("MAL_POSITIVE_706", march, 100.0);
		// A second pair, as a facility could add
		Map<String, Object> pair = new HashMap<String, Object>();
		pair.put("numerator", "MAL_TREATED_743");
		pair.put("denominator", "MAL_POSITIVE_706");
		@SuppressWarnings("unchecked")
		List<Object> pairs = (List<Object>) context.getConfig().getRuleDefinition(DqRuleEngine.X_RPT_01).getValues().get("pairs");
		pairs.add(pair);
		IndicatorRatioRule rule = DqTrends.ratioRule(context.getConfig(), DqRuleEngine.X_RPT_01);

		Assert.assertEquals(2, rule.pairs().size());
		Assert.assertEquals(DqOutcome.PASS, DqTrends.ratioTrend(context, rule, 0, march, march).get(0).getOutcome());
		Assert.assertEquals(DqOutcome.WARN, DqTrends.ratioTrend(context, rule, 1, march, march).get(0).getOutcome());
	}

	@Test
	public void weeklyTrend_shouldGiveEachWeeksSavedResultAndLeaveUncheckedWeeksEmpty() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		Date twoBefore = TestUtils.date(2026, 9, 28);
		DqRule rule = DqRuleTypes.rule(DqConfigStore.getDefaultConfig(), "T22-08");
		// 16 of 40 positive against 10 of 40 the week before: a 60% rise
		DqResult result = rule.evaluate(FakeRuleContext.weekly(monday).indicator(CONFIRMED_705, monday, 16.0)
				.indicator(TESTED_705, monday, 40.0).indicator(CONFIRMED_705, previous, 10.0)
				.indicator(TESTED_705, previous, 40.0)).get(0);
		DqResult otherProgram = rule.evaluate(FakeRuleContext.weekly(previous)).get(0);
		otherProgram.setProgramCode("HIV");

		List<DqTrends.WeekPoint> points = DqTrends.weeklyTrend(Arrays.asList(result, otherProgram), "MALARIA",
				TestUtils.date(2026, 10, 1), TestUtils.date(2026, 10, 18));

		Assert.assertEquals(3, points.size());
		Assert.assertEquals(twoBefore, points.get(0).getWeek());
		Assert.assertNull(points.get(0).getOutcome());
		Assert.assertNull(points.get(1).getOutcome());
		DqTrends.WeekPoint week = points.get(2);
		Assert.assertEquals(monday, week.getWeek());
		Assert.assertEquals(DqOutcome.TICKET, week.getOutcome());
		Assert.assertEquals(0.4, week.getRate(), 1e-12);
		Assert.assertEquals(10.0, week.getPreviousNumerator(), 0);
		Assert.assertEquals(40.0, week.getPreviousDenominator(), 0);
		Assert.assertEquals(0.6, week.getChange(), 1e-12);
	}

	@Test
	public void ratioRule_shouldOnlyFindRatioRules() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		Assert.assertNotNull(DqTrends.ratioRule(config, DqRuleEngine.X_RPT_01));
		Assert.assertNull(DqTrends.ratioRule(config, ConsistencyOverTimeRule.ID));
		Assert.assertNull(DqTrends.ratioRule(config, "NOPE"));
	}

	@Test
	public void expectedRangeRule_shouldBeTheRuleThatListsTheIndicator() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		Assert.assertEquals("T22-03", DqTrends.expectedRangeRule(config, config.getIndicator("MAL_CONFIRMED_705")).getId());
		// An indicator no rule checks still gets a range drawn with the default settings
		Assert.assertEquals("T22-03", DqTrends.expectedRangeRule(config, config.getIndicator("MAL_TESTS_706")).getId());
	}
}
