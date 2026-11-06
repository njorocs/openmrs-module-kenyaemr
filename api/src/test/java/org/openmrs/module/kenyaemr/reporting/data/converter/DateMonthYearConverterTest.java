/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyacore.test.TestUtils;

/**
 * Tests for {@link DateMonthYearConverter}
 */
public class DateMonthYearConverterTest {

	private final DateMonthYearConverter converter = new DateMonthYearConverter();

	@Test
	public void convert_shouldGiveTheMonthAndYearOfADate() {
		Assert.assertEquals("03/2024", converter.convert(TestUtils.date(2024, 3, 15)));
		// As the database returns it
		Assert.assertEquals("03/2024", converter.convert(java.sql.Date.valueOf("2024-03-15")));
	}

	@Test
	public void convert_shouldSayMissingWithoutADate() {
		Assert.assertEquals("Missing", converter.convert(null));
		// A text date, as the TB start date query returned before it was cast, is not cast to Obs
		Assert.assertEquals("Missing", converter.convert("2024-03-15"));
	}
}
