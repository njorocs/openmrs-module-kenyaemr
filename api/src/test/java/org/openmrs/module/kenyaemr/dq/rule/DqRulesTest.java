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
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the TR-26-110 rules, at their threshold edges, against the bundled default configuration
 */
public class DqRulesTest {

	private static final String MOH_743 = "MOH-743 Report";

	private static final String MOH_705A = "MOH 705A Outpatient summary";

	private static final String MOH_706 = "MOH 706";

	private static final String TREATED = "MAL_TREATED_ACT_743";

	private static final String CONFIRMED = "MAL_CONFIRMED_743";

	private final Date march = TestUtils.date(2026, 3, 1);

	// T22-04: treated ÷ confirmed must not exceed 1

	@Test
	public void t2204_shouldPassAtExactlyOne() {
		DqResult result = single(t2204(), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 120.0).indicator(CONFIRMED, march, 120.0));

		Assert.assertEquals(DqOutcome.PASS, result.getOutcome());
		Assert.assertEquals(1.0, result.getMeasureValue(), 0);
		Assert.assertEquals(TREATED + "/" + CONFIRMED, result.getIndicatorCode());
	}

	@Test
	public void t2204_shouldBlockAboveOne() {
		DqResult result = single(t2204(), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 121.0).indicator(CONFIRMED, march, 120.0));

		Assert.assertEquals(DqOutcome.BLOCK, result.getOutcome());
		Assert.assertEquals("≤ 1", result.getThreshold());
	}

	@Test
	public void t2204_shouldBlockTreatmentWithNoConfirmedCases() {
		Assert.assertEquals(DqOutcome.BLOCK, single(t2204(), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 3.0).indicator(CONFIRMED, march, 0.0)).getOutcome());
		Assert.assertEquals(DqOutcome.PASS, single(t2204(), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 0.0).indicator(CONFIRMED, march, 0.0)).getOutcome());
	}

	@Test
	public void t2204_shouldOnlyApplyToReportsItReads() {
		Assert.assertTrue(t2204().appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertFalse(t2204().appliesTo(FakeRuleContext.monthly(MOH_706, march)));
	}

	// X-RPT-01: tested (MOH 705A/B) ÷ tests (MOH 706) within 90-110%

	@Test
	public void xrpt01_shouldWarnOutsideTheBand() {
		Assert.assertEquals(DqOutcome.PASS, single(xrpt01(), FakeRuleContext.monthly(MOH_705A, march)
				.indicator("MAL_TESTED_705", march, 90.0).indicator("MAL_TESTS_706", march, 100.0)).getOutcome());
		Assert.assertEquals(DqOutcome.WARN, single(xrpt01(), FakeRuleContext.monthly(MOH_705A, march)
				.indicator("MAL_TESTED_705", march, 89.0).indicator("MAL_TESTS_706", march, 100.0)).getOutcome());
	}

	@Test
	public void xrpt01_shouldWaitForTheOtherReport() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).notGenerated(MOH_706, march)
				.indicator("MAL_TESTED_705", march, 89.0);

		DqResult result = single(xrpt01(), context);

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, result.getOutcome());
		Assert.assertTrue(result.getMessage(), result.getMessage().startsWith("MOH 706 has not been generated"));
	}

	@Test
	public void xrpt01_shouldApplyToEitherSide() {
		Assert.assertTrue(xrpt01().appliesTo(FakeRuleContext.monthly(MOH_705A, march)));
		Assert.assertTrue(xrpt01().appliesTo(FakeRuleContext.monthly(MOH_706, march)));
		Assert.assertFalse(xrpt01().appliesTo(FakeRuleContext.monthly(MOH_743, march)));
	}

	// T22-03: consistency over time

	@Test
	public void t2203_shouldPassAtExactlyTwentyPercent() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_743, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 120.0).indicator(TREATED, march, 100.0);

		DqResult rolling = baseline(new ConsistencyOverTimeRule().evaluate(context), CONFIRMED,
				ConsistencyOverTimeRule.BASELINE_ROLLING);

		Assert.assertEquals(DqOutcome.PASS, rolling.getOutcome());
		Assert.assertEquals(0.2, rolling.getMeasureValue(), 1e-12);
	}

	@Test
	public void t2203_shouldWarnAboveTwentyPercentAndAgainstLastYear() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_743, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 121.0).indicator(TREATED, march, 100.0);

		List<DqResult> results = new ConsistencyOverTimeRule().evaluate(context);

		Assert.assertEquals(DqOutcome.WARN, baseline(results, CONFIRMED, ConsistencyOverTimeRule.BASELINE_ROLLING).getOutcome());
		DqResult lastYear = baseline(results, CONFIRMED, ConsistencyOverTimeRule.BASELINE_LAST_YEAR);
		Assert.assertEquals(DqOutcome.WARN, lastYear.getOutcome());
		Assert.assertEquals(1.21, lastYear.getMeasureValue(), 1e-12);
		// Both tracked MOH 743 indicators are checked, each against both baselines
		Assert.assertEquals(4, results.size());
	}

	@Test
	public void t2203_shouldNeedThreeMonthsOfHistoryAndIgnoreIncompleteMonths() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_743, march), CONFIRMED, 100.0, 2)
				.incompleteIndicator(CONFIRMED, DqPeriods.addMonths(march, -3), 100.0)
				.indicator(CONFIRMED, march, 500.0).indicator(TREATED, march, 100.0);

		DqResult rolling = baseline(new ConsistencyOverTimeRule().evaluate(context), CONFIRMED,
				ConsistencyOverTimeRule.BASELINE_ROLLING);

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, rolling.getOutcome());
		Assert.assertTrue(rolling.getMessage(), rolling.getMessage().contains("only 2 earlier month(s)"));
	}

	@Test
	public void t2203_shouldUseAnIndicatorsOwnBand() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_743, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 130.0).indicator(TREATED, march, 100.0);
		Map<String, Object> own = new HashMap<String, Object>();
		own.put("band", 0.35);
		context.getConfig().getIndicator(CONFIRMED).getRules().put(ConsistencyOverTimeRule.ID, own);

		Assert.assertEquals(DqOutcome.PASS, baseline(new ConsistencyOverTimeRule().evaluate(context), CONFIRMED,
				ConsistencyOverTimeRule.BASELINE_ROLLING).getOutcome());
	}

	// T22-05: disabled until M&E defines the ACT product pairs

	@Test
	public void t2205_shouldBeOffByDefault() {
		Assert.assertFalse(new CommodityReconciliationRule().appliesTo(FakeRuleContext.monthly(MOH_743, march)));
	}

	// T22-07: repeat positive within 14 days

	@Test
	public void t2207_shouldWarnWhenRepeatPositivesAreFound() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march).queryReturns(new Object[] { 2L });

		DqResult result = single(new DuplicateEpisodeRule(), context);

		Assert.assertEquals(DqOutcome.WARN, result.getOutcome());
		Assert.assertEquals(2.0, result.getMeasureValue(), 0);
		Assert.assertEquals(14, context.queryParameters.get(0).get("windowDays"));
		Assert.assertEquals(march, context.queryParameters.get(0).get("startDate"));
	}

	@Test
	public void t2207_shouldPassWithNoPairsAndOnlyApplyToMoh743() {
		Assert.assertEquals(DqOutcome.PASS, single(new DuplicateEpisodeRule(),
				FakeRuleContext.monthly(MOH_743, march).queryReturns(new Object[] { 0L })).getOutcome());
		Assert.assertFalse(new DuplicateEpisodeRule().appliesTo(FakeRuleContext.monthly(MOH_706, march)));
	}

	// T22-08: weekly positivity change

	@Test
	public void t2208_shouldTicketAChangeAboveFiftyPercent() {
		Date monday = TestUtils.date(2026, 10, 12);
		// This week 16 of 40 positive (40%), last week 10 of 40 (25%): a 60% rise
		FakeRuleContext context = FakeRuleContext.weekly(monday)
				.queryReturns(new Object[] { 40L, 16L })
				.queryReturns(new Object[] { 40L, 10L });

		DqResult result = single(new PositivityChangeRule(), context);

		Assert.assertEquals(DqOutcome.TICKET, result.getOutcome());
		Assert.assertEquals(0.6, result.getMeasureValue(), 1e-12);
		Assert.assertEquals(TestUtils.date(2026, 10, 5), context.queryParameters.get(1).get("startDate"));
	}

	@Test
	public void t2208_shouldPassAtFiftyPercentAndSkipSmallWeeks() {
		Date monday = TestUtils.date(2026, 10, 12);
		Assert.assertEquals(DqOutcome.PASS, single(new PositivityChangeRule(), FakeRuleContext.weekly(monday)
				.queryReturns(new Object[] { 40L, 15L })
				.queryReturns(new Object[] { 40L, 10L })).getOutcome());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, single(new PositivityChangeRule(), FakeRuleContext.weekly(monday)
				.queryReturns(new Object[] { 19L, 15L })
				.queryReturns(new Object[] { 40L, 10L })).getOutcome());
	}

	@Test
	public void t2208_shouldOnlyRunWeekly() {
		Assert.assertFalse(new PositivityChangeRule().appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertTrue(new PositivityChangeRule().appliesTo(FakeRuleContext.weekly(TestUtils.date(2026, 10, 12))));
	}

	// Engine

	@Test
	public void engine_shouldBlockTheRunAndTicketOnlyConfiguredRules() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_743, march), CONFIRMED, 100.0, 12)
				.indicator(TREATED, march, 130.0).indicator(CONFIRMED, march, 100.0)
				.queryReturns(new Object[] { 0L });
		withHistory(context, TREATED, 130.0, 12);

		List<DqResult> results = DqRuleEngine.evaluate(context, DqRuleEngine.monthlyRules());

		Assert.assertEquals(DqRunStatus.BLOCKED, DqRuleEngine.statusFor(results));
		Assert.assertEquals("T22-04", results.get(0).getRuleId());
		// T22-04 is a block, not a ticket; nothing else failed
		Assert.assertTrue(DqRuleEngine.ticketsFor(results, context.getConfig()).isEmpty());
	}

	@Test
	public void engine_shouldTicketCrossReportWarnings() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_706, march)
				.indicator("MAL_TESTED_705", march, 80.0).indicator("MAL_TESTS_706", march, 100.0);

		List<DqResult> results = DqRuleEngine.evaluate(context, Collections.<DqRule>singletonList(xrpt01()));
		List<DqTicket> tickets = DqRuleEngine.ticketsFor(results, context.getConfig());

		Assert.assertEquals(DqRunStatus.AWAITING_APPROVAL, DqRuleEngine.statusFor(results));
		Assert.assertEquals(1, tickets.size());
		Assert.assertEquals(-20.0, tickets.get(0).getVariancePct(), 1e-9);
		Assert.assertEquals(DqTicket.ROLE_HRIO, tickets.get(0).getAssigneeRole());
	}

	@Test
	public void engine_shouldTurnARuleErrorIntoANotApplicableResult() {
		DqRule broken = new AbstractDqRule("T22-99") {

			@Override
			public boolean appliesTo(DqRuleContext context) {
				return true;
			}

			@Override
			public List<DqResult> evaluate(DqRuleContext context) {
				throw new IllegalStateException("ETL table missing");
			}
		};

		List<DqResult> results = DqRuleEngine.evaluate(FakeRuleContext.monthly(MOH_743, march),
				Arrays.asList(broken, xrpt01()));

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, results.get(0).getOutcome());
		Assert.assertTrue(results.get(0).getMessage().contains("ETL table missing"));
	}

	private static IndicatorRatioRule t2204() {
		return new IndicatorRatioRule(DqRuleEngine.T22_04, DqOutcome.BLOCK);
	}

	private static IndicatorRatioRule xrpt01() {
		return new IndicatorRatioRule(DqRuleEngine.X_RPT_01, DqOutcome.WARN);
	}

	private FakeRuleContext withHistory(FakeRuleContext context, String code, Double value, int months) {
		for (int i = 1; i <= months; i++) {
			context.indicator(code, DqPeriods.addMonths(march, -i), value);
		}
		return context;
	}

	private static DqResult single(DqRule rule, DqRuleContext context) {
		List<DqResult> results = rule.evaluate(context);
		Assert.assertEquals(1, results.size());
		return results.get(0);
	}

	private static DqResult baseline(List<DqResult> results, String indicator, String baseline) {
		for (DqResult result : results) {
			if (indicator.equals(result.getIndicatorCode()) && result.getDetail().contains("\"" + baseline + "\"")) {
				return result;
			}
		}
		throw new AssertionError("No " + baseline + " result for " + indicator);
	}
}
