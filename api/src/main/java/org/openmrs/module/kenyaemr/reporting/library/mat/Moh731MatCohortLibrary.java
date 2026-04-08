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
				"    where t.voided = 0 \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberInducted");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Inducted");
		return cd;
	}

	public CohortDefinition matNumberInducted() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberInducted", ReportUtils.map(matAllNumberInducted(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberInducted");
		return cd;
	}

    /**
     * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT) during the current reporting period
     * Composed using matAllNumberInductedInReportingPeriod
     *
     * @return
     */

    public CohortDefinition matAllNumberInductedInReportingPeriod() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				" where t.voided = 0 and t.visit_date  BETWEEN \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
        SqlCohortDefinition cd = new SqlCohortDefinition();
        cd.setName("matNumberInductedInReportingPeriod");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("MAT Number Inducted");
        return cd;
    }

    public CohortDefinition matNumberInductedInReportingPeriod() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("matAllNumberInductedInReportingPeriod", ReportUtils.map(matAllNumberInductedInReportingPeriod(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("matAllNumberInductedInReportingPeriod");
        return cd;
    }

	public CohortDefinition matAllNumberOnMethadone() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter ce on t.patient_id = ce.patient_id \n" +
				"    where t.voided = 0 and ce.methadone_induction > 0 \n" +
				" and t.visit_date between date (:startDate) and date (:endDate)\n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberOnMethadone");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Methadone");
		return cd;
	}

	public CohortDefinition matNumberOnMethadone() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberOnMethadone", ReportUtils.map(matAllNumberOnMethadone(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberOnMethadone");
		return cd;
	}

	public CohortDefinition matAllNumberOnBuprenorphine() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter ce on t.patient_id = ce.patient_id \n" +
				"    where t.voided = 0 and ce.buprenorphine_induction > 0 \n" +
				" and t.visit_date between date (:startDate) and date (:endDate)\n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberOnBuprenorphine");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Buprenorphine");
		return cd;
	}

	public CohortDefinition matNumberOnBuprenorphine() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberOnBuprenorphine", ReportUtils.map(matAllNumberOnBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberOnBuprenorphine");
		return cd;
	}

	// in transit and on methadone
	public CohortDefinition matAllNumberOnMethadoneInTransit() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_transit tr on t.patient_id = tr.patient_id \n" +
				"    where t.voided = 0 and tr.on_transit='167060' and cle.methadone_induction > 0 \n" +
				" 	and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberOnMethadoneInTransit");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Methadone In Transit");
		return cd;
	}

	public CohortDefinition matNumberOnMethadoneInTransit() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberOnMethadoneInTransit", ReportUtils.map(matAllNumberOnMethadoneInTransit(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberOnMethadoneInTransit");
		return cd;
	}

	// in transit and on Buprenorphine
	public CohortDefinition matAllNumberOnBuprenorphineInTransit() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_transit tr on t.patient_id = tr.patient_id \n" +
				"    where t.voided = 0 and tr.on_transit='167060' and cle.buprenorphine_induction > 0 \n" +
				" 	and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberOnBuprenorphineInTransit");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Buprenorphine In Transit");
		return cd;
	}

	public CohortDefinition matNumberOnBuprenorphineInTransit() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberOnBuprenorphineInTransit", ReportUtils.map(matAllNumberOnBuprenorphineInTransit(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberOnBuprenorphineInTransit");
		return cd;
	}

	public CohortDefinition matAllNumberWeanedOffMethadone() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_cessation cess on t.patient_id = cess.patient_id \n" +
				"  where t.voided = 0 and cle.methadone_induction > 0 and cess.weaned_off_methadone ='1065' \n" +
				" and t.visit_date between date (:startDate) and date (:endDate)\n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberWeanedOffMethadone");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Weaned Off Methadone");
		return cd;
	}

	public CohortDefinition matNumberWeanedOffMethadone() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberWeanedOffMethadone", ReportUtils.map(matAllNumberWeanedOffMethadone(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberWeanedOffMethadone");
		return cd;
	}

	public CohortDefinition matAllNumberWeanedOffBuprenorphine() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_cessation cess on t.patient_id = cess.patient_id \n" +
				"  where t.voided = 0 and cle.buprenorphine_induction > 0 and cess.weaned_off_methadone ='1065' \n" +
				" and t.visit_date between date (:startDate) and date (:endDate)\n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberWeanedOffBuprenorphine");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Weaned Off Buprenorphine");
		return cd;
	}

	public CohortDefinition matNumberWeanedOffBuprenorphine() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberWeanedOffBuprenorphine", ReportUtils.map(matAllNumberWeanedOffBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberWeanedOffBuprenorphine");
		return cd;
	}

	public CohortDefinition matAllNumberExperienceOverdose() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"  where t.voided = 0 and cle.has_drug_use_history='1065' and cle.experienced_overdose ='1065' \n" +
				" and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberExperienceOverdose");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number experienced overdose");
		return cd;
	}

	public CohortDefinition matNumberExperienceOverdose() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberExperienceOverdose", ReportUtils.map(matAllNumberExperienceOverdose(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberExperienceOverdose");
		return cd;
	}

	public CohortDefinition matAllNumberReceivedInterventions() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_clinical_encounter cle on t.patient_id = cle.patient_id \n" +
				"  where t.voided = 0 and cle.has_drug_use_history='1065' and cle.experienced_overdose ='1065' \n" +
				" and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberReceivedInterventions");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number received Psychosocial Interventions");
		return cd;
	}

	public CohortDefinition matNumberReceivedInterventions() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberReceivedInterventions", ReportUtils.map(matAllNumberReceivedInterventions(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberReceivedInterventions");
		return cd;
	}


	public CohortDefinition matAllNumberSupportedWithReintegration() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps on t.patient_id = ps.patient_id \n" +
				"  where t.voided = 0 and ps.reintegrated_back='1065' \n" +
				" and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberSupportedWithReintegration");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number received Psychosocial Interventions");
		return cd;
	}

	public CohortDefinition matNumberSupportedWithReintegration() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberSupportedWithReintegration", ReportUtils.map(matAllNumberSupportedWithReintegration(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberSupportedWithReintegration");
		return cd;
	}

	public CohortDefinition matAllNumberExperienceViolence() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps on t.patient_id = ps.patient_id \n" +
				"  where t.voided = 0 and ps.type_of_gbv_experienced=:typeOfViolence \n" +
				" and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberExperienceViolence");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("typeOfViolence", "type Of Violence", String.class));
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number experienced Violence");
		return cd;
	}

	public CohortDefinition matNumberExperienceViolence() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("typeOfViolence", "type Of Violence", String.class));
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberExperienceViolence", ReportUtils.map(matAllNumberExperienceViolence(), "typeOfViolence=${typeOfViolence},startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberExperienceViolence");
		return cd;
	}

	public CohortDefinition matAllNumberReceivedViolenceSupport() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id \n" +
				"inner join kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps on t.patient_id = ps.patient_id \n" +
				"  where t.voided = 0 and ps.received_violence_support is not null \n" +
				" and t.visit_date between \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-01') \n" +
				" AND \n" +
				" DATE_FORMAT(CURDATE(), '%Y-%m-15') \n" +
				"    group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberExperienceViolence");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number received Violence support");
		return cd;
	}

	public CohortDefinition matNumberReceivedViolenceSupport() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberReceivedViolenceSupport", ReportUtils.map(matAllNumberReceivedViolenceSupport(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberReceivedViolenceSupport");
		return cd;
	}
}