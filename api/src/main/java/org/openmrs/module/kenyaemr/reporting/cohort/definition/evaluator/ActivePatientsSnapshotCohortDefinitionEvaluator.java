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
import org.openmrs.module.kenyaemr.reporting.cohort.definition.ActivePatientsSnapshotCohortDefinition;
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
 * Evaluator for ActivePatientsSnapshotCohortDefinition
 * Includes patients who are active on ART.
 * Provides a snapshot of a patient with regard to the last visit
 */
@Handler(supports = {ActivePatientsSnapshotCohortDefinition.class})
public class ActivePatientsSnapshotCohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());

	@Autowired
	EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

		ActivePatientsSnapshotCohortDefinition definition = (ActivePatientsSnapshotCohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

		Cohort newCohort = new Cohort();

		String qry="select t.patient_id\n" +
				"     from(\n" +
				"         select fup.visit_date,fup.patient_id, max(e.visit_date) as enroll_date,\n" +
				"                greatest(max(e.visit_date), ifnull(max(date(e.transfer_in_date)),'0000-00-00')) as latest_enrolment_date,\n" +
				"                greatest(max(fup.visit_date), ifnull(max(d.visit_date),'0000-00-00')) as latest_vis_date,\n" +
				"                greatest(mid(max(concat(fup.visit_date,fup.next_appointment_date)),11), ifnull(max(d.visit_date),'0000-00-00')) as latest_tca,\n" +
				"max(date(e.visit_date)) as latest_hiv_enrollment_date,\n" +
				"mid(max(concat(e.visit_date,e.patient_type)),11) as patient_type,\n" +
				"                d.patient_id as disc_patient,\n" +
				"                d.effective_disc_date as effective_disc_date,\n" +
				"                max(d.visit_date) as date_discontinued,\n" +
				"                de.patient_id as started_on_drugs,\n" +
				"                de.art_status as art_status\n" +
				"         from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
				"                join kenyaemr_etl.etl_patient_demographics p on p.patient_id=fup.patient_id\n" +
				"                join kenyaemr_etl.etl_hiv_enrollment e on fup.patient_id=e.patient_id\n" +
				"                left join (SELECT e1.patient_id, e1.discontinued as art_status,e1.program as program, e1.date_started as date_started\n" +
				"FROM kenyaemr_etl.etl_drug_event e1\n" +
				"INNER JOIN (\n" +
				"Select patient_id, MAX(date_started) as date_started\n" +
				"FROM kenyaemr_etl.etl_drug_event\n" +
				"GROUP BY patient_id\n" +
				") e2 on e1.patient_id = e2.patient_id and e1.date_started = e2.date_started) de on e.patient_id = de.patient_id and de.program='HIV' and date(date_started) <= date(:endDate)\n" +
				"                left outer JOIN\n" +
				"                  (select patient_id, coalesce(date(effective_discontinuation_date),visit_date) visit_date,max(date(effective_discontinuation_date)) as effective_disc_date from kenyaemr_etl.etl_patient_program_discontinuation\n" +
				"                   where date(visit_date) <= date(:endDate) and program_name='HIV'\n" +
				"                   group by patient_id\n" +
				"                  ) d on d.patient_id = fup.patient_id\n" +
				"         where fup.visit_date <= date(:endDate)\n" +
				"         group by patient_id\n" +
				"         having (started_on_drugs is not null and started_on_drugs <> '' and art_status is null) and (\n" +
				"             (\n" +
				"                 (timestampdiff(DAY,date(latest_tca),date(:endDate)) <= 30 and ((date(d.effective_disc_date) > date(:endDate) or date(enroll_date) > date(d.effective_disc_date)) or d.effective_disc_date is null))\n" +
				"                   and (date(latest_vis_date) >= date(date_discontinued) or date(latest_tca) >= date(date_discontinued) or disc_patient is null)\n" +
				"                 ) and patient_type != 164931\n" +
				"             )\n" +
				"         ) t;";

		SqlQueryBuilder builder = new SqlQueryBuilder();
		builder.append(qry);
		Date startDate = (Date)context.getParameterValue("startDate");
		Date endDate = (Date)context.getParameterValue("endDate");
		builder.addParameter("startDate", startDate);
		builder.addParameter("endDate", endDate);

		List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
		newCohort.setMemberIds(new HashSet<Integer>(ptIds));
		return new EvaluatedCohort(newCohort, definition, context);
    }
}
