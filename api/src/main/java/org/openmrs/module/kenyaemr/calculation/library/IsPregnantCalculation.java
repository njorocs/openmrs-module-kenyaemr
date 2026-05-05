/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.kenyacore.CoreUtils.latest;

/**
 * Calculates the recorded pregnancy status of patients
 */
public class IsPregnantCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {

	@Override
	public String getFlagMessage() {
		return "Pregnant";
	}

    /**
	 * Evaluates the calculation
     * @should calculate false for deceased patients
	 * @should calculate false for patients with no recorded status
	 * @should calculate last recorded pregnancy status for all patients
     */
    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		Set<Integer> aliveAndFemale = Filters.female(Filters.alive(cohort, context), context);
		EncounterType mchEnrollment = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ENROLLMENT);
		EncounterType ancEnrollment = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_ENROLLMENT);
		EncounterType mchDiscontinuation = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_DISCONTINUATION);
		EncounterType ancDiscontinuation = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_DISCONTINUATION);

		Concept yes = Dictionary.getConcept(Dictionary.YES);
		final Concept MCH_SERVICE_TYPE = Dictionary.getConcept(Dictionary.MCH_SERVICE_TYPE);
		final Concept ANC_SERVICE = Dictionary.getConcept(Dictionary.ANC_SERVICE);

		CalculationResultMap pregStatusObss = Calculations.lastObs(Dictionary.getConcept(Dictionary.PREGNANCY_STATUS), aliveAndFemale, context);
		CalculationResultMap ret = new CalculationResultMap();
		CalculationResultMap mchEnrollmentMap = Calculations.lastEncounter(mchEnrollment, aliveAndFemale, context);
		CalculationResultMap ancEnrollmentMap = Calculations.lastEncounter(ancEnrollment, aliveAndFemale, context);
		CalculationResultMap mchDiscontinuationMap = Calculations.lastEncounter(mchDiscontinuation, aliveAndFemale, context);
		CalculationResultMap ancDiscontinuationMap = Calculations.lastEncounter(ancDiscontinuation, aliveAndFemale, context);
		CalculationResultMap confinementDateMap = Calculations.lastObs(Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT), aliveAndFemale, context);

		for (Integer ptId : cohort) {
			Obs pregStatusObs = EmrCalculationUtils.obsResultForPatient(pregStatusObss, ptId);
			Encounter mchEncounter = EmrCalculationUtils.encounterResultForPatient(mchEnrollmentMap, ptId);
			Encounter ancEncounter = EmrCalculationUtils.encounterResultForPatient(ancEnrollmentMap, ptId);
			Encounter encounterMCHDiscontinuation = EmrCalculationUtils.encounterResultForPatient(mchDiscontinuationMap, ptId);
			Encounter ancEncounterDiscontinuation = EmrCalculationUtils.encounterResultForPatient(ancDiscontinuationMap, ptId);
			Obs confinement = EmrCalculationUtils.obsResultForPatient(confinementDateMap, ptId);

			Date latestPositiveEvidence = null;
			Date latestNegativeEvidence = null;

			boolean mchANCEntry = mchEncounter != null && EmrUtils.encounterThatPassCodedAnswer(mchEncounter, MCH_SERVICE_TYPE, ANC_SERVICE);

			if (pregStatusObs != null) {
				if (yes.equals(pregStatusObs.getValueCoded())) {
					latestPositiveEvidence = latest(latestPositiveEvidence, pregStatusObs.getObsDatetime());
				}
				else {
					latestNegativeEvidence = latest(latestNegativeEvidence, pregStatusObs.getObsDatetime());
				}
			}

			if (isActiveEnrollment(mchEncounter, encounterMCHDiscontinuation) && mchANCEntry) {
				latestPositiveEvidence = latest(latestPositiveEvidence, mchEncounter.getEncounterDatetime());
			}
			if (isActiveEnrollment(ancEncounter, ancEncounterDiscontinuation)) {
				latestPositiveEvidence = latest(latestPositiveEvidence, ancEncounter.getEncounterDatetime());
			}

			if (encounterMCHDiscontinuation != null) {
				latestNegativeEvidence = latest(latestNegativeEvidence, encounterMCHDiscontinuation.getEncounterDatetime());
			}
			if (ancEncounterDiscontinuation != null) {
				latestNegativeEvidence = latest(latestNegativeEvidence, ancEncounterDiscontinuation.getEncounterDatetime());
			}
			if (confinement != null && confinement.getValueDatetime() != null) {
				latestNegativeEvidence = latest(latestNegativeEvidence, confinement.getValueDatetime());
			}

			boolean result = latestPositiveEvidence != null && (latestNegativeEvidence == null || !latestPositiveEvidence.before(latestNegativeEvidence));

			ret.put(ptId, new BooleanResult(result, this));
		}
		return ret;
    }

	private static boolean isActiveEnrollment(Encounter enrollment, Encounter discontinuation) {
		return enrollment != null && (discontinuation == null || discontinuation.getEncounterDatetime().before(enrollment.getEncounterDatetime()));
	}

}
