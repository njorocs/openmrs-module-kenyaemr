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
import org.apache.commons.lang3.time.DateUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.reporting.MohReportUtils.DiagnosisLists;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.MOH705.MOH705CohortLibrary;

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
 * Rule type REPEAT_EPISODE (malaria T22-07): the same patient with two confirmed malaria episodes
 * within windowDays, where the second falls in the period.
 * <p>
 * An episode is what MOH 705A/B count as confirmed malaria, read with the report's own query
 * ({@link MOH705CohortLibrary#diagnosisVisitsSql}): a final diagnosis (dx_rank 2) from "diagnoses"
 * (concept ids) on a new visit in etl_clinical_encounter, on its visit date. The
 * report's day columns count each visit, so two episodes inside the month show on two days; the
 * total counts the patient once. A patient late in one month and again early in the next is
 * counted in both months. Both are listed; with "crossPeriodOnly": true only the second kind is.
 * <p>
 * Both episodes are visits of the report's indicator. A second episode that is severe is a new
 * episode, not a duplicate: it is not flagged when a diagnosis on that visit's encounter is severe.
 * A second episode that is not severe is flagged. Severe diagnoses are the concepts in
 * "severeConcepts" (ids), else those whose name matches "severeNamePatterns" (SQL LIKE, default
 * '%severe%malaria%' and '%malaria%severe%'). The first episode may be severe or not.
 * <p>
 * The warning attaches to the report that counts the patient: "reports" lists each report with its
 * age band and diagnoses, {report, minAgeYears, maxAgeYears, diagnoses} (age on the last day of the
 * period, with the report's {@link MOH705CohortLibrary#patientAgeSql}; max exclusive). Without it
 * the warning attaches to "report", for patients of any age. Without "diagnoses" the band uses the MOH 705B confirmed malaria list.
 * <p>
 * Patient-level detail is not stored: the list is rebuilt with {@link #listPairs(DqRuleContext)}
 * when someone opens it. Settings: reports or report, windowDays, crossPeriodOnly, severeConcepts,
 * severeNamePatterns, severity.
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
		boolean crossPeriodOnly = settings.getBoolean("crossPeriodOnly", false);

		List<Object[]> rows = context.query("select count(distinct patient_id) from (" + pairsSql(context, settings) + ") pairs",
				parameters(context, settings));
		long pairs = rows.isEmpty() || rows.get(0)[0] == null ? 0 : ((Number) rows.get(0)[0]).longValue();

		String which = crossPeriodOnly ? "counted with confirmed malaria last month and again within " + windowDays
				+ " days, so counted again this month"
				: "diagnosed with confirmed malaria again within " + windowDays + " days of an earlier episode";
		DqResult result;
		if (pairs == 0) {
			result = result(context, null, DqOutcome.PASS, "No patient was " + which + ".");
		} else {
			result = result(context, null, settings.getOutcome("severity", DqOutcome.WARN), pairs
					+ (pairs == 1 ? " patient was " : " patients were ") + which
					+ "; severe second episodes are left out. Review the patient list.");
		}
		result.setMeasureValue((double) pairs);
		result.setThreshold("no repeat confirmed malaria within " + windowDays + " days unless severe");

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
		detail.put("diagnoses", diagnoses(band));
		detail.put("severeConcepts", severeConcepts(settings));
		detail.put("severeNamePatterns", severeNamePatterns(settings));
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	/**
	 * @return rows of {patient_id, first episode date, repeat episode date} for the period
	 */
	public List<Object[]> listPairs(DqRuleContext context) {
		DqRuleSettings settings = settings();
		return context.query(pairsSql(context, settings) + " order by second_date, patient_id", parameters(context, settings));
	}

	private static String pairsSql(DqRuleContext context, DqRuleSettings settings) {
		String etl = context.getEtlDatabase();
		boolean byId = !severeConcepts(settings).isEmpty();
		String severe = byId ? "(:severeConcepts)"
				: "(select n.concept_id from concept_name n where n.voided = 0 and (" + namePredicates(settings) + "))";
		Map<String, Object> band = band(context, settings);
		List<Integer> diagnoses = diagnoses(band);
		Integer minAge = age(band, "minAgeYears");
		Integer maxAge = age(band, "maxAgeYears");
		return "select b.patient_id, max(a.visit_date) as first_date, b.visit_date as second_date "
				// Earlier episodes from windowDays before the period, the period's from its start
				+ "from " + episodesSql(etl, diagnoses, "windowStart", severe) + " a "
				+ "join " + episodesSql(etl, diagnoses, "startDate", severe) + " b on b.patient_id = a.patient_id "
				+ "and b.visit_date > a.visit_date "
				+ "and b.visit_date <= date_add(a.visit_date, interval :windowDays day) "
				// A severe second episode is a new episode
				+ "where b.severe = 0 "
				// The report's age on the last day of the period
				+ (minAge != null ? "and b.patient_id in (" + MOH705CohortLibrary.patientAgeSql(etl, ">=" + minAge) + ") " : "")
				+ (maxAge != null ? "and b.patient_id in (" + MOH705CohortLibrary.patientAgeSql(etl, "<" + maxAge) + ") " : "")
				+ (settings.getBoolean("crossPeriodOnly", false) ? "and a.visit_date < date(:startDate) " : "")
				+ "group by b.patient_id, b.visit_date";
	}

	/**
	 * The confirmed malaria visits MOH 705 counts, with its own query, from :startParameter to
	 * :endDate: one row per patient and visit date, severe (1) when a diagnosis on the visit's
	 * encounter is severe
	 */
	private static String episodesSql(String etl, List<Integer> diagnoses, String startParameter, String severe) {
		return "(select ce.patient_id, date(ce.visit_date) as visit_date, "
				+ "max(exists (select 1 from encounter_diagnosis s where s.encounter_id = ce.encounter_id "
				+ "and s.voided = 0 and s.diagnosis_coded in " + severe + ")) as severe "
				+ MOH705CohortLibrary.diagnosisVisitsSql(etl, diagnoses, startParameter, "endDate")
				+ " group by ce.patient_id, date(ce.visit_date))";
	}

	/**
	 * @return each report the rule checks, with its age band and the diagnoses it counts as an
	 * episode (the defaults filled in), for showing how the rule works
	 */
	public static List<Map<String, Object>> describeBands(DqRuleSettings settings) {
		List<Map<String, Object>> described = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> band : bands(settings)) {
			Map<String, Object> item = new LinkedHashMap<String, Object>();
			item.put("report", band.get("report"));
			item.put("minAgeYears", age(band, "minAgeYears"));
			item.put("maxAgeYears", age(band, "maxAgeYears"));
			item.put("diagnoses", diagnoses(band));
			described.add(item);
		}
		return described;
	}

	/**
	 * @return the severe diagnoses by id; empty when they are found by name (severeNamePatterns)
	 */
	public static List<Integer> severeConceptIds(DqRuleSettings settings) {
		return severeConcepts(settings);
	}

	/**
	 * @return the band's "diagnoses", else the MOH 705B confirmed malaria list
	 */
	static List<Integer> diagnoses(Map<String, Object> band) {
		List<Integer> diagnoses = new ArrayList<Integer>();
		Object value = band != null ? band.get("diagnoses") : null;
		if (value instanceof List) {
			for (Object item : (List<?>) value) {
				try {
					diagnoses.add(item instanceof Number ? ((Number) item).intValue() : Integer.valueOf(item.toString().trim()));
				}
				catch (NumberFormatException ignored) {
					// Not a concept id: skipped
				}
			}
		}
		return diagnoses.isEmpty() ? DiagnosisLists.getConfirmedMalariaList() : diagnoses;
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
		parameters.put("startDate", context.getPeriodStart());
		parameters.put("windowStart", DateUtils.addDays(context.getPeriodStart(), -settings.getInteger("windowDays", 14)));
		parameters.put("endDate", context.getPeriodEnd());
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
