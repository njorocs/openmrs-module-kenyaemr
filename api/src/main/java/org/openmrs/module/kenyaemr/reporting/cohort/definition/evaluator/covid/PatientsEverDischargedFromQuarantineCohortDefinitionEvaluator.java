/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.cohort.definition.evaluator.covid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.covid.PatientsEverDischargedFromCovidCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.covid.PatientsEverDischargedFromQuarantineCohortDefinition;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.evaluator.CohortDefinitionEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Evaluator for Ever enrolled on Covid-19
 */
@Handler(supports = {PatientsEverDischargedFromQuarantineCohortDefinition.class})
public class PatientsEverDischargedFromQuarantineCohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());
    @Autowired
    EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

        PatientsEverDischargedFromQuarantineCohortDefinition definition = (PatientsEverDischargedFromQuarantineCohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

        Cohort newCohort = new Cohort();

        SqlQueryBuilder builder = new SqlQueryBuilder();

        String qry = "select e.patient_id from kenyaemr_etl.etl_covid_19_enrolment e inner join kenyaemr_etl.etl_patient_program_discontinuation d on e.patient_id = d.patient_id\n" +
                "    where d.program_name ='COVID-19 Quarantine Outcome'\n" +
                "and d.discontinuation_reason !=160034;";

        builder.append(qry);
        Date startDate = (Date) context.getParameterValue("startDate");
        Date endDate = (Date) context.getParameterValue("endDate");
        builder.addParameter("endDate", endDate);
        builder.addParameter("startDate", startDate);

        List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
        newCohort.setMemberIds(new HashSet<Integer>(ptIds));

        return new EvaluatedCohort(newCohort, definition, context);
    }

}
