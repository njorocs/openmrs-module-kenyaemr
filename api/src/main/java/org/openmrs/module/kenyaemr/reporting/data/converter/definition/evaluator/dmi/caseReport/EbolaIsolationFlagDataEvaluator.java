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
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.EbolaIsolationFlagDataDefinition;
import org.openmrs.module.reporting.data.person.EvaluatedPersonData;
import org.openmrs.module.reporting.data.person.definition.PersonDataDefinition;
import org.openmrs.module.reporting.data.person.evaluator.PersonDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Handler(supports = EbolaIsolationFlagDataDefinition.class, order = 50)
public class EbolaIsolationFlagDataEvaluator implements PersonDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedPersonData evaluate(PersonDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedPersonData c = new EvaluatedPersonData(definition, context);

        String qry = "select o.person_id, o.value_text from obs o " +
                "inner join (select person_id, max(obs_datetime) as latest from obs where voided = 0 and value_text like 'ebola_flag:%' group by person_id) latest " +
                "on o.person_id = latest.person_id and o.obs_datetime = latest.latest " +
                "where o.voided = 0 and o.value_text like 'ebola_flag:%'";

        SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
        queryBuilder.append(qry);
        Map<Integer, Object> data = new HashMap<Integer, Object>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date now = new Date();
        Map<Integer, Object> rows = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
        for (Map.Entry<Integer, Object> e : rows.entrySet()) {
            Integer pid = e.getKey();
            String valueText = e.getValue() != null ? e.getValue().toString() : null;
            if (valueText == null) {
                data.put(pid, null);
                continue;
            }
            try {
                String[] parts = valueText.split("expires=");
                if (parts.length == 2) {
                    String ts = parts[1].split(";")[0];
                    Date expiry = sdf.parse(ts);
                    if (expiry.after(now)) {
                        String label = valueText.split(";")[0].replaceFirst("ebola_flag:", "");
                        data.put(pid, label);
                    } else {
                        data.put(pid, null);
                    }
                } else {
                    data.put(pid, valueText);
                }
            } catch (Exception ex) {
                data.put(pid, valueText);
            }
        }

        c.setData(data);
        return c;
    }
}

