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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The reasons a patient is in a report cell or its source records but not both
 */
public class DqReconciliationTest {

	private static final int COA = 2002649;

	private static final int MALARIA_UNSPECIFIED = 116128;

	@Test
	public void missed_shouldNameADiagnosisTheReportDoesNotCount() {
		DqReconciliation.Finding finding = DqReconciliation.explainMissed(confirmed(), 7,
				tests(test(1, "MPS Seen")), Collections.singletonList(diagnosis(1, MALARIA_UNSPECIFIED, "Malaria, unspecified", 2, "New", true)));

		Assert.assertEquals(Collections.singletonList("'Malaria, unspecified' was recorded on 1 Sep 2026, but it is not one of "
				+ "the confirmed malaria diagnoses this report counts."), finding.getReasons());
	}

	@Test
	public void missed_shouldSayWhenAPositiveTestHasNoMalariaDiagnosis() {
		DqReconciliation.Finding finding = DqReconciliation.explainMissed(confirmed(), 7,
				tests(test(1, "No Malaria Parasites"), test(6, "Trophozoites")),
				Collections.singletonList(diagnosis(1, 1219, "CRYPTOCOCCOSIS", 2, "New", true)));

		// The positive test is the one named, not the earlier negative
		Assert.assertEquals(Collections.singletonList("Blood smear on 6 Sep 2026 was Trophozoites, but no malaria diagnosis "
				+ "was recorded this month."), finding.getReasons());
	}

	@Test
	public void missed_shouldExplainRankCategoryAndEtl() {
		List<String> reasons = DqReconciliation.explainMissed(confirmed(), 7, tests(test(1, "MPS Seen")),
				Arrays.asList(diagnosis(1, COA, "Confirmed malaria", 1, "New", true),
						diagnosis(2, COA, "Confirmed malaria", 2, "Existing", true),
						diagnosis(3, COA, "Confirmed malaria", 2, null, false))).getReasons();

		Assert.assertTrue(reasons.get(0), reasons.get(0).endsWith("with rank 1; the report counts only final diagnoses (rank 2)."));
		Assert.assertTrue(reasons.get(1), reasons.get(1).endsWith("category is 'Existing'; the report counts only 'New'."));
		Assert.assertTrue(reasons.get(2), reasons.get(2).endsWith("but that visit is not in the ETL tables yet."));
	}

	@Test
	public void missedTest_shouldSayTheTestDayHasNoNewFinalDiagnosis() {
		DqReconciliation.Finding finding = DqReconciliation.explainMissed(tested(), 7, tests(test(4, "No Malaria Parasites")),
				Collections.singletonList(diagnosis(4, 1219, "CRYPTOCOCCOSIS", 1, "New", true)));

		Assert.assertEquals(1, finding.getReasons().size());
		Assert.assertTrue(finding.getReasons().get(0), finding.getReasons().get(0)
				.contains("but that day's visit has no new final diagnosis"));
	}

	@Test
	public void extra_shouldSayWhenConfirmedMalariaHasNoTestOrANegativeOne() {
		DqReconciliation.DiagnosisRecord counted = diagnosis(9, COA, "Confirmed malaria", 2, "New", true);

		Assert.assertEquals(Collections.singletonList("Counted as confirmed malaria (Confirmed malaria on 9 Sep 2026), but no "
				+ "malaria test is recorded this month."), DqReconciliation.explainExtra(confirmed(), 7,
				Collections.<DqReconciliation.TestRecord>emptyList(), Collections.singletonList(counted)).getReasons());
		Assert.assertEquals(Collections.singletonList("Counted as confirmed malaria (Confirmed malaria on 9 Sep 2026), but the "
				+ "Blood smear of 9 Sep 2026 was No Malaria Parasites."), DqReconciliation.explainExtra(confirmed(), 7,
				tests(test(9, "No Malaria Parasites")), Collections.singletonList(counted)).getReasons());
	}

	@Test
	public void testRecord_shouldTreatAnyResultButANegativeOneAsPositive() {
		Assert.assertTrue(test(1, "MPS Seen").isPositive());
		Assert.assertTrue(test(1, "Plasmodium falciparum").isPositive());
		Assert.assertTrue(test(1, "9600").isPositive());
		Assert.assertFalse(test(1, "No Malaria Parasites").isPositive());
		Assert.assertFalse(test(1, "Negative").isPositive());
		Assert.assertFalse(test(1, null).isPositive());
	}

	private static DqConfig.Reconciliation confirmed() {
		DqConfig.Reconciliation definition = new DqConfig.Reconciliation();
		definition.setId("705B-CONFIRMED");
		definition.setSource(DqConfig.Reconciliation.SOURCE_POSITIVE);
		definition.setMinAgeYears(5);
		definition.setLabTests(Arrays.asList(2017917, 1643, 2032282));
		definition.setDiagnoses(Collections.singletonList(COA));
		return definition;
	}

	private static DqConfig.Reconciliation tested() {
		DqConfig.Reconciliation definition = confirmed();
		definition.setId("705B-TESTED");
		definition.setSource(DqConfig.Reconciliation.SOURCE_TESTED);
		definition.setDiagnoses(Collections.<Integer>emptyList());
		return definition;
	}

	private static List<DqReconciliation.TestRecord> tests(DqReconciliation.TestRecord... tests) {
		return Arrays.asList(tests);
	}

	private static DqReconciliation.TestRecord test(int day, String result) {
		return new DqReconciliation.TestRecord(TestUtils.date(2026, 9, day), "Blood smear", result);
	}

	private static DqReconciliation.DiagnosisRecord diagnosis(int day, int conceptId, String name, Integer rank,
	                                                          String category, boolean inEtl) {
		return new DqReconciliation.DiagnosisRecord(TestUtils.date(2026, 9, day), conceptId, name, rank, "CONFIRMED", category,
				inEtl, name.toLowerCase().contains("malaria"));
	}
}
