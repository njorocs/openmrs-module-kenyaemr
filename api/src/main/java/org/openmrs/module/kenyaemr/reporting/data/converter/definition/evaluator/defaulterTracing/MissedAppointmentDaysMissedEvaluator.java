/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter.definition.evaluator.defaulterTracing;

import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.defaulterTracing.MissedAppointmentDaysMissedDataDefinition;
import org.openmrs.module.reporting.data.encounter.EvaluatedEncounterData;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDataDefinition;
import org.openmrs.module.reporting.data.encounter.evaluator.EncounterDataEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

/**
 * Returns the number of days a patient missed an appointment.
 * It is the difference between the date of missed appointment and the immediate return to clinic date or the end of reporting period
 */
@Handler(supports= MissedAppointmentDaysMissedDataDefinition.class, order=50)
public class MissedAppointmentDaysMissedEvaluator implements EncounterDataEvaluator {

    @Autowired
    private EvaluationService evaluationService;

    public EvaluatedEncounterData evaluate(EncounterDataDefinition definition, EvaluationContext context) throws EvaluationException {
        EvaluatedEncounterData c = new EvaluatedEncounterData(definition, context);

        String qry = "select encounter_id,patient_id, timestampdiff(DAY, appdate,ifnull(rtc,date(current_date))) as days_missed\n" +
                "from (\n" +
                "         select fup.encounter_id,\n" +
                "                fup.patient_id,\n" +
                "                firstvisit.patient_id as rtc,\n" +
                "                null as honouredRefill,\n" +
                "                fup.next_appointment_date as appdate\n" +
                "         from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
                "                  left join kenyaemr_etl.etl_patient_hiv_followup firstvisit\n" +
                "                            on firstvisit.patient_id = fup.patient_id\n" +
                "                                and fup.next_appointment_date < firstvisit.visit_date\n" +
                "                                and firstvisit.visit_date > fup.visit_date\n" +
                "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n" +
                "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n" +
                "         where date(fup.next_appointment_date) between date(:startDate) and date(:endDate)\n" +
                "         group by fup.encounter_id\n" +
                "\n" +
                "         union\n" +
                "\n" +
                "         select fup.encounter_id,\n" +
                "                fup.patient_id,\n" +
                "                null as honored_appt,\n" +
                "                refillvisit.patient_id as honouredRefill,\n" +
                "                fup.refill_date as appdate\n" +
                "         from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
                "                  left join kenyaemr_etl.etl_art_fast_track refillvisit\n" +
                "                            on refillvisit.patient_id = fup.patient_id\n" +
                "                                and fup.refill_date < refillvisit.visit_date\n" +
                "                                and refillvisit.visit_date > fup.visit_date\n" +
                "                  join kenyaemr_etl.etl_patient_demographics p on p.patient_id = fup.patient_id\n" +
                "                  join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id = e.patient_id\n" +
                "         where date(fup.refill_date) between date(:startDate) and date(:endDate)\n" +
                "         group by fup.encounter_id\n" +
                "     ) as mApp1;";

        SqlQueryBuilder queryBuilder = new SqlQueryBuilder();
        Date startDate = (Date)context.getParameterValue("startDate");
        Date endDate = (Date)context.getParameterValue("endDate");
        queryBuilder.addParameter("endDate", endDate);
        queryBuilder.addParameter("startDate", startDate);
        queryBuilder.append(qry);
        Map<Integer, Object> data = evaluationService.evaluateToMap(queryBuilder, Integer.class, Object.class, context);
        c.setData(data);
        return c;
    }
}
