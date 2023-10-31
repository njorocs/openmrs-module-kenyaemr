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
import org.openmrs.module.kenyaemr.reporting.cohort.definition.MaternityRegisterCohortDefinition;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.evaluator.CohortDefinitionEvaluator;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Evaluator for Maternity
 */
@Handler(supports = {MaternityRegisterCohortDefinition.class})
public class MaternityRegisterCohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private final Log log = LogFactory.getLog(this.getClass());
	@Autowired
	EvaluationService evaluationService;

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

		MaternityRegisterCohortDefinition definition = (MaternityRegisterCohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

		Cohort newCohort = new Cohort();

		context = ObjectUtil.nvl(context, new EvaluationContext());

        String qry = "SELECT ld.patient_id\n" +
				"from kenyaemr_etl.etl_mchs_delivery ld\n" +
				"         inner join (select e.patient_id, max(e.visit_date) as enr_date, date(d.visit_date) as disc_date\n" +
				"                     from kenyaemr_etl.etl_mch_enrollment e\n" +
				"                              left join (select patient_id,\n" +
				"                                                coalesce(date(effective_discontinuation_date), visit_date) visit_date\n" +
				"                                         from kenyaemr_etl.etl_patient_program_discontinuation\n" +
				"                                         where date(visit_date) <= date(:endDate)\n" +
				"                                           and program_name = 'MCH Mother'\n" +
				"                                         group by patient_id) d on e.patient_id = d.patient_id\n" +
				"                     group by e.patient_id) ed\n" +
				"                    on ed.patient_id = ld.patient_id\n" +
				"where enr_date <= ld.visit_date\n" +
				"  and (disc_date is null or disc_date < enr_date)\n" +
				"  and coalesce(date(ld.date_of_delivery), date(ld.visit_date))\n" +
				"    BETWEEN date(:startDate) AND date(:endDate);";

		SqlQueryBuilder builder = new SqlQueryBuilder();
		builder.append(qry);
		Date startDate = (Date)context.getParameterValue("startDate");
		Date endDate = (Date)context.getParameterValue("endDate");
		builder.addParameter("endDate", endDate);
		builder.addParameter("startDate", startDate);

		List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
		newCohort.setMemberIds(new HashSet<Integer>(ptIds));


//		queryResult.getMemberIds().addAll(results);
//		return queryResult;

        return new EvaluatedCohort(newCohort, definition, context);
    }

}
