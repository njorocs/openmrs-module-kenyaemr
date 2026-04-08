/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.mat;

import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyaemr.reporting.indicator.HivCareVisitsIndicator;
import org.openmrs.module.kenyaemr.reporting.library.moh731.Moh731CohortLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.hiv.art.ArtCohortLibrary;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.indicator.Indicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;
import static org.openmrs.module.kenyaemr.reporting.EmrReportingUtils.cohortIndicator;

/**
 * Indicators specific to the MOH731 MAT report
 */
@Component
public class Moh731MatIndicatorLibrary {

	@Autowired
	private Moh731MatCohortLibrary moh731MatCohorts;

	/**
	 * Number of all patients currently in MAT
	 * @return the indicator
	 */
	public CohortIndicator matAllNumberInducted() {
		return cohortIndicator("Individuals Ever Inducted in MAT", map(moh731MatCohorts.matNumberInducted(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT in reporting period
	 * @return the indicator
	 */
    public CohortIndicator matNumberInductedInReportingPeriod() {
        return cohortIndicator("Individuals Inducted in MAT in reporting period", map(moh731MatCohorts.matNumberInductedInReportingPeriod(), "startDate=${startDate},endDate=${endDate}"));
    }

	/**
	 * Number of patients who are inducted in MAT on methadone
	 * @return the indicator
	 */
	public CohortIndicator matNumberOnMethadone() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine", map(moh731MatCohorts.matNumberOnMethadone(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT on Buprenorphine
	 * @return the indicator
	 */
	public CohortIndicator matNumberOnBuprenorphine() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine", map(moh731MatCohorts.matNumberOnBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT on methadone and in transit
	 * @return the indicator
	 */
	public CohortIndicator matNumberOnMethadoneInTransit() {
		return cohortIndicator("MAT enrolled Clients on Methadone and in transit", map(moh731MatCohorts.matNumberOnMethadoneInTransit(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT on Buprenorphine in transit
	 * @return the indicator
	 */
	public CohortIndicator matNumberOnBuprenorphineInTransit() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine and in transit", map(moh731MatCohorts.matNumberOnBuprenorphineInTransit(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT Weaned off methadone
	 * @return the indicator
	 */
	public CohortIndicator matNumberWeanedOffMethadone() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine and in transit", map(moh731MatCohorts.matNumberWeanedOffMethadone(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT Weaned off Buprenorphine
	 * @return the indicator
	 */
	public CohortIndicator matNumberWeanedOffBuprenorphine() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine and in transit", map(moh731MatCohorts.matNumberWeanedOffBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT experienced overdose
	 * @return the indicator
	 */
	public CohortIndicator matNumberExperienceOverdose() {
		return cohortIndicator("MAT enrolled Clients experienced overdose", map(moh731MatCohorts.matNumberExperienceOverdose(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT received intervention
	 * @return the indicator
	 */
	public CohortIndicator matNumberReceivedInterventions() {
		return cohortIndicator("MAT enrolled Clients received Psychosocial Interventions", map(moh731MatCohorts.matNumberReceivedInterventions(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT reintegrated
	 * @return the indicator
	 */
	public CohortIndicator matNumberSupportedWithReintegration() {
		return cohortIndicator("MAT enrolled Clients Supported with Reintegration", map(moh731MatCohorts.matNumberSupportedWithReintegration(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT experience GBV
	 * @return the indicator
	 */
	public CohortIndicator matNumberExperienceViolence() {
		CohortIndicator ci =  cohortIndicator("MAT enrolled Clients experience violence", map(moh731MatCohorts.matNumberExperienceViolence(), "typeOfViolence=${typeOfViolence},startDate=${startDate},endDate=${endDate}"));
		ci.addParameter(new Parameter("typeOfViolence", "Type Of Violence", String.class));

		return ci;
	}
	/**
	 * Number of patients who are inducted in MAT given GBV support
	 * @return the indicator
	 */
	public CohortIndicator matNumberReceivedViolenceSupport() {
		return cohortIndicator("MAT enrolled Clients given violence support", map(moh731MatCohorts.matNumberReceivedViolenceSupport(), "startDate=${startDate},endDate=${endDate}"));
	}
//	public CohortIndicator revisitsArt() {
//		return cohortIndicator("Revisits ART", ReportUtils.map(moh731MatCohorts.revisitsArt(), "fromDate=${startDate},toDate=${endDate}"));
//	}
//
//	/**
//	 * Number of patients who are currently on ART
//	 * @return the indicator
//	 */
//	public CohortIndicator currentlyOnArt() {
//		return cohortIndicator("Currently on ART", ReportUtils.map(moh731MatCohorts.currentlyOnArt(), "fromDate=${startDate},toDate=${endDate}"));
//	}
//
//	/**
//	 * Cumulative number of patients on ART
//	 * @return the indicator
//	 */
//	public CohortIndicator cumulativeOnArt() {
//		return cohortIndicator("Cumulative ever on ART", ReportUtils.map(artCohorts.startedArtExcludingTransferinsOnDate(), "onOrBefore=${endDate}"));
//	}
//
//	/**
//	 * Number of patients in the ART 12 month cohort
//	 * @return the indicator
//	 */
//	public CohortIndicator art12MonthNetCohort() {
//		//add a hacky way to determine if art start date is at the end of every month then add one day
//		//to avoid reporting twice in the previouse and the following month
//		return cohortIndicator("ART 12 Month Net Cohort", ReportUtils.map(artCohorts.netCohortMonths(12), "onDate=${endDate + 1d}"));
//	}
//
//	/**
//	 * Number of patients in the 12 month cohort who are on their original first-line regimen
//	 * @return the indicator
//	 */
//	public CohortIndicator onOriginalFirstLineAt12Months() {
//		return cohortIndicator("On original 1st line at 12 months", ReportUtils.map(moh731MatCohorts.onOriginalFirstLineAt12Months(), "fromDate=${startDate},toDate=${endDate + 1d}"));
//	}
//
//	/**
//	 * Number of patients in the 12 month cohort who are on an alternate first-line regimen
//	 * @return the indicator
//	 */
//	public CohortIndicator onAlternateFirstLineAt12Months() {
//		return cohortIndicator("On alternate 1st line at 12 months", ReportUtils.map(moh731MatCohorts.onAlternateFirstLineAt12Months(), "fromDate=${startDate},toDate=${endDate + 1d}"));
//	}
//
//	/**
//	 * Number of patients in the 12 month cohort who are on a second-line regimen
//	 * @return the indicator
//	 */
//	public CohortIndicator onSecondLineAt12Months() {
//		return cohortIndicator("On 2nd line at 12 months", ReportUtils.map(moh731MatCohorts.onSecondLineAt12Months(), "fromDate=${startDate},toDate=${endDate + 1d}"));
//	}
//
//	/**
//	 * Number of patients in the 12 month cohort who are on ART
//	 * @return the indicator
//	 */
//	public CohortIndicator onTherapyAt12Months() {
//		return cohortIndicator("On therapy at 12 months", ReportUtils.map(moh731MatCohorts.onTherapyAt12Months(), "fromDate=${startDate},toDate=${endDate + 1d}"));
//	}
//
//	/**
//	 * Number of HIV care visits for females aged 18 and over
//	 * @return the indicator
//	 */
//	public Indicator hivCareVisitsFemale18() {
//		HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
//		ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		ind.addParameter(new Parameter("endDate", "End Date", Date.class));
//		ind.setFilter(HivCareVisitsIndicator.Filter.FEMALES_18_AND_OVER);
//		return ind;
//	}
//
//	/**
//	 * Number of scheduled HIV care visits
//	 * @return the indicator
//	 */
//	public Indicator hivCareVisitsScheduled() {
//		HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
//		ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		ind.addParameter(new Parameter("endDate", "End Date", Date.class));
//		ind.setFilter(HivCareVisitsIndicator.Filter.SCHEDULED);
//		return ind;
//	}
//
//	/**
//	 * Number of unscheduled HIV care visits
//	 * @return the indicator
//	 */
//	public Indicator hivCareVisitsUnscheduled() {
//		HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
//		ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		ind.addParameter(new Parameter("endDate", "End Date", Date.class));
//		ind.setFilter(HivCareVisitsIndicator.Filter.UNSCHEDULED);
//		return ind;
//	}
//
//	/**
//	 * Total number of HIV care visits
//	 * @return the indicator
//	 */
//	public Indicator hivCareVisitsTotal() {
//		HivCareVisitsIndicator ind = new HivCareVisitsIndicator();
//		ind.addParameter(new Parameter("startDate", "Start Date", Date.class));
//		ind.addParameter(new Parameter("endDate", "End Date", Date.class));
//		return ind;
//	}
}