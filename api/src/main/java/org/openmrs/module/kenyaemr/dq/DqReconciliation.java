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
import org.openmrs.module.kenyaemr.reporting.MohReportUtils.MalariaLabResults;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares a report cell, patient by patient, with its source records, so the reviewer of an
 * anomaly can find the patient who was missed or counted wrongly. Both sides are counted now, for
 * the report's month: the cell by running the report's own definition for that column, the source
 * from the lab results in the ETL (etl_laboratory_extract): a malaria test with a result, or one
 * whose result is not negative. Each patient on one side only gets
 * the records that explain why, read from encounter_diagnosis and the ETL.
 */
public final class DqReconciliation {

	/** The diagnosis rank the MOH 705 reports count: the final diagnosis */
	static final int FINAL_DIAGNOSIS_RANK = 2;

	/** The visit category the MOH 705 reports count */
	static final String NEW_DIAGNOSIS = "New";

	/** Compared in lower case with etl_laboratory_extract.result_name; any other result is positive */
	static final List<String> NEGATIVE_RESULTS = MalariaLabResults.NEGATIVE;

	private final DqService service;

	private final String etl;

	public DqReconciliation(DqService service) {
		this(service, CommonMetadata.EmrDatabaseEtl.ETL_DATABASE);
	}

	DqReconciliation(DqService service, String etlDatabase) {
		this.service = service;
		this.etl = etlDatabase;
	}

	/**
	 * @param month any date in the month
	 */
	public Result reconcile(DqConfig.Reconciliation definition, Date month) {
		Date start = DqPeriods.monthStart(month);
		Date end = DqPeriods.lastDayOfMonth(month);
		Set<Integer> reported = new DqReportGenerator().evaluateCohort(definition.getReport(), definition.getDataset(),
				definition.getColumn(), start, end);
		if (reported == null) {
			throw new APIException(definition.getReport() + " / " + definition.getColumn() + " is not a patient count");
		}
		return compare(definition, start, end, reported);
	}

	/**
	 * The comparison once the cell's patients are known
	 */
	Result compare(DqConfig.Reconciliation definition, Date start, Date end, Set<Integer> reported) {
		Set<Integer> source = sourcePatients(definition, start, end);
		Set<Integer> missed = new TreeSet<Integer>(source);
		missed.removeAll(reported);
		Set<Integer> extra = new TreeSet<Integer>(reported);
		extra.removeAll(source);

		Set<Integer> checked = new TreeSet<Integer>(missed);
		checked.addAll(extra);
		Map<Integer, List<TestRecord>> tests = tests(definition, checked, start, end);
		Map<Integer, List<DiagnosisRecord>> diagnoses = diagnoses(checked, start, end);

		Result result = new Result(definition, start, reported.size(), source.size(), reported.size() - extra.size());
		for (Integer patientId : missed) {
			result.missed.add(explainMissed(definition, patientId, list(tests, patientId), list(diagnoses, patientId)));
		}
		for (Integer patientId : extra) {
			result.extra.add(explainExtra(definition, patientId, list(tests, patientId), list(diagnoses, patientId)));
		}
		return result;
	}

	/**
	 * A patient in the source records but not counted in the cell
	 */
	static Finding explainMissed(DqConfig.Reconciliation definition, Integer patientId, List<TestRecord> tests,
	                             List<DiagnosisRecord> diagnoses) {
		List<String> reasons = new ArrayList<String>();
		TestRecord test = firstTest(tests, isPositiveSource(definition));
		String testText = test != null ? test.describe() : "A malaria test";
		if (isPositiveSource(definition)) {
			List<DiagnosisRecord> malaria = new ArrayList<DiagnosisRecord>();
			for (DiagnosisRecord diagnosis : diagnoses) {
				if (diagnosis.isMalaria()) {
					malaria.add(diagnosis);
				}
			}
			if (malaria.isEmpty()) {
				reasons.add(testText + ", but no malaria diagnosis was recorded this month.");
			}
			for (DiagnosisRecord diagnosis : malaria) {
				reasons.add(whyNotCounted(definition, diagnosis));
			}
		} else {
			boolean visit = false;
			for (DiagnosisRecord diagnosis : diagnoses) {
				if (test != null && sameDay(diagnosis.date, test.date) && diagnosis.isCountedVisit()) {
					visit = true;
				}
			}
			if (!visit) {
				reasons.add(testText + ", but that day's visit has no new final diagnosis. The report counts a test only "
						+ "when the visit on the test day has one.");
			}
			for (DiagnosisRecord diagnosis : diagnoses) {
				if (test != null && sameDay(diagnosis.date, test.date) && !diagnosis.inEtl) {
					reasons.add("The visit of " + format(diagnosis.date) + " is not in the ETL tables yet.");
					break;
				}
			}
			if (reasons.isEmpty()) {
				reasons.add(testText + ", but it is not counted in the report.");
			}
		}
		return new Finding(patientId, reasons, tests, diagnoses);
	}

	/**
	 * A patient counted in the cell but not in the source records
	 */
	static Finding explainExtra(DqConfig.Reconciliation definition, Integer patientId, List<TestRecord> tests,
	                            List<DiagnosisRecord> diagnoses) {
		List<String> reasons = new ArrayList<String>();
		String what = isPositiveSource(definition) ? "Counted as confirmed malaria" : "Counted as tested for malaria";
		DiagnosisRecord counted = null;
		for (DiagnosisRecord diagnosis : diagnoses) {
			if (diagnosis.isMalaria() && diagnosis.isCountedVisit() && definition.getDiagnoses().contains(diagnosis.conceptId)) {
				counted = diagnosis;
				break;
			}
		}
		if (counted != null) {
			what += " (" + counted.name + " on " + format(counted.date) + ")";
		}
		if (tests.isEmpty()) {
			reasons.add(what + ", but no malaria test is recorded this month.");
		}
		for (TestRecord test : tests) {
			if (test.result == null || test.result.trim().isEmpty()) {
				reasons.add(what + ", but the " + test.name + " of " + format(test.date) + " has no result.");
			} else if (isPositiveSource(definition) && !test.isPositive()) {
				reasons.add(what + ", but the " + test.name + " of " + format(test.date) + " was " + test.result + ".");
			}
		}
		if (reasons.isEmpty()) {
			reasons.add(what + ", but no matching malaria test was found for this age group this month.");
		}
		return new Finding(patientId, reasons, tests, diagnoses);
	}

	private static String whyNotCounted(DqConfig.Reconciliation definition, DiagnosisRecord diagnosis) {
		String recorded = "'" + diagnosis.name + "' was recorded on " + format(diagnosis.date);
		if (!definition.getDiagnoses().isEmpty() && !definition.getDiagnoses().contains(diagnosis.conceptId)) {
			return recorded + ", but it is not one of the confirmed malaria diagnoses this report counts.";
		}
		if (!diagnosis.inEtl) {
			return recorded + ", but that visit is not in the ETL tables yet.";
		}
		if (diagnosis.rank == null || diagnosis.rank != FINAL_DIAGNOSIS_RANK) {
			return recorded + " with rank " + diagnosis.rank + "; the report counts only final diagnoses (rank "
					+ FINAL_DIAGNOSIS_RANK + ").";
		}
		if (!NEW_DIAGNOSIS.equals(diagnosis.category)) {
			return recorded + " on a visit whose diagnosis category is "
					+ (diagnosis.category != null ? "'" + diagnosis.category + "'" : "empty")
					+ "; the report counts only '" + NEW_DIAGNOSIS + "'.";
		}
		return recorded + ", but it is not counted in the report.";
	}

	private Set<Integer> sourcePatients(DqConfig.Reconciliation definition, Date start, Date end) {
		Map<String, Object> parameters = parameters(definition, start, end);
		StringBuilder sql = new StringBuilder("select distinct x.patient_id, 0 from ").append(etl)
				.append(".etl_laboratory_extract x inner join ").append(etl)
				.append(".etl_patient_demographics d on d.patient_id = x.patient_id and d.voided = 0 ")
				.append("where x.lab_test in (:labTests) and date(x.date_test_requested) between date(:startDate) and date(:endDate) ")
				.append("and trim(coalesce(x.test_result, '')) <> '' ");
		if (isPositiveSource(definition)) {
			sql.append("and x.result_name is not null and lower(trim(x.result_name)) not in (:negativeResults) ");
			parameters.put("negativeResults", NEGATIVE_RESULTS);
		}
		if (definition.getMinAgeYears() != null) {
			sql.append("and timestampdiff(YEAR, date(d.dob), date(:endDate)) >= :minAge ");
			parameters.put("minAge", definition.getMinAgeYears());
		}
		if (definition.getMaxAgeYears() != null) {
			sql.append("and timestampdiff(YEAR, date(d.dob), date(:endDate)) < :maxAge ");
			parameters.put("maxAge", definition.getMaxAgeYears());
		}
		Set<Integer> patients = new TreeSet<Integer>();
		for (Object[] row : service.executeSqlQuery(sql.toString(), parameters)) {
			patients.add(((Number) row[0]).intValue());
		}
		return patients;
	}

	private Map<Integer, List<TestRecord>> tests(DqConfig.Reconciliation definition, Collection<Integer> patients,
	                                            Date start, Date end) {
		Map<Integer, List<TestRecord>> tests = new HashMap<Integer, List<TestRecord>>();
		if (patients.isEmpty()) {
			return tests;
		}
		Map<String, Object> parameters = parameters(definition, start, end);
		parameters.put("patients", patients);
		String sql = "select x.patient_id, date(x.date_test_requested), coalesce(n.name, x.lab_test), x.result_name, x.test_result "
				+ "from " + etl + ".etl_laboratory_extract x "
				+ "left join concept_name n on n.concept_id = x.lab_test and n.locale = 'en' and n.concept_name_type = 'FULLY_SPECIFIED' and n.voided = 0 "
				+ "where x.patient_id in (:patients) and x.lab_test in (:labTests) "
				+ "and date(x.date_test_requested) between date(:startDate) and date(:endDate) "
				+ "order by x.patient_id, x.date_test_requested";
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			Integer patientId = ((Number) row[0]).intValue();
			// result_name is the result as read; a test ordered but not resulted has neither
			String result = row[3] != null ? row[3].toString() : null;
			list(tests, patientId).add(new TestRecord((Date) row[1], String.valueOf(row[2]), result));
		}
		return tests;
	}

	/**
	 * Every diagnosis of the month from encounter_diagnosis, with the ETL visit it belongs to, if
	 * any: the reports read the diagnosis from OpenMRS but the visit date and category from the ETL
	 */
	private Map<Integer, List<DiagnosisRecord>> diagnoses(Collection<Integer> patients, Date start, Date end) {
		Map<Integer, List<DiagnosisRecord>> diagnoses = new HashMap<Integer, List<DiagnosisRecord>>();
		if (patients.isEmpty()) {
			return diagnoses;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("patients", patients);
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		String sql = "select ed.patient_id, date(e.encounter_datetime), ed.diagnosis_coded, coalesce(n.name, ed.diagnosis_non_coded), "
				+ "ed.dx_rank, ed.certainty, ce.diagnosis_category, ce.encounter_id, "
				+ "(select count(*) from concept_name m where m.concept_id = ed.diagnosis_coded and m.voided = 0 and lower(m.name) like '%malaria%') "
				+ "from encounter_diagnosis ed "
				+ "inner join encounter e on e.encounter_id = ed.encounter_id and e.voided = 0 "
				+ "left join " + etl + ".etl_clinical_encounter ce on ce.encounter_id = ed.encounter_id and ce.voided = 0 "
				+ "left join concept_name n on n.concept_id = ed.diagnosis_coded and n.locale = 'en' and n.concept_name_type = 'FULLY_SPECIFIED' and n.voided = 0 "
				+ "where ed.voided = 0 and ed.patient_id in (:patients) "
				+ "and date(e.encounter_datetime) between date(:startDate) and date(:endDate) "
				+ "order by ed.patient_id, e.encounter_datetime";
		for (Object[] row : service.executeSqlQuery(sql, parameters)) {
			Integer patientId = ((Number) row[0]).intValue();
			list(diagnoses, patientId).add(new DiagnosisRecord((Date) row[1],
					row[2] != null ? ((Number) row[2]).intValue() : null, row[3] != null ? row[3].toString() : "",
					row[4] != null ? ((Number) row[4]).intValue() : null, row[5] != null ? row[5].toString() : null,
					row[6] != null ? row[6].toString() : null, row[7] != null,
					row[8] != null && ((Number) row[8]).intValue() > 0));
		}
		return diagnoses;
	}

	private static Map<String, Object> parameters(DqConfig.Reconciliation definition, Date start, Date end) {
		if (definition.getLabTests().isEmpty()) {
			throw new APIException("Reconciliation " + definition.getId() + " has no labTests");
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("labTests", definition.getLabTests());
		parameters.put("startDate", start);
		parameters.put("endDate", end);
		return parameters;
	}

	private static boolean isPositiveSource(DqConfig.Reconciliation definition) {
		return DqConfig.Reconciliation.SOURCE_POSITIVE.equals(definition.getSource());
	}

	private static TestRecord firstTest(List<TestRecord> tests, boolean positiveOnly) {
		for (TestRecord test : tests) {
			if (!positiveOnly || test.isPositive()) {
				return test;
			}
		}
		return tests.isEmpty() ? null : tests.get(0);
	}

	private static <T> List<T> list(Map<Integer, List<T>> map, Integer key) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<T>());
		}
		return map.get(key);
	}

	private static boolean sameDay(Date a, Date b) {
		return a != null && b != null && format(a).equals(format(b));
	}

	static String format(Date date) {
		return date != null ? new SimpleDateFormat("d MMM yyyy").format(date) : "an unknown date";
	}

	/**
	 * The comparison of one cell for one month
	 */
	public static class Result {

		private final DqConfig.Reconciliation definition;

		private final Date month;

		private final int reportedCount;

		private final int sourceCount;

		private final int inBoth;

		private final List<Finding> missed = new ArrayList<Finding>();

		private final List<Finding> extra = new ArrayList<Finding>();

		Result(DqConfig.Reconciliation definition, Date month, int reportedCount, int sourceCount, int inBoth) {
			this.definition = definition;
			this.month = month;
			this.reportedCount = reportedCount;
			this.sourceCount = sourceCount;
			this.inBoth = inBoth;
		}

		public DqConfig.Reconciliation getDefinition() {
			return definition;
		}

		public Date getMonth() {
			return month;
		}

		/** Patients the cell counts now */
		public int getReportedCount() {
			return reportedCount;
		}

		public int getSourceCount() {
			return sourceCount;
		}

		public int getInBoth() {
			return inBoth;
		}

		/** In the source records but not counted: possibly missed */
		public List<Finding> getMissed() {
			return missed;
		}

		/** Counted but not in the source records: possibly counted wrongly */
		public List<Finding> getExtra() {
			return extra;
		}
	}

	/**
	 * One patient on one side only, with the reasons found and the records behind them
	 */
	public static class Finding {

		private final Integer patientId;

		private final List<String> reasons;

		private final List<TestRecord> tests;

		private final List<DiagnosisRecord> diagnoses;

		Finding(Integer patientId, List<String> reasons, List<TestRecord> tests, List<DiagnosisRecord> diagnoses) {
			this.patientId = patientId;
			this.reasons = reasons;
			this.tests = tests;
			this.diagnoses = diagnoses;
		}

		public Integer getPatientId() {
			return patientId;
		}

		public List<String> getReasons() {
			return reasons;
		}

		public List<TestRecord> getTests() {
			return tests;
		}

		public List<DiagnosisRecord> getDiagnoses() {
			return diagnoses;
		}
	}

	/**
	 * A malaria test of the month, as the ETL holds it
	 */
	public static class TestRecord {

		final Date date;

		final String name;

		final String result;

		TestRecord(Date date, String name, String result) {
			this.date = date;
			this.name = name;
			this.result = result;
		}

		boolean isPositive() {
			return MalariaLabResults.isPositive(result);
		}

		String describe() {
			return name + " on " + format(date) + " was " + (result != null ? result : "without a result");
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("date", date);
			map.put("test", name);
			map.put("result", result);
			map.put("positive", isPositive());
			return map;
		}
	}

	/**
	 * A diagnosis of the month, with what the report needs from its ETL visit
	 */
	public static class DiagnosisRecord {

		final Date date;

		final Integer conceptId;

		final String name;

		final Integer rank;

		final String certainty;

		final String category;

		final boolean inEtl;

		final boolean malaria;

		DiagnosisRecord(Date date, Integer conceptId, String name, Integer rank, String certainty, String category,
		                boolean inEtl, boolean malaria) {
			this.date = date;
			this.conceptId = conceptId;
			this.name = name;
			this.rank = rank;
			this.certainty = certainty;
			this.category = category;
			this.inEtl = inEtl;
			this.malaria = malaria;
		}

		boolean isMalaria() {
			return malaria;
		}

		/** A final diagnosis on a new visit that the ETL holds: what the MOH 705 rows count */
		boolean isCountedVisit() {
			return inEtl && rank != null && rank == FINAL_DIAGNOSIS_RANK && NEW_DIAGNOSIS.equals(category);
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("date", date);
			map.put("diagnosis", name);
			map.put("rank", rank);
			map.put("certainty", certainty);
			map.put("category", category);
			map.put("inEtl", inEtl);
			map.put("malaria", malaria);
			return map;
		}
	}
}
