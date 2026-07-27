/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.definition.evaluator.dmi;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.VisitDateWithComplaintsDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.evaluator.PersonDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

/**
 * Evaluates a VisitDateDataDefinition
 */
@Handler(supports= VisitDateWithComplaintsDataDefinition.class, order=50)
public class VisitDateWithComplaintsDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        // Prefer complaint-linked visit dates (ILI/SARI path); fall back to febrile triage
        // so Ebola / fever suspects without cough still get Visit Date on All IDSR Cases.
        String qry = "select x.patient_id, max(x.visit_date) as visit_date\n" +
                "from (\n" +
                "    select a.patient_id, a.visit_date\n" +
                "    from (select patient_id,\n" +
                "                 c.visit_date\n" +
                "          from "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_allergy_chronic_illness c\n" +
                "          where c.complaint = 143264\n" +
                "            and c.complaint_duration < 10\n" +
                "            and date(c.visit_date) between date(:startDate) and date(:endDate)\n" +
                "          group by patient_id) a\n" +
                "             join "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_clinical_encounter v\n" +
                "                  on a.patient_id = v.patient_id and date(a.visit_date) = date(v.visit_date)\n" +
                "             join "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_triage t\n" +
                "                  on a.patient_id = t.patient_id and date(t.visit_date) = date(v.visit_date) and t.temperature >= 38\n" +
                "    union all\n" +
                "    select t.patient_id, t.visit_date\n" +
                "    from "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_triage t\n" +
                "    where date(t.visit_date) between date(:startDate) and date(:endDate)\n" +
                "      and t.voided = 0\n" +
                "      and t.temperature >= 38\n" +
                ") x\n" +
                "group by x.patient_id";

        SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
        queryBuilder.append(qry);
        Date startDate = (Date)context.getParameterValue("startDate");
        Date endDate = (Date)context.getParameterValue("endDate");
        queryBuilder.addParameter("endDate", endDate);
        queryBuilder.addParameter("startDate", startDate);
        Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
        c.setData(data);
        return c;
    }
}
