/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.cohort.definition.evaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.PartiallyVaccinatedCovid19CohortDefinition;
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
 * Evaluator for Covid-19 partially vaccinated patients
 */
@Handler(supports = {PartiallyVaccinatedCovid19CohortDefinition.class})
public class PartiallyVaccinatedCovid19CohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());
    @Autowired
    EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

        PartiallyVaccinatedCovid19CohortDefinition definition = (PartiallyVaccinatedCovid19CohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

        Cohort newCohort = new Cohort();

        String qry = "select p.patient_id\n" +
                "from (SELECT t.patient_id from kenyaemr_etl.etl_current_in_care t\n" +
                "             inner join (\n" +
                "                        select patient_id from kenyaemr_etl.etl_covid19_assessment\n" +
                "                        group by patient_id\n" +
                "                        having mid(max(concat(visit_date,final_vaccination_status)),11) = 166192 and mid(max(concat(visit_date,ever_vaccinated)),11) is not null\n" +
                "                           and mid(max(concat(visit_date,first_vaccine_type)),11) <> 166355)c\n" +
                "               on t.patient_id = c.patient_id)p\n" +
                "group by p.patient_id;";

        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.append(qry);
        Date endDate = (Date) context.getParameterValue("endDate");
        builder.addParameter("endDate", endDate);
        List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);

        newCohort.setMemberIds(new HashSet<Integer>(ptIds));


        return new EvaluatedCohort(newCohort, definition, context);
    }

}
