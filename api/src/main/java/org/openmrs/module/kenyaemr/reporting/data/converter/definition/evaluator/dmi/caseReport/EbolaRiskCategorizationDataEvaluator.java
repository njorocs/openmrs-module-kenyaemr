/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.definition.evaluator.dmi.caseReport;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.EbolaRiskCategorizationDataDefinition;
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

@Handler(supports = EbolaRiskCategorizationDataDefinition.class, order = 50)
public class EbolaRiskCategorizationDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry =
            "select t.patient_id, " +
            "    case " +
            "        when t.temperature >= 38 " +
            "             and (t.history_of_travel in (165748, 162884, 2032318, 5622) or t.contact_with_evd_suspected_case = 1065) " +
            "             and ( " +
            "                 (select count(distinct c.complaint) from kenyaemr_etl.etl_allergy_chronic_illness c " +
            "                     where c.patient_id = t.patient_id " +
            "                       and c.complaint in (139084, 116334, 2032542, 122983, 142412, 151, 162628) " +
            "                       and c.voided = 0 " +
            "                       and date(c.visit_date) between date(:startDate) and date(:endDate)) >= 3 " +
            "             ) " +
            "        then 'Suspected Ebola Case' " +
            "        when t.temperature >= 38 " +
            "             and (t.history_of_travel in (165748, 162884, 2032318, 5622) or t.contact_with_evd_suspected_case = 1065) " +
            "        then 'Suspected Ebola Case' " +
            "        else 'Suspected Ebola Case - Low Risk' " +
            "    end as risk_category " +
            "from kenyaemr_etl.etl_patient_triage t " +
            "inner join ( " +
            "    select patient_id, max(visit_date) as latest_visit " +
            "    from kenyaemr_etl.etl_patient_triage " +
            "    where date(visit_date) between date(:startDate) and date(:endDate) " +
            "    and voided = 0 " +
            "    group by patient_id " +
            ") latest on t.patient_id = latest.patient_id and t.visit_date = latest.latest_visit " +
            "where t.voided = 0 " +
            "group by t.patient_id";

        SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
        queryBuilder.append(qry);
        Date startDate = (Date) context.getParameterValue("startDate");
        Date endDate = (Date) context.getParameterValue("endDate");
        queryBuilder.addParameter("endDate", endDate);
        queryBuilder.addParameter("startDate", startDate);
        Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
        c.setData(data);
        return c;
    }
}
