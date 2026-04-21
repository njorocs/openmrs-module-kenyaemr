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
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.RevisedDatim.DatimCohortLibrary;

import java.util.Date;

/**
 * Library of cohort definitions used specifically in the MOH731 report
 */
@Component
public class Moh731MatCohortLibrary {
	@Autowired
	private DatimCohortLibrary datimCohortLibrary;

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * Composed using matALLNumberInducted
	 *
	 * @return
	 */
	public CohortDefinition activeOnMAT() {
		String sqlQuery = "SELECT  x.patient_id FROM ( \n" +
				"    SELECT  \n" +
				"        t.patient_id, \n" +
				"        MAX(t.visit_date) AS last_visit, \n" +
				"        dc.last_discontinued \n" +
				"    FROM kenyaemr_etl.etl_mat_intial_registrations t \n" +
				"    INNER JOIN kenyaemr_etl.etl_patient_demographics d  \n" +
				"        ON d.patient_id = t.patient_id \n" +
				"    LEFT JOIN ( \n" +
				"        SELECT patient_id, MAX(visit_date) AS last_discontinued \n" +
				"        FROM kenyaemr_etl.etl_mat_discontinuation \n" +
				"        GROUP BY patient_id \n" +
				"    ) dc  \n" +
				"        ON dc.patient_id = t.patient_id \n" +
				"    WHERE t.voided = 0 \n" +
				"    GROUP BY t.patient_id \n" +
				") x \n" +
				" WHERE  \n" +
				"    x.last_discontinued IS NULL \n" +
				"    OR x.last_visit > x.last_discontinued;" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("activeOnMAT");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Active on MAT");
		return cd;
	}

	public CohortDefinition matNumberInducted() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("activeOnMAT", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("activeOnMAT");
		return cd;
	}


	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * Composed using matALLNumberInducted
	 *
	 * @return
	 */
	public CohortDefinition matClientsWeanedOff() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_mat_cessation cess on t.patient_id = cess.patient_id \n" +
				"  where t.voided = 0 and (cle.methadone_induction > 0 or cle.buprenorphine_induction > 0 ) \n" +
				" and cess.weaned_off_methadone ='1065' \n" +
				" and DATE(cess.visit_date) between date(:startDate) and date(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsWeanedOff");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Inducted and weaned off in reporting period");
		return cd;
	}

	public CohortDefinition matNumberWeanedOff() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsWeanedOff", ReportUtils.map(matClientsWeanedOff(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active and matClientsWeanedOff");
		return cd;
	}

    /**
     * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT) during the current reporting period
     * 1.1. MAT INDUCTION WITHIN THE REPORTING PERIOD
     *
     * @return
     */
    public CohortDefinition matClientsInductedInReportingPeriod() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				" where cle.voided = 0 and (cle.buprenorphine_induction > 0 or cle.methadone_induction > 0) \n" +
				" and DATE(cle.visit_date) between date(:startDate) and date(:endDate) \n" +
				" group by cle.patient_id;";
        SqlCohortDefinition cd = new SqlCohortDefinition();
        cd.setName("matClientsInductedInReportingPeriod");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("MAT Number Newly Inducted on Buprephone and methadone");
        return cd;
    }

    public CohortDefinition matNumberInductedInReportingPeriod() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsInductedInReportingPeriod", ReportUtils.map(matClientsInductedInReportingPeriod(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("active AND matClientsInductedInReportingPeriod");
        return cd;
    }

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.2 Currently on MAT Methadone
	 *
	 * @return
	 */
	public CohortDefinition matClientsOnMethadone() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"    where cle.voided = 0 and cle.methadone_induction > 0 \n" +
				" and DATE(cle.visit_date) between date(:startDate) and date(:endDate)\n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsOnMethadone");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsOnMethadone", ReportUtils.map(matClientsOnMethadone(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsOnMethadone");
		return cd;
	}
	// in transit and on methadone
	public CohortDefinition matClientsOnMethadoneInTransit() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_mat_transit tr on cle.patient_id = tr.patient_id \n" +
				"    where cle.voided = 0 and tr.on_transit='167060' and cle.methadone_induction > 0 \n" +
				" 	and DATE(tr.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsOnMethadoneInTransit");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsOnMethadoneInTransit", ReportUtils.map(matClientsOnMethadoneInTransit(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsOnMethadoneInTransit");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.3 Currently on MAT(Buprenorphine)
	 *
	 * @return
	 */
	public CohortDefinition matClientsOnBuprenorphine() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"    where cle.voided = 0 and cle.buprenorphine_induction > 0 \n" +
				" and DATE(cle.visit_date) between date(:startDate) and date(:endDate)\n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsOnBuprenorphine");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsOnBuprenorphine", ReportUtils.map(matClientsOnBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsOnBuprenorphine");
		return cd;
	}

	// in transit and on Buprenorphine
	public CohortDefinition matClientsOnBuprenorphineInTransit() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_mat_transit tr on cle.patient_id = tr.patient_id \n" +
				"    where cle.voided = 0 and tr.on_transit='167060' and cle.buprenorphine_induction > 0 \n" +
				" 	and DATE(tr.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsOnBuprenorphineInTransit");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsOnBuprenorphineInTransit", ReportUtils.map(matClientsOnBuprenorphineInTransit(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsOnBuprenorphineInTransit");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.4 WEANING OFF
	 *
	 * @return
	 */
	public CohortDefinition matClientsWeanedOffMethadone() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_mat_cessation cess on cle.patient_id = cess.patient_id \n" +
				"  where cle.voided = 0 and cle.methadone_induction > 0 and cess.weaned_off_methadone ='1065' \n" +
				" and DATE(cess.visit_date) between date (:startDate) and date (:endDate)\n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsWeanedOffMethadone");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsWeanedOffMethadone", ReportUtils.map(matClientsWeanedOffMethadone(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsWeanedOffMethadone");
		return cd;
	}

	public CohortDefinition matClientsWeanedOffBuprenorphine() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_mat_cessation cess on cle.patient_id = cess.patient_id \n" +
				"  where cle.voided = 0 and cle.buprenorphine_induction > 0 and cess.weaned_off_methadone ='1065' \n" +
				" and DATE(cess.visit_date) between date (:startDate) and date (:endDate)\n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsWeanedOffBuprenorphine");
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsWeanedOffBuprenorphine", ReportUtils.map(matClientsWeanedOffBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsWeanedOffBuprenorphine");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.5 MAT INTERUPTIONS
	 *
	 * @return
	 */
	public CohortDefinition matClientDiscontinued() {
		String sqlQuery = "select disc.patient_id from kenyaemr_etl.etl_mat_discontinuation disc \n" +
				"where disc.voided = 0  \n" +
				" and DATE(disc.visit_date) between date (:startDate) and date (:endDate)\n" +
				"    group by disc.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientDiscontinued");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Discontinued MAT");
		return cd;
	}

	public CohortDefinition matNumberDiscontinued() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsDiscontinued", ReportUtils.map(matClientDiscontinued(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsDiscontinued");
		return cd;
	}

	public CohortDefinition clientsDied() {
		String sqlQuery = "select d.patient_id from kenyaemr_etl.etl_patient_demographics d \n" +
				"where d.voided = 0 and d.dead=1  \n" +
				" and d.death_date between date (:startDate) and date (:endDate)\n" +
				"    group by d.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("clientsDied");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Discontinued MAT");
		return cd;
	}

	public CohortDefinition matNumberDied() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsDied", ReportUtils.map(clientsDied(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND clientsDied");
		return cd;
	}

	public CohortDefinition matClientsMissingDoses() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"where cle.voided = 0 and (cle.methadone_induction < 0 or cle.buprenorphine_induction < 0) \n" +
				" and DATE(cle.visit_date) between date (:startDate) and date (:endDate)\n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsMissingDoses");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number Discontinued MAT");
		return cd;
	}

	public CohortDefinition matNumberMissingDoses() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsMissingDoses", ReportUtils.map(matClientsMissingDoses(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsMissingDoses");
		return cd;
	}

	public CohortDefinition matClientsLTFU() {
		String sqlQuery = "select drug.patient_id from kenyaemr_etl.etl_drug_order drug  \n" +
				"where drug.voided = 0  \n" +
				"AND (drug.drug_name like '%Buprenorphine%'  \n" +
				"or drug.drug_name like '%Methadone%') \n" +
				"group by drug.patient_id \n" +
				"HAVING  \n" +
				"    MAX(drug.visit_date) < DATE((:startDate)) \n" +
				"    AND MAX(drug.visit_date) < (DATE((:endDate)) - INTERVAL 30 DAY); \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matClientsLTFU");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number LTFU");
		return cd;
	}

	public CohortDefinition matNumberLTFU() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matClientsLTFU", ReportUtils.map(matClientsLTFU(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matClientsLTFU");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.6 HIV TESTING
	 *
	 * @return
	 */
	public CohortDefinition numberTestedHIV() {
		String sqlQuery = "select ts.patient_id from kenyaemr_etl.etl_hts_test ts \n" +
				"  where ts.voided = 0 and ts.test_type = 1 \n" +
				" and ts.date_created between  date(:startDate) and date (:endDate) \n" +
				" group by ts.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("numberTestedHIV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number on Weaned Off Buprenorphine");
		return cd;
	}

	public CohortDefinition matNumberTestedHIV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("numberTestedHIV", ReportUtils.map(numberTestedHIV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND numberTestedHIV");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.7 MAT Clients New-HIV Positive
	 *
	 * @return
	 */
	public CohortDefinition numberHIVPositive() {
		String sqlQuery = "SELECT ht.patient_id \n" +
				"FROM kenyaemr_etl.etl_hts_test ht \n" +
				"WHERE  ht.final_test_result='Positive' and ht.test_type=2 \n" +
				"and DATE(ht.visit_date) BETWEEN date(:startDate) and date(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("numberHIVPositive");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT enrolled Clients HIV positive");
		return cd;
	}

	public CohortDefinition matNumberHIVpositive() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("numberHIVPositive", ReportUtils.map(numberHIVPositive(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND numberHIVPositive");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.8 Number of MAT Clients Started on ART both offsite & Onsite
	 *
	 * @return
	 */
	public CohortDefinition matNumberStartedART() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("startedOnART", ReportUtils.map(datimCohortLibrary.startedOnART(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("startedOnART AND active");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 1.9 Number of MAT clients with Known HIV Positive Status
	 *
	 * @return
	 */
	public CohortDefinition totalNumberHIVPositive() {
		String sqlQuery = "SELECT ht.patient_id \n" +
				"FROM kenyaemr_etl.etl_hts_test ht \n" +
				"WHERE  ht.final_test_result='Positive' and ht.test_type=2 \n" +
				"and DATE(ht.visit_date) <= date(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("totalNumberHIVPositive");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Total MAT enrolled Clients HIV positive");
		return cd;
	}

	public CohortDefinition matNumberTotalHIVpositive() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("totalNumberHIVPositive", ReportUtils.map(totalNumberHIVPositive(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND totalNumberHIVPositive");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.0 Total Number of MAT Clients Currently on ART both offsite and onsite
	 *
	 * @return
	 */
	public CohortDefinition matNumberTotalStartedART() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("startedOnART", ReportUtils.map(datimCohortLibrary.startedOnART(), "startDate=1900-01-01,endDate=2100-12-31"));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("startedOnART AND active");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.1 Viral load tracking MAT Clients
	 *
	 * @return
	 */
	public CohortDefinition validViralLoadResult() {
		String sqlQuery = "SELECT t.patient_id FROM kenyaemr_etl.etl_viral_load_validity_tracker t \n" +
				" WHERE t.vl_due_date >= DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("viralLoadResult");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Viral load result");
		return cd;
	}

	public CohortDefinition matNumberViralLoadResult() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("currentlyOnArt", ReportUtils.map(datimCohortLibrary.currentlyOnArt(), "startDate=1900-01-01,endDate=${endDate}"));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("validViralLoadResult", ReportUtils.map(validViralLoadResult(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("currentlyOnArt AND active AND validViralLoadResult");
		return cd;
	}

	public CohortDefinition validViralLoadResultLt200() {
		String sqlQuery = "SELECT t.patient_id FROM kenyaemr_etl.etl_viral_load_validity_tracker t \n" +
				" WHERE (vl_result < 200 or (vl_result = 1302 and lab_test = 1305)) \n" +
				" and t.vl_due_date >= DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("viralLoadResult");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Viral load result");
		return cd;
	}

	public CohortDefinition matNumberViralLoadResultLt200() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("currentlyOnArt", ReportUtils.map(datimCohortLibrary.currentlyOnArt(), "startDate=1900-01-01,endDate=${endDate}"));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("validViralLoadResultLt200", ReportUtils.map(validViralLoadResultLt200(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("currentlyOnArt AND active AND validViralLoadResultLt200");
		return cd;
	}

	public CohortDefinition validViralLoadResultLt50() {
		String sqlQuery = "SELECT t.patient_id FROM kenyaemr_etl.etl_viral_load_validity_tracker t \n" +
				" WHERE (vl_result < 50 or (vl_result = 1302 and lab_test = 1305)) \n" +
				" and t.vl_due_date >= DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("viralLoadResult");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Viral load result");
		return cd;
	}

	public CohortDefinition matNumberViralLoadResultLt50() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("currentlyOnArt", ReportUtils.map(datimCohortLibrary.currentlyOnArt(), "startDate=1900-01-01,endDate=${endDate}"));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("validViralLoadResultLt50", ReportUtils.map(validViralLoadResultLt50(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("currentlyOnArt AND active AND validViralLoadResultLt50");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.2 Overdose MAT Clients
	 *
	 * @return
	 */
//	public CohortDefinition validViralLoadResults() {
//		String sqlQuery = "SELECT t.patient_id FROM kenyaemr_etl.etl_viral_load_validity_tracker t WHERE t.vl_due_date >= DATE(:endDate)";
//		SqlCohortDefinition cd = new SqlCohortDefinition();
//		cd.setName("matOnARTWithValidViralLoadResults");
//		cd.setQuery(sqlQuery);
//		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
//		cd.setDescription("total number of MAT Clients living with HIV and on ART with a valid VL result");
//		return cd;
//	}
//
//	public CohortDefinition matViralLoadResults() {
//		CompositionCohortDefinition cd = new CompositionCohortDefinition();
//		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
//		cd.addSearch("txcurr", ReportUtils.map(datimCohortLibrary.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
//		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
//		cd.addSearch("validViralLoadResults", ReportUtils.map(validViralLoadResults(), "startDate=${startDate},endDate=${endDate}"));
//		cd.setCompositionString("txcurr AND active AND validViralLoadResults");
//
//		return cd;
//	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.2 Overdose MAT Clients
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberExperienceOverdose() {
		String sqlQuery = "SELECT cle.patient_id  \n" +
				"FROM kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"WHERE cle.has_drug_use_history = '1065'  \n" +
				"AND cle.experienced_overdose = '1065' \n" +
				"AND DATE(cle.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate);";
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberExperienceOverdose", ReportUtils.map(matAllNumberExperienceOverdose(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberExperienceOverdose");
		return cd;
	}

	public CohortDefinition matAllNumberReceivedNaloxone() {
		String sqlQuery = "select od.client_id from kenyaemr_etl.etl_overdose_reporting od  \n" +
				"where od.voided = 0  \n" +
				"and od.naloxone_provided='1065' \n" +
				" and  DATE(od.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by od.client_id;" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberReceivedNaloxone");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number received naloxone");
		return cd;
	}

	public CohortDefinition matNumberReceivedNaloxone() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberReceivedNaloxone", ReportUtils.map(matAllNumberReceivedNaloxone(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberReceivedNaloxone");
		return cd;
	}

	public CohortDefinition matAllNumberOverdoseDeath() {
		String sqlQuery = "SELECT d.patient_id \n" +
				"FROM kenyaemr_etl.etl_patient_demographics d \n" +
				"INNER JOIN openmrs.concept_name cn ON cn.concept_id = d.cause_of_death \n" +
				"AND cn.voided = 0 \n" +
				"AND cn.concept_name_type = 'FULLY_SPECIFIED' \n" +
				"AND cn.locale = 'en' \n" +
				"AND cn.locale_preferred = 1 \n" +
				"WHERE d.dead=1 AND LOWER(cn.name) LIKE '%overdose%' \n" +
				"AND d.death_date BETWEEN DATE(:startDate) AND DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberOverdoseDeaths");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number experienced overdose");
		return cd;
	}

	public CohortDefinition matNumberOverdoseDeaths() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberOverdoseDeath", ReportUtils.map(matAllNumberOverdoseDeath(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberOverdoseDeath");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.3 Psychosocial Interventions
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberReceivedInterventions() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"  where cle.voided = 0 and cle.psychosocial_support in (160050, 165163) \n" +
				" and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberReceivedInterventions", ReportUtils.map(matAllNumberReceivedInterventions(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberReceivedInterventions");
		return cd;
	}


	public CohortDefinition matAllNumberSupportedWithReintegration() {
		String sqlQuery = "select ps.patient_id from kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps \n" +
				"  where ps.voided = 0 and ps.reintegrated_back='1065' \n" +
				" and DATE(ps.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by ps.patient_id;";
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberSupportedWithReintegration", ReportUtils.map(matAllNumberSupportedWithReintegration(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberSupportedWithReintegration");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.4 Violence prevention and Support (REPORT CLIENTS ONLY ONCE IN EACH VIOLENCE TYPE)
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberExperienceViolence() {
		String sqlQuery = "select ps.patient_id from kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps \n" +
				"  where ps.voided = 0 and ps.type_of_gbv_experienced=:typeOfViolence \n" +
				" and DATE(ps.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by ps.patient_id;";
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberExperienceViolence", ReportUtils.map(matAllNumberExperienceViolence(), "typeOfViolence=${typeOfViolence},startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberExperienceViolence");
		return cd;
	}

	public CohortDefinition matAllNumberReceivedViolenceSupport() {
		String sqlQuery = "select ps.patient_id from kenyaemr_etl.etl_mat_psychosocial_intake_and_followup ps \n" +
				"  where ps.voided = 0 and ps.type_of_gbv_experienced is not null and ps.received_violence_support is not null \n" +
				" and DATE(ps.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by ps.patient_id;";
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
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberReceivedViolenceSupport", ReportUtils.map(matAllNumberReceivedViolenceSupport(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberReceivedViolenceSupport");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.5  Mental Health
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberScreenedMH() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"  where cle.voided = 0 and cle.is_suffering_mental_disorder != 1107  \n" +
				" and DATE(cle.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberScreenedMH");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened MH");
		return cd;
	}

	public CohortDefinition matNumberScreenedMH() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberScreenedMH", ReportUtils.map(matAllNumberScreenedMH(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberScreenedMH");
		return cd;
	}

	public CohortDefinition matAllNumberTreatedWithinFacilityMH() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"  where cle.voided = 0 and cle.treating_mental_disorder = 135795  \n" +
				" and DATE(cle.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberTreatedWithinFacilityMH");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number treated MH");
		return cd;
	}

	public CohortDefinition matNumberTreatedWithinFacilityMH() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberTreatedWithinFacilityMH", ReportUtils.map(matAllNumberTreatedWithinFacilityMH(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberTreatedWithinFacilityMH");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.6 STI  MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberScreenedSTI() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"  where cle.voided = 0 and cle.diagnosed_illnesses = 1065 and cle.has_disease_type= 165098 \n" +
				" and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberScreenedSTI");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened STI");
		return cd;
	}

	public CohortDefinition matNumberScreenedSTI() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberScreenedSTI", ReportUtils.map(matAllNumberScreenedSTI(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberScreenedSTI");
		return cd;
	}

	public CohortDefinition matAllNumberTreatedSTI() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"  where cle.voided = 0 and cle.diagnosed_illnesses = 1065 and cle.has_disease_type= 165098 \n" +
				"  AND cle.treated_disease is not null \n" +
				" and DATE(cle.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberTreatedSTI");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number treated STI");
		return cd;
	}

	public CohortDefinition matNumberTreatedSTI() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberTreatedSTI", ReportUtils.map(matAllNumberTreatedSTI(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberTreatedSTI");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.7 HCV (Hepatitis C) MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberScreenedHCV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle  \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_C_screened = '703'   \n" +
				"and lab.result_test_name='Hepatitis C viral antigen measurement' \n" +
				"and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberScreenedHCV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened HCV");
		return cd;
	}

	public CohortDefinition matNumberScreenedHCV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberScreenedHCV", ReportUtils.map(matAllNumberScreenedHCV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberScreenedHCV");
		return cd;
	}

	public CohortDefinition matAllNumberPositiveHCV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle  \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_C_screened = '703'   \n" +
				"and lab.result_test_name='Hepatitis C viral antigen measurement' and lab.test_result=1228 \n" +
				"and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberPositiveHCV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number positive HCV");
		return cd;
	}

	public CohortDefinition matNumberPositiveHCV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberPositiveHCV", ReportUtils.map(matAllNumberPositiveHCV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberPositiveHCV");
		return cd;
	}

	public CohortDefinition matNumberHCVConfirmatoryPCRtestPositive() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle  \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_C_screened = '703'   \n" +
				"and lab.set_member_conceptId=167786 and lab.test_result=1301 \n" +
				"and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberPositiveHCVConfirmatoryPCRtest");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number positive (Confirmatory PCR test)");
		return cd;
	}

	public CohortDefinition matNumberPositiveHCVConfirmatoryPCRtest() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("positiveHCV", ReportUtils.map(matAllNumberPositiveHCV(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matNumberHCVConfirmatoryPCRtestPositive", ReportUtils.map(matNumberHCVConfirmatoryPCRtestPositive(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND positiveHCV AND matNumberHCVConfirmatoryPCRtestPositive");
		return cd;
	}

	public CohortDefinition matAllNumberTreatedHCV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_C_treated = '1065'   \n" +
				"and lab.result_test_name='Hepatitis C viral antigen measurement' and lab.test_result=1228 \n" +
				"and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberTreatedHCV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number treated HCV");
		return cd;
	}

	public CohortDefinition matNumberTreatedHCV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberTreatedHCV", ReportUtils.map(matAllNumberTreatedHCV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberTreatedHCV");
		return cd;
	}

	public CohortDefinition matAllTotalNumberTreatedHCV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_C_treated = '1065'   \n" +
				"and lab.result_test_name='Hepatitis C viral antigen measurement' and lab.test_result=1228 \n" +
				"and DATE(cle.visit_date) <= DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matTotalNumberTreatedHCV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number ever treated HCV");
		return cd;
	}

	public CohortDefinition matTotalNumberTreatedHCV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllTotalNumberTreatedHCV", ReportUtils.map(matAllTotalNumberTreatedHCV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllTotalNumberTreatedHCV");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.8 HBV (Hepatitis B) MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberScreenedHBV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle   \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_B_screened = '703'   \n" +
				"and lab.result_test_name='Hepatitis B Surface Antigen Test' \n" +
				"and DATE(lab.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberScreenedHBV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened HBV");
		return cd;
	}

	public CohortDefinition matNumberScreenedHBV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberScreenedHBV", ReportUtils.map(matAllNumberScreenedHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberScreenedHBV");
		return cd;
	}

	public CohortDefinition matAllNumberNegativeHBV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle   \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_B_screened = '703'   \n" +
				"and lab.result_test_name='Hepatitis B Surface Antigen Test' and lab.test_result=664 \n" +
				"and DATE(lab.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberNegativeHBV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number negative HBV");
		return cd;
	}

	public CohortDefinition matNumberNegativeHBV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberNegativeHBV", ReportUtils.map(matAllNumberNegativeHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberNegativeHBV");
		return cd;
	}

	public CohortDefinition matAllNumberHBVvaccinated() {
		String sqlQuery = "select imm.patient_id from kenyaemr_etl.etl_immunization imm \n" +
				"where imm.DPT_Hep_B_Hib_1 != '' and imm.DPT_Hep_B_Hib_2 != '' and imm.DPT_Hep_B_Hib_3 != ''  \n" +
				"and imm.fully_immunized=1 \n" +
				"and DATE(imm.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by imm.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberNegativeHBVvaccinated");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number negative HBV");
		return cd;
	}

	public CohortDefinition matNumberNegativeHBVvaccinated() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("negativeHBV", ReportUtils.map(matAllNumberNegativeHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberHBVvaccinated", ReportUtils.map(matAllNumberHBVvaccinated(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND negativeHBV AND matAllNumberHBVvaccinated");
		return cd;
	}

	public CohortDefinition matAllNumberPositiveHBV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle  \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_B_screened = '703'   \n" +
				"and lab.result_test_name='Hepatitis B Surface Antigen Test' and lab.test_result=703 \n" +
				"and DATE(lab.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberPositiveHBV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number positive HBV");
		return cd;
	}

	public CohortDefinition matNumberPositiveHBV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberPositiveHBV", ReportUtils.map(matAllNumberPositiveHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberPositiveHBV");
		return cd;
	}

	public CohortDefinition matNumberHBVConfirmatoryPCRtestPositive() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle  \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"where cle.voided = 0 and cle.hepatitis_B_screened = '703'   \n" +
				"and lab.set_member_conceptId=2032394 and lab.test_result=1301\n" +
				"and DATE(lab.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberPositiveHBVConfirmatoryPCRtest");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number HBV positive (Confirmatory PCR test)");
		return cd;
	}

	public CohortDefinition matNumberPositiveHBVConfirmatoryPCRtest() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("positiveHBV", ReportUtils.map(matAllNumberPositiveHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matNumberHBVConfirmatoryPCRtestPositive", ReportUtils.map(matNumberHBVConfirmatoryPCRtestPositive(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND positiveHBV AND matNumberHBVConfirmatoryPCRtestPositive");
		return cd;
	}

	public CohortDefinition matAllNumberTreatedHBV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"  where cle.voided = 0 and cle.hepatitis_B_treated =1065 \n" +
				" and lab.result_test_name='Hepatitis B Surface Antigen Test' and lab.test_result=703 \n" +
				" and DATE(cle.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberTreatedHBV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number treated HBV");
		return cd;
	}

	public CohortDefinition matNumberTreatedHBV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberTreatedHBV", ReportUtils.map(matAllNumberTreatedHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active and matAllNumberTreatedHBV");
		return cd;
	}

	public CohortDefinition matAllTotalNumberTreatedHBV() {
		String sqlQuery = "select cle.patient_id from kenyaemr_etl.etl_mat_clinical_encounter cle \n" +
				"inner join kenyaemr_etl.etl_laboratory_extract lab on cle.patient_id = lab.patient_id  \n" +
				"  where cle.voided = 0 and cle.hepatitis_B_treated =1065 \n" +
				" and lab.result_test_name='Hepatitis B Surface Antigen Test' and lab.test_result=703 \n" +
				" and DATE(cle.visit_date) <= DATE(:endDate) \n" +
				"    group by cle.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matTotalNumberTreatedHBV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number ever treated HBV");
		return cd;
	}

	public CohortDefinition matTotalNumberTreatedHBV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllTotalNumberTreatedHBV", ReportUtils.map(matAllTotalNumberTreatedHBV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllTotalNumberTreatedHBV");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 2.9 TB  MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberScreenedTB() {
		String sqlQuery = "select tbs.patient_id from kenyaemr_etl.etl_tb_follow_up_visit tb \n" +
				"inner join kenyaemr_etl.etl_tb_screening tbs on tb.patient_id = tbs.patient_id  \n" +
				"where tbs.resulting_tb_status is not NULL or tbs.resulting_tb_status= 160737 \n" +
				"and DATE(tbs.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by tbs.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberScreenedTB");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened for TB");
		return cd;
	}

	public CohortDefinition matNumberScreenedTB() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberScreenedTB", ReportUtils.map(matAllNumberScreenedTB(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberScreenedTB");
		return cd;
	}

	public CohortDefinition clientsDiagnosedWithTB() {
		String sqlQuery = "select tbs.patient_id from kenyaemr_etl.etl_tb_follow_up_visit tb \n" +
				"inner join kenyaemr_etl.etl_tb_screening tbs on tb.patient_id = tbs.patient_id   \n" +
				"where tbs.resulting_tb_status=1662 \n" +
				"and DATE(tbs.visit_date) between DATE(:startDate) AND DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("clientsDiagnosedWithTB");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number screened for TB");
		return cd;
	}

	public CohortDefinition matNumberDiagnosedTB() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsDiagnosedWithTB", ReportUtils.map(clientsDiagnosedWithTB(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND clientsDiagnosedWithTB");
		return cd;
	}

	public CohortDefinition clientsStartedTBRX() {
		String sqlQuery = "select de.patient_id from kenyaemr_etl.etl_tb_enrollment tbe \n" +
				"inner join kenyaemr_etl.etl_drug_event de on tbe.patient_id = de.patient_id \n" +
				"where de.voided = 0 and de.program='TB' \n" +
				"and de.date_started BETWEEN DATE(:startDate) AND DATE(:endDate);" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("clientsStartedTBRX");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number started TB treatment");
		return cd;
	}

	public CohortDefinition matNumberStartedTBRX() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsStartedTBRX", ReportUtils.map(clientsStartedTBRX(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND clientsStartedTBRX");
		return cd;
	}

	public CohortDefinition clientsStartedTPT() {
		String sqlQuery ="select tpt.patient_id from kenyaemr_etl.etl_ipt_initiation tpt  \n" +
				"where tpt.voided = 0 \n" +
				"and DATE(tpt.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate);";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberStartedTPT");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number started TPT");
		return cd;
	}

	public CohortDefinition matNumberStartedTPT() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsStartedTPT", ReportUtils.map(clientsStartedTPT(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND clientsStartedTPT");
		return cd;
	}

	public CohortDefinition clientsTBEnrolled() {
		String sqlQuery ="SELECT tbe.patient_id \n" +
				"    FROM kenyaemr_etl.etl_tb_enrollment tbe \n" +
				"    WHERE DATE(tbe.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) \n" +
				"    GROUP BY tbe.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("clientsTBEnrolled");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Clients enrolled in TB");
		return cd;
	}
	public CohortDefinition clientsHivPositive() {
		String sqlQuery = "SELECT DISTINCT hiv.patient_id   \n" +
				"    FROM ( \n" +
				"        -- Already HIV+ before TB diagnosis \n" +
				"        SELECT hiv.patient_id \n" +
				"        FROM kenyaemr_etl.etl_hiv_enrollment hiv \n" +
				"        UNION \n" +
				"        -- Tested positive during reporting period \n" +
				"        SELECT ht.patient_id \n" +
				"        FROM kenyaemr_etl.etl_hts_test ht \n" +
				"        WHERE ht.final_test_result = 'Positive' \n" +
				"          AND ht.test_type = 2 \n" +
				"          AND DATE(ht.visit_date) BETWEEN  DATE(:startDate) AND DATE(:endDate)" +
				"	) hiv;" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("clientsHivPositive");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("Clients found HIV positive");
		return cd;
	}

	public CohortDefinition matTotalNumberTBclientsHIVpositive() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsTbDiagnosed", ReportUtils.map(clientsTBEnrolled(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsHivPositive", ReportUtils.map(clientsHivPositive(), "startDate=${startDate},endDate=${endDate}"));
//		cd.setCompositionString("active AND matAllTotalNumberTBclientsHIVpositive");
		cd.setCompositionString("active AND clientsTbDiagnosed AND clientsHivPositive");
		return cd;
	}

	public CohortDefinition matTotalNumberTBClientsOnHAART() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("txcurr", ReportUtils.map(datimCohortLibrary.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("clientsTBEnrolled", ReportUtils.map(clientsTBEnrolled(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND txcurr AND clientsTBEnrolled");

		return cd;
	}


	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 3.0 PrEP MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberInitiatedPrep() {
		String sqlQuery = "select prep.patient_id from kenyaemr_etl.etl_prep_enrolment prep \n" +
				"where prep.voided = 0  \n" +
				"and DATE(prep.visit_date) between DATE(:startDate) AND DATE(:endDate) \n" +
				"group by prep.patient_id;" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberInitiatedPrep");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number initiated to PREP");
		return cd;
	}

	public CohortDefinition matNumberInitiatedPrep() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberInitiatedPrep", ReportUtils.map(matAllNumberInitiatedPrep(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberInitiatedPrep");
		return cd;
	}

	public CohortDefinition matAllNumberHIVPositiveOnPrEP() {
		String sqlQuery = "select prep.patient_id from kenyaemr_etl.etl_prep_enrolment prep \n" +
				"inner join kenyaemr_etl.etl_hts_test ts on prep.patient_id = ts.patient_id  \n" +
				"where prep.voided = 0 and ts.final_test_result='Positive' and ts.test_type =1 \n" +
				"and DATE(ts.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"group by prep.patient_id; ";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matAllNumberHIVPositiveOnPrEP");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number that's HIV positive");
		return cd;
	}

	public CohortDefinition matNumberHIVPositiveOnPrEP() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberHIVPositiveOnPrEP", ReportUtils.map(matAllNumberHIVPositiveOnPrEP(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberHIVPositiveOnPrEP");
		return cd;
	}
	public CohortDefinition matAllNumberPrEPClientDiagnosedSTIs() {
		String sqlQuery = "select prep.patient_id  from kenyaemr_etl.etl_prep_enrolment prep  \n" +
				"inner join kenyaemr_etl.etl_prep_followup fo on prep.patient_id = fo.patient_id  \n" +
				"where prep.voided = 0 and fo.sti_screened='Yes' and  \n" +
				"(fo.genital_ulcer_disease='GUD' or vaginal_discharge='' or cervical_discharge='CD'  \n" +
				"or urethral_discharge='UD' or anal_discharge='AD') \n" +
				"and DATE(fo.visit_date) between  DATE(:startDate) AND DATE(:endDate) \n" +
				"group by prep.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberPrEPClientDiagnosedSTIs");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number that's PREP client and diagnosed with STIs");
		return cd;
	}

	public CohortDefinition matNumberPrEPClientDiagnosedSTIs() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberPrEPClientDiagnosedSTIs", ReportUtils.map(matAllNumberPrEPClientDiagnosedSTIs(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberPrEPClientDiagnosedSTIs");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 3.1 PEP MAT
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberExposedToHIV() {
		String sqlQuery = "select t.patient_id from kenyaemr_etl.etl_mat_intial_registrations t  \n" +
				" inner join kenyaemr_etl.etl_patient_demographics d on d.patient_id = t.patient_id  \n" +
				" inner join kenyaemr_etl.etl_pep_management_survivor pep on t.patient_id = pep.patient_id  \n" +
				" where t.voided = 0  \n" +
				" and date(t.visit_date) between date (:startDate) and date (:endDate) \n" +
				" group by t.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberExposedToHIV");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number exposed to HIV");
		return cd;
	}

	public CohortDefinition matNumberExposedToHIV() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberExposedToHIV", ReportUtils.map(matAllNumberExposedToHIV(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberExposedToHIV");
		return cd;
	}

	public CohortDefinition matAllNumberReceivePEPlt72hrs() {
		String sqlQuery = "select pep.patient_id from kenyaemr_etl.etl_pep_management_survivor pep \n" +
				"where pep.voided = 0 \n" +
				"and timestampdiff(HOUR, COALESCE(pep.incident_date, pep.incident_reporting_date, pep.visit_date), \n" +
				" COALESCE(pep.date_given_pep, pep.visit_date)) between 0 and 72 \n" +
				" AND COALESCE(pep.incident_date, pep.incident_reporting_date, pep.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) \n" +
				"and pep.given_pep=1065;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberReceivePEPlt72hrs");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number ever treated HBV");
		return cd;
	}

	public CohortDefinition matNumberReceivePEPlt72hrs() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberReceivePEPlt72hrs", ReportUtils.map(matAllNumberReceivePEPlt72hrs(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberReceivePEPlt72hrs");
		return cd;
	}

	/**
	 * MAT cohort includes those who were inducted on Medically Assisted Therapy (MAT)
	 * 4.0 Nutrition support
	 *
	 * @return
	 */
	public CohortDefinition matAllNumberMATClientsSAM() {
		String sqlQuery = "select nut.patient_id from \n" +
				"(SELECT sc.patient_id \n" +
				"FROM kenyaemr_etl.etl_special_clinics sc \n" +
				"where special_clinic='Nutrition' and nutritional_status in (1687,160033,1655) \n" +
				"and DATE(sc.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) \n" +
				"UNION \n" +
				"SELECT tr.patient_id \n" +
				"FROM kenyaemr_etl.etl_patient_triage tr \n" +
				"WHERE tr.nutritional_status = :nutritionalStatus \n" +
				"and DATE(tr.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) \n" +
				") nut \n" +
				"join kenyaemr_etl.etl_patient_demographics d on nut.patient_id=d.patient_id \n" +
				"join kenyaemr_etl.etl_mat_intial_registrations t on nut.patient_id = t.patient_id  \n" +
				"where date(t.visit_date) <= DATE(:endDate) \n" +
				"group by d.patient_id;";
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberMATClientsSAM");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("nutritionalStatus", "Nutritional status", String.class));
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number experienced Violence");
		return cd;
	}

	public CohortDefinition matNumberMATClientsSAM() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("nutritionalStatus", "Nutritional status", String.class));
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("active", ReportUtils.map(activeOnMAT(), "startDate=${startDate},endDate=${endDate}"));
		cd.addSearch("matAllNumberMATClientsSAM", ReportUtils.map(matAllNumberMATClientsSAM(), "nutritionalStatus=${nutritionalStatus},startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("active AND matAllNumberMATClientsSAM");
		return cd;
	}
	public CohortDefinition matAllNumberInitiatedNutritionSupport() {
		String sqlQuery = "select sc.patient_id from kenyaemr_etl.etl_special_clinics sc \n" +
				"inner join kenyaemr_etl.etl_patient_triage tr on sc.patient_id = tr.patient_id \n" +
				"where sc.nutritional_intervention=1065 \n" +
				"and DATE(sc.visit_date) between  DATE(:startDate) AND DATE(:endDate);" ;
		SqlCohortDefinition cd = new SqlCohortDefinition();
		cd.setName("matNumberInitiatedNutritionSupport");
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription("MAT Number initiated on nutrition support");
		return cd;
	}

	public CohortDefinition matNumberInitiatedNutritionSupport() {
		CompositionCohortDefinition cd = new CompositionCohortDefinition();
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.addSearch("matAllNumberInitiatedNutritionSupport", ReportUtils.map(matAllNumberInitiatedNutritionSupport(), "startDate=${startDate},endDate=${endDate}"));
		cd.setCompositionString("matAllNumberInitiatedNutritionSupport");
		return cd;
	}

}