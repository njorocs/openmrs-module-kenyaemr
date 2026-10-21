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

import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T22-07: the same patient with two positive malaria results within windowDays, where the second
 * falls in the period. Each such second result may be a duplicate episode counted twice.
 * <p>
 * The rule text's exception "unless the second episode is severe" is not complete yet, so every
 * pair is listed; the HRIO reviews them from the patient list. Patient-level detail is not stored:
 * the list is rebuilt with {@link #listPairs(DqRuleContext)} when someone opens it.
 * <p>
 * Settings: report (whose approval the warning attaches to), windowDays, severity, labTests,
 * positiveResults.
 */
public class DuplicateEpisodeRule extends AbstractDqRule {

	public static final String ID = "T22-07";

	public static final String DEFAULT_REPORT = "MOH-743 Report";

	public DuplicateEpisodeRule() {
		super(ID);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		return settings.isEnabled() && context.getRun() != null
				&& settings.getString("report", DEFAULT_REPORT).equals(context.getRun().getReportName());
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		int windowDays = settings.getInteger("windowDays", 14);

		List<Object[]> rows = context.query("select count(*) from (" + pairsSql(context) + ") pairs",
				parameters(context, settings));
		long pairs = rows.isEmpty() || rows.get(0)[0] == null ? 0 : ((Number) rows.get(0)[0]).longValue();

		DqResult result;
		if (pairs == 0) {
			result = result(context, null, DqOutcome.PASS,
					"No patient has two positive malaria results within " + windowDays + " days.");
		} else {
			result = result(context, null, settings.getOutcome("severity", DqOutcome.WARN), pairs + " positive malaria "
					+ (pairs == 1 ? "result follows" : "results follow") + " another positive result for the same patient within "
					+ windowDays + " days and may be counted twice. Review the patient list; the exception for severe "
					+ "second episodes is not applied yet.");
		}
		result.setMeasureValue((double) pairs);
		result.setThreshold("no repeat positive within " + windowDays + " days");

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("windowDays", windowDays);
		detail.put("labTests", MalariaLabSettings.labTests(settings));
		detail.put("positiveResults", MalariaLabSettings.positiveResults(settings));
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	/**
	 * @return rows of {patient_id, first positive date, repeat positive date} for the period
	 */
	public List<Object[]> listPairs(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		return context.query(pairsSql(context) + " order by second_date, patient_id", parameters(context, settings));
	}

	private static String pairsSql(DqRuleContext context) {
		String lab = context.getEtlDatabase() + ".etl_laboratory_extract";
		return "select b.patient_id, max(a.date_test_requested) as first_date, b.date_test_requested as second_date "
				+ "from " + lab + " a "
				+ "join " + lab + " b on b.patient_id = a.patient_id "
				+ "and b.date_test_requested > a.date_test_requested "
				+ "and b.date_test_requested <= date_add(a.date_test_requested, interval :windowDays day) "
				+ "where a.lab_test in (:labTests) and b.lab_test in (:labTests) "
				+ "and lower(a.result_name) in (:positiveResults) and lower(b.result_name) in (:positiveResults) "
				+ "and b.date_test_requested between :startDate and :endDate "
				+ "group by b.patient_id, b.date_test_requested";
	}

	private static Map<String, Object> parameters(DqRuleContext context, DqRuleSettings settings) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("windowDays", settings.getInteger("windowDays", 14));
		parameters.put("labTests", MalariaLabSettings.labTests(settings));
		parameters.put("positiveResults", MalariaLabSettings.positiveResults(settings));
		parameters.put("startDate", context.getPeriodStart());
		parameters.put("endDate", context.getPeriodEnd());
		return parameters;
	}
}
