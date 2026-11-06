/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.MohReportUtils;

import org.junit.Assert;
import org.junit.Test;

/**
 * A malaria result is positive unless it is negative
 */
public class MalariaLabResultsTest {

	@Test
	public void isPositive_shouldAcceptEverySmearSpellingButANegativeOne() {
		for (String result : new String[] { "MPS Seen", "Plasmodium falciparum", "Trophozoites", "Schizonts", "9600", "Positive",
				"DETECTED" }) {
			Assert.assertTrue(result, MalariaLabResults.isPositive(result));
		}
		for (String result : new String[] { "No Malaria Parasites", "Negative", "NEGATIVE", " not detected ", "0", "", null }) {
			Assert.assertFalse(String.valueOf(result), MalariaLabResults.isPositive(result));
		}
	}

	@Test
	public void sql_shouldMatchTheSameResults() {
		Assert.assertEquals("(x.result_name is not null and trim(x.result_name) <> '' and lower(trim(x.result_name)) not in "
				+ "('negative', 'no malaria parasites', 'no mps seen', 'not detected', '0'))", MalariaLabResults.positiveSql("x.result_name"));
		Assert.assertEquals("lower(trim(x.result_name)) in ('negative', 'no malaria parasites', 'no mps seen', 'not detected', '0')",
				MalariaLabResults.negativeSql("x.result_name"));
	}
}
