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
import org.openmrs.module.kenyacore.test.TestUtils;

import java.util.Date;

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
}
