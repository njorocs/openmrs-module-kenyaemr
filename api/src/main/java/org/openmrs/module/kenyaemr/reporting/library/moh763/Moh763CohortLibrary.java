/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.moh763;

import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.MohReportUtils.Moh763DiagnosisLists;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Library of cohort definitions used specifically in the MOH763 report (Mental Health).
 *
 */
@Component
public class Moh763CohortLibrary {
	
	private CohortDefinition byDiagnosisUuids(String name, String description, String visitType, List<String> conceptUuids) {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		StringBuilder uuidList = new StringBuilder();
		for (int i = 0; i < conceptUuids.size(); i++) {
			if (i > 0) uuidList.append(",");
			uuidList.append("'").append(conceptUuids.get(i)).append("'");
		}
		String sqlQuery = "select ce.patient_id\n" +
				"from encounter_diagnosis d\n" +
				"inner join concept_name cn on cn.concept_id = d.diagnosis_coded\n" +
				"  and cn.concept_name_type = 'FULLY_SPECIFIED' and cn.locale = 'en'\n" +
				"inner join concept c on c.concept_id = cn.concept_id\n" +
				"inner join " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_clinical_encounter ce\n" +
				"  on ce.patient_id = d.patient_id and ce.encounter_id = d.encounter_id\n" +
				"where d.dx_rank = 1\n" +
				"  and d.voided = 0\n" +
				"  and c.uuid in (" + uuidList + ")\n" +
				"  and ce.visit_type = '" + visitType + "'\n" +
				"  and date(ce.visit_date) between date(:startDate) and date(:endDate);";
		cd.setName(name);
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription(description);
		return cd;
	}

	public CohortDefinition anxietyDisorders(String visitType) {
		return byDiagnosisUuids("anxietyDisorders", "Anxiety disorders", visitType, Moh763DiagnosisLists.getAnxietyDisordersUuidsList());
	}

	public CohortDefinition depressiveDisorders(String visitType) {
		return byDiagnosisUuids("depressiveDisorders", "Depressive disorders", visitType, Moh763DiagnosisLists.getDepressiveDisordersUuidsList());
	}

	public CohortDefinition schizophreniaSpectrumDisorders(String visitType) {
		return byDiagnosisUuids("schizophreniaSpectrumDisorders", "Schizophrenia spectrum disorders", visitType, Moh763DiagnosisLists.getSchizophreniaSpectrumDisordersUuidsList());
	}

	public CohortDefinition bipolarDisorders(String visitType) {
		return byDiagnosisUuids("bipolarDisorders", "Bipolar disorders", visitType, Moh763DiagnosisLists.getBipolarDisordersUuidsList());
	}

	public CohortDefinition otherPsychoticDisorders(String visitType) {
		return byDiagnosisUuids("otherPsychoticDisorders", "Other psychotic disorders", visitType, Moh763DiagnosisLists.getOtherPsychoticDisordersUuidsList());
	}

	public CohortDefinition neurodevelopmentalDisorders(String visitType) {
		return byDiagnosisUuids("neurodevelopmentalDisorders", "Neurodevelopmental disorders", visitType, Moh763DiagnosisLists.getNeurodevelopmentalDisordersUuidsList());
	}

	public CohortDefinition alcoholUseDisorders(String visitType) {
		return byDiagnosisUuids("alcoholUseDisorders", "Alcohol use disorders", visitType, Moh763DiagnosisLists.getAlcoholUseDisordersUuidsList());
	}

	public CohortDefinition otherSubstanceUseDisorders(String visitType) {
		return byDiagnosisUuids("otherSubstanceUseDisorders", "Other disorders due to substance use", visitType, Moh763DiagnosisLists.getOtherSubstanceUseDisordersUuidsList());
	}

	public CohortDefinition otherBehaviouralAddictions(String visitType) {
		return byDiagnosisUuids("otherBehaviouralAddictions", "Other behavioral addictions", visitType, Moh763DiagnosisLists.getOtherBehaviouralAddictionsUuidsList());
	}

	public CohortDefinition neurocognitiveDisorders(String visitType) {
		return byDiagnosisUuids("neurocognitiveDisorders", "Neurocognitive disorders", visitType, Moh763DiagnosisLists.getNeurocognitiveDisordersUuidsList());
	}

	public CohortDefinition stressTraumaRelatedDisorders(String visitType) {
		return byDiagnosisUuids("stressTraumaRelatedDisorders", "Stress-trauma related disorders", visitType, Moh763DiagnosisLists.getStressTraumaRelatedDisordersUuidsList());
	}

	public CohortDefinition somatoformDisorders(String visitType) {
		return byDiagnosisUuids("somatoformDisorders", "Somatoform disorders", visitType, Moh763DiagnosisLists.getSomatoformDisordersUuidsList());
	}

	public CohortDefinition suicidalBehaviour(String visitType) {
		return byDiagnosisUuids("suicidalBehaviour", "Suicidal behaviour", visitType, Moh763DiagnosisLists.getSuicidalBehaviourUuidsList());
	}

	public CohortDefinition sleepWakeDisorders(String visitType) {
		return byDiagnosisUuids("sleepWakeDisorders", "Sleep-wake disorders", visitType, Moh763DiagnosisLists.getSleepWakeDisordersUuidsList());
	}

	public CohortDefinition perinatalMentalDisorders(String visitType) {
		return byDiagnosisUuids("perinatalMentalDisorders", "Perinatal mental disorders", visitType, Moh763DiagnosisLists.getPerinatalMentalDisordersUuidsList());
	}

	public CohortDefinition eatingDisorders(String visitType) {
		return byDiagnosisUuids("eatingDisorders", "Eating disorders", visitType, Moh763DiagnosisLists.getEatingDisordersUuidsList());
	}

	public CohortDefinition personalityDisorders(String visitType) {
		return byDiagnosisUuids("personalityDisorders", "Personality disorders", visitType, Moh763DiagnosisLists.getPersonalityDisordersUuidsList());
	}

	public CohortDefinition sexualDysfunctionalDisorders(String visitType) {
		return byDiagnosisUuids("sexualDysfunctionalDisorders", "Sexual dysfunctional disorders", visitType, Moh763DiagnosisLists.getSexualDysfunctionalDisordersUuidsList());
	}

	public CohortDefinition obsessiveCompulsiveAndRelatedDisorders(String visitType) {
		return byDiagnosisUuids("obsessiveCompulsiveAndRelatedDisorders", "Obsessive-compulsive & related disorders", visitType, Moh763DiagnosisLists.getObsessiveCompulsiveAndRelatedDisordersUuidsList());
	}

	public CohortDefinition disruptiveImpulseControlAndConductDisorder(String visitType) {
		return byDiagnosisUuids("disruptiveImpulseControlAndConductDisorder", "Disruptive impulse control & conduct disorder", visitType, Moh763DiagnosisLists.getDisruptiveImpulseControlAndConductDisorderUuidsList());
	}
}
