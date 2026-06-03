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
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.kenyacore.CoreUtils.latest;

/**
 * Calculates the recorded breastfeeding status of patients
 */
public class IsBreastFeedingCalculation extends AbstractPatientCalculation {

	/**
	 * Evaluates the calculation
	 * @should calculate false for deceased patients
	 * @should calculate false for patients with no recorded status
	 * @should calculate last recorded breastfeeding status for all patients on PNC
	 * @should calculate last recorded breastfeeding status for all patients on Hiv Greencard
	 */
	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
		Set<Integer> aliveAndFemale = Filters.female(Filters.alive(cohort, context), context);
		EncounterType pncEnrollment = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_PNC_ENROLLMENT);
		EncounterType pncDiscontinuation = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_PNC_DISCONTINUATION);
		EncounterType mchDiscontinuation = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_DISCONTINUATION);

		Concept exclusiveBreastFeeding = Dictionary.getConcept(Dictionary.BREASTFED_EXCLUSIVELY);
		Concept mixedBreastFeeding = Dictionary.getConcept(Dictionary.MIXED_FEEDING);
		Concept yes = Dictionary.getConcept(Dictionary.YES);
		Concept breastFeedingFollowupQuestion = Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING);

		CalculationResultMap infantFeedingStatusObs = Calculations.lastObs(Dictionary.getConcept(Dictionary.INFANT_FEEDING_METHOD), aliveAndFemale, context);
		CalculationResultMap breastFeedingFollowupObs = Calculations.lastObs(breastFeedingFollowupQuestion, aliveAndFemale, context);
		CalculationResultMap pncEnrollmentMap = Calculations.lastEncounter(pncEnrollment, aliveAndFemale, context);
		CalculationResultMap pncDiscontinuationMap = Calculations.lastEncounter(pncDiscontinuation, aliveAndFemale, context);
		CalculationResultMap mchDiscontinuationMap = Calculations.lastEncounter(mchDiscontinuation, aliveAndFemale, context);

		CalculationResultMap ret = new CalculationResultMap();
		for (Integer ptId : aliveAndFemale) {
			Obs feedingStatusObs = EmrCalculationUtils.obsResultForPatient(infantFeedingStatusObs, ptId);
			Obs breastFeedingStatusObs = EmrCalculationUtils.obsResultForPatient(breastFeedingFollowupObs, ptId);
			Encounter pncEnrollmentEncounter = EmrCalculationUtils.encounterResultForPatient(pncEnrollmentMap, ptId);
			Encounter pncDiscontinuationEncounter = EmrCalculationUtils.encounterResultForPatient(pncDiscontinuationMap, ptId);
			Encounter mchDiscontinuationEncounter = EmrCalculationUtils.encounterResultForPatient(mchDiscontinuationMap, ptId);

			Date latestPositiveEvidence = null;
			Date latestNegativeEvidence = null;

			if (feedingStatusObs != null) {
				Concept feedingStatus = feedingStatusObs.getValueCoded();
				if (exclusiveBreastFeeding.equals(feedingStatus) || mixedBreastFeeding.equals(feedingStatus)) {
					latestPositiveEvidence = latest(latestPositiveEvidence, feedingStatusObs.getObsDatetime());
				}
				else {
					latestNegativeEvidence = latest(latestNegativeEvidence, feedingStatusObs.getObsDatetime());
				}
			}

			if (breastFeedingStatusObs != null) {
				if (yes.equals(breastFeedingStatusObs.getValueCoded())) {
					latestPositiveEvidence = latest(latestPositiveEvidence, breastFeedingStatusObs.getObsDatetime());
				}
				else {
					latestNegativeEvidence = latest(latestNegativeEvidence, breastFeedingStatusObs.getObsDatetime());
				}
			}

			if (isActiveEnrollment(pncEnrollmentEncounter, pncDiscontinuationEncounter)) {
				latestPositiveEvidence = latest(latestPositiveEvidence, pncEnrollmentEncounter.getEncounterDatetime());
			}

			if (pncDiscontinuationEncounter != null) {
				latestNegativeEvidence = latest(latestNegativeEvidence, pncDiscontinuationEncounter.getEncounterDatetime());
			}
			if (mchDiscontinuationEncounter != null) {
				latestNegativeEvidence = latest(latestNegativeEvidence, mchDiscontinuationEncounter.getEncounterDatetime());
			}

			boolean breastFeeding = latestPositiveEvidence != null
					&& (latestNegativeEvidence == null || !latestPositiveEvidence.before(latestNegativeEvidence));
			ret.put(ptId, new BooleanResult(breastFeeding, this));
		}

		return ret;
	}

	private static boolean isActiveEnrollment(Encounter enrollment, Encounter discontinuation) {
		return enrollment != null && (discontinuation == null || discontinuation.getEncounterDatetime().before(enrollment.getEncounterDatetime()));
	}
}
