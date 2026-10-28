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
import org.openmrs.module.kenyaemr.reporting.library.malaria.MalariaDrugs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule type RECORD_COMPLETENESS (mRDQA I.C, data element completeness): the share of the month's
 * malaria register entries ({@link MalariaRecords}) with none of the register's columns missing.
 * The columns, as in the mRDQA checklist: the patient's unique ID, temperature recorded at triage
 * that day, a diagnosis recorded that day, age (date of birth), sex, and for a positive test a
 * malaria treatment ordered within treatmentDays (default 3) of the test.
 * <p>
 * One result for the month: numerator the complete entries, denominator all entries, measure the
 * share, with each column's missing count in the detail. Settings: fields (default all six: id,
 * temperature, diagnosis, age, sex, treatment), min (default 0.9), treatmentDays, labTests,
 * positiveResults, checkAfterDay, severity (default WARN).
 */
public class RecordCompletenessRule extends AbstractDqRule {

	public static final String ID = "SRC-01";

	static final List<String> FIELDS = Arrays.asList("id", "temperature", "diagnosis", "age", "sex", "treatment");

	public RecordCompletenessRule(DqConfig.RuleDefinition definition) {
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
		List<String> fields = new ArrayList<String>(settings.getStrings("fields"));
		fields.retainAll(FIELDS);
		if (fields.isEmpty()) {
			fields = FIELDS;
		}
		double min = settings.getDouble("min", 0.9);
		Map<String, Object> parameters = MalariaRecords.parameters(context, settings);
		parameters.put("treatmentDays", settings.getInteger("treatmentDays", 3));
		parameters.put("treatmentDrugs", MalariaDrugs.TREATMENT);
		List<Object[]> rows = context.query(sql(context, fields), parameters);
		Object[] row = rows.isEmpty() ? new Object[0] : rows.get(0);

		long entries = row.length > 0 ? MalariaRecords.number(row[0]) : 0;
		long incomplete = row.length > 1 ? MalariaRecords.number(row[1]) : 0;
		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("entries", entries);
		Map<String, Object> missing = new LinkedHashMap<String, Object>();
		for (int i = 0; i < fields.size(); i++) {
			missing.put(fields.get(i), row.length > i + 2 ? MalariaRecords.number(row[i + 2]) : 0);
		}
		detail.put("missing", missing);

		DqResult result;
		if (entries == 0) {
			result = result(context, null, DqOutcome.NOT_APPLICABLE, "No malaria tests were recorded in the month.");
		} else {
			double share = (double) (entries - incomplete) / entries;
			boolean low = share < min - EPSILON;
			result = result(context, null, low ? settings.getOutcome("severity", DqOutcome.WARN) : DqOutcome.PASS,
					format((double) (entries - incomplete)) + " of " + format((double) entries) + " malaria register entries ("
							+ percent(share) + ") have no missing data" + (incomplete > 0 ? "; missing: " + describe(missing) : "")
							+ (low ? ". Below the " + percent(min) + " target." : "."));
			result.setMeasureValue(share);
		}
		result.setNumerator((double) (entries - incomplete));
		result.setDenominator((double) entries);
		result.setThreshold("at least " + percent(min) + " of entries complete");
		result.setDetail(json(detail));
		return Collections.singletonList(result);
	}

	private static String describe(Map<String, Object> missing) {
		List<String> parts = new ArrayList<String>();
		for (Map.Entry<String, Object> entry : missing.entrySet()) {
			if (MalariaRecords.number(entry.getValue()) > 0) {
				parts.add(entry.getKey() + " " + entry.getValue());
			}
		}
		return join(parts, ", ");
	}

	static String sql(DqRuleContext context, List<String> fields) {
		String etl = context.getEtlDatabase();
		Map<String, String> present = new LinkedHashMap<String, String>();
		present.put("id", "exists (select 1 from patient_identifier pi where pi.patient_id = e.patient_id and pi.voided = 0)");
		present.put("temperature", "exists (select 1 from " + etl + ".etl_patient_triage t where t.patient_id = e.patient_id "
				+ "and date(t.visit_date) = e.test_date and t.temperature is not null and (t.voided is null or t.voided = 0))");
		present.put("diagnosis", "exists (select 1 from encounter_diagnosis d where d.patient_id = e.patient_id and d.voided = 0 "
				+ "and date(d.date_created) between e.test_date and date_add(e.test_date, interval 1 day))");
		present.put("age", "exists (select 1 from person p where p.person_id = e.patient_id and p.birthdate is not null)");
		present.put("sex", "exists (select 1 from person p where p.person_id = e.patient_id and p.gender in ('M', 'F'))");
		present.put("treatment", "(e.positive = 0 or exists (select 1 from orders o join drug_order dor on dor.order_id = o.order_id "
				+ "where o.patient_id = e.patient_id and o.voided = 0 and dor.drug_inventory_id in (:treatmentDrugs) "
				+ "and date(o.date_activated) between e.test_date and date_add(e.test_date, interval :treatmentDays day)))");
		List<String> flags = new ArrayList<String>();
		List<String> missing = new ArrayList<String>();
		List<String> sums = new ArrayList<String>();
		for (String field : fields) {
			flags.add("case when " + present.get(field) + " then 1 else 0 end as has_" + field);
			missing.add("f.has_" + field + " = 0");
			sums.add("sum(1 - f.has_" + field + ")");
		}
		return "select count(*), sum(case when " + join(missing, " or ") + " then 1 else 0 end), " + join(sums, ", ")
				+ " from (select " + join(flags, ", ") + " from " + MalariaRecords.entriesSql(context) + " e) f";
	}

	private static String join(List<String> parts, String separator) {
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (sb.length() > 0) {
				sb.append(separator);
			}
			sb.append(part);
		}
		return sb.toString();
	}
}
