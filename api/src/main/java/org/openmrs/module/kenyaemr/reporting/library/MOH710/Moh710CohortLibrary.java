/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.MOH710;

import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Moh710CohortLibrary {

	/**
	 * Unique children given a vaccine dose on a specific calendar day of the month with age filter as-of that day
	 * @param minAge the minimum age in years
	 * @param maxAge the maximum age in years (exclusive, null for no upper limit)
	 * @param sequence the dose number in etl_immunization.sequence (null for any dose)
	 * @param vaccines the vaccine concept ids in etl_immunization.vaccine
	 * @return the cohort definition
	 */
	public CohortDefinition vaccinatedOnDay(Integer minAge, Integer maxAge, Integer sequence, Integer... vaccines) {
		return onDay(vaccineGiven(sequence, vaccines), "i.date_given", minAge, maxAge);
	}

	/**
	 * Unique children given a vaccine dose during the reporting period with age filter as-of vaccination date
	 * @param minAge the minimum age in years
	 * @param maxAge the maximum age in years (exclusive, null for no upper limit)
	 * @param sequence the dose number in etl_immunization.sequence (null for any dose)
	 * @param vaccines the vaccine concept ids in etl_immunization.vaccine
	 * @return the cohort definition
	 */
	public CohortDefinition vaccinatedTotal(Integer minAge, Integer maxAge, Integer sequence, Integer... vaccines) {
		return total(vaccineGiven(sequence, vaccines), "i.date_given", minAge, maxAge);
	}

	/**
	 * Unique children recorded as fully immunized at a visit on a specific calendar day of the month
	 */
	public CohortDefinition fullyImmunizedOnDay(Integer minAge, Integer maxAge) {
		return onDay("i.voided = 0 AND i.fully_immunized = 1065", "i.visit_date", minAge, maxAge);
	}

	/**
	 * Unique children recorded as fully immunized at a visit during the reporting period
	 */
	public CohortDefinition fullyImmunizedTotal(Integer minAge, Integer maxAge) {
		return total("i.voided = 0 AND i.fully_immunized = 1065", "i.visit_date", minAge, maxAge);
	}

	private String vaccineGiven(Integer sequence, Integer... vaccines) {
		StringBuilder sb = new StringBuilder("i.voided = 0 AND i.vaccine IN (");
		for (int k = 0; k < vaccines.length; k++) {
			sb.append(k > 0 ? "," : "").append(vaccines[k]);
		}
		sb.append(")");
		if (sequence != null) {
			sb.append(" AND i.sequence = ").append(sequence);
		}
		return sb.toString();
	}

	private CohortDefinition onDay(String condition, String dateColumn, Integer minAge, Integer maxAge) {
		SqlCohortDefinition cd = new SqlCohortDefinition();

		String sql = "SELECT i.patient_id\n" +
				"FROM "+CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_immunization i\n" +
				"INNER JOIN "+CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_demographics p ON p.patient_id = i.patient_id\n" +
				"INNER JOIN (\n" +
				"    SELECT DATE_ADD(\n" +
				"        DATE_SUB(DATE(:startDate), INTERVAL DAYOFMONTH(:startDate)-1 DAY),\n" +
				"        INTERVAL :dayOfMonth-1 DAY\n" +
				"    ) AS target_date\n" +
				") d_ref\n" +
				"WHERE " + condition + "\n" +
				"  AND DATE(" + dateColumn + ") = d_ref.target_date\n" +
				(minAge != null
						? "  AND TIMESTAMPDIFF(YEAR, p.dob, d_ref.target_date) >= " + minAge + "\n"
						: "") +
				(maxAge != null
						? "  AND TIMESTAMPDIFF(YEAR, p.dob, d_ref.target_date) < " + maxAge + "\n"
						: "") +
				"GROUP BY i.patient_id";

		cd.setQuery(sql);

		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addParameter(new Parameter("dayOfMonth", "Day of Month", Integer.class));

		return cd;
	}

	private CohortDefinition total(String condition, String dateColumn, Integer minAge, Integer maxAge) {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		String date = "DATE(" + dateColumn + ")";

		String sql = "SELECT i.patient_id\n" +
				"FROM " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_immunization i\n" +
				"INNER JOIN " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_patient_demographics p ON p.patient_id = i.patient_id\n" +
				"WHERE " + condition + "\n" +
				"  AND " + date + " BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
				(minAge != null
						? "  AND TIMESTAMPDIFF(YEAR, p.dob, " + date + ") >= " + minAge + "\n"
						: "") +
				(maxAge != null
						? "  AND TIMESTAMPDIFF(YEAR, p.dob, " + date + ") < " + maxAge + "\n"
						: "") +
				"GROUP BY i.patient_id";

		cd.setQuery(sql);

		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));

		return cd;
	}
}
