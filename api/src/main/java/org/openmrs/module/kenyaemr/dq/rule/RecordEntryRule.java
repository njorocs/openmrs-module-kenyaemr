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

import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule type RECORD_ENTRY (mRDQA I.D, source documents up to date): the share of the month's
 * malaria register entries ({@link MalariaRecords}) entered in the EMR within maxDays (default 7)
 * of the visit, by the date the test's encounter was created. An entry whose encounter cannot be
 * found is not counted as up to date.
 * <p>
 * One result for the month: numerator the entries entered in time, denominator all entries,
 * measure the share. Settings: maxDays, min (default 0.9), labTests, positiveResults,
 * checkAfterDay, severity (default WARN).
 */
public class RecordEntryRule extends AbstractDqRule {

	public static final String ID = "SRC-02";

	public RecordEntryRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.MONTH;
	}

	@Override
	public Date checkAfter(DqConfig config, Date month) {
		return MalariaRecords.checkAfter(settings(), month);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		int maxDays = settings.getInteger("maxDays", 7);
		double min = settings.getDouble("min", 0.9);
		Map<String, Object> parameters = MalariaRecords.parameters(context, settings);
		parameters.put("maxDays", maxDays);
		List<Object[]> rows = context.query(sql(context), parameters);
		Object[] row = rows.isEmpty() ? new Object[0] : rows.get(0);
		long entries = row.length > 0 ? MalariaRecords.number(row[0]) : 0;
		long inTime = row.length > 1 ? MalariaRecords.number(row[1]) : 0;
		long noEncounter = row.length > 2 ? MalariaRecords.number(row[2]) : 0;

		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("entries", entries);
		detail.put("inTime", inTime);
		detail.put("noEncounter", noEncounter);
		detail.put("maxDays", maxDays);
		DqResult result;
		if (entries == 0) {
			result = result(context, null, DqOutcome.NOT_APPLICABLE, "No malaria tests were recorded in the month.");
		} else {
			double share = (double) inTime / entries;
			boolean low = share < min - EPSILON;
			result = result(context, null, low ? settings.getOutcome("severity", DqOutcome.WARN) : DqOutcome.PASS,
					format((double) inTime) + " of " + format((double) entries) + " malaria register entries (" + percent(share)
							+ ") were entered within " + maxDays + " days of the visit"
							+ (low ? "; below the " + percent(min) + " target." : "."));
			result.setMeasureValue(share);
		}
		result.setNumerator((double) inTime);
		result.setDenominator((double) entries);
		result.setThreshold("at least " + percent(min) + " entered within " + maxDays + " days");
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	static String sql(DqRuleContext context) {
		return "select count(*), "
				+ "sum(case when en.encounter_id is not null and datediff(en.date_created, en.encounter_datetime) <= :maxDays "
				+ "then 1 else 0 end), sum(case when en.encounter_id is null then 1 else 0 end) "
				+ "from " + MalariaRecords.entriesSql(context) + " e left join encounter en on en.encounter_id = e.encounter_id";
	}
}
