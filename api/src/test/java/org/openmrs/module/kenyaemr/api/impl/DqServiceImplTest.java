/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DqServiceImplTest {

	private static DqConfig.ExpectedReport report(String frequency, Integer deadlineDay) {
		DqConfig.ExpectedReport report = new DqConfig.ExpectedReport();
		report.setReport("MOH 705A");
		report.setFrequency(frequency);
		report.setDeadlineDay(deadlineDay);
		return report;
	}

	@Test
	public void isDeadlineReached_shouldStartAtMidnightOnTheDeadlineDay() {
		Date september = TestUtils.date(2026, 9, 1);
		DqConfig.ExpectedReport monthly = report(DqConfig.ExpectedReport.FREQUENCY_MONTHLY, 5);

		Assert.assertFalse(DqServiceImpl.isDeadlineReached(monthly, september, TestUtils.date(2026, 10, 4, 23, 59, 59)));
		Assert.assertTrue(DqServiceImpl.isDeadlineReached(monthly, september, TestUtils.date(2026, 10, 5, 0, 0, 0)));
		Assert.assertTrue(DqServiceImpl.isDeadlineReached(monthly, september, TestUtils.date(2026, 10, 9)));
	}

	@Test
	public void isDeadlineReached_shouldIgnoreReportsWithoutAMonthlyDeadline() {
		Date september = TestUtils.date(2026, 9, 1);
		Date late = TestUtils.date(2026, 10, 20);

		Assert.assertFalse(DqServiceImpl.isDeadlineReached(report(DqConfig.ExpectedReport.FREQUENCY_MONTHLY, null), september, late));
		Assert.assertFalse(DqServiceImpl.isDeadlineReached(report(DqConfig.ExpectedReport.FREQUENCY_WEEKLY, 5), september, late));
	}

	@Test
	public void isStale_shouldBeTrueOnlyWhenOneOfTheRulesReportsWasGeneratedAfterTheCheck() {
		List<DqResult> checked = Collections.singletonList(result("MAL_CONFIRMED_705", DqOutcome.PASS,
				TestUtils.date(2026, 10, 30, 13, 31, 59)));
		Set<String> reports = new HashSet<String>(Arrays.asList("MOH 705A", "MOH 705B"));

		Assert.assertFalse(DqServiceImpl.isStale(checked, reports,
				Collections.singletonList(run("MOH 705B", TestUtils.date(2026, 10, 30, 13, 24, 45)))));
		// Another report of the month does not matter
		Assert.assertFalse(DqServiceImpl.isStale(checked, reports,
				Collections.singletonList(run("MOH 711", TestUtils.date(2026, 11, 2)))));
		// MOH 705A generated after the check
		Assert.assertTrue(DqServiceImpl.isStale(checked, reports, Arrays.asList(
				run("MOH 705B", TestUtils.date(2026, 10, 30, 13, 24, 45)), run("MOH 705A", TestUtils.date(2026, 10, 30, 14, 28, 27)))));
	}

	@Test
	public void replace_shouldUpdateThePreviousResultInPlaceAndOfferOnlyChangesForTickets() {
		DqResult previousConfirmed = result("MAL_CONFIRMED_705", DqOutcome.PASS, TestUtils.date(2026, 10, 30));
		previousConfirmed.setId(7);
		DqResult previousTested = result("MAL_TESTED_705", DqOutcome.WARN, TestUtils.date(2026, 10, 30));
		DqResult confirmed = result("MAL_CONFIRMED_705", DqOutcome.WARN, TestUtils.date(2026, 11, 2));
		confirmed.setNumerator(10.0);
		DqResult tested = result("MAL_TESTED_705", DqOutcome.WARN, TestUtils.date(2026, 11, 2));
		DqResult added = result("MAL_TESTS_706", DqOutcome.WARN, TestUtils.date(2026, 11, 2));
		List<DqResult> changed = new ArrayList<DqResult>();

		List<DqResult> saved = DqServiceImpl.replace(Arrays.asList(confirmed, tested, added),
				new ArrayList<DqResult>(Arrays.asList(previousConfirmed, previousTested)), changed);

		Assert.assertEquals(Arrays.asList(previousConfirmed, previousTested, added), saved);
		Assert.assertEquals(Integer.valueOf(7), previousConfirmed.getId());
		Assert.assertEquals(DqOutcome.WARN, previousConfirmed.getOutcome());
		Assert.assertEquals(Double.valueOf(10), previousConfirmed.getNumerator());
		Assert.assertEquals(TestUtils.date(2026, 11, 2), previousConfirmed.getEvaluatedAt());
		// MAL_TESTED_705 was already a warning, so it keeps the ticket it may have
		Assert.assertEquals(Arrays.asList(previousConfirmed, added), changed);
	}

	@Test
	public void replace_shouldPutEachBaselineOfT2203InPlaceOfItsOwn() {
		DqResult previousRolling = baselineResult(ConsistencyOverTimeRule.BASELINE_ROLLING, DqOutcome.PASS);
		DqResult previousLastYear = baselineResult(ConsistencyOverTimeRule.BASELINE_LAST_YEAR, DqOutcome.TICKET);
		DqResult lastYear = baselineResult(ConsistencyOverTimeRule.BASELINE_LAST_YEAR, DqOutcome.TICKET);
		lastYear.setNumerator(24.0);
		DqResult rolling = baselineResult(ConsistencyOverTimeRule.BASELINE_ROLLING, DqOutcome.PASS);
		List<DqResult> changed = new ArrayList<DqResult>();

		// The new results come in the other order
		List<DqResult> saved = DqServiceImpl.replace(Arrays.asList(lastYear, rolling),
				new ArrayList<DqResult>(Arrays.asList(previousRolling, previousLastYear)), changed);

		Assert.assertEquals(Arrays.asList(previousLastYear, previousRolling), saved);
		Assert.assertEquals(Double.valueOf(24), previousLastYear.getNumerator());
		Assert.assertTrue(changed.isEmpty());
	}

	@Test
	public void readingReport_shouldKeepTheRulesResultsAboutIndicatorsCountedFromTheReport() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqResult confirmed = baselineResult(ConsistencyOverTimeRule.BASELINE_LAST_YEAR, DqOutcome.TICKET);
		DqResult otherRule = result("MAL_CONFIRMED_705", DqOutcome.PASS, TestUtils.date(2026, 11, 2));
		Set<String> rules = Collections.singleton(ConsistencyOverTimeRule.ID);

		Assert.assertEquals(Collections.singletonList(confirmed),
				DqServiceImpl.readingReport(Arrays.asList(confirmed, otherRule), rules, config, "MOH 705A Outpatient summary"));
		Assert.assertTrue(DqServiceImpl.readingReport(Arrays.asList(confirmed, otherRule), rules, config, "MOH 743").isEmpty());
	}

	@Test
	public void readingReport_shouldKeepACrossCheckWhenEitherOfItsIndicatorsIsCountedFromTheReport() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqResult cross = result("MAL_SUSPECTED_705/MAL_TESTS_706", DqOutcome.WARN, TestUtils.date(2026, 11, 2));
		cross.setRuleId("X-RPT-01");
		Set<String> rules = Collections.singleton("X-RPT-01");

		Assert.assertEquals(1, DqServiceImpl.readingReport(Collections.singletonList(cross), rules, config,
				"MOH 705A Outpatient summary").size());
		Assert.assertEquals(1, DqServiceImpl.readingReport(Collections.singletonList(cross), rules, config,
				"MOH 706").size());
		Assert.assertTrue(DqServiceImpl.readingReport(Collections.singletonList(cross), rules, config, "MOH 743").isEmpty());
	}

	@Test
	public void sameFinding_shouldMatchProgramRuleIndicatorAndPeriod() {
		DqTicket other = ticket("MALARIA", "MAL_SUSPECTED_705/MAL_BST_706", TestUtils.date(2026, 10, 1));
		DqTicket open = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1));
		DqTicket raised = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1));

		Assert.assertSame(open, DqServiceImpl.sameFinding(Arrays.asList(other, open), raised));
		Assert.assertNull(DqServiceImpl.sameFinding(Collections.singletonList(other), raised));
		Assert.assertNull(DqServiceImpl.sameFinding(Collections.singletonList(
				ticket("TB", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1))), raised));
		Assert.assertNull(DqServiceImpl.sameFinding(Collections.singletonList(
				ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 9, 1))), raised));
		// A rule about the whole month has no indicator
		Assert.assertNotNull(DqServiceImpl.sameFinding(Collections.singletonList(ticket("MALARIA", null,
				TestUtils.date(2026, 10, 1))), ticket("MALARIA", null, TestUtils.date(2026, 10, 1))));
	}

	@Test
	public void mergeInto_shouldTakeTheLatestFiguresAndKeepTheFollowUp() {
		DqTicket open = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1));
		open.setStatus(DqTicketStatus.IN_PROGRESS);
		open.setResolutionNote("Checking the lab register");
		open.setOpenedAt(TestUtils.date(2026, 11, 2));
		open.setEmrValue(230.0);
		open.setVariancePct(15.0);
		DqTicket raised = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1));
		DqResult latest = result("MAL_BST_706/MAL_MICRO_TESTED_505", DqOutcome.TICKET, TestUtils.date(2026, 11, 4));
		raised.setResult(latest);
		raised.setEmrValue(240.0);
		raised.setVariancePct(20.0);
		raised.setOpenedAt(TestUtils.date(2026, 11, 4));

		Assert.assertSame(open, DqServiceImpl.mergeInto(open, raised));
		Assert.assertSame(latest, open.getResult());
		Assert.assertEquals(Double.valueOf(240), open.getEmrValue());
		Assert.assertEquals(Double.valueOf(20), open.getVariancePct());
		Assert.assertEquals(DqTicketStatus.IN_PROGRESS, open.getStatus());
		Assert.assertEquals("Checking the lab register", open.getResolutionNote());
		Assert.assertEquals(TestUtils.date(2026, 11, 2), open.getOpenedAt());
	}

	@Test
	public void closeAsPassed_shouldResolveByNoOneAndKeepTheNote() {
		DqTicket ticket = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", TestUtils.date(2026, 10, 1));
		ticket.setStatus(DqTicketStatus.IN_PROGRESS);
		ticket.setResolutionNote("Checking the lab register");

		DqServiceImpl.closeAsPassed(ticket, TestUtils.date(2026, 11, 4));

		Assert.assertEquals(DqTicketStatus.RESOLVED, ticket.getStatus());
		Assert.assertEquals(TestUtils.date(2026, 11, 4), ticket.getResolvedAt());
		Assert.assertNull(ticket.getResolvedBy());
		Assert.assertEquals("Checking the lab register\nClosed automatically: the check passed when it was run again",
				ticket.getResolutionNote());

		DqTicket untouched = ticket("MALARIA", null, TestUtils.date(2026, 10, 1));
		DqServiceImpl.closeAsPassed(untouched, TestUtils.date(2026, 11, 4));
		Assert.assertEquals("Closed automatically: the check passed when it was run again", untouched.getResolutionNote());
	}

	@Test
	public void noLongerChecked_shouldFindTicketsOfRulesReportsAndPairsTakenOut() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		Date october = TestUtils.date(2026, 10, 1);

		DqTicket kept = ticket("MALARIA", "MAL_SUSPECTED_705/MAL_TESTS_706", october);
		kept.setResult(new DqResult());
		kept.getResult().setReportRun(run("MOH 706", october));
		Assert.assertNull(DqServiceImpl.noLongerChecked(kept, config));

		// A pair X-RPT-01 no longer compares
		DqTicket pair = ticket("MALARIA", "MAL_BST_706/MAL_MICRO_TESTED_505", october);
		Assert.assertEquals("MAL_BST_706 was removed from X-RPT-01", DqServiceImpl.noLongerChecked(pair, config));

		// A report no longer checked
		DqTicket report = ticket("MALARIA", "MAL_SUSPECTED_705/MAL_TESTS_706", october);
		report.setResult(new DqResult());
		report.getResult().setReportRun(run("MOH 505 IDSR Epidemic Case Monitoring", october));
		Assert.assertEquals("MOH 505 IDSR Epidemic Case Monitoring was removed from the data quality settings",
				DqServiceImpl.noLongerChecked(report, config));

		// A rule taken out
		DqTicket rule = ticket("MALARIA", "MAL_TESTED_705", october);
		rule.setRuleId("T22-02");
		Assert.assertEquals("rule T22-02 was removed from the data quality settings", DqServiceImpl.noLongerChecked(rule, config));

		// T22-05 is still checked, with MOH 643
		DqTicket reconciliation = ticket("MALARIA", "MAL_ACT_DISPENSED_643/MAL_TREATED_ACT_743", october);
		reconciliation.setRuleId("T22-05");
		reconciliation.setResult(new DqResult());
		reconciliation.getResult().setReportRun(run("MOH 643", october));
		Assert.assertNull(DqServiceImpl.noLongerChecked(reconciliation, config));
	}

	private static DqTicket ticket(String programCode, String indicatorCode, Date periodStart) {
		DqTicket ticket = new DqTicket();
		ticket.setProgramCode(programCode);
		ticket.setRuleId("X-RPT-01");
		ticket.setIndicatorCode(indicatorCode);
		ticket.setPeriodStart(periodStart);
		ticket.setStatus(DqTicketStatus.OPEN);
		return ticket;
	}

	private static DqResult result(String indicatorCode, DqOutcome outcome, Date evaluatedAt) {
		DqResult result = new DqResult();
		result.setRuleId("VF-01");
		result.setIndicatorCode(indicatorCode);
		result.setOutcome(outcome);
		result.setEvaluatedAt(evaluatedAt);
		return result;
	}

	private static DqResult baselineResult(String baseline, DqOutcome outcome) {
		DqResult result = result("MAL_CONFIRMED_705", outcome, TestUtils.date(2026, 11, 2));
		result.setRuleId(ConsistencyOverTimeRule.ID);
		result.setDetail("{\"baseline\":\"" + baseline + "\"}");
		return result;
	}

	private static DqReportRun run(String reportName, Date generatedAt) {
		DqReportRun run = new DqReportRun();
		run.setReportName(reportName);
		run.setGeneratedAt(generatedAt);
		return run;
	}
}
