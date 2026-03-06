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

import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;
import static org.openmrs.module.kenyaemr.reporting.EmrReportingUtils.cohortIndicator;

@Component
public class Moh743IndicatorLibrary {

    @Autowired
    private Moh743CohortLibrary moh743CohortLibrary;

    public CohortIndicator totalPatientsByAgeSuspectedMalaria(String comparisonOperator, int age) {
        return cohortIndicator("Patients with suspected malaria " + comparisonOperator + " " + age, map(moh743CohortLibrary.totalPatientsByAgeSuspectedMalaria(comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
    }

    public CohortIndicator totalPatientsTreatedWithAntiMalarialAgeWeightRange(double minWeight, double maxWeight, String comparisonOperator, int age) {
        return cohortIndicator("Total patients treated with AL, DHAP, or ASPY antimalarials", map(moh743CohortLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(minWeight, maxWeight, comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
    }
    public CohortIndicator totalPatientsGivenArtesunateInjectionAgeWeightRange(double minWeight, double maxWeight, String comparisonOperator, int age) {
        return cohortIndicator("Total patients treated with Artesunate Injection", map(moh743CohortLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(minWeight, maxWeight, comparisonOperator, age), "startDate=${startDate},endDate=${endDate}"));
    }
    public CohortIndicator totalPatientsSuspectedMalariaByAge(String comparisonOperator, int age, int labTest, String resultName) {
        CohortIndicator indicator = cohortIndicator("Total patients Malaria Diagnosis", map(moh743CohortLibrary.totalPatientsSuspectedMalariaByAge(comparisonOperator, age, labTest, resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        indicator.addParameter(new Parameter("startDate", "Start Date", Date.class));
        indicator.addParameter(new Parameter("endDate", "End Date", Date.class));
        indicator.addParameter(new Parameter("resultName", "Result Name", String.class));
        return indicator;
    }

    public CohortIndicator totalPatientsSuspectedRDTMICROByAge(String comparisonOperator, int age, String resultName) {
        CohortIndicator indicator = cohortIndicator("Total patients Malaria Diagnosis For RDT and Microscopic", map(moh743CohortLibrary.totalPatientsSuspectedRDTMICROByAge(comparisonOperator, age, resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        indicator.addParameter(new Parameter("startDate", "Start Date", Date.class));
        indicator.addParameter(new Parameter("endDate", "End Date", Date.class));
        indicator.addParameter(new Parameter("resultName", "Result Name", String.class));
        return indicator;
    }
    public CohortIndicator totalPatientsNotTestedRDTMICROByAge(String comparisonOperator, int age, String resultName) {
        CohortIndicator indicator = cohortIndicator("Total patients Malaria Diagnosis For RDT and Microscopic", map(moh743CohortLibrary.totalPatientsNotTestedRDTMICROByAge(comparisonOperator, age, resultName), "startDate=${startDate},endDate=${endDate},resultName=${resultName}"));
        indicator.addParameter(new Parameter("startDate", "Start Date", Date.class));
        indicator.addParameter(new Parameter("endDate", "End Date", Date.class));
        return indicator;
    }


}