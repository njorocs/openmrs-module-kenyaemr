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
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.TravelCountryDataDefinition;
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

@Handler(supports = TravelCountryDataDefinition.class, order = 50)
public class TravelCountryDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry = "select t.patient_id, " +
                "    case t.history_of_travel " +
                "        when 165748  then 'Democratic Republic of Congo' " +
                "        when 162884  then 'Uganda' " +
                "        when 2032318 then coalesce(t.history_of_travel_other_specify, 'Other') " +
                "        when 1066    then 'No travel history' " +
                "        else null " +
                "    end as travel_country " +
                "from "+ CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_triage t " +
                "inner join ( " +
                "    select patient_id, max(visit_date) as latest_visit " +
                "    from "+CommonMetadata.EmrDatabaseEtl.ETL_DATABASE+".etl_patient_triage " +
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
