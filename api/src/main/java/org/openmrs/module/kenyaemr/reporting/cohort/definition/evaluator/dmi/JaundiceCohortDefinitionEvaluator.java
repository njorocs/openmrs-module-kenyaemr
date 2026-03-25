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
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.JaundiceCohortDefinition;
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
 * Evaluator for Jaundice Cohort
 */
@Handler(supports = { JaundiceCohortDefinition.class })
public class JaundiceCohortDefinitionEvaluator implements CohortDefinitionEvaluator {
	
	private final Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	EvaluationService evaluationService;
	
	@Override
	public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

		JaundiceCohortDefinition definition = (JaundiceCohortDefinition) cohortDefinition;
		
		if (definition == null)
			return null;
		Cohort newCohort = new Cohort();
		String sqlQuery = "SELECT p.patient_id FROM " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_patient_demographics p " +
				" WHERE EXISTS (" +
				"    SELECT 1 FROM " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_clinical_encounter c " +
				"    WHERE c.patient_id = p.patient_id " +
				"    AND c.general_examination LIKE '%Jaundice%' " +
				"    AND DATE(c.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate)" +
				" ) " +
				" OR EXISTS (" +
				"    SELECT 1 FROM " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_patient_hiv_followup h " +
				"    WHERE h.patient_id = p.patient_id " +
				"    AND h.general_examination LIKE '%Jaundice%' " +
				"    AND DATE(h.visit_date) BETWEEN DATE(:startDate) AND DATE(:endDate)" +
				" )";

		SqlQueryBuilder builder = new SqlQueryBuilder();
		builder.append(sqlQuery);
		Date startDate = (Date) context.getParameterValue("startDate");
		Date endDate = (Date) context.getParameterValue("endDate");
		builder.addParameter("startDate", startDate);
		builder.addParameter("endDate", endDate);
		
		List<Integer> ptIds = evaluationService.evaluateToList(builder, Integer.class, context);
		newCohort.setMemberIds(new HashSet<Integer>(ptIds));
		return new EvaluatedCohort(newCohort, definition, context);
	}
}
