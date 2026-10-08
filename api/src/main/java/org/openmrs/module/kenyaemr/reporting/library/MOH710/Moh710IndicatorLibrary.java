/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.MOH710;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;
import static org.openmrs.module.kenyaemr.reporting.EmrReportingUtils.cohortIndicator;

import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Moh710IndicatorLibrary {

	@Autowired
	private Moh710CohortLibrary moh710CohortLibrary;

	public CohortIndicator vaccinatedOnDay(Integer minAge, Integer maxAge, Integer sequence, Integer... vaccines) {
		return onDay(moh710CohortLibrary.vaccinatedOnDay(minAge, maxAge, sequence, vaccines));
	}

	public CohortIndicator vaccinatedTotal(Integer minAge, Integer maxAge, Integer sequence, Integer... vaccines) {
		return total(moh710CohortLibrary.vaccinatedTotal(minAge, maxAge, sequence, vaccines));
	}

	public CohortIndicator fullyImmunizedOnDay(Integer minAge, Integer maxAge) {
		return onDay(moh710CohortLibrary.fullyImmunizedOnDay(minAge, maxAge));
	}

	public CohortIndicator fullyImmunizedTotal(Integer minAge, Integer maxAge) {
		return total(moh710CohortLibrary.fullyImmunizedTotal(minAge, maxAge));
	}

	private CohortIndicator onDay(CohortDefinition cd) {
		CohortIndicator ind = cohortIndicator(null, map(cd, "startDate=${startDate},endDate=${endDate},dayOfMonth=${dayOfMonth}"));
		ind.addParameter(new org.openmrs.module.reporting.evaluation.parameter.Parameter("dayOfMonth", "Day of Month", Integer.class));
		return ind;
	}

	private CohortIndicator total(CohortDefinition cd) {
		return cohortIndicator(null, map(cd, "startDate=${startDate},endDate=${endDate}"));
	}
}


