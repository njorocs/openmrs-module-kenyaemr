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

import org.openmrs.module.kenyaemr.reporting.MohReportUtils.MalariaLabResults;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Which lab tests count as malaria tests, and which results count as negative, for the rules that
 * read etl_laboratory_extract. Any other recorded result is positive ({@link MalariaLabResults}). The
 * MOH 743 and MOH 706 queries use different smear concepts, so the default takes both; settings
 * "labTests" and "negativeResults" override them.
 */
final class MalariaLabSettings {

	/** RDT, blood smear (MOH 706), blood smear (MOH 743), PCR */
	static final List<Integer> DEFAULT_LAB_TESTS = Arrays.asList(1643, 2017901, 2017917, 2032282);

	/** Compared in lower case with etl_laboratory_extract.result_name */
	static final List<String> DEFAULT_NEGATIVE_RESULTS = MalariaLabResults.NEGATIVE;

	private MalariaLabSettings() {
	}

	static List<Integer> labTests(DqRuleSettings settings) {
		List<Integer> tests = new ArrayList<Integer>();
		for (Object item : settings.getList("labTests")) {
			if (item instanceof Number) {
				tests.add(((Number) item).intValue());
			} else if (item != null) {
				tests.add(Integer.valueOf(item.toString().trim()));
			}
		}
		return tests.isEmpty() ? DEFAULT_LAB_TESTS : tests;
	}

	static List<String> negativeResults(DqRuleSettings settings) {
		List<String> results = new ArrayList<String>();
		for (Object item : settings.getList("negativeResults")) {
			if (item != null) {
				results.add(item.toString().trim().toLowerCase());
			}
		}
		return results.isEmpty() ? DEFAULT_NEGATIVE_RESULTS : results;
	}
}
