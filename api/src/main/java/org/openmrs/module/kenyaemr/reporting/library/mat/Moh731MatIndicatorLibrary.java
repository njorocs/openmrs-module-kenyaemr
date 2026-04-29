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
	 * Number of all patients weaned off MAT
	 * @return the indicator
	 */
	public CohortIndicator matAllNumberWeanedOff() {
		return cohortIndicator("Individuals Ever Weaned off MAT", map(moh731MatCohorts.matNumberWeanedOff(), "startDate=${startDate},endDate=${endDate}"));
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
	 * Number of patients who are inducted in MAT Weaned off methadone and buprenorphine
	 * @return the indicator
	 */
	public CohortIndicator matNumberWeanedOffMethadone() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine and in transit", map(moh731MatCohorts.matNumberWeanedOffMethadone(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberWeanedOffBuprenorphine() {
		return cohortIndicator("MAT enrolled Clients on Buprenorphine and in transit", map(moh731MatCohorts.matNumberWeanedOffBuprenorphine(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT
	 * @return 1.5 MAT INTERUPTIONS
	 */
	public CohortIndicator matNumberDiscontinued() {
		return cohortIndicator("MAT enrolled Clients discoontinued from MAT", map(moh731MatCohorts.matNumberDiscontinued(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberDied() {
		return cohortIndicator("MAT enrolled Clients died", map(moh731MatCohorts.matNumberDied(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberMissingDosage() {
		return cohortIndicator("MAT enrolled Clients missing Buprenorphine doses", map(moh731MatCohorts.matNumberMissingDoses(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberLTFU() {
		return cohortIndicator("MAT enrolled Clients LTFU", map(moh731MatCohorts.matNumberLTFU(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT Weaned off Buprenorphine
	 * @return 1.6 HIV TESTING
	 */
	public CohortIndicator matNumberTestedHIV() {
		return cohortIndicator("MAT enrolled Clients tested HIV", map(moh731MatCohorts.matNumberTestedHIV(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberHIVpositive() {
		return cohortIndicator("MAT enrolled Clients HIV positive", map(moh731MatCohorts.matNumberHIVpositive(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberStartedART() {
		return cohortIndicator("MAT enrolled Clients started ART", map(moh731MatCohorts.matNumberStartedART(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberTotalHIVpositive() {
		return cohortIndicator("Total MAT enrolled Clients HIV positive", map(moh731MatCohorts.matNumberTotalHIVpositive(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberTotalStartedART() {
		return cohortIndicator("Total MAT enrolled Clients started ART", map(moh731MatCohorts.matNumberTotalStartedART(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT experienced overdose
	 * @return 2.1 Viral load tracking MAT Clients
	 */
	public CohortIndicator matNumberViralLoadResult() {
		return cohortIndicator("MAT enrolled Clients with Viral load result in the last 12 months", map(moh731MatCohorts.matNumberViralLoadResult(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberViralLoadResultLt200() {
		return cohortIndicator("MAT enrolled Clients with Viral load result less than 200", map(moh731MatCohorts.matNumberViralLoadResultLt200(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberViralLoadResultLt50() {
		return cohortIndicator("MAT enrolled Clients with Viral load result less than 50", map(moh731MatCohorts.matNumberViralLoadResultLt50(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT experienced overdose
	 * @return 2.2 Overdose MAT clients
	 */
	public CohortIndicator matNumberExperienceOverdose() {
		return cohortIndicator("MAT enrolled Clients experienced overdose", map(moh731MatCohorts.matNumberExperienceOverdose(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberReceivedNaloxone() {
		return cohortIndicator("MAT enrolled Clients received naloxone", map(moh731MatCohorts.matNumberReceivedNaloxone(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberOverdoseDeaths() {
		return cohortIndicator("MAT enrolled Clients overdose deaths", map(moh731MatCohorts.matNumberOverdoseDeaths(), "startDate=${startDate},endDate=${endDate}"));
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

	/**
	 * Number of patients who are inducted in MAT screened MH
	 * @return the indicator
	 */
	public CohortIndicator matNumberScreenedMH() {
		return cohortIndicator("MAT enrolled Clients screened MH", map(moh731MatCohorts.matNumberScreenedMH(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT treated MH
	 * @return the indicator
	 */
	public CohortIndicator matNumberTreatedWithinFacilityMH() {
		return cohortIndicator("MAT enrolled Clients treated within facility MH", map(moh731MatCohorts.matNumberTreatedWithinFacilityMH(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT screened STI
	 * @return the indicator
	 */
	public CohortIndicator matNumberScreenedSTI() {
		return cohortIndicator("MAT enrolled Clients screened STI", map(moh731MatCohorts.matNumberScreenedSTI(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT treated STI
	 * @return the indicator
	 */
	public CohortIndicator matNumberTreatedSTI() {
		return cohortIndicator("MAT enrolled Clients treated STI", map(moh731MatCohorts.matNumberTreatedSTI(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT screened HCV in reporting period
	 * @return the indicator
	 */
	public CohortIndicator matNumberScreenedHCV() {
		return cohortIndicator("MAT enrolled Clients screened HCV", map(moh731MatCohorts.matNumberScreenedHCV(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT positive HCV in reporting period
	 * @return the indicator
	 */
	public CohortIndicator matNumberPositiveHCV() {
		return cohortIndicator("MAT enrolled Clients positive HCV", map(moh731MatCohorts.matNumberPositiveHCV(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberPositiveHCVConfirmatoryPCRtest() {
		return cohortIndicator("MAT enrolled Clients positive HCV(Confirmatory PCR Test)", map(moh731MatCohorts.matNumberPositiveHCVConfirmatoryPCRtest(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberTreatedHCV() {
		return cohortIndicator("MAT enrolled Clients treated HCV", map(moh731MatCohorts.matNumberTreatedHCV(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT All treated HCV ever
	 * @return the indicator
	 */
	public CohortIndicator matTotalNumberTreatedHCV() {
		return cohortIndicator("All MAT enrolled Clients treated HCV ever", map(moh731MatCohorts.matTotalNumberTreatedHCV(), "startDate=${startDate},endDate=${endDate}"));
	}

	/**
	 * Number of patients who are inducted in MAT screened HBV in reporting period
	 * @return the indicator
	 */
	public CohortIndicator matNumberScreenedHBV() {
		return cohortIndicator("MAT enrolled Clients screened HBV", map(moh731MatCohorts.matNumberScreenedHBV(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberNegativeHBV() {
		return cohortIndicator("MAT enrolled Clients negative result HBV", map(moh731MatCohorts.matNumberNegativeHBV(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberNegativeHBVvaccinated() {
		return cohortIndicator("MAT enrolled Clients negative HBV and vaccinated", map(moh731MatCohorts.matNumberNegativeHBVvaccinated(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT positive HBV in reporting period
	 * @return the indicator
	 */
	public CohortIndicator matNumberPositiveHBV() {
		return cohortIndicator("MAT enrolled Clients positive HBV", map(moh731MatCohorts.matNumberPositiveHBV(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberPositiveHBVConfirmatoryPCRtest() {
		return cohortIndicator("MAT enrolled Clients positive HBV(Confirmatory PCR Test)", map(moh731MatCohorts.matNumberPositiveHBVConfirmatoryPCRtest(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT  treated HBV in reporting period
	 * @return the indicator
	 */
	public CohortIndicator matNumberTreatedHBV() {
		return cohortIndicator("MAT enrolled Clients treated HBV", map(moh731MatCohorts.matNumberTreatedHBV(), "startDate=${startDate},endDate=${endDate}"));
	}
	/**
	 * Number of patients who are inducted in MAT All treated HBV ever
	 * @return the indicator
	 */
	public CohortIndicator matTotalNumberTreatedHBV() {
		return cohortIndicator("All MAT enrolled Clients treated HBV ever", map(moh731MatCohorts.matTotalNumberTreatedHBV(), "startDate=${startDate},endDate=${endDate}"));
	}

	public CohortIndicator matNumberScreenedTB() {
		return cohortIndicator("All MAT enrolled Clients screened TB", map(moh731MatCohorts.matNumberScreenedTB(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberDiagnosedTB() {
		return cohortIndicator("All MAT enrolled Clients diagnosed TB", map(moh731MatCohorts.matNumberDiagnosedTB(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberStartedTBRX() {
		return cohortIndicator("All MAT enrolled Clients treated HBV ever", map(moh731MatCohorts.matNumberStartedTBRX(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberStartedTPT() {
		return cohortIndicator("All MAT enrolled Clients treated HBV ever", map(moh731MatCohorts.matNumberStartedTPT(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matTotalNumberTBclientsHIVpositive() {
		return cohortIndicator("All MAT enrolled Clients treated HBV ever", map(moh731MatCohorts.matTotalNumberTBclientsHIVpositive(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matTotalNumberTBClientsOnHAART() {
		return cohortIndicator("All MAT enrolled Clients treated HBV ever", map(moh731MatCohorts.matTotalNumberTBClientsOnHAART(), "startDate=${startDate},endDate=${endDate}"));
	}

	public CohortIndicator matNumberMATClientsSAM() {
		CohortIndicator ci =  cohortIndicator("MAT enrolled Clients experience violence", map(moh731MatCohorts.matNumberMATClientsSAM(), "nutritionalStatus=${nutritionalStatus},startDate=${startDate},endDate=${endDate}"));
		ci.addParameter(new Parameter("nutritionalStatus", "Nutritional status", String.class));

		return ci;
	}

	public CohortIndicator matNumberInitiatedNutritionSupport() {
		return cohortIndicator("MAT enrolled Clients SAM Severe", map(moh731MatCohorts.matNumberInitiatedNutritionSupport(), "startDate=${startDate},endDate=${endDate}"));
	}

	public CohortIndicator matNumberExposedToHIV() {
		return cohortIndicator("MAT enrolled Clients SAM Moderate", map(moh731MatCohorts.matNumberExposedToHIV(), "startDate=${startDate},endDate=${endDate}"));
	}

	public CohortIndicator matNumberReceivePEPlt72hrs() {
		return cohortIndicator("MAT enrolled Clients initiated on nutrition support", map(moh731MatCohorts.matNumberReceivePEPlt72hrs(), "startDate=${startDate},endDate=${endDate}"));
	}

	public CohortIndicator matNumberInitiatedPrep() {
		return cohortIndicator("MAT enrolled Clients Initiated PrEP", map(moh731MatCohorts.matNumberInitiatedPrep(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberHIVPositiveOnPrEP() {
		return cohortIndicator("MAT enrolled Clients HIV_positive while on PrEt", map(moh731MatCohorts.matNumberHIVPositiveOnPrEP(), "startDate=${startDate},endDate=${endDate}"));
	}
	public CohortIndicator matNumberPrEPClientDiagnosedSTIs() {
		return cohortIndicator("MAT enrolled Clients of PrEP users diagnosed with STIs", map(moh731MatCohorts.matNumberPrEPClientDiagnosedSTIs(), "startDate=${startDate},endDate=${endDate}"));
	}
}