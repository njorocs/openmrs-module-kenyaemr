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

import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The EMR's malaria register for the source-document rules (mRDQA I.C and I.D): one entry per
 * patient and day with a malaria test (etl_laboratory_extract), positive when any of that day's
 * results is. Both rules are checked once the month's data has settled, from checkAfterDay
 * (default the 10th, the KHIS read-back day).
 */
final class MalariaRecords {

	static final int DEFAULT_CHECK_AFTER_DAY = 10;

	private MalariaRecords() {
	}

	/**
	 * @return the entries as a derived table with columns patient_id, test_date, positive and
	 * encounter_id
	 */
	static String entriesSql(DqRuleContext context) {
		return "(select x.patient_id, date(x.date_test_requested) as test_date, "
				+ "max(case when lower(x.result_name) in (:positiveResults) then 1 else 0 end) as positive, "
				+ "min(x.encounter_id) as encounter_id "
				+ "from " + context.getEtlDatabase() + ".etl_laboratory_extract x "
				+ "where x.lab_test in (:labTests) and date(x.date_test_requested) between date(:startDate) and date(:endDate) "
				+ "group by x.patient_id, date(x.date_test_requested))";
	}

	static Map<String, Object> parameters(DqRuleContext context, DqRuleSettings settings) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("labTests", MalariaLabSettings.labTests(settings));
		parameters.put("positiveResults", MalariaLabSettings.positiveResults(settings));
		parameters.put("startDate", context.getPeriodStart());
		parameters.put("endDate", context.getPeriodEnd());
		return parameters;
	}

	static Date checkAfter(DqRuleSettings settings, Date month) {
		return DqPeriods.addDays(DqPeriods.deadline(month, settings.getInteger("checkAfterDay", DEFAULT_CHECK_AFTER_DAY)), 1);
	}

	static long number(Object value) {
		return value instanceof Number ? ((Number) value).longValue() : 0;
	}
}
