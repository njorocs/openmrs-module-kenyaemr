/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.malaria;

import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.MOH731Greencard.ETLMoh731GreenCardCohortLibrary;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
@Component
public class Moh743CohortLibrary {
    public CohortDefinition totalPatientByAge(String comparisonOperator, int age) {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "SELECT a.patient_id " +
                "FROM (SELECT ce.patient_id, MAX(ce.visit_date) AS visit_date, p.DOB " +
                "FROM kenyaemr_etl.etl_clinical_encounter ce " +
                "INNER JOIN kenyaemr_etl.etl_patient_demographics p " +
                "ON p.patient_id = ce.patient_id AND p.voided = 0 " +
                "WHERE DATE(ce.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) " +
                "AND ce.voided = 0 " +
                "GROUP BY ce.patient_id) a " +
                "WHERE TIMESTAMPDIFF(YEAR, DATE(a.DOB), a.visit_date) " + comparisonOperator + " " + age;
        cd.setName("totalPatientByAge");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients by age comparison");

        return cd;
    }


    public CohortDefinition totalPatientByWeightRange(double minWeight, double maxWeight) {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "SELECT a.patient_id " +
                "FROM (SELECT ce.patient_id, MAX(ce.visit_date) AS visit_date " +
                "FROM kenyaemr_etl.etl_clinical_encounter ce " +
                "INNER JOIN kenyaemr_etl.etl_patient_demographics p " +
                "ON p.patient_id = ce.patient_id AND p.voided = 0 " +
                "WHERE DATE(ce.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate) " +
                "AND ce.voided = 0 " +
                "GROUP BY ce.patient_id) a " +
                "INNER JOIN kenyaemr_etl.etl_patient_triage tr " +
                "ON tr.patient_id = a.patient_id " +
                "AND DATE(tr.visit_date) = DATE(a.visit_date) " +
                "WHERE tr.weight BETWEEN " + minWeight + " AND " + maxWeight + " " +
                "AND tr.voided = 0";
        cd.setName("totalPatientByWeightRange");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients by weight range " + minWeight + "-" + maxWeight + "kg");

        return cd;
    }

    public CohortDefinition totalPatientsForMalariaDiagnosis(int labTest) {
        SqlCohortDefinition cd = new SqlCohortDefinition();
String sqlQuery = "select x.patient_id\n" +
                "from kenyaemr_etl.etl_laboratory_extract x\n" +
                "inner join kenyaemr_etl.etl_patient_demographics p on p.patient_id = x.patient_id and p.voided = 0\n" +
//                "inner join (select v.patient_id, v.encounter_id, v.visit_date\n" +
//                "            from kenyaemr_etl.etl_clinical_encounter v\n" +
//                "            inner join encounter_diagnosis ed\n" +
//                "            on v.patient_id = ed.patient_id and v.encounter_id = ed.encounter_id and ed.dx_rank = 2\n" +
//                "            where v.diagnosis_category = 'New') v\n" +
//                "on v.patient_id = x.patient_id and v.visit_date = x.date_test_requested\n" +
                "where x.lab_test in (" + labTest + ") and date(x.date_test_requested) between date(:startDate) and date(:endDate)\n" +
                "group by x.patient_id, x.lab_test";
        cd.setName("totalPatientsForMalariaDiagnosis");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addParameter(new Parameter("drugId", "Drug ID", Integer.class));
        cd.setDescription("Total patients who received malaria Diagnosis");

        return cd;
    }

    public CohortDefinition totalPatientsForMalariaResultName(String resultName) {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "select x.patient_id\n" +
                "from kenyaemr_etl.etl_laboratory_extract x\n" +
                "inner join kenyaemr_etl.etl_patient_demographics p on p.patient_id = x.patient_id and p.voided = 0\n" +
                "inner join (select v.patient_id, v.encounter_id, v.visit_date\n" +
                "            from kenyaemr_etl.etl_clinical_encounter v\n" +
                "            inner join encounter_diagnosis ed\n" +
                "            on v.patient_id = ed.patient_id and v.encounter_id = ed.encounter_id and ed.dx_rank = 2\n" +
                "            where v.diagnosis_category = 'New') v\n" +
                "on v.patient_id = x.patient_id and v.visit_date = x.date_test_requested\n" +
                "where x.result_name = '"+ resultName +"'  and date(x.date_test_requested) between date(:startDate) and date(:endDate)\n" +
                "group by x.patient_id, x.lab_test";
        cd.setName("totalPatientsForMalariaResultName");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addParameter(new Parameter("resultName", "Result Name", String.class));
        cd.setDescription("Total patients who received malaria Diagnosis with specific result");

        return cd;
    }


    public CohortDefinition totalPatientsNotTestedMalaria() {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "select v.patient_id,v.visit_date" +
                "    from kenyaemr_etl.etl_clinical_encounter v" +
                "    inner join openmrs.encounter_diagnosis ed on v.patient_id = ed.patient_id and v.encounter_id = ed.encounter_id and ed.diagnosis_coded = 2002652 and ed.dx_rank = 2" +
                "    where v.diagnosis_category = 'New' and date(v.visit_date) between date(:startDate) and date(:endDate);";
        cd.setName("totalPatientsNotTestedMalaria");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients not Tested malaria");
        return cd;
    }


    public CohortDefinition totalPatientsSuspectedMalaria() {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "select x.patient_id\n" +
                "from kenyaemr_etl.etl_laboratory_extract x\n" +
                "         inner join kenyaemr_etl.etl_patient_demographics p on p.patient_id = x.patient_id and p.voided = 0\n" +
                "         inner join (select v.patient_id, v.encounter_id, v.visit_date\n" +
                "                     from kenyaemr_etl.etl_clinical_encounter v\n" +
                "                              inner join openmrs.encounter_diagnosis ed\n" +
                "                                         on v.patient_id = ed.patient_id and v.encounter_id = ed.encounter_id and ed.dx_rank = 2\n" +
                "                     where v.diagnosis_category = 'New') v\n" +
                "                    on v.patient_id = x.patient_id and v.visit_date = x.date_test_requested\n" +
                "where x.lab_test in (2017917, 2032282, 1643)  and date(x.date_test_requested) BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
                "group by x.patient_id,x.lab_test;";
        cd.setName("totalPatientsSuspectedMalaria");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients who are suspected malaria");
        return cd;
    }

    public CohortDefinition totalPatientsSuspectedMalariaByAge(String comparisonOperator, int age, int labTest, String resultName) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addParameter(new Parameter("resultName", "Result Name", String.class));
        cd.addSearch("totalPatientsForMalariaDiagnosis", ReportUtils.map(totalPatientsForMalariaDiagnosis(labTest), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientsForMalariaResultName", ReportUtils.map(totalPatientsForMalariaResultName(resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("totalPatientByAge AND totalPatientsForMalariaDiagnosis and totalPatientsForMalariaResultName");
        return cd;
    }

    public CohortDefinition totalPatientsSuspectedRDTMICROByAge(String comparisonOperator, int age, String resultName) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addParameter(new Parameter("resultName", "Result Name", String.class));
        cd.addSearch("totalPatientsSuspectedMalaria", ReportUtils.map(totalPatientsSuspectedMalaria(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientsForMalariaResultName", ReportUtils.map(totalPatientsForMalariaResultName(resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("totalPatientsSuspectedMalaria AND totalPatientByAge AND totalPatientsForMalariaResultName");
        return cd;
    }
    public CohortDefinition totalPatientsNotTestedRDTMICROByAge(String comparisonOperator, int age, String resultName) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addParameter(new Parameter("resultName", "Result Name", String.class));
        cd.addSearch("totalPatientsNotTestedMalaria", ReportUtils.map(totalPatientsNotTestedMalaria(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientsForMalariaResultName", ReportUtils.map(totalPatientsForMalariaResultName(resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        cd.setCompositionString("totalPatientsNotTestedMalaria AND totalPatientByAge AND totalPatientsForMalariaResultName");
        return cd;
    }

    public CohortDefinition totalPatientsByAgeSuspectedMalaria(String comparisonOperator, int age) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("totalPatientsSuspectedMalaria", ReportUtils.map(totalPatientsSuspectedMalaria(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("totalPatientsSuspectedMalaria AND totalPatientByAge");
        return cd;
    }



    public CohortDefinition totalPatientsTreatedWithAntimalarials() {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "SELECT o.patient_id " +
                "FROM drug_order do " +
                "INNER JOIN orders o ON do.order_id = o.order_id " +
                "INNER JOIN encounter e ON o.encounter_id = e.encounter_id " +
                "INNER JOIN kenyaemr_etl.etl_patient_demographics p " +
                "  ON p.patient_id = o.patient_id AND p.voided = 0 " +
                "WHERE do.drug_inventory_id IN (\n" +
                "  -- Artemether-Lumefantrine (AL) drug IDs\n" +
                "  8580, 14009, 14010, 14011, 14018, 14019, 14020, 14021, 14022, 14023, 14024,\n" +
                "  14025, 14026, 14027, 14028, 14029, 14030, 14031, 14032, 14033, 14034, 14035,\n" +
                "  14036, 14037, 14038, 14039, 14040, 14041, 14042, 14043, 14044, 14049, 14050,\n" +
                "  15092, 16260, 16266, 16267, 16269, 16272, 16273, 16274, 16275, 16276, 16281,\n" +
                "  16282, 16284, 16303,\n" +
                "  -- DHAP drug IDs\n" +
                "  5600, 5601,\n" +
                "  -- ASPY drug IDs\n" +
                "  14494, 14495, 14496, 14497, 15096, 16293, 16296, 16300\n" +
                ")\n" +
                "  AND o.voided = 0 " +
                "  AND e.voided = 0 " +
                "  AND DATE(e.encounter_datetime) BETWEEN DATE(:startDate) AND DATE(:endDate)";
        cd.setName("totalPatientsTreatedWithAntimalarials");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients treated with AL, DHAP, or ASPY antimalarials");
        return cd;
    }

    public CohortDefinition totalPatientsGivenArtesunateInjection() {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "SELECT o.patient_id " +
                "FROM drug_order do " +
                "INNER JOIN orders o ON do.order_id = o.order_id " +
                "INNER JOIN encounter e ON o.encounter_id = e.encounter_id " +
                "INNER JOIN kenyaemr_etl.etl_patient_demographics p " +
                "  ON p.patient_id = o.patient_id AND p.voided = 0 " +
                "WHERE do.drug_inventory_id IN (\n" +
                " 9580,9581,9583,9584,9593,9595,9596,9598,9604,9605,9607,9608,15620,16288,16289,16298\n" +
                ")\n" +
                "  AND o.voided = 0 " +
                "  AND e.voided = 0 " +
                "  AND DATE(e.encounter_datetime) BETWEEN DATE(:startDate) AND DATE(:endDate)";
        cd.setName("totalPatientsGivenArtesunateInjection");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Total patients Given Artesunate Injection");
        return cd;
    }
    public CohortDefinition totalPatientsGivenArtesunateInjectionAgeWeightRange(double minWeight, double maxWeight, String comparisonOperator, int age) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("totalPatientsGivenArtesunateInjection", ReportUtils.map(totalPatientsGivenArtesunateInjection(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByWeightRange", ReportUtils.map(totalPatientByWeightRange(minWeight, maxWeight), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));

        cd.setCompositionString("totalPatientsGivenArtesunateInjection AND totalPatientByWeightRange AND totalPatientByAge");
        return cd;
    }
    public CohortDefinition totalPatientsTreatedWithAntiMalarialAgeWeightRange(double minWeight, double maxWeight, String comparisonOperator, int age) {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("totalPatientsTreatedWithAntimalarials", ReportUtils.map(totalPatientsTreatedWithAntimalarials(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByWeightRange", ReportUtils.map(totalPatientByWeightRange(minWeight, maxWeight), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("totalPatientByAge", ReportUtils.map(totalPatientByAge(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));

        cd.setCompositionString("totalPatientsTreatedWithAntimalarials AND totalPatientByWeightRange AND totalPatientByAge");
        return cd;
    }
}

