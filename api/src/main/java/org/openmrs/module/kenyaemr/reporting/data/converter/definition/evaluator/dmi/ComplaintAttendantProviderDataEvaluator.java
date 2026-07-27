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
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.ComplaintAttendantProviderDataDefinition;
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
 * Evaluates a ComplaintAttendantProviderDataDefinition
 */
@Handler(supports= ComplaintAttendantProviderDataDefinition.class, order=50)
public class ComplaintAttendantProviderDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry = "select x.patient_id,\n" +
                "       substring_index(group_concat(x.provider order by x.visit_date desc separator '||'), '||', 1) as provider\n" +
                "from (\n" +
                "    select a.patient_id,\n" +
                "           a.visit_date,\n" +
                "           concat_ws(' ', pn.given_name, pn.middle_name, pn.family_name) as provider\n" +
                "    from "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_allergy_chronic_illness a\n" +
                "             inner join encounter e on a.encounter_id = e.encounter_id\n" +
                "             inner join encounter_provider ep on ep.encounter_id = e.encounter_id\n" +
                "             inner join provider p on ep.provider_id = p.provider_id\n" +
                "             inner join person_name pn on pn.person_id = p.person_id and pn.voided = 0\n" +
                "    where date(a.visit_date) between date(:startDate) and date(:endDate)\n" +
                "      and a.voided = 0\n" +
                "    union all\n" +
                "    select t.patient_id,\n" +
                "           t.visit_date,\n" +
                "           concat_ws(' ', pn.given_name, pn.middle_name, pn.family_name) as provider\n" +
                "    from "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_triage t\n" +
                "             inner join provider p on t.encounter_provider = p.provider_id\n" +
                "             inner join person_name pn on pn.person_id = p.person_id and pn.voided = 0\n" +
                "    where date(t.visit_date) between date(:startDate) and date(:endDate)\n" +
                "      and t.voided = 0\n" +
                "      and t.temperature >= 38\n" +
                "      and t.encounter_provider is not null\n" +
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