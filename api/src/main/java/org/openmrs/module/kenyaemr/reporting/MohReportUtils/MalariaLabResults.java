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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Which malaria lab results are positive, for the reports and the data quality checks alike. A
 * result is positive unless it is negative: smear results are recorded as "MPS Seen", the
 * species, the stage or the parasite density, so no list of positive spellings is complete.
 */
public final class MalariaLabResults {

	/** etl_laboratory_extract.result_name values that are negative, in lower case */
	public static final List<String> NEGATIVE = Collections.unmodifiableList(Arrays.asList("negative",
			"no malaria parasites", "no mps seen", "not detected", "0"));

	private MalariaLabResults() {
	}

	/**
	 * @return true when the result is recorded and is not negative
	 */
	public static boolean isPositive(String result) {
		return result != null && !result.trim().isEmpty() && !NEGATIVE.contains(result.trim().toLowerCase());
	}

	/**
	 * @return true when the result is one of the negative results
	 */
	public static boolean isNegative(String result) {
		return result != null && NEGATIVE.contains(result.trim().toLowerCase());
	}

	/**
	 * @param column a result column, e.g. x.result_name
	 * @return a SQL condition true when the column holds a positive result
	 */
	public static String positiveSql(String column) {
		return "(" + column + " is not null and trim(" + column + ") <> '' and lower(trim(" + column + ")) not in ("
				+ quotedNegatives() + "))";
	}

	/**
	 * @param column a result column, e.g. x.result_name
	 * @return a SQL condition true when the column holds a negative result
	 */
	public static String negativeSql(String column) {
		return "lower(trim(" + column + ")) in (" + quotedNegatives() + ")";
	}

	private static String quotedNegatives() {
		List<String> quoted = new ArrayList<String>();
		for (String negative : NEGATIVE) {
			quoted.add("'" + negative + "'");
		}
		return String.join(", ", quoted);
	}
}
