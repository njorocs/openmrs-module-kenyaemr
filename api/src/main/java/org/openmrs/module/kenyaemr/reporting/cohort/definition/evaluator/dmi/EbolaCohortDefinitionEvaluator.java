/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.cohort.definition.evaluator.dmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.annotation.Handler;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.EbolaCohortDefinition;
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
 * Evaluator for EbolaCohortDefinition
 * Includes patients who are at high risk for Ebola.
 * Provides a snapshot of a patient with regard to the last visit
 */
@Handler(supports = {EbolaCohortDefinition.class})
public class EbolaCohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());

	@Autowired
	EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

		EbolaCohortDefinition definition = (EbolaCohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

		Cohort newCohort = new Cohort();

		// Criteria:
		// - Temperature >= 38C at triage
		// - AND (travel to an affected country OR contact with suspected/probable case)
		// Note: travel country codes used here follow TravelCountryDataEvaluator mappings (e.g. DRC, Uganda, Other)
		String qry = "select patient_id from kenyaemr_etl.etl_patient_triage t \n" +
				"where t.temperature >= 38 \n" +
				"  and (t.history_of_travel in (165748, 162884, 2032318, 5622) or t.contact_with_evd_suspected_case = 1065) \n" +
				"  and (\n" +
				"    select count(distinct c.complaint)\n" +
				"    from kenyaemr_etl.etl_allergy_chronic_illness c\n" +
				"    where c.patient_id = t.patient_id\n" +
				"      and c.complaint in (139084, 116334, 2032542, 151, 122983, 142412, 162628)\n" +
				"      and c.voided = 0\n" +
				"      and date(c.visit_date) between date(:startDate) and date(:endDate)\n" +
				"  ) >= 3 \n" +
				"  and date(t.visit_date) between date(:startDate) and date(:endDate) \n" +
				"  and t.voided = 0 \n" +
				"group by t.patient_id";

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