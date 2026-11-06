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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DqDispensingReconciliationTest {

	private static final Date START = TestUtils.date(2026, 10, 1);

	private static final Date END = TestUtils.date(2026, 10, 31);

	private static final int AL_6S = 1403;

	private static final int AL_24S = 4000;

	@Test
	public void compare_shouldExplainEachPatientOnOneSideOnly() {
		// MOH 743 counts 1, 2, 3 and 4; MOH 643 one course for 1, two for 4, and one for 5
		Set<Integer> given = new TreeSet<Integer>(Arrays.asList(1, 2, 3, 4));
		List<DqDispensingReconciliation.Dispensing> dispensed = Arrays.asList(
				dispensing(1, 11, TestUtils.date(2026, 10, 4), true),
				dispensing(4, 41, TestUtils.date(2026, 10, 9), true),
				dispensing(4, 42, TestUtils.date(2026, 10, 20), true),
				dispensing(5, null, TestUtils.date(2026, 10, 22), true));
		List<DqDispensingReconciliation.OrderRecord> orders = Arrays.asList(
				order(1, 11, AL_6S, TestUtils.date(2026, 10, 4)),
				order(2, 21, AL_24S, TestUtils.date(2026, 10, 6)),
				order(3, 31, AL_6S, TestUtils.date(2026, 10, 30)),
				order(4, 41, AL_6S, TestUtils.date(2026, 10, 9)),
				order(4, 42, AL_6S, TestUtils.date(2026, 10, 20)));
		Map<Integer, List<DqDispensingReconciliation.Dispensing>> ofOrders = new HashMap<Integer, List<DqDispensingReconciliation.Dispensing>>();
		// Patient 3's order was dispensed in November
		ofOrders.put(31, Collections.singletonList(dispensing(3, 31, TestUtils.date(2026, 11, 2), true)));
		Map<Integer, DqDispensingReconciliation.Visit> visits = new HashMap<Integer, DqDispensingReconciliation.Visit>();

		DqDispensingReconciliation.Result result = DqDispensingReconciliation.compare(START, END, given, dispensed, orders,
				new HashSet<Integer>(Collections.singletonList(AL_6S)), ofOrders, visits);

		// Courses are counted per order, or per day without one, as MOH 643 counts them
		Assert.assertEquals(4, result.getDispensedCourses());
		Assert.assertEquals(4, result.getGivenPatients());

		Assert.assertEquals(2, result.getNotDispensed().size());
		DqDispensingReconciliation.Finding noStockItem = result.getNotDispensed().get(0);
		Assert.assertEquals(Integer.valueOf(2), noStockItem.getPatientId());
		Assert.assertTrue(noStockItem.getReasons().get(0), noStockItem.getReasons().get(0).contains("no stock item for this drug"));
		DqDispensingReconciliation.Finding otherMonth = result.getNotDispensed().get(1);
		Assert.assertTrue(otherMonth.getReasons().get(0), otherMonth.getReasons().get(0).contains("in another month"));

		Assert.assertEquals(1, result.getNotCounted().size());
		Assert.assertEquals(Integer.valueOf(5), result.getNotCounted().get(0).getPatientId());
		Assert.assertTrue(result.getNotCounted().get(0).getReasons().get(0).contains("without an ACT order this month"));

		Assert.assertEquals(1, result.getMoreThanOneCourse().size());
		Assert.assertEquals(Integer.valueOf(4), result.getMoreThanOneCourse().get(0).getPatientId());
		Assert.assertEquals(4, result.getMoreThanOneCourse().get(0).getRecords().size());
	}

	@Test
	public void explainNotDispensed_shouldNameAStockItemNotLinkedToAnAct() {
		Map<Integer, List<DqDispensingReconciliation.Dispensing>> ofOrders = new HashMap<Integer, List<DqDispensingReconciliation.Dispensing>>();
		ofOrders.put(21, Collections.singletonList(new DqDispensingReconciliation.Dispensing(2, 21, "Coartem",
				TestUtils.date(2026, 10, 6), 24.0, false)));

		DqDispensingReconciliation.Finding finding = DqDispensingReconciliation.explainNotDispensed(2,
				Collections.singletonList(order(2, 21, AL_6S, TestUtils.date(2026, 10, 6))),
				new HashSet<Integer>(Collections.singletonList(AL_6S)), ofOrders, START, END);

		Assert.assertTrue(finding.getReasons().get(0), finding.getReasons().get(0).contains("\"Coartem\", a stock item not linked"));
		Assert.assertEquals(Arrays.asList("6 Oct 2026: ordered AL", "6 Oct 2026: dispensed Coartem × 24"), finding.getRecords());
	}

	@Test
	public void explainNotCounted_shouldSayWhatMoh743Needs() {
		List<DqDispensingReconciliation.OrderRecord> orders = Collections.singletonList(order(5, 51, AL_6S, TestUtils.date(2026, 10, 3)));
		List<DqDispensingReconciliation.Dispensing> dispensings = Collections.singletonList(dispensing(5, 51, TestUtils.date(2026, 10, 3), true));

		Assert.assertTrue(DqDispensingReconciliation.explainNotCounted(5, orders, dispensings,
				new DqDispensingReconciliation.Visit(false, null, null)).getReasons().get(0).contains("Not in the ETL"));
		Assert.assertTrue(DqDispensingReconciliation.explainNotCounted(5, orders, dispensings,
				new DqDispensingReconciliation.Visit(true, null, null)).getReasons().get(0).contains("No clinical visit"));
		Assert.assertEquals("No weight recorded at triage on 3 Oct 2026, the last visit of the month; MOH 743 counts a "
				+ "patient only in a weight band.", DqDispensingReconciliation.explainNotCounted(5, orders, dispensings,
				new DqDispensingReconciliation.Visit(true, TestUtils.date(2026, 10, 3), null)).getReasons().get(0));
		Assert.assertTrue(DqDispensingReconciliation.explainNotCounted(5, orders, dispensings,
				new DqDispensingReconciliation.Visit(true, TestUtils.date(2026, 10, 3), 4.5)).getReasons().get(0)
				.startsWith("Weight 4.5 kg on 3 Oct 2026"));
	}

	private static DqDispensingReconciliation.Dispensing dispensing(Integer patientId, Integer orderId, Date date, boolean act) {
		return new DqDispensingReconciliation.Dispensing(patientId, orderId, "AL", date, 6.0, act);
	}

	private static DqDispensingReconciliation.OrderRecord order(Integer patientId, Integer orderId, Integer drugId, Date date) {
		return new DqDispensingReconciliation.OrderRecord(patientId, orderId, drugId, "AL", date);
	}
}
