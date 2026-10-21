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

/**
 * Tests for {@link DqPeriods}
 */
public class DqPeriodsTest {

	@Test
	public void monthBoundaries_shouldCoverTheWholeMonth() {
		Assert.assertEquals(TestUtils.date(2026, 2, 1), DqPeriods.monthStart(TestUtils.date(2026, 2, 17, 13, 5, 0)));
		Assert.assertEquals(TestUtils.date(2026, 2, 28), DqPeriods.lastDayOfMonth(TestUtils.date(2026, 2, 17)));
		Assert.assertEquals(TestUtils.date(2026, 2, 28, 23, 59, 59).getTime() + 999,
				DqPeriods.monthEnd(TestUtils.date(2026, 2, 17)).getTime());
		Assert.assertEquals(TestUtils.date(2025, 12, 1), DqPeriods.previousMonth(TestUtils.date(2026, 1, 1)));
	}

	@Test
	public void weekStart_shouldReturnMonday() {
		// 2026-10-21 is a Wednesday, 2026-10-25 a Sunday
		Assert.assertEquals(TestUtils.date(2026, 10, 19), DqPeriods.weekStart(TestUtils.date(2026, 10, 21)));
		Assert.assertEquals(TestUtils.date(2026, 10, 19), DqPeriods.weekStart(TestUtils.date(2026, 10, 25)));
		Assert.assertEquals(TestUtils.date(2026, 10, 19), DqPeriods.weekStart(TestUtils.date(2026, 10, 19)));
	}
}
