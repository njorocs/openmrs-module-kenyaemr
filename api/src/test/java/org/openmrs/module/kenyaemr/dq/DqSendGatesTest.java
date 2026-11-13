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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * When a report generated on the reports page may be sent to KHIS
 */
public class DqSendGatesTest {

	private static final List<DqResult> NONE = Collections.emptyList();

	@Test
	public void isChecked_shouldCoverOnlyAWholeMonthThatHasEnded() {
		Assert.assertTrue(DqSendGates.isChecked(TestUtils.date(2026, 10, 1), TestUtils.date(2026, 10, 31, 23, 59, 59),
				TestUtils.date(2026, 11, 3)));
		// The month is not over
		Assert.assertFalse(DqSendGates.isChecked(TestUtils.date(2026, 11, 1), TestUtils.date(2026, 11, 30, 23, 59, 59),
				TestUtils.date(2026, 11, 3)));
		// A week
		Assert.assertFalse(DqSendGates.isChecked(TestUtils.date(2026, 10, 5), TestUtils.date(2026, 10, 11, 23, 59, 59),
				TestUtils.date(2026, 11, 3)));
	}

	@Test
	public void isCheckedWeek_shouldCoverOnlyAWholeWeekMondayToSundayThatHasEnded() {
		// Monday 26 October to Sunday 1 November 2026
		Assert.assertTrue(DqSendGates.isCheckedWeek(TestUtils.date(2026, 10, 26), TestUtils.date(2026, 11, 1, 23, 59, 59),
				TestUtils.date(2026, 11, 2)));
		// Sunday has not passed
		Assert.assertFalse(DqSendGates.isCheckedWeek(TestUtils.date(2026, 10, 26), TestUtils.date(2026, 11, 1),
				TestUtils.date(2026, 11, 1, 18, 0, 0)));
		// Tuesday to Monday
		Assert.assertFalse(DqSendGates.isCheckedWeek(TestUtils.date(2026, 10, 27), TestUtils.date(2026, 11, 2),
				TestUtils.date(2026, 11, 10)));
	}

	@Test
	public void wrongPeriod_shouldHoldAReportSentWeeklyAndSayWhy() {
		DqSendGate tenDays = DqSendGates.wrongPeriod("MOH 505", TestUtils.date(2026, 10, 1), TestUtils.date(2026, 10, 10),
				TestUtils.date(2026, 11, 3));
		DqSendGate thisWeek = DqSendGates.wrongPeriod("MOH 505", TestUtils.date(2026, 11, 2), TestUtils.date(2026, 11, 8),
				TestUtils.date(2026, 11, 3));

		Assert.assertEquals(DqSendGate.State.WRONG_PERIOD, tenDays.getState());
		Assert.assertFalse(tenDays.isSendable());
		Assert.assertTrue(tenDays.getMessage(), tenDays.getMessage().contains("a whole week (Monday to Sunday) or a whole month"));
		Assert.assertTrue(thisWeek.getMessage(), thisWeek.getMessage().startsWith("The period is not over yet"));
	}

	@Test
	public void gate_shouldWaitWhileTheChecksRunAndSayWhenTheyHaveNotRun() {
		Assert.assertEquals(DqSendGate.State.CHECKING, DqSendGates.gate(null, false, true, NONE).getState());
		Assert.assertEquals(DqSendGate.State.NOT_CHECKED, DqSendGates.gate(null, false, false, NONE).getState());
		// The same figures stored unchecked, by a backfill
		Assert.assertEquals(DqSendGate.State.NOT_CHECKED,
				DqSendGates.gate(run(DqRunStatus.GENERATED), true, false, NONE).getState());
		Assert.assertFalse(DqSendGates.gate(null, false, true, NONE).isSendable());
	}

	@Test
	public void gate_shouldHoldBackFiguresThatAreNotTheLatest() {
		DqSendGate gate = DqSendGates.gate(run(DqRunStatus.APPROVED), false, false, NONE);

		Assert.assertEquals(DqSendGate.State.NOT_LATEST, gate.getState());
		Assert.assertFalse(gate.isSendable());
	}

	@Test
	public void gate_shouldHoldBackABlockAndAnUnreviewedWarning() {
		DqSendGate blocked = DqSendGates.gate(run(DqRunStatus.BLOCKED), true, false,
				Arrays.asList(result("T22-01", DqOutcome.BLOCK, "Tested is more than suspected"), result("T22-03", DqOutcome.PASS, "")));
		DqSendGate review = DqSendGates.gate(run(DqRunStatus.AWAITING_APPROVAL), true, false,
				Arrays.asList(result("T22-04", DqOutcome.WARN, "Treated exceeds confirmed"), result("T22-03", DqOutcome.TICKET, "")));

		Assert.assertEquals(DqSendGate.State.BLOCKED, blocked.getState());
		Assert.assertEquals(Collections.singletonList("T22-01: Tested is more than suspected"), blocked.getProblems());
		Assert.assertEquals(DqSendGate.State.NEEDS_REVIEW, review.getState());
		Assert.assertEquals(Collections.singletonList("T22-04: Treated exceeds confirmed"), review.getProblems());
		Assert.assertFalse(review.isSendable());
	}

	@Test
	public void gate_shouldGroupTheChecksByWhatTheyNeed() {
		List<DqResult> results = Arrays.asList(result("T22-04", DqOutcome.BLOCK, "Treated exceeds confirmed"),
				result("X-RPT-01", DqOutcome.WARN, "Suspected is 23% below tests"),
				result("T22-03", DqOutcome.TICKET, "24 against 1 in Oct 2025"), result("T22-07", DqOutcome.PASS, ""));

		DqSendGate blocked = DqSendGates.gate(run(DqRunStatus.BLOCKED), true, false, results);
		DqSendGate approved = DqSendGates.gate(run(DqRunStatus.APPROVED), true, false, results.subList(1, 4));

		Assert.assertEquals(Collections.singletonList("T22-04: Treated exceeds confirmed"), blocked.getBlockers());
		Assert.assertEquals(Collections.singletonList("X-RPT-01: Suspected is 23% below tests"), blocked.getWarnings());
		Assert.assertEquals(Collections.singletonList("T22-03: 24 against 1 in Oct 2025"), blocked.getTickets());
		Assert.assertEquals("1 check blocks this report. Correct the records, refresh the ETL and generate the report again.",
				blocked.getMessage());
		// Approving answered the warnings; the ticket is still followed up
		Assert.assertTrue(approved.getWarnings().isEmpty());
		Assert.assertEquals(1, approved.getTickets().size());
		Assert.assertEquals("Checked and approved.", approved.getMessage());
	}

	@Test
	public void gate_shouldAskForTheReasonsWithoutNamingTheVersion() {
		DqSendGate review = DqSendGates.gate(run(DqRunStatus.AWAITING_APPROVAL), true, false,
				Collections.singletonList(result("X-RPT-01", DqOutcome.WARN, "")));

		Assert.assertEquals("1 warning needs a reason before sending. Give the reasons and approve the report on the Data "
				+ "quality page, then come back here to send.", review.getMessage());
	}

	@Test
	public void gate_shouldLetThroughAnApprovedVersionOrOneWithNothingToReview() {
		// A ticket is followed up separately and needs no reason
		Assert.assertEquals(DqSendGate.State.READY, DqSendGates.gate(run(DqRunStatus.AWAITING_APPROVAL), true, false,
				Collections.singletonList(result("T22-03", DqOutcome.TICKET, ""))).getState());
		Assert.assertEquals(DqSendGate.State.READY, DqSendGates.gate(run(DqRunStatus.APPROVED), true, false,
				Collections.singletonList(result("T22-04", DqOutcome.WARN, ""))).getState());
		Assert.assertTrue(DqSendGates.gate(run(DqRunStatus.APPROVED), true, false, NONE).isSendable());
	}

	private static DqReportRun run(DqRunStatus status) {
		DqReportRun run = new DqReportRun();
		run.setReportName("MOH 705B Outpatient summary");
		run.setVersion(2);
		run.setStatus(status);
		run.setGeneratedAt(TestUtils.date(2026, 11, 2, 16, 58, 59));
		return run;
	}

	private static DqResult result(String ruleId, DqOutcome outcome, String message) {
		DqResult result = new DqResult();
		result.setRuleId(ruleId);
		result.setOutcome(outcome);
		result.setMessage(message);
		return result;
	}
}
