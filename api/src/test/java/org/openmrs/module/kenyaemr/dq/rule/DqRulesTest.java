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
import org.openmrs.module.kenyaemr.reporting.MohReportUtils.DiagnosisLists;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.MOH705.MOH705CohortLibrary;
import org.junit.Test;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.KhisSubmissionState;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * The malaria program's rules, as configured in the bundled khis/dq_programs/malaria.default.json
 */
public class DqRulesTest {

	private static final String MOH_743 = "MOH-743 Report";

	private static final String MOH_705A = "MOH 705A Outpatient summary";

	private static final String MOH_705B = "MOH 705B Outpatient summary";

	private static final String MOH_706 = "MOH 706";

	private static final String MOH_505 = "MOH 505 IDSR Epidemic Case Monitoring";

	private static final String TREATED = "MAL_TREATED_743";

	private static final String CONFIRMED = "MAL_CONFIRMED_705";

	private static final String TESTED = "MAL_TESTED_705";

	private static final String SUSPECTED = "MAL_SUSPECTED_705";

	private static final String POSITIVE_706 = "MAL_POSITIVE_706";

	private static final String TESTS_706 = "MAL_TESTS_706";

	private final Date march = TestUtils.date(2026, 3, 1);

	// T22-04: patients treated (MOH 743) ÷ confirmed cases (MOH 705A/B) must not exceed 1

	@Test
	public void t2204_shouldPassAtExactlyOne() {
		DqResult result = single(rule("T22-04"), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 120.0).indicator(CONFIRMED, march, 120.0));

		Assert.assertEquals(DqOutcome.PASS, result.getOutcome());
		Assert.assertEquals(1.0, result.getMeasureValue(), 0);
		Assert.assertEquals(TREATED + "/" + CONFIRMED, result.getIndicatorCode());
		Assert.assertEquals("MALARIA", result.getProgramCode());
		Assert.assertTrue(result.getParams(), result.getParams().contains("\"max\":1.0"));
	}

	@Test
	public void t2204_shouldFailAboveOneAndTreatmentWithNoConfirmedCases() {
		Assert.assertEquals(DqOutcome.BLOCK, single(rule("T22-04"), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 121.0).indicator(CONFIRMED, march, 120.0)).getOutcome());
		Assert.assertEquals(DqOutcome.BLOCK, single(rule("T22-04"), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 3.0).indicator(CONFIRMED, march, 0.0)).getOutcome());
		Assert.assertEquals(DqOutcome.PASS, single(rule("T22-04"), FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 0.0).indicator(CONFIRMED, march, 0.0)).getOutcome());
	}

	@Test
	public void t2204_shouldApplyToMoh743AndMoh705Only() {
		Assert.assertTrue(rule("T22-04").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertTrue(rule("T22-04").appliesTo(FakeRuleContext.monthly(MOH_705A, march)));
		Assert.assertFalse(rule("T22-04").appliesTo(FakeRuleContext.monthly(MOH_706, march)));
	}

	// X-RPT-01: suspected (MOH 705A/B) against all malaria tests (MOH 706)

	@Test
	public void xrpt01_shouldCheckSuspectedAgainstTests() {
		List<DqResult> results = rule("X-RPT-01").evaluate(FakeRuleContext.monthly(MOH_705A, march)
				.indicator(SUSPECTED, march, 90.0).indicator("MAL_TESTS_706", march, 100.0));
		List<DqResult> low = rule("X-RPT-01").evaluate(FakeRuleContext.monthly(MOH_705A, march)
				.indicator(SUSPECTED, march, 70.0).indicator("MAL_TESTS_706", march, 100.0));

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(SUSPECTED + "/MAL_TESTS_706", results.get(0).getIndicatorCode());
		Assert.assertEquals(DqOutcome.PASS, results.get(0).getOutcome());
		Assert.assertEquals(DqOutcome.WARN, low.get(0).getOutcome());
	}

	@Test
	public void xrpt01_shouldWaitForTheOtherReport() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).notGenerated(MOH_706, march)
				.indicator(SUSPECTED, march, 89.0);

		DqResult result = rule("X-RPT-01").evaluate(context).get(0);

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, result.getOutcome());
		Assert.assertTrue(result.getMessage(), result.getMessage().startsWith("MOH 706 has not been generated"));
	}

	@Test
	public void xrpt01_shouldApplyToEachReportItCompares() {
		Assert.assertTrue(rule("X-RPT-01").appliesTo(FakeRuleContext.monthly(MOH_705A, march)));
		Assert.assertTrue(rule("X-RPT-01").appliesTo(FakeRuleContext.monthly(MOH_706, march)));
		// MOH 505 is not compared
		Assert.assertFalse(rule("X-RPT-01").appliesTo(FakeRuleContext.monthly(MOH_505, march)));
		Assert.assertFalse(rule("X-RPT-01").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
	}

	@Test
	public void recheck_shouldFindTheReportsEachValueReadingRuleReads() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		// Checked with each version: the re-checks take the rules whose indicators' reports changed
		Assert.assertTrue(rule("T22-03").getReports(config).containsAll(Arrays.asList(MOH_705A, MOH_705B)));
		Assert.assertTrue(rule("T22-04").getReports(config).contains(MOH_743));
		Assert.assertTrue(rule("X-RPT-01").getReports(config).contains(MOH_706));
		// Checked each week: checked again once a newer version of its reports is stored
		Assert.assertTrue(rule("T22-08").readsReportValues());
	}

	// T22-03: consistency over time of confirmed cases (MOH 705A/B)

	@Test
	public void t2203_shouldPassAtExactlyTwentyPercentOfTheSameMonthLastYear() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_705A, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 120.0);

		DqResult lastYear = single(rule("T22-03"), context);

		Assert.assertEquals(ConsistencyOverTimeRule.BASELINE_LAST_YEAR, baselineOf(lastYear));
		Assert.assertEquals(DqOutcome.PASS, lastYear.getOutcome());
		Assert.assertEquals(1.2, lastYear.getMeasureValue(), 1e-12);
	}

	@Test
	public void t2203_shouldTicketBeyondTwentyPercentOfTheSameMonthLastYearAndCheckOnlyItsIndicators() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_705A, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 79.0);

		List<DqResult> results = rule("T22-03").evaluate(context);

		// Only confirmed cases, and only against the same month last year
		Assert.assertEquals(1, results.size());
		DqResult lastYear = results.get(0);
		Assert.assertEquals(ConsistencyOverTimeRule.BASELINE_LAST_YEAR, baselineOf(lastYear));
		Assert.assertEquals(DqOutcome.TICKET, lastYear.getOutcome());
		Assert.assertEquals(0.79, lastYear.getMeasureValue(), 1e-12);
		Assert.assertEquals("79 against 100 in Mar 2025, 21% less; allowed ±20%.", lastYear.getMessage());
		Assert.assertFalse(rule("T22-03").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
	}

	@Test
	public void t2203_shouldShowTheTwelveMonthAverageWithoutFlagging() {
		// Far above the 12-month average, but within 20% of the same month last year
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_705A, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, DqPeriods.addMonths(march, -12), 150.0).indicator(CONFIRMED, march, 160.0);
		ConsistencyOverTimeRule rule = (ConsistencyOverTimeRule) rule("T22-03");

		Assert.assertEquals(DqOutcome.PASS, single(rule, context).getOutcome());
		DqResult rolling = baseline(rule.evaluateIndicator(context, context.getConfig().getIndicator(CONFIRMED)), CONFIRMED,
				ConsistencyOverTimeRule.BASELINE_ROLLING);
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, rolling.getOutcome());
		// The average takes in last year's 150: (11 × 100 + 150) ÷ 12
		Assert.assertEquals("160 is 53.6% above the 12-month average of 104.17; allowed ±20%.", rolling.getMessage());
	}

	@Test
	public void t2203_shouldNeedThreeMonthsOfHistoryAndIgnoreIncompleteMonths() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_705A, march), CONFIRMED, 100.0, 2)
				.incompleteIndicator(CONFIRMED, DqPeriods.addMonths(march, -3), 100.0)
				.indicator(CONFIRMED, march, 500.0);
		ConsistencyOverTimeRule rule = (ConsistencyOverTimeRule) rule("T22-03");

		DqResult rolling = baseline(rule.evaluateIndicator(context, context.getConfig().getIndicator(CONFIRMED)), CONFIRMED,
				ConsistencyOverTimeRule.BASELINE_ROLLING);

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, rolling.getOutcome());
		Assert.assertTrue(rolling.getMessage(), rolling.getMessage().startsWith("Only 2 earlier month(s)"));
	}

	@Test
	public void t2203_shouldJudgeBothBaselinesWithAnIndicatorsOwnBandWhenConfigured() {
		FakeRuleContext context = withHistory(FakeRuleContext.monthly(MOH_705A, march), CONFIRMED, 100.0, 12)
				.indicator(CONFIRMED, march, 130.0);
		Map<String, Object> own = new HashMap<String, Object>();
		own.put("baseline", ConsistencyOverTimeRule.BASELINE_BOTH);
		own.put("band", 0.35);
		context.getConfig().getIndicator(CONFIRMED).getRules().put(ConsistencyOverTimeRule.ID, own);

		List<DqResult> results = new ConsistencyOverTimeRule(context.getConfig().getRuleDefinition("T22-03")).evaluate(context);

		Assert.assertEquals(2, results.size());
		Assert.assertEquals(DqOutcome.PASS, baseline(results, CONFIRMED, ConsistencyOverTimeRule.BASELINE_ROLLING).getOutcome());
		Assert.assertEquals(DqOutcome.TICKET, baseline(results, CONFIRMED, ConsistencyOverTimeRule.BASELINE_LAST_YEAR).getOutcome());
	}

	// T22-07: confirmed malaria again within 14 days of an earlier episode, in MOH 705A/B

	@Test
	public void t2207_shouldWarnWhenRepeatPositivesAreFound() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).queryReturns(new Object[] { 2L });

		DqResult result = single(rule("T22-07"), context);

		Assert.assertEquals(DqOutcome.WARN, result.getOutcome());
		Assert.assertEquals(2.0, result.getMeasureValue(), 0);
		Assert.assertTrue(result.getMessage(), result.getMessage().contains("confirmed malaria again within 14 days of an earlier episode"));
		Map<String, Object> parameters = context.queryParameters.get(0);
		Assert.assertEquals(14, parameters.get("windowDays"));
		Assert.assertEquals(march, parameters.get("startDate"));
		Assert.assertEquals(Arrays.asList(152295, 2002634, 158379, 2002639, 160155, 145851), parameters.get("severeConcepts"));
		Assert.assertNotNull(parameters.get("windowStart"));
		String sql = context.queries.get(0);
		// The MOH 705A query: its under-5 confirmed malaria, including congenital falciparum malaria
		List<Integer> diagnoses = DuplicateEpisodeRule.diagnoses(DuplicateEpisodeRule.band(context, ((DuplicateEpisodeRule) rule("T22-07")).settings()));
		Assert.assertTrue(diagnoses.contains(155686));
		Assert.assertTrue(sql, sql.contains(MOH705CohortLibrary.diagnosisVisitsSql(context.getEtlDatabase(), diagnoses, "startDate", "endDate")));
		Assert.assertTrue(sql, sql.contains(MOH705CohortLibrary.diagnosisVisitsSql(context.getEtlDatabase(), diagnoses, "windowStart", "endDate")));
		Assert.assertTrue(sql, sql.contains(MOH705CohortLibrary.patientAgeSql(context.getEtlDatabase(), "<5")));
		Assert.assertFalse(sql, sql.contains(">=5"));
	}

	@Test
	public void t2207_shouldListRepeatsInsideTheMonthByDefault() {
		// e.g. positive on 1 Oct and again on 12 Oct: both counted in October's MOH 705
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).queryReturns(new Object[] { 1L });

		DqResult result = single(rule("T22-07"), context);

		Assert.assertEquals(DqOutcome.WARN, result.getOutcome());
		String sql = context.queries.get(0);
		Assert.assertFalse(sql, sql.contains("a.visit_date < date(:startDate)"));
		Assert.assertTrue(sql, sql.contains("count(distinct patient_id)"));
		// The MOH 705 confirmed malaria: a final diagnosis on a new visit
		Assert.assertTrue(sql, sql.contains("AND ed.dx_rank = 2"));
		Assert.assertFalse(sql, sql.contains("etl_laboratory_extract"));
	}

	@Test
	public void t2207_shouldOnlyListRepeatsFromLastMonthWhenCrossPeriodOnly() {
		Map<String, Object> values = definition("T22-07");
		values.put("crossPeriodOnly", true);
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).queryReturns(new Object[] { 1L });

		DqResult result = single(new DuplicateEpisodeRule(new DqConfig.RuleDefinition(null, values)), context);

		Assert.assertTrue(result.getMessage(), result.getMessage().contains("counted again this month"));
		Assert.assertTrue(context.queries.get(0), context.queries.get(0).contains("a.visit_date < date(:startDate)"));
	}

	@Test
	public void t2207_shouldPassWithNoPairsAndOnlyApplyToMoh705() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705B, march).queryReturns(new Object[] { 0L });

		Assert.assertEquals(DqOutcome.PASS, single(rule("T22-07"), context).getOutcome());
		String sql = context.queries.get(0);
		Assert.assertTrue(sql, sql.contains(MOH705CohortLibrary.patientAgeSql(context.getEtlDatabase(), ">=5")));
		Assert.assertFalse(sql, sql.contains("155686"));
		Assert.assertFalse(rule("T22-07").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertFalse(rule("T22-07").appliesTo(FakeRuleContext.monthly(MOH_706, march)));
		Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList(MOH_705A, MOH_705B)),
				rule("T22-07").getReports(FakeRuleContext.monthly(MOH_705A, march).getConfig()));
	}

	@Test
	public void t2207_shouldCheckOneReportForAllAgesWithoutReports() {
		Map<String, Object> values = definition("T22-07");
		values.remove("reports");
		values.put("report", MOH_743);
		DuplicateEpisodeRule rule = new DuplicateEpisodeRule(new DqConfig.RuleDefinition(null, values));
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march).queryReturns(new Object[] { 0L });

		single(rule, context);

		Assert.assertFalse(rule.appliesTo(FakeRuleContext.monthly(MOH_705A, march)));
		String sql = context.queries.get(0);
		Assert.assertFalse(sql, sql.contains("etl_patient_demographics d where"));
		Assert.assertTrue(sql, sql.contains(MOH705CohortLibrary.diagnosisVisitsSql(context.getEtlDatabase(),
				DiagnosisLists.getConfirmedMalariaList(), "startDate", "endDate")));
	}

	@Test
	public void t2207_shouldOnlyLeaveOutASecondEpisodeThatIsSevere() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705B, march).queryReturns(new Object[] { 0L });

		single(rule("T22-07"), context);

		String sql = context.queries.get(0);
		// Severity is read from the diagnoses of the episode's own encounter
		Assert.assertTrue(sql, sql.contains("select 1 from encounter_diagnosis s where s.encounter_id = ce.encounter_id "
				+ "and s.voided = 0 and s.diagnosis_coded in (:severeConcepts)"));
		Assert.assertTrue(sql, sql.contains("where b.severe = 0 "));
		Assert.assertFalse(sql, sql.contains("a.severe"));
		Assert.assertFalse(sql, sql.contains("from obs"));
	}

	@Test
	public void t2207_shouldUseConfiguredSevereConcepts() {
		Map<String, Object> values = definition("T22-07");
		values.put("severeConcepts", Arrays.asList(160155, "not a number"));
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).queryReturns(new Object[] { 0L });

		single(new DuplicateEpisodeRule(new DqConfig.RuleDefinition(null, values)), context);

		Assert.assertEquals(Collections.singletonList(160155), context.queryParameters.get(0).get("severeConcepts"));
		Assert.assertNull(context.queryParameters.get(0).get("severeName0"));
	}

	// T22-08: week-on-week test positivity, malaria tests positive ÷ tests performed in MOH 706

	@Test
	public void t2208_shouldTicketAChangeAboveFiftyPercent() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		// This week 16 of 40 positive (40%), last week 10 of 40 (25%): a 60% rise
		FakeRuleContext context = FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 16.0).indicator(TESTS_706, monday, 40.0)
				.indicator(POSITIVE_706, previous, 10.0).indicator(TESTS_706, previous, 40.0);

		DqResult result = single(rule("T22-08"), context);

		Assert.assertEquals(DqOutcome.TICKET, result.getOutcome());
		Assert.assertEquals(0.6, result.getMeasureValue(), 1e-12);
		List<DqTicket> tickets = DqRuleEngine.ticketsFor(Collections.singletonList(result), context.getConfig());
		Assert.assertEquals(DqTicket.ROLE_SCHRIO, tickets.get(0).getAssigneeRole());
		Assert.assertEquals("MALARIA", tickets.get(0).getProgramCode());
	}

	@Test
	public void t2208_shouldPassAtFiftyPercentAndCompareSmallWeeks() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		Assert.assertEquals(DqOutcome.PASS, single(rule("T22-08"), FakeRuleContext.weekly(monday)
				.indicator(POSITIVE_706, monday, 15.0).indicator(TESTS_706, monday, 40.0)
				.indicator(POSITIVE_706, previous, 10.0).indicator(TESTS_706, previous, 40.0)).getOutcome());
		// No minimum: a small week is compared, even with a minimum left in an older configuration
		Assert.assertEquals(DqOutcome.TICKET, single(rule("T22-08"), FakeRuleContext.weekly(monday)
				.indicator(POSITIVE_706, monday, 2.0).indicator(TESTS_706, monday, 3.0)
				.indicator(POSITIVE_706, previous, 1.0).indicator(TESTS_706, previous, 3.0)).getOutcome());
		Map<String, Object> values = definition("T22-08");
		values.put("minDenominator", 20);
		// 15 of 19 (78.9%) against 10 of 40 (25%): compared, and flagged
		Assert.assertEquals(DqOutcome.TICKET, single(new PositivityChangeRule(new DqConfig.RuleDefinition(null, values)),
				FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 15.0).indicator(TESTS_706, monday, 19.0)
						.indicator(POSITIVE_706, previous, 10.0).indicator(TESTS_706, previous, 40.0)).getOutcome());
	}

	@Test
	public void t2208_shouldFlagARiseOrAFallAboveFiftyPercent() {
		Date w1 = TestUtils.date(2026, 10, 5);
		Date w2 = TestUtils.date(2026, 10, 12);
		Date w3 = TestUtils.date(2026, 10, 19);
		Date w4 = TestUtils.date(2026, 10, 26);
		// 20%, then 36% (a rise of 80%), 15% (a fall of 58.3%), 20% (a rise of 33.3%)
		FakeRuleContext weeks = FakeRuleContext.weekly(w2).indicator(POSITIVE_706, w1, 10.0).indicator(TESTS_706, w1, 50.0)
				.indicator(POSITIVE_706, w2, 18.0).indicator(TESTS_706, w2, 50.0);
		DqResult rise = single(rule("T22-08"), weeks);
		Assert.assertEquals(DqOutcome.TICKET, rise.getOutcome());
		Assert.assertEquals(0.8, rise.getMeasureValue(), 1e-12);
		Assert.assertTrue(rise.getMessage(), rise.getMessage().contains("a rise of 80%; above the 50% threshold."));

		DqResult fall = single(rule("T22-08"), FakeRuleContext.weekly(w3).indicator(POSITIVE_706, w2, 18.0)
				.indicator(TESTS_706, w2, 50.0).indicator(POSITIVE_706, w3, 9.0).indicator(TESTS_706, w3, 60.0));
		Assert.assertEquals(DqOutcome.TICKET, fall.getOutcome());
		Assert.assertTrue(fall.getMessage(), fall.getMessage().contains("a fall of 58.3%; above the 50% threshold."));

		DqResult smallRise = single(rule("T22-08"), FakeRuleContext.weekly(w4).indicator(POSITIVE_706, w3, 9.0)
				.indicator(TESTS_706, w3, 60.0).indicator(POSITIVE_706, w4, 10.0).indicator(TESTS_706, w4, 50.0));
		Assert.assertEquals(DqOutcome.PASS, smallRise.getOutcome());
		Assert.assertTrue(smallRise.getMessage(), smallRise.getMessage().endsWith("a rise of 33.3%."));
		Assert.assertEquals("change ≤ 50%", smallRise.getThreshold());
	}

	@Test
	public void t2208_shouldNotCompareAWeekWithNoTests() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		DqResult result = single(rule("T22-08"), FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 0.0)
				.indicator(TESTS_706, monday, 0.0).indicator(POSITIVE_706, previous, 1.0).indicator(TESTS_706, previous, 2.0));

		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, result.getOutcome());
		Assert.assertEquals("Not compared: Malaria tests performed, all methods was 0 this week and 2 the week before. "
				+ "Positivity can only be compared when both weeks have tests.", result.getMessage());
		Assert.assertFalse(result.getMessage(), result.getMessage().contains("NaN"));
	}

	@Test
	public void t2208_shouldFlagPositivesAfterAWeekWithNoTests() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		DqResult result = single(rule("T22-08"), FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 1.0)
				.indicator(TESTS_706, monday, 2.0).indicator(POSITIVE_706, previous, 0.0).indicator(TESTS_706, previous, 0.0));

		Assert.assertEquals(DqOutcome.TICKET, result.getOutcome());
		Assert.assertNull(result.getMeasureValue());
		Assert.assertEquals("Malaria tests positive, all methods ÷ Malaria tests performed, all methods was 50% (1 of 2) "
				+ "this week, after a week with no tests; positives after a week with no tests are flagged.", result.getMessage());

		// Tests but no positives: nothing to flag, and still no rate the week before to compare with
		DqResult negative = single(rule("T22-08"), FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 0.0)
				.indicator(TESTS_706, monday, 2.0).indicator(POSITIVE_706, previous, 0.0).indicator(TESTS_706, previous, 0.0));
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, negative.getOutcome());
	}

	@Test
	public void t2208_shouldFlagPositivesAfterAWeekWithNone() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		DqResult result = single(rule("T22-08"), FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 1.0)
				.indicator(TESTS_706, monday, 2.0).indicator(POSITIVE_706, previous, 0.0).indicator(TESTS_706, previous, 3.0));

		Assert.assertEquals(DqOutcome.TICKET, result.getOutcome());
		Assert.assertNull(result.getMeasureValue());
	}

	@Test
	public void t2208_shouldReadMoh706Tests() {
		Date monday = TestUtils.date(2026, 10, 12);
		Date previous = TestUtils.date(2026, 10, 5);
		DqResult result = single(rule("T22-08"), FakeRuleContext.weekly(monday).indicator(POSITIVE_706, monday, 10.0)
				.indicator(TESTS_706, monday, 40.0).indicator(POSITIVE_706, previous, 10.0).indicator(TESTS_706, previous, 40.0));

		Assert.assertEquals(POSITIVE_706 + "/" + TESTS_706, result.getIndicatorCode());
		Assert.assertEquals(DqOutcome.PASS, result.getOutcome());
		Assert.assertEquals(Double.valueOf(10), result.getNumerator());
		Assert.assertEquals(Double.valueOf(40), result.getDenominator());
	}

	@Test
	public void t2208_shouldOnlyRunWeekly() {
		Assert.assertEquals(DqRule.Schedule.WEEK, rule("T22-08").getSchedule());
		Assert.assertFalse(rule("T22-08").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertTrue(rule("T22-08").appliesTo(FakeRuleContext.weekly(TestUtils.date(2026, 10, 12))));
	}

	// G-RPT-01: in KHIS by the 5th

	@Test
	public void grpt01_shouldPassOnTimeAndWarnLateOrMissing() {
		FakeRuleContext context = FakeRuleContext.month(march)
				.sent(MOH_705A, march, TestUtils.date(2026, 4, 5, 23, 59, 0))
				.sent(MOH_706, march, TestUtils.date(2026, 4, 7, 9, 0, 0));

		List<DqResult> results = rule("G-RPT-01").evaluate(context);

		// MOH 705A, 705B, 706, 711 and 743; not MOH 505
		Assert.assertEquals(5, results.size());
		for (DqResult result : results) {
			Assert.assertNotEquals(MOH_505, result.getIndicatorCode());
		}
		DqResult onTime = result(results, MOH_705A);
		Assert.assertEquals(DqOutcome.PASS, onTime.getOutcome());
		DqResult late = result(results, MOH_706);
		Assert.assertEquals(DqOutcome.WARN, late.getOutcome());
		Assert.assertEquals(2.0, late.getMeasureValue(), 0);
		Assert.assertTrue(late.getMessage(), late.getMessage().contains("2 days after the deadline of 5 Apr 2026"));
		DqResult missing = result(results, MOH_743);
		Assert.assertEquals(DqOutcome.WARN, missing.getOutcome());
		Assert.assertTrue(missing.getMessage(), missing.getMessage().contains("had not been sent to KHIS"));
	}

	@Test
	public void grpt01_shouldCountAReportKhisDoesNotHoldAsCompleteAsMissing() {
		Date sent = TestUtils.date(2026, 4, 3, 10, 0, 0);
		FakeRuleContext context = FakeRuleContext.month(march)
				// Values saved on time, but completing it failed
				.khis(MOH_705A, march, new KhisSubmissionState(sent, null, false, "SENT",
						"Values saved, but KHIS did not register the dataset as complete (HTTP 409): missing compulsory fields"))
				// The EMR completed it on time, but KHIS read back says it is not complete
				.khis(MOH_706, march, new KhisSubmissionState(sent, null, true, "COMPLETED_IN_KHIS", null))
				// Every value zero, so never sent
				.khis(MOH_743, march, new KhisSubmissionState(null, null, false, KhisSubmissionState.STATUS_NO_VALUES,
						"Every value in this report is zero"))
				// Completed in KHIS by hand two days late
				.khis(MOH_705B, march, new KhisSubmissionState(sent, TestUtils.date(2026, 4, 7, 9, 0, 0), true, "COMPLETED_IN_KHIS", null));

		List<DqResult> results = rule("G-RPT-01").evaluate(context);

		DqResult notComplete = result(results, MOH_705A);
		Assert.assertEquals(DqOutcome.WARN, notComplete.getOutcome());
		Assert.assertTrue(notComplete.getMessage(), notComplete.getMessage().contains("reached KHIS on 3 Apr 2026 but is not "
				+ "marked complete there, so it is incomplete in KHIS. Values saved, but KHIS did not register"));
		Assert.assertNull(notComplete.getMeasureValue());
		Assert.assertEquals(DqOutcome.WARN, result(results, MOH_706).getOutcome());
		DqResult empty = result(results, MOH_743);
		Assert.assertEquals(DqOutcome.WARN, empty.getOutcome());
		Assert.assertTrue(empty.getMessage(), empty.getMessage().contains("was not sent to KHIS because every value in it is zero"));
		DqResult late = result(results, MOH_705B);
		Assert.assertEquals(2.0, late.getMeasureValue(), 0);
		Assert.assertTrue(late.getDetail(), late.getDetail().contains("\"completionSource\":\"KHIS\""));
	}

	@Test
	public void grpt01_shouldTakeTheLatestKhisAnswerOverTheEmrsRecord() {
		Date sent = TestUtils.date(2026, 4, 3);
		Date read = TestUtils.date(2026, 4, 10);
		// Version 2 is latest; version 1 was completed by the EMR, and KHIS read back says not complete
		KhisSubmissionState state = KhisSubmissionState.fromLedger(Arrays.asList(
				new Object[] { "QUEUED", null, null, "will retry", null, null },
				new Object[] { "COMPLETED_IN_KHIS", sent, sent, null, read, null }));
		Assert.assertEquals(sent, state.getFirstSentAt());
		Assert.assertNull(state.getCompletedAt());
		Assert.assertTrue(state.isCompletionReadFromKhis());
		Assert.assertEquals("QUEUED", state.getLatestStatus());
		Assert.assertEquals("will retry", state.getLatestError());

		// Not read back: the EMR's record stands
		state = KhisSubmissionState.fromLedger(Collections.singletonList(new Object[] { "COMPLETED_IN_KHIS", sent, sent, null, null, null }));
		Assert.assertEquals(sent, state.getCompletedAt());
		Assert.assertFalse(state.isCompletionReadFromKhis());
		Assert.assertNull(KhisSubmissionState.fromLedger(Collections.<Object[]>emptyList()));
	}

	@Test
	public void grpt01_shouldWaitForTheLastDeadlineAndNeedTheLedger() {
		TimelinessRule rule = (TimelinessRule) rule("G-RPT-01");
		Assert.assertEquals(DqRule.Schedule.MONTH, rule.getSchedule());
		Assert.assertEquals(TestUtils.date(2026, 4, 6), rule.lastDeadline(DqConfigStore.getDefaultConfig(), march));
		// Checked once the KHIS read-back day is over too
		Assert.assertEquals(TestUtils.date(2026, 4, 11), rule.checkAfter(DqConfigStore.getDefaultConfig(), march));

		FakeRuleContext context = FakeRuleContext.month(march);
		context.khisTracked = false;
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, rule.evaluate(context).get(0).getOutcome());
	}

	// VF-01: verification factors, recount against the report and against KHIS

	@Test
	public void vf01_shouldDivideTheRecountByTheReportAndByKhis() {
		FakeRuleContext context = FakeRuleContext.month(march)
				.indicator(CONFIRMED, march, 100.0).recount(CONFIRMED, march, 104.0).khisIndicator(CONFIRMED, march, 80.0)
				.indicator(TESTED, march, 200.0).recount(TESTED, march, 200.0).khisIndicator(TESTED, march, 200.0)
				.indicator("MAL_SUSPECTED_705", march, 0.0).recount("MAL_SUSPECTED_705", march, 3.0);

		List<DqResult> results = rule("VF-01").evaluate(context);

		Assert.assertEquals(10, results.size());
		DqResult confirmed = result(results, CONFIRMED);
		Assert.assertEquals(DqOutcome.WARN, confirmed.getOutcome());
		Assert.assertEquals(1.04, confirmed.getMeasureValue(), 1e-12);
		Assert.assertTrue(confirmed.getDetail(), confirmed.getDetail().contains("\"vfKhis\":1.3"));
		Assert.assertEquals(DqOutcome.PASS, result(results, TESTED).getOutcome());
		// Recounted 3 where the report had 0: no ratio, but a discrepancy
		Assert.assertEquals(DqOutcome.WARN, result(results, "MAL_SUSPECTED_705").getOutcome());
		// Not generated for the month
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, result(results, "MAL_LLIN_U1_711").getOutcome());
		Assert.assertEquals(TestUtils.date(2026, 4, 11), rule("VF-01").checkAfter(DqConfigStore.getDefaultConfig(), march));
	}

	@Test
	public void vf01_shouldTakeEqualZerosAsAgreement() {
		Assert.assertEquals(1.0, VerificationRule.vf(0.0, 0.0), 0);
		Assert.assertNull(VerificationRule.vf(3.0, 0.0));
		Assert.assertNull(VerificationRule.vf(null, 4.0));
		Assert.assertEquals(0.5, VerificationRule.vf(2.0, 4.0), 0);
	}

	// G-RPT-02: EMR against KHIS, within 5%

	@Test
	public void grpt02_shouldCompareEveryDataElementOfTheReport() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march)
				.value("MOH705A", "CMC-32", "deCMC", "male", 60.0, 56.0)
				.value("MOH705A", "CMC-32F", "deCMC", "female", 40.0, 38.0)
				.value("MOH705A", "DIA-32", "deDIA", "total", 50.0, 50.0)
				.value("MOH705A", "date", null, null, 1.0, null);

		List<DqResult> results = rule("G-RPT-02").evaluate(context);

		Assert.assertEquals(2, results.size());
		DqResult confirmed = result(results, "deCMC");
		Assert.assertEquals(DqOutcome.TICKET, confirmed.getOutcome());
		Assert.assertEquals(100.0, confirmed.getNumerator(), 0);
		Assert.assertEquals(94.0, confirmed.getDenominator(), 0);
		Assert.assertTrue(confirmed.getMessage(), confirmed.getMessage().startsWith("CMC-32, CMC-32F in MOH 705A"));
		// Not a malaria indicator, compared all the same
		Assert.assertEquals(DqOutcome.PASS, result(results, "deDIA").getOutcome());

		DqTicket ticket = DqRuleEngine.ticketsFor(Collections.singletonList(confirmed), context.getConfig()).get(0);
		Assert.assertEquals("deCMC", ticket.getIndicatorCode());
		Assert.assertEquals(-6.0, ticket.getVariancePct(), 1e-9);
	}

	@Test
	public void grpt02_shouldSayWhenNothingCanBeCompared() {
		List<DqResult> results = rule("G-RPT-02").evaluate(FakeRuleContext.monthly(MOH_705A, march)
				.value("MOH705A", "SMC-32", null, 250.0, null));

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, results.get(0).getOutcome());
	}

	@Test
	public void grpt02_shouldTicketAnIndicatorVarianceAboveFivePercent() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march)
				.value("MOH705A", "CMC-32", "deCMC", 100.0, 94.0)
				.value("MOH705A", "TMC-32", "deTMC", 200.0, 200.0)
				.value("MOH705A", "SMC-32", null, 250.0, null);

		List<DqResult> results = indicatorParity(context).evaluate(context);

		DqResult confirmed = result(results, CONFIRMED);
		Assert.assertEquals(DqOutcome.TICKET, confirmed.getOutcome());
		Assert.assertEquals(-0.06, confirmed.getMeasureValue(), 1e-12);
		Assert.assertEquals(DqOutcome.PASS, result(results, TESTED).getOutcome());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, result(results, SUSPECTED).getOutcome());

		DqTicket ticket = DqRuleEngine.ticketsFor(Collections.singletonList(confirmed), context.getConfig()).get(0);
		Assert.assertEquals(100.0, ticket.getEmrValue(), 0);
		Assert.assertEquals(94.0, ticket.getKhisValue(), 0);
		Assert.assertEquals(-6.0, ticket.getVariancePct(), 1e-9);
	}

	@Test
	public void grpt02_shouldOnlyCompareTheReportsSentToKhis() {
		DqConfig config = FakeRuleContext.monthly(MOH_705A, march).getConfig();

		Assert.assertFalse(rule("G-RPT-02").getReports(config).contains("MOH 643"));
		Assert.assertTrue(rule("G-RPT-02").getReports(config).contains(MOH_743));
		Assert.assertFalse(rule("G-RPT-02").appliesTo(FakeRuleContext.monthly("MOH 643", march)));
		Assert.assertTrue(rule("G-RPT-02").appliesTo(FakeRuleContext.monthly(MOH_743, march)));
		Assert.assertTrue(rule("G-RPT-02").appliesTo(FakeRuleContext.monthly("MOH 711", march)));
	}

	@Test
	public void grpt02_shouldCountAValueKhisDoesNotHoldAsZero() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_705A, march).value("MOH705A", "CMC-32", "deCMC", 10.0, null);

		DqResult confirmed = result(rule("G-RPT-02").evaluate(context), "deCMC");

		Assert.assertEquals(DqOutcome.TICKET, confirmed.getOutcome());
		Assert.assertEquals(0.0, confirmed.getDenominator(), 0);
	}

	/**
	 * G-RPT-02 comparing the malaria indicators only
	 */
	private static KhisParityRule indicatorParity(FakeRuleContext context) {
		Map<String, Object> values = definition("G-RPT-02");
		values.put("compare", KhisParityRule.COMPARE_INDICATORS);
		return new KhisParityRule(new DqConfig.RuleDefinition(context.getConfig().getProgram("MALARIA"), values));
	}

	// Engine

	@Test
	public void engine_shouldNotLetACrossReportRuleBlock() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 130.0).indicator(CONFIRMED, march, 100.0);

		List<DqResult> results = DqRuleEngine.evaluate(context, Collections.singletonList(rule("T22-04")));

		Assert.assertEquals(DqOutcome.WARN, results.get(0).getOutcome());
		Assert.assertTrue(results.get(0).getMessage(), results.get(0).getMessage().contains("compares several reports"));
		Assert.assertEquals(DqRunStatus.AWAITING_APPROVAL, DqRuleEngine.statusFor(results));
	}

	@Test
	public void engine_shouldLetAGatedRuleBlockOnlyItsReport() {
		Map<String, Object> values = definition("T22-04");
		values.put("gates", MOH_743);
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqRule gated = new IndicatorRatioRule(new DqConfig.RuleDefinition(config.getProgram("MALARIA"), values));

		List<DqResult> on743 = DqRuleEngine.evaluate(FakeRuleContext.monthly(MOH_743, march)
				.indicator(TREATED, march, 130.0).indicator(CONFIRMED, march, 100.0), Collections.singletonList(gated));
		List<DqResult> on705 = DqRuleEngine.evaluate(FakeRuleContext.monthly(MOH_705A, march)
				.indicator(TREATED, march, 130.0).indicator(CONFIRMED, march, 100.0), Collections.singletonList(gated));

		Assert.assertEquals(DqRunStatus.BLOCKED, DqRuleEngine.statusFor(on743));
		Assert.assertEquals(DqOutcome.WARN, on705.get(0).getOutcome());
		Assert.assertTrue(on705.get(0).getMessage(), on705.get(0).getMessage().contains("Malaria does not own " + MOH_705A));
	}

	@Test
	public void engine_shouldLetASingleReportRuleBlockAReportItsProgramOwns() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put("id", "T22-04-743");
		values.put("type", "RATIO");
		values.put("numerator", "MAL_TREATED_ACT_743");
		values.put("denominator", "MAL_CONFIRMED_743");
		values.put("max", 1.0);
		values.put("severity", "BLOCK");
		DqRule rule = new IndicatorRatioRule(new DqConfig.RuleDefinition(config.getProgram("MALARIA"), values));

		List<DqResult> results = DqRuleEngine.evaluate(FakeRuleContext.monthly(MOH_743, march)
				.indicator("MAL_TREATED_ACT_743", march, 130.0).indicator("MAL_CONFIRMED_743", march, 100.0),
				Collections.singletonList(rule));

		Assert.assertEquals(DqRunStatus.BLOCKED, DqRuleEngine.statusFor(results));
		// A block is not a ticket
		Assert.assertTrue(DqRuleEngine.ticketsFor(results, config).isEmpty());
	}

	@Test
	public void engine_shouldTicketCrossReportWarningsToTheProgramOwner() {
		FakeRuleContext context = FakeRuleContext.monthly(MOH_706, march).indicator(SUSPECTED, march, 80.0)
				.indicator("MAL_TESTS_706", march, 100.0);

		List<DqResult> results = DqRuleEngine.evaluate(context, Collections.singletonList(rule("X-RPT-01")));
		List<DqTicket> tickets = DqRuleEngine.ticketsFor(results, context.getConfig());

		Assert.assertEquals(DqRunStatus.AWAITING_APPROVAL, DqRuleEngine.statusFor(results));
		// 705 against all tests in 706 is 20% low
		Assert.assertEquals(1, tickets.size());
		Assert.assertEquals(-20.0, tickets.get(0).getVariancePct(), 1e-9);
		Assert.assertEquals(DqTicket.ROLE_HRIO, tickets.get(0).getAssigneeRole());
		Assert.assertEquals("MALARIA", tickets.get(0).getProgramCode());
	}

	@Test
	public void engine_shouldTurnARuleErrorIntoANotApplicableResult() {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put("id", "T22-99");
		DqRule broken = new AbstractDqRule(new DqConfig.RuleDefinition(null, values)) {

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
				Arrays.asList(broken, rule("X-RPT-01")));

		Assert.assertEquals(1, results.size());
		Assert.assertEquals(DqOutcome.NOT_APPLICABLE, results.get(0).getOutcome());
		Assert.assertTrue(results.get(0).getMessage().contains("ETL table missing"));
	}

	@Test
	public void types_shouldBuildEveryMalariaRuleOnItsSchedule() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		// T22-03, T22-04, T22-07 and X-RPT-01
		Assert.assertEquals(4, DqRuleTypes.rules(config, DqRule.Schedule.REPORT).size());
		Assert.assertEquals("T22-08", DqRuleTypes.rules(config, DqRule.Schedule.WEEK).get(0).getId());
		Assert.assertEquals("G-RPT-01", DqRuleTypes.rules(config, DqRule.Schedule.MONTH).get(0).getId());
		Assert.assertEquals("G-RPT-02", DqRuleTypes.rules(config, DqRule.Schedule.KHIS_READBACK).get(0).getId());
	}

	private static DqRule rule(String id) {
		DqRule rule = DqRuleTypes.rule(DqConfigStore.getDefaultConfig(), id);
		Assert.assertNotNull("No rule " + id, rule);
		return rule;
	}

	private static Map<String, Object> definition(String id) {
		return new LinkedHashMap<String, Object>(DqConfigStore.getDefaultConfig().getRuleDefinition(id).getValues());
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

	private static DqResult result(List<DqResult> results, String indicatorCode) {
		for (DqResult result : results) {
			if (indicatorCode.equals(result.getIndicatorCode())) {
				return result;
			}
		}
		throw new AssertionError("No result for " + indicatorCode);
	}

	private static DqResult baseline(List<DqResult> results, String indicator, String baseline) {
		for (DqResult result : results) {
			if (indicator.equals(result.getIndicatorCode()) && result.getDetail().contains("\"" + baseline + "\"")) {
				return result;
			}
		}
		throw new AssertionError("No " + baseline + " result for " + indicator);
	}

	private static String baselineOf(DqResult result) {
		return result.getDetail().contains("\"" + ConsistencyOverTimeRule.BASELINE_ROLLING + "\"")
				? ConsistencyOverTimeRule.BASELINE_ROLLING : ConsistencyOverTimeRule.BASELINE_LAST_YEAR;
	}
}
