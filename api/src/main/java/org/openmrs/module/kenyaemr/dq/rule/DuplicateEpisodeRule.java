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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule type REPEAT_EPISODE (malaria T22-07): the same patient with two positive malaria lab
 * results within windowDays, where the second falls in the period.
 * <p>
 * The MOH reports count each patient once a month, so two positives in the same month are already
 * one case. The duplicate the rule looks for is a patient positive late in one month and again
 * early in the next, within windowDays: they are counted in both months. With "crossPeriodOnly":
 * false, repeats inside the month are listed too.
 * <p>
 * A second episode that is severe is a new episode, not a duplicate: it is left out when the
 * patient has a severe malaria diagnosis from the day of the second test to the day after. Severe
 * diagnoses are the concepts in "severeConcepts" (ids), else those whose name matches
 * "severeNamePatterns" (SQL LIKE, default '%severe%malaria%' and '%malaria%severe%'), as an
 * encounter diagnosis or a coded observation.
 * <p>
 * Positive tests are counted in MOH 705A (under 5) and MOH 705B (5 and over), so the warning
 * attaches to the report that counts the patient: "reports" lists each report with its age band,
 * {report, minAgeYears, maxAgeYears} (age at the repeat test, max exclusive). Without it the
 * warning attaches to "report", for patients of any age.
 * <p>
 * Patient-level detail is not stored: the list is rebuilt with {@link #listPairs(DqRuleContext)}
 * when someone opens it. Settings: reports or report, windowDays, crossPeriodOnly, severeConcepts,
 * severeNamePatterns, severity, labTests, negativeResults.
 */
public class DuplicateEpisodeRule extends AbstractDqRule {

	public static final String ID = "T22-07";

	public static final String DEFAULT_REPORT = "MOH-743 Report";

	static final List<String> DEFAULT_SEVERE_NAME_PATTERNS = Arrays.asList("%severe%malaria%", "%malaria%severe%");

	public DuplicateEpisodeRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Set<String> getReports(DqConfig config) {
		Set<String> reports = new LinkedHashSet<String>();
		for (Map<String, Object> band : bands(settings())) {
			reports.add(String.valueOf(band.get("report")));
		}
		return reports;
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return band(context, settings()) != null;
	}

	/**
	 * @return the "reports" entries, else one entry for "report" with no age band
	 */
	static List<Map<String, Object>> bands(DqRuleSettings settings) {
		List<Map<String, Object>> bands = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> band : settings.getObjectList("reports")) {
			if (band.get("report") != null) {
				bands.add(band);
			}
		}
		if (bands.isEmpty()) {
			bands.add(Collections.<String, Object>singletonMap("report", settings.getString("report", DEFAULT_REPORT)));
		}
		return bands;
	}

	/**
	 * @return the age band of the report being checked, or null when the rule does not cover it
	 */
	static Map<String, Object> band(DqRuleContext context, DqRuleSettings settings) {
		if (context.getRun() == null) {
			return null;
		}
		for (Map<String, Object> band : bands(settings)) {
			if (context.getRun().getReportName().equals(band.get("report"))) {
				return band;
			}
		}
		return null;
	}

	private static Integer age(Map<String, Object> band, String key) {
		Object value = band != null ? band.get(key) : null;
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return value != null ? Integer.valueOf(value.toString().trim()) : null;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		int windowDays = settings.getInteger("windowDays", 14);
		boolean crossPeriodOnly = settings.getBoolean("crossPeriodOnly", true);

		List<Object[]> rows = context.query("select count(*) from (" + pairsSql(context, settings) + ") pairs",
				parameters(context, settings));
		long pairs = rows.isEmpty() || rows.get(0)[0] == null ? 0 : ((Number) rows.get(0)[0]).longValue();

		String which = crossPeriodOnly ? "counted last month and positive again within " + windowDays
				+ " days, so counted again this month"
				: "positive again within " + windowDays + " days of an earlier positive";
		DqResult result;
		if (pairs == 0) {
			result = result(context, null, DqOutcome.PASS, "No patient was " + which + ".");
		} else {
			result = result(context, null, settings.getOutcome("severity", DqOutcome.WARN), pairs
					+ (pairs == 1 ? " patient was " : " patients were ") + which
					+ "; severe second episodes are left out. Review the patient list.");
		}
		result.setMeasureValue((double) pairs);
		result.setThreshold("no repeat positive within " + windowDays + " days unless severe");

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		Map<String, Object> band = band(context, settings);
		if (age(band, "minAgeYears") != null) {
			detail.put("minAgeYears", age(band, "minAgeYears"));
		}
		if (age(band, "maxAgeYears") != null) {
			detail.put("maxAgeYears", age(band, "maxAgeYears"));
		}
		detail.put("windowDays", windowDays);
		detail.put("crossPeriodOnly", crossPeriodOnly);
		detail.put("labTests", MalariaLabSettings.labTests(settings));
		detail.put("negativeResults", MalariaLabSettings.negativeResults(settings));
		detail.put("severeConcepts", severeConcepts(settings));
		detail.put("severeNamePatterns", severeNamePatterns(settings));
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	/**
	 * @return rows of {patient_id, first positive date, repeat positive date} for the period
	 */
	public List<Object[]> listPairs(DqRuleContext context) {
		DqRuleSettings settings = settings();
		return context.query(pairsSql(context, settings) + " order by second_date, patient_id", parameters(context, settings));
	}

	private static String pairsSql(DqRuleContext context, DqRuleSettings settings) {
		String lab = context.getEtlDatabase() + ".etl_laboratory_extract";
		boolean byId = !severeConcepts(settings).isEmpty();
		String severe = byId ? "(:severeConcepts)"
				: "(select n.concept_id from concept_name n where n.voided = 0 and (" + namePredicates(settings) + "))";
		Map<String, Object> band = band(context, settings);
		boolean byAge = age(band, "minAgeYears") != null || age(band, "maxAgeYears") != null;
		String ageAtTest = "timestampdiff(year, dem.DOB, b.date_test_requested)";
		return "select b.patient_id, max(a.date_test_requested) as first_date, b.date_test_requested as second_date "
				+ "from " + lab + " a "
				+ "join " + lab + " b on b.patient_id = a.patient_id "
				+ "and b.date_test_requested > a.date_test_requested "
				+ "and b.date_test_requested <= date_add(a.date_test_requested, interval :windowDays day) "
				+ (byAge ? "join " + context.getEtlDatabase() + ".etl_patient_demographics dem on dem.patient_id = b.patient_id " : "")
				+ "where a.lab_test in (:labTests) and b.lab_test in (:labTests) "
				+ "and (a.result_name is not null and trim(a.result_name) <> '' and lower(trim(a.result_name)) not in (:negativeResults)) and (b.result_name is not null and trim(b.result_name) <> '' and lower(trim(b.result_name)) not in (:negativeResults)) "
				+ "and b.date_test_requested between :startDate and :endDate "
				+ (age(band, "minAgeYears") != null ? "and " + ageAtTest + " >= :minAgeYears " : "")
				+ (age(band, "maxAgeYears") != null ? "and " + ageAtTest + " < :maxAgeYears " : "")
				+ (settings.getBoolean("crossPeriodOnly", true) ? "and a.date_test_requested < :startDate " : "")
				+ "and not exists (select 1 from encounter_diagnosis d where d.patient_id = b.patient_id and d.voided = 0 "
				+ "and d.diagnosis_coded in " + severe + " "
				+ "and date(d.date_created) between date(b.date_test_requested) and date_add(date(b.date_test_requested), interval 1 day)) "
				+ "and not exists (select 1 from obs o where o.person_id = b.patient_id and o.voided = 0 "
				+ "and o.value_coded in " + severe + " "
				+ "and date(o.obs_datetime) between date(b.date_test_requested) and date_add(date(b.date_test_requested), interval 1 day)) "
				+ "group by b.patient_id, b.date_test_requested";
	}

	private static String namePredicates(DqRuleSettings settings) {
		List<String> predicates = new ArrayList<String>();
		for (int i = 0; i < severeNamePatterns(settings).size(); i++) {
			predicates.add("lower(n.name) like :severeName" + i);
		}
		return StringUtils.join(predicates, " or ");
	}

	private static Map<String, Object> parameters(DqRuleContext context, DqRuleSettings settings) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("windowDays", settings.getInteger("windowDays", 14));
		parameters.put("labTests", MalariaLabSettings.labTests(settings));
		parameters.put("negativeResults", MalariaLabSettings.negativeResults(settings));
		parameters.put("startDate", context.getPeriodStart());
		parameters.put("endDate", context.getPeriodEnd());
		Map<String, Object> band = band(context, settings);
		if (age(band, "minAgeYears") != null) {
			parameters.put("minAgeYears", age(band, "minAgeYears"));
		}
		if (age(band, "maxAgeYears") != null) {
			parameters.put("maxAgeYears", age(band, "maxAgeYears"));
		}
		List<Integer> concepts = severeConcepts(settings);
		if (!concepts.isEmpty()) {
			parameters.put("severeConcepts", concepts);
		} else {
			List<String> patterns = severeNamePatterns(settings);
			for (int i = 0; i < patterns.size(); i++) {
				parameters.put("severeName" + i, patterns.get(i));
			}
		}
		return parameters;
	}

	static List<Integer> severeConcepts(DqRuleSettings settings) {
		List<Integer> concepts = new ArrayList<Integer>();
		for (String item : settings.getStrings("severeConcepts")) {
			try {
				concepts.add(Integer.valueOf(item));
			}
			catch (NumberFormatException ignored) {
				// Not a concept id: skipped
			}
		}
		return concepts;
	}

	static List<String> severeNamePatterns(DqRuleSettings settings) {
		List<String> patterns = new ArrayList<String>();
		for (String pattern : settings.getStrings("severeNamePatterns")) {
			patterns.add(pattern.toLowerCase());
		}
		return patterns.isEmpty() ? DEFAULT_SEVERE_NAME_PATTERNS : patterns;
	}
}
