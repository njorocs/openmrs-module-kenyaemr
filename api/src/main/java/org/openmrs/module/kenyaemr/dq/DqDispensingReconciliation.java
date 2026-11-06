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

import org.openmrs.api.APIException;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.library.malaria.MalariaDrugs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The patients behind a T22-05 gap between ACT courses dispensed (MOH 643) and patients prescribed
 * an ACT (MOH 743), so each can be corrected in their chart or in the pharmacy. Both sides are counted
 * now, for the month:
 * <ul>
 * <li>prescribed an ACT: the MOH 743 cells of the "treated" indicator, run with the report's own
 * definition; MOH 743 counts a patient with an ACT order at a visit who has a weight at triage on
 * their last clinical visit of the month</li>
 * <li>dispensed: the ACT dispensing transactions in stock management, as MOH 643 counts them, one
 * course per patient and order (or per day, without an order)</li>
 * </ul>
 * A patient prescribed an ACT with nothing dispensed, one dispensed to but not counted as prescribed, and one
 * dispensed more than one course each get the records that explain why.
 */
public final class DqDispensingReconciliation {

	/** The value of a RECONCILIATION rule's "patients" setting this class answers */
	public static final String ACT_DISPENSING = "ACT_DISPENSING";

	private final DqService service;

	private final String etl;

	public DqDispensingReconciliation(DqService service) {
		this(service, CommonMetadata.EmrDatabaseEtl.ETL_DATABASE);
	}

	DqDispensingReconciliation(DqService service, String etlDatabase) {
		this.service = service;
		this.etl = etlDatabase;
	}

	/**
	 * @param treated the indicator of the patients given an ACT (MOH 743)
	 * @param month any date in the month
	 */
	public Result reconcile(DqConfig.Indicator treated, Date month) {
		Date start = DqPeriods.monthStart(month);
		Date end = DqPeriods.lastDayOfMonth(month);
		Set<Integer> given = new TreeSet<Integer>();
		DqReportGenerator generator = new DqReportGenerator();
		for (DqConfig.Part part : treated.getParts()) {
			Set<Integer> patients = generator.evaluateCohorts(part.getReport(), part.getDataset(), part.getColumns(), start, end);
			if (patients == null) {
				throw new APIException(treated.getCode() + " is not a patient count");
			}
			given.addAll(patients);
		}
		List<Dispensing> dispensed = dispensings(start, end);

		Set<Integer> patients = new TreeSet<Integer>(given);
		for (Dispensing dispensing : dispensed) {
			patients.add(dispensing.patientId);
		}
		List<OrderRecord> orders = orders(patients, start, end);
		Set<Integer> orderIds = new HashSet<Integer>();
		for (OrderRecord order : orders) {
			orderIds.add(order.orderId);
		}
		return compare(start, end, given, dispensed, orders, stockedDrugs(), dispensingsOf(orderIds), visits(patients, start, end));
	}

	/**
	 * The comparison once both sides and the records are known
	 *
	 * @param stocked the drugs that have a stock item
	 * @param ofOrders every dispensing of the month's orders, in any month and from any stock item
	 */
	static Result compare(Date start, Date end, Set<Integer> given, List<Dispensing> dispensed, List<OrderRecord> orders,
	                      Set<Integer> stocked, Map<Integer, List<Dispensing>> ofOrders, Map<Integer, Visit> visits) {
		Map<Integer, Set<String>> courses = new LinkedHashMap<Integer, Set<String>>();
		Map<Integer, List<Dispensing>> dispensedTo = new HashMap<Integer, List<Dispensing>>();
		int courseCount = 0;
		for (Dispensing dispensing : dispensed) {
			Set<String> patientCourses = courses.get(dispensing.patientId);
			if (patientCourses == null) {
				patientCourses = new LinkedHashSet<String>();
				courses.put(dispensing.patientId, patientCourses);
			}
			if (patientCourses.add(dispensing.course())) {
				courseCount++;
			}
			list(dispensedTo, dispensing.patientId).add(dispensing);
		}
		Map<Integer, List<OrderRecord>> ordersOf = new HashMap<Integer, List<OrderRecord>>();
		for (OrderRecord order : orders) {
			list(ordersOf, order.patientId).add(order);
		}

		Result result = new Result(start, courseCount, given.size());
		for (Integer patientId : given) {
			if (!courses.containsKey(patientId)) {
				result.notDispensed.add(explainNotDispensed(patientId, list(ordersOf, patientId), stocked, ofOrders, start, end));
			} else if (courses.get(patientId).size() > 1) {
				result.moreThanOneCourse.add(explainCourses(patientId, list(ordersOf, patientId), list(dispensedTo, patientId),
						courses.get(patientId).size()));
			}
		}
		for (Integer patientId : courses.keySet()) {
			if (!given.contains(patientId)) {
				result.notCounted.add(explainNotCounted(patientId, list(ordersOf, patientId), list(dispensedTo, patientId),
						visits.get(patientId)));
			}
		}
		return result;
	}

	/**
	 * Prescribed an ACT (counted in MOH 743) with no ACT dispensed this month
	 */
	static Finding explainNotDispensed(Integer patientId, List<OrderRecord> orders, Set<Integer> stocked,
	                                   Map<Integer, List<Dispensing>> ofOrders, Date start, Date end) {
		Finding finding = new Finding(patientId);
		for (OrderRecord order : orders) {
			finding.records.add(order.describe());
			List<Dispensing> elsewhere = ofOrders.get(order.orderId);
			Dispensing other = elsewhere != null && !elsewhere.isEmpty() ? elsewhere.get(0) : null;
			if (other != null && !other.act) {
				finding.records.add(other.describe());
				finding.reasons.add(order.drug + " ordered " + DqReconciliation.format(order.date) + " was dispensed from \""
						+ other.drug + "\", a stock item not linked to an ACT drug, so MOH 643 does not count it.");
			} else if (other != null && (other.date.before(start) || other.date.after(end))) {
				finding.records.add(other.describe());
				finding.reasons.add(order.drug + " ordered " + DqReconciliation.format(order.date) + " was dispensed on "
						+ DqReconciliation.format(other.date) + ", in another month.");
			} else if (order.drugId == null || !stocked.contains(order.drugId)) {
				finding.reasons.add(order.drug + " ordered " + DqReconciliation.format(order.date)
						+ ": there is no stock item for this drug in stock management, so its dispensing cannot be recorded.");
			} else {
				finding.reasons.add(order.drug + " ordered " + DqReconciliation.format(order.date)
						+ ": no dispensing was recorded in the pharmacy.");
			}
		}
		if (finding.reasons.isEmpty()) {
			finding.reasons.add("Counted as prescribed an ACT in MOH 743, but no ACT dispensing was recorded this month.");
		}
		return finding;
	}

	/**
	 * Dispensed an ACT (counted in MOH 643) but not counted as prescribed one in MOH 743
	 */
	static Finding explainNotCounted(Integer patientId, List<OrderRecord> orders, List<Dispensing> dispensings, Visit visit) {
		Finding finding = new Finding(patientId);
		for (OrderRecord order : orders) {
			finding.records.add(order.describe());
		}
		for (Dispensing dispensing : dispensings) {
			finding.records.add(dispensing.describe());
		}
		Dispensing first = dispensings.isEmpty() ? null : dispensings.get(0);
		String dispensed = first != null ? first.drug + " dispensed " + DqReconciliation.format(first.date) : "An ACT dispensed";
		if (orders.isEmpty()) {
			finding.reasons.add(dispensed + " without an ACT order this month; MOH 743 counts patients with an ACT order "
					+ "at a visit.");
		} else if (visit == null || !visit.inEtl) {
			finding.reasons.add("Not in the ETL patient list yet; refresh the ETL from the ETL administration page.");
		} else if (visit.lastVisit == null) {
			finding.reasons.add("No clinical visit in the ETL this month, so MOH 743 has no weight to place them in a "
					+ "weight band.");
		} else if (visit.weight == null) {
			finding.reasons.add("No weight recorded at triage on " + DqReconciliation.format(visit.lastVisit)
					+ ", the last visit of the month; MOH 743 counts a patient only in a weight band.");
		} else {
			finding.reasons.add("Weight " + formatNumber(visit.weight) + " kg on "
					+ DqReconciliation.format(visit.lastVisit) + ", but not counted in MOH 743.");
		}
		return finding;
	}

	/**
	 * In both, but dispensed more than one course: MOH 643 counts each course, MOH 743 each patient
	 */
	static Finding explainCourses(Integer patientId, List<OrderRecord> orders, List<Dispensing> dispensings, int courses) {
		Finding finding = new Finding(patientId);
		for (OrderRecord order : orders) {
			finding.records.add(order.describe());
		}
		for (Dispensing dispensing : dispensings) {
			finding.records.add(dispensing.describe());
		}
		finding.reasons.add(courses + " courses dispensed this month; MOH 643 counts each course and MOH 743 each patient "
				+ "once. Check that none was dispensed twice.");
		return finding;
	}

	/**
	 * The ACT dispensings of the month, as MOH 643 counts them (DQ_ACT_COURSES)
	 */
	private List<Dispensing> dispensings(Date start, Date end) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		String sql = "select coalesce(t.patient_id, o.patient_id), t.order_id, coalesce(d.name, concat('Stock item ', i.stock_item_id)), "
				+ "t.date_created, -t.quantity "
				+ "from stockmgmt_stock_item_transaction t "
				+ "inner join stockmgmt_stock_item i on i.stock_item_id = t.stock_item_id and i.voided = 0 "
				+ "and i.drug_id in " + MalariaDrugs.sql(MalariaDrugs.ACTS) + " "
				+ "left join orders o on o.order_id = t.order_id "
				+ "left join drug d on d.drug_id = i.drug_id "
				+ "where t.stock_operation_id is null and t.quantity < 0 "
				+ "and (t.patient_id is not null or t.order_id is not null) "
				+ "and date(t.date_created) between date(:startDate) and date(:endDate) "
				+ "order by t.date_created";
		List<Dispensing> dispensings = new ArrayList<Dispensing>();
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			if (row[0] == null) {
				continue;
			}
			dispensings.add(new Dispensing(((Number) row[0]).intValue(), row[1] != null ? ((Number) row[1]).intValue() : null,
					String.valueOf(row[2]), (Date) row[3], row[4] != null ? ((Number) row[4]).doubleValue() : null, true));
		}
		return dispensings;
	}

	/**
	 * The ACT orders of the month of these patients, as MOH 743 reads them
	 */
	private List<OrderRecord> orders(Collection<Integer> patients, Date start, Date end) {
		List<OrderRecord> orders = new ArrayList<OrderRecord>();
		if (patients.isEmpty()) {
			return orders;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("patients", patients);
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		String sql = "select o.patient_id, o.order_id, do.drug_inventory_id, coalesce(d.name, 'An ACT'), date(e.encounter_datetime) "
				+ "from drug_order do "
				+ "inner join orders o on o.order_id = do.order_id and o.voided = 0 "
				+ "inner join encounter e on e.encounter_id = o.encounter_id and e.voided = 0 "
				+ "left join drug d on d.drug_id = do.drug_inventory_id "
				+ "where do.drug_inventory_id in " + MalariaDrugs.sql(MalariaDrugs.ACTS) + " "
				+ "and o.patient_id in (:patients) "
				+ "and date(e.encounter_datetime) between date(:startDate) and date(:endDate) "
				+ "order by o.patient_id, e.encounter_datetime";
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			orders.add(new OrderRecord(((Number) row[0]).intValue(), ((Number) row[1]).intValue(),
					row[2] != null ? ((Number) row[2]).intValue() : null, String.valueOf(row[3]), (Date) row[4]));
		}
		return orders;
	}

	/**
	 * Every dispensing of these orders, in any month and from any stock item
	 */
	private Map<Integer, List<Dispensing>> dispensingsOf(Collection<Integer> orderIds) {
		Map<Integer, List<Dispensing>> dispensings = new HashMap<Integer, List<Dispensing>>();
		if (orderIds.isEmpty()) {
			return dispensings;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("orders", orderIds);
		String sql = "select coalesce(t.patient_id, o.patient_id), t.order_id, "
				+ "coalesce(d.name, n.name, concat('Stock item ', i.stock_item_id)), t.date_created, -t.quantity, "
				+ "case when i.drug_id in " + MalariaDrugs.sql(MalariaDrugs.ACTS) + " then 1 else 0 end "
				+ "from stockmgmt_stock_item_transaction t "
				+ "inner join stockmgmt_stock_item i on i.stock_item_id = t.stock_item_id "
				+ "left join orders o on o.order_id = t.order_id "
				+ "left join drug d on d.drug_id = i.drug_id "
				+ "left join concept_name n on n.concept_id = i.concept_id and n.locale = 'en' "
				+ "and n.concept_name_type = 'FULLY_SPECIFIED' and n.voided = 0 "
				+ "where t.order_id in (:orders) and t.stock_operation_id is null and t.quantity < 0 "
				+ "order by t.date_created";
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			Integer orderId = ((Number) row[1]).intValue();
			list(dispensings, orderId).add(new Dispensing(row[0] != null ? ((Number) row[0]).intValue() : null, orderId,
					String.valueOf(row[2]), (Date) row[3], row[4] != null ? ((Number) row[4]).doubleValue() : null,
					((Number) row[5]).intValue() == 1));
		}
		return dispensings;
	}

	/**
	 * @return the ACT drugs that have a stock item, so that their dispensing can be recorded
	 */
	private Set<Integer> stockedDrugs() {
		Set<Integer> drugs = new HashSet<Integer>();
		String sql = "select distinct i.drug_id, 0 from stockmgmt_stock_item i where i.voided = 0 and i.drug_id in "
				+ MalariaDrugs.sql(MalariaDrugs.ACTS);
		for (Object[] row : service.executeSqlQuery(sql, new HashMap<String, Object>())) {
			drugs.add(((Number) row[0]).intValue());
		}
		return drugs;
	}

	/**
	 * Each patient's last clinical visit of the month in the ETL and the weight at triage that day,
	 * which MOH 743 places them in a weight band by
	 */
	private Map<Integer, Visit> visits(Collection<Integer> patients, Date start, Date end) {
		Map<Integer, Visit> visits = new HashMap<Integer, Visit>();
		if (patients.isEmpty()) {
			return visits;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("patients", patients);
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		String sql = "select p.patient_id, a.visit_date, "
				+ "(select max(tr.weight) from " + etl + ".etl_patient_triage tr where tr.patient_id = p.patient_id "
				+ "and tr.voided = 0 and date(tr.visit_date) = date(a.visit_date)) "
				+ "from " + etl + ".etl_patient_demographics p "
				+ "left join (select ce.patient_id, max(ce.visit_date) as visit_date from " + etl + ".etl_clinical_encounter ce "
				+ "where ce.voided = 0 and ce.patient_id in (:patients) "
				+ "and date(ce.visit_date) between date(:startDate) and date(:endDate) group by ce.patient_id) a "
				+ "on a.patient_id = p.patient_id "
				+ "where p.voided = 0 and p.patient_id in (:patients)";
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			Integer patientId = ((Number) row[0]).intValue();
			visits.put(patientId, new Visit(true, (Date) row[1], row[2] != null ? ((Number) row[2]).doubleValue() : null));
		}
		for (Integer patientId : patients) {
			if (!visits.containsKey(patientId)) {
				visits.put(patientId, new Visit(false, null, null));
			}
		}
		return visits;
	}

	/**
	 * @return e.g. 24 or 12.5
	 */
	static String formatNumber(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

	private static <K, T> List<T> list(Map<K, List<T>> map, K key) {
		List<T> values = map.get(key);
		if (values == null) {
			values = new ArrayList<T>();
			map.put(key, values);
		}
		return values;
	}

	/**
	 * Both sides of one month, and the patients on one side only or dispensed more than once
	 */
	public static class Result {

		private final Date month;

		private final int dispensedCourses;

		private final int givenPatients;

		private final List<Finding> notDispensed = new ArrayList<Finding>();

		private final List<Finding> notCounted = new ArrayList<Finding>();

		private final List<Finding> moreThanOneCourse = new ArrayList<Finding>();

		Result(Date month, int dispensedCourses, int givenPatients) {
			this.month = month;
			this.dispensedCourses = dispensedCourses;
			this.givenPatients = givenPatients;
		}

		public Date getMonth() {
			return month;
		}

		/**
		 * @return the courses dispensed, as MOH 643 counts them now
		 */
		public int getDispensedCourses() {
			return dispensedCourses;
		}

		/**
		 * @return the patients prescribed an ACT, as MOH 743 counts them now
		 */
		public int getGivenPatients() {
			return givenPatients;
		}

		/**
		 * @return counted as prescribed an ACT in MOH 743, with no ACT dispensed this month
		 */
		public List<Finding> getNotDispensed() {
			return notDispensed;
		}

		/**
		 * @return dispensed an ACT this month, but not counted in MOH 743
		 */
		public List<Finding> getNotCounted() {
			return notCounted;
		}

		/**
		 * @return in both, but dispensed more than one course
		 */
		public List<Finding> getMoreThanOneCourse() {
			return moreThanOneCourse;
		}
	}

	/**
	 * One patient, with why and the orders and dispensings behind it
	 */
	public static class Finding {

		private final Integer patientId;

		private final List<String> reasons = new ArrayList<String>();

		private final List<String> records = new ArrayList<String>();

		Finding(Integer patientId) {
			this.patientId = patientId;
		}

		public Integer getPatientId() {
			return patientId;
		}

		public List<String> getReasons() {
			return reasons;
		}

		/**
		 * @return the month's ACT orders and dispensings, e.g. "4 Oct 2026: ordered AL 20/120 mg 6s"
		 */
		public List<String> getRecords() {
			return records;
		}
	}

	/**
	 * One dispensing transaction
	 */
	static class Dispensing {

		final Integer patientId;

		final Integer orderId;

		final String drug;

		final Date date;

		final Double quantity;

		/** False when the stock item is not linked to an ACT drug, so MOH 643 does not count it */
		final boolean act;

		Dispensing(Integer patientId, Integer orderId, String drug, Date date, Double quantity, boolean act) {
			this.patientId = patientId;
			this.orderId = orderId;
			this.drug = drug;
			this.date = date;
			this.quantity = quantity;
			this.act = act;
		}

		/**
		 * @return what MOH 643 counts as one course: the order, or the day without one
		 */
		String course() {
			return orderId != null ? "order " + orderId : "day " + DqReconciliation.format(date);
		}

		String describe() {
			return DqReconciliation.format(date) + ": dispensed " + drug
					+ (quantity != null ? " × " + formatNumber(quantity) : "");
		}
	}

	/**
	 * One ACT drug order
	 */
	static class OrderRecord {

		final Integer patientId;

		final Integer orderId;

		final Integer drugId;

		final String drug;

		final Date date;

		OrderRecord(Integer patientId, Integer orderId, Integer drugId, String drug, Date date) {
			this.patientId = patientId;
			this.orderId = orderId;
			this.drugId = drugId;
			this.drug = drug;
			this.date = date;
		}

		String describe() {
			return DqReconciliation.format(date) + ": ordered " + drug;
		}
	}

	/**
	 * A patient's last clinical visit of the month and the weight at triage that day
	 */
	static class Visit {

		/** False when the patient is not in the ETL's patient list */
		final boolean inEtl;

		final Date lastVisit;

		final Double weight;

		Visit(boolean inEtl, Date lastVisit, Double weight) {
			this.inEtl = inEtl;
			this.lastVisit = lastVisit;
			this.weight = weight;
		}
	}
}
