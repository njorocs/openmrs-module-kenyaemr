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

import java.util.Date;

/**
 * Whether a report reached KHIS by the 5th, and how many days late
 */
public class DqKhisTimelinessTest {

	private static final Date DEADLINE = TestUtils.date(2026, 11, 5);

	@Test
	public void judge_shouldCountTheWholeDeadlineDayAsOnTime() {
		DqKhisTimeliness.Entry entry = DqKhisTimeliness.judge("MOH 705A", DEADLINE, sent(TestUtils.date(2026, 11, 5, 9, 0, 0),
				TestUtils.date(2026, 11, 5, 23, 30, 0)), TestUtils.date(2026, 11, 20));
		Assert.assertEquals(DqKhisTimeliness.Status.ON_TIME, entry.getStatus());
		Assert.assertNull(entry.getDaysLate());
		Assert.assertNull(entry.getNote());
	}

	@Test
	public void judge_shouldGiveTheDaysLateOfAReportCompletedAfterTheDeadline() {
		// Sent on time, but marked complete only on the 6th and the 8th
		DqKhisTimeliness.Entry sixth = DqKhisTimeliness.judge("MOH 743", DEADLINE, sent(TestUtils.date(2026, 11, 4, 8, 0, 0),
				TestUtils.date(2026, 11, 6, 8, 0, 0)), TestUtils.date(2026, 11, 20));
		DqKhisTimeliness.Entry eighth = DqKhisTimeliness.judge("MOH 743", DEADLINE, sent(TestUtils.date(2026, 11, 8, 16, 0, 0),
				TestUtils.date(2026, 11, 8, 16, 0, 0)), TestUtils.date(2026, 11, 20));
		Assert.assertEquals(DqKhisTimeliness.Status.LATE, sixth.getStatus());
		Assert.assertEquals(Integer.valueOf(1), sixth.getDaysLate());
		Assert.assertNull(sixth.getNote());
		Assert.assertEquals(Integer.valueOf(3), eighth.getDaysLate());
	}

	@Test
	public void judge_shouldNotCountAReportSentButNotMarkedComplete() {
		DqKhisTimeliness.Entry entry = DqKhisTimeliness.judge("MOH 705A", DEADLINE, sent(TestUtils.date(2026, 11, 3, 9, 0, 0),
				null), TestUtils.date(2026, 11, 20));
		Assert.assertEquals(DqKhisTimeliness.Status.NOT_SUBMITTED, entry.getStatus());
		Assert.assertEquals(Integer.valueOf(15), entry.getDaysLate());
		Assert.assertEquals("Reached KHIS on 3 Nov 2026 but is not marked complete there", entry.getNote());
		Assert.assertNotNull(entry.getSubmittedAt());
	}

	@Test
	public void judge_shouldCountTheDaysOverdueOfAReportNotSubmitted() {
		DqKhisTimeliness.Entry entry = DqKhisTimeliness.judge("MOH 505", DEADLINE, null, TestUtils.date(2026, 11, 17, 10, 0, 0));

		Assert.assertEquals(DqKhisTimeliness.Status.NOT_SUBMITTED, entry.getStatus());
		Assert.assertEquals(Integer.valueOf(12), entry.getDaysLate());
	}

	@Test
	public void judge_shouldNotCountAReportDueLaterAsLate() {
		DqKhisTimeliness.Entry entry = DqKhisTimeliness.judge("MOH 711", DEADLINE, null, TestUtils.date(2026, 11, 3));

		Assert.assertEquals(DqKhisTimeliness.Status.NOT_DUE, entry.getStatus());
		Assert.assertNull(entry.getDaysLate());
	}

	@Test
	public void judge_shouldQuoteKhisOwnStatusForASendKhisRefused() {
		KhisSubmissionState refused = new KhisSubmissionState(null, null, false, "REJECTED",
				"{\"httpStatus\":\"Conflict\",\"status\":\"ERROR\",\"message\":\"Period is locked\"}");
		Assert.assertEquals("KHIS: ERROR, Period is locked", DqKhisTimeliness.judge("MOH 705A", DEADLINE, refused,
				TestUtils.date(2026, 11, 10)).getNote());
		Assert.assertEquals("Connection refused", KhisSubmissionState.khisWords("Connection refused"));
	}

	@Test
	public void judge_shouldSayWhyAReportWasNotSent() {
		KhisSubmissionState empty = new KhisSubmissionState(null, null, false, KhisSubmissionState.STATUS_NO_VALUES, null);

		Assert.assertEquals("Every value was zero, so it was not sent", DqKhisTimeliness.judge("MOH 643", DEADLINE, empty,
				TestUtils.date(2026, 11, 10)).getNote());
	}

	private static KhisSubmissionState sent(Date sentAt, Date completedAt) {
		return new KhisSubmissionState(sentAt, completedAt, false, "SENT", null);
	}
}
