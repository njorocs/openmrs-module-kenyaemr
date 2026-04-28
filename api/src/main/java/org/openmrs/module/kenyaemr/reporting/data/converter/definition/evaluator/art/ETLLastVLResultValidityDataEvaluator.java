/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.definition.evaluator.art;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.art.ETLLastVLResultValidityDataDefinition;
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
 * Evaluates Last VL result validity Data Definition
 */
@Handler(supports= ETLLastVLResultValidityDataDefinition.class, order=50)
public class ETLLastVLResultValidityDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry = "SELECT t.patient_id,\n" +
                "       CASE\n" +
                "           WHEN DATE(:endDate) > t.vl_due_date THEN 'Invalid'\n" +
                "           WHEN t.vl_due_date IS NULL           THEN 'Pending Results'\n" +
                "           ELSE 'Valid'\n" +
                "       END AS vl_status\n" +
                "FROM kenyaemr_etl.etl_viral_load_validity_tracker t\n" +
                "INNER JOIN (\n" +
                "    SELECT patient_id, MAX(visit_date) AS latest_visit\n" +
                "    FROM kenyaemr_etl.etl_viral_load_validity_tracker\n" +
                "    WHERE visit_date <= DATE(:endDate)\n" +
                "    GROUP BY patient_id\n" +
                ") lv ON lv.patient_id = t.patient_id\n" +
                "     AND lv.latest_visit = t.visit_date;";

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
