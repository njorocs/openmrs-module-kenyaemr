/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.ETLReports.MOH705;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;

import java.util.Arrays;

/**
 * The MOH 705 diagnosis and age queries, which data quality rule T22-07 shares: the report's SQL
 * must stay as it was
 */
public class MOH705CohortLibraryTest {

	@Test
	public void patientDiagnosis_shouldKeepTheReportQuery() {
		String query = ((SqlCohortDefinition) new MOH705CohortLibrary().patientDiagnosis(Arrays.asList(2002636, 155686)))
				.getQuery();

		Assert.assertEquals("SELECT ce.patient_id\n"
				+ "FROM etl.etl_clinical_encounter ce\n"
				+ "         INNER JOIN etl.etl_patient_demographics p ON p.patient_id = ce.patient_id AND p.voided = 0\n"
				+ "         INNER JOIN encounter_diagnosis ed ON ed.patient_id = ce.patient_id AND ed.encounter_id = ce.encounter_id\n"
				+ "WHERE date(ce.visit_date) BETWEEN date(:startDate) AND date(:endDate)\n"
				+ "  AND ce.voided = 0\n"
				+ "  AND ed.dx_rank = 2\n"
				+ "  AND ed.diagnosis_coded in (2002636, 155686)\n"
				+ "  AND ce.diagnosis_category = 'New';", query.replace(etl() + ".", "etl."));
	}

	@Test
	public void patientAge_shouldKeepTheReportQuery() {
		String query = ((SqlCohortDefinition) new MOH705CohortLibrary().patientAge("<5")).getQuery();

		Assert.assertEquals("select d.patient_id from etl.etl_patient_demographics d "
				+ "where timestampdiff(YEAR, date(d.dob),date(:endDate)) <5;", query.replace(etl() + ".", "etl."));
	}

	private static String etl() {
		return org.openmrs.module.kenyaemr.metadata.CommonMetadata.EmrDatabaseEtl.ETL_DATABASE;
	}
}
