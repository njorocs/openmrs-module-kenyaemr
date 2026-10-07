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

import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;
import static org.openmrs.module.kenyaemr.reporting.EmrReportingUtils.cohortIndicator;

/**
 * Indicators specific to the MOH763 report (Mental Health). Each is disaggregated by visit type
 * (new vs. revisit) via the visitType parameter, mirroring Moh711IndicatorLibrary's visit-type indicators.
 */
@Component
public class Moh763IndicatorLibrary {

	@Autowired
	private Moh763CohortLibrary moh763Cohorts;

	private static final String PARAMS = "startDate=${startDate},endDate=${endDate}";

	public CohortIndicator anxietyDisorders(String visitType) {
		return cohortIndicator("Anxiety disorders", map(moh763Cohorts.anxietyDisorders(visitType), PARAMS));
	}

	public CohortIndicator depressiveDisorders(String visitType) {
		return cohortIndicator("Depressive disorders", map(moh763Cohorts.depressiveDisorders(visitType), PARAMS));
	}

	public CohortIndicator schizophreniaSpectrumDisorders(String visitType) {
		return cohortIndicator("Schizophrenia spectrum disorders", map(moh763Cohorts.schizophreniaSpectrumDisorders(visitType), PARAMS));
	}

	public CohortIndicator bipolarDisorders(String visitType) {
		return cohortIndicator("Bipolar disorders", map(moh763Cohorts.bipolarDisorders(visitType), PARAMS));
	}

	public CohortIndicator otherPsychoticDisorders(String visitType) {
		return cohortIndicator("Other psychotic disorders", map(moh763Cohorts.otherPsychoticDisorders(visitType), PARAMS));
	}

	public CohortIndicator neurodevelopmentalDisorders(String visitType) {
		return cohortIndicator("Neurodevelopmental disorders", map(moh763Cohorts.neurodevelopmentalDisorders(visitType), PARAMS));
	}

	public CohortIndicator alcoholUseDisorders(String visitType) {
		return cohortIndicator("Alcohol use disorders", map(moh763Cohorts.alcoholUseDisorders(visitType), PARAMS));
	}

	public CohortIndicator otherSubstanceUseDisorders(String visitType) {
		return cohortIndicator("Other disorders due to substance use", map(moh763Cohorts.otherSubstanceUseDisorders(visitType), PARAMS));
	}

	public CohortIndicator otherBehaviouralAddictions(String visitType) {
		return cohortIndicator("Other behavioral addictions", map(moh763Cohorts.otherBehaviouralAddictions(visitType), PARAMS));
	}

	public CohortIndicator neurocognitiveDisorders(String visitType) {
		return cohortIndicator("Neurocognitive disorders", map(moh763Cohorts.neurocognitiveDisorders(visitType), PARAMS));
	}

	public CohortIndicator stressTraumaRelatedDisorders(String visitType) {
		return cohortIndicator("Stress-trauma related disorders", map(moh763Cohorts.stressTraumaRelatedDisorders(visitType), PARAMS));
	}

	public CohortIndicator somatoformDisorders(String visitType) {
		return cohortIndicator("Somatoform disorders", map(moh763Cohorts.somatoformDisorders(visitType), PARAMS));
	}

	public CohortIndicator suicidalBehaviour(String visitType) {
		return cohortIndicator("Suicidal behaviour", map(moh763Cohorts.suicidalBehaviour(visitType), PARAMS));
	}

	public CohortIndicator sleepWakeDisorders(String visitType) {
		return cohortIndicator("Sleep-wake disorders", map(moh763Cohorts.sleepWakeDisorders(visitType), PARAMS));
	}

	public CohortIndicator perinatalMentalDisorders(String visitType) {
		return cohortIndicator("Perinatal mental disorders", map(moh763Cohorts.perinatalMentalDisorders(visitType), PARAMS));
	}

	public CohortIndicator eatingDisorders(String visitType) {
		return cohortIndicator("Eating disorders", map(moh763Cohorts.eatingDisorders(visitType), PARAMS));
	}

	public CohortIndicator personalityDisorders(String visitType) {
		return cohortIndicator("Personality disorders", map(moh763Cohorts.personalityDisorders(visitType), PARAMS));
	}

	public CohortIndicator sexualDysfunctionalDisorders(String visitType) {
		return cohortIndicator("Sexual dysfunctional disorders", map(moh763Cohorts.sexualDysfunctionalDisorders(visitType), PARAMS));
	}

	public CohortIndicator obsessiveCompulsiveAndRelatedDisorders(String visitType) {
		return cohortIndicator("Obsessive-compulsive & related disorders", map(moh763Cohorts.obsessiveCompulsiveAndRelatedDisorders(visitType), PARAMS));
	}

	public CohortIndicator disruptiveImpulseControlAndConductDisorder(String visitType) {
		return cohortIndicator("Disruptive impulse control and conduct disorder", map(moh763Cohorts.disruptiveImpulseControlAndConductDisorder(visitType), PARAMS));
	}
}
