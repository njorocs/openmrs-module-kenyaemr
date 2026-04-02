/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.mat;

import org.openmrs.EncounterType;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.cohort.definition.CalculationCohortDefinition;
import org.openmrs.module.kenyaemr.calculation.library.MissedLastAppointmentCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.CtxFromAListOfMedicationOrdersCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.NextOfVisitHigherThanContextCalculation;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.library.shared.common.CommonCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.hiv.HivCohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.hiv.art.ArtCohortLibrary;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Library of cohort definitions used specifically in the MOH731 report
 */
@Component
public class Moh731MatCohortLibrary {
	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * Composed using matALLNumberInducted
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberInducted() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"    where t.voided = 0 and t.visit_date between date (:startDate) and date (:endDate)\n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberInducted");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Inducted");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * Composed using matAllNumberInducted
	 *
	 * @return
	 */
	public CohortDefinition matNumberInducted() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberInducted", ReportUtils.map(matAllNumberInducted(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberInducted");
		return cd;
	}

//	public CohortDefinition hivEnrollment(){
//		SqlCohortDefinition cd = new SqlCohortDefinition();
//		String sqlQuery = "select  e.patient_id " +
//				"from kenyaemr_etl.etl_hiv_enrollment e " +
//				"join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id " +
//				"where  e.entry_point <> 160563  and transfer_in_date is null " +
//				"and date(e.visit_date) between date(:startDate) and date(:endDate) and (e.patient_type not in (160563, 164931, 159833) or e.patient_type is null" +
//				";";
//		cd.setName("newHhivEnrollment");
//		cd.setQuery(sqlQuery);
//		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
//		cd.setDescription("New HIV Enrollment");
//
//		return cd;
//	}

}