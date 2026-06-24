/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.mchms;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.GreenCardVelocityCalculation;
import org.openmrs.module.kenyaemr.calculation.library.mchcs.InfantMotherGuardianRelationsCalculation;
import org.openmrs.module.kenyaemr.util.EmrUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MotherToBabyPairRiskCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {

	private String mtctFlagMessage = "";

	@Override
	public String getFlagMessage() {
		return mtctFlagMessage;
	}

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		CalculationResultMap infantRelationsMap = calculate(new InfantMotherGuardianRelationsCalculation(), cohort, context);

		Set<Integer> allParentIds = new HashSet<Integer>();
		for (Integer ptId : cohort) {
			if (infantRelationsMap.get(ptId) != null && infantRelationsMap.get(ptId).getValue() != null) {
				allParentIds.addAll((Set<Integer>) infantRelationsMap.get(ptId).getValue());
			}
		}

		// Include cohort members themselves so pre-fetched maps cover mothers evaluated directly
		// (i.e. when the mother, not an infant, is the cohort member).
		Set<Integer> allEvaluationIds = new HashSet<Integer>(allParentIds);
		allEvaluationIds.addAll(cohort);

		CalculationResultMap greenCardMap = calculate(new GreenCardVelocityCalculation(), allEvaluationIds, context);

		Concept tcaConcept = Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE);
		Concept stiScreeningConcept = Dictionary.getConcept(Dictionary.STI_SCREENING);
		// KEY POP - PWID, FSW
		Concept keyPopulationConcept = Context.getConceptService().getConceptByUuid(Dictionary.HIV_RISK_FACTOR);
		Concept peopleWhoInjectDrugsConcept = Context.getConceptService().getConceptByUuid(Dictionary.PEOPLE_WHO_INJECT_DRUGS);
		Concept fswConcept = Context.getConceptService().getConceptByUuid(Dictionary.FEMALE_SEX_WORKER);
		Concept usedAlcoholOrSubstanceUseConcept = Context.getConceptService().getConceptByUuid(Dictionary.PEOPLE_WHO_USE_SUBSTANCES);

		CalculationResultMap lastTcaMap = Calculations.lastObs(tcaConcept, allEvaluationIds, context);
		CalculationResultMap lastStiMap = Calculations.lastObs(stiScreeningConcept, allEvaluationIds, context);
		CalculationResultMap lastKeyPopulationMap = Calculations.lastObs(keyPopulationConcept, allEvaluationIds, context);

		Date now = context.getNow();
		CalculationResultMap ret = new CalculationResultMap();

		for (Integer ptId : cohort) {
			mtctFlagMessage = "";

			Patient patient = Context.getPatientService().getPatient(ptId);
			Date birthDate = patient.getBirthdate();
			int infantAgeMonths = (birthDate != null) ? Months.monthsBetween(new DateTime(birthDate), new DateTime(now)).getMonths() : 0;
			boolean childUnder24Months = infantAgeMonths < 24;

			Set<Integer> motherIds = Collections.emptySet();
			if (infantRelationsMap.get(ptId) != null && infantRelationsMap.get(ptId).getValue() != null) {
				motherIds = (Set<Integer>) infantRelationsMap.get(ptId).getValue();
			}

			if (motherIds.isEmpty()) {
				boolean isFemale = "F".equalsIgnoreCase(patient.getGender());
				boolean hasChildren = !EmrUtils.getPersonChildren(patient).isEmpty();
				if (!childUnder24Months && isFemale && hasChildren) {
					// Cohort member is the mother herself (female, has children, no guardian relations found).
					motherIds = Collections.singleton(ptId);
				} else {
					ret.put(ptId, new BooleanResult(false, this));
					continue;
				}
			}

			boolean anyMotherHighRisk = false;
			boolean anyMotherLowRisk = false;
			boolean isSelfEvaluation = motherIds.contains(ptId);

			for (Integer motherId : motherIds) {

				Patient mother = Context.getPatientService().getPatient(motherId);
				int motherAgeYears = (mother != null && mother.getAge() != null) ? mother.getAge() : Integer.MAX_VALUE;
				boolean motherAgeUnder19 = motherAgeYears < 19;

				String greenCardResult = "";
				if (greenCardMap.get(motherId) != null && greenCardMap.get(motherId).getValue() != null) {
					greenCardResult = greenCardMap.get(motherId).getValue().toString();
				}

				boolean onArt = EmrUtils.parseKeyValueBoolean(greenCardResult, "onART");
				int artDurationMonths = EmrUtils.parseKeyValueInt(greenCardResult, "duration");
				double vlResult = EmrUtils.parseKeyValueDouble(greenCardResult, "vlResult");
				String ldlResult = EmrUtils.parseKeyValueString(greenCardResult, "ldlResult");
				boolean goodAdherence = EmrUtils.parseKeyValueBoolean(greenCardResult, "goodAdherence");
				String firstIllnessControl = EmrUtils.parseKeyValueString(greenCardResult, "firstChronicIllnessControl");
				String secondIllnessControl = EmrUtils.parseKeyValueString(greenCardResult, "secondChronicIllnessControl");

				boolean newlyOnArt = onArt && artDurationMonths < 6;
				boolean vlAvailable = "LDL".equals(ldlResult) || vlResult > 0.0;
				boolean isVlSuppressed = "LDL".equals(ldlResult) || (vlResult > 0.0 && vlResult <= 50.0);
				boolean comorbidityControlled = (firstIllnessControl.isEmpty() || "Controlled".equalsIgnoreCase(firstIllnessControl))
						&& (secondIllnessControl.isEmpty() || "Controlled".equalsIgnoreCase(secondIllnessControl));

				Obs tcaObs = EmrCalculationUtils.obsResultForPatient(lastTcaMap, motherId);
				boolean isIIT = false;
				if (tcaObs != null && tcaObs.getValueDatetime() != null) {
					int daysPastTca = Days.daysBetween(new DateTime(tcaObs.getValueDatetime()), new DateTime(now)).getDays();
					isIIT = daysPastTca > 90;
				}

				Obs stiObs = EmrCalculationUtils.obsResultForPatient(lastStiMap, motherId);
				boolean hasRecentSti = false;
				if (stiObs != null) {
					int daysSinceSti = Math.abs(Days.daysBetween(new DateTime(stiObs.getObsDatetime()), new DateTime(now)).getDays());
					hasRecentSti = daysSinceSti <= 90;
				}

				Concept keyPopulation = EmrCalculationUtils.codedObsResultForPatient(lastKeyPopulationMap, motherId);
				boolean isFswOrPwid = keyPopulation != null && (keyPopulation.equals(fswConcept) || keyPopulation.equals(peopleWhoInjectDrugsConcept) || keyPopulation.equals(usedAlcoholOrSubstanceUseConcept));

				boolean motherHighRisk = motherAgeUnder19
						&& newlyOnArt
						&& (vlAvailable && !isVlSuppressed)
						&& !goodAdherence
						&& isIIT
						&& hasRecentSti
						&& !comorbidityControlled
						&& isFswOrPwid;

				boolean motherLowRisk = isVlSuppressed && goodAdherence && comorbidityControlled;

				if (motherHighRisk) {
					anyMotherHighRisk = true;
				} else if (motherLowRisk) {
					anyMotherLowRisk = true;
				}
			}

			boolean showFlag = false;
			if (anyMotherHighRisk) {
				showFlag = true;
				mtctFlagMessage = "High MTCT Risk";
			} else if (anyMotherLowRisk) {
				showFlag = true;
				mtctFlagMessage = "Low MTCT Risk";
			}

			// Self-evaluation (ptId is the mother): always apply the flag.
			// Infant in cohort: only apply the flag if the infant is under 24 months.
			boolean flagCohortMember = isSelfEvaluation || childUnder24Months;
			ret.put(ptId, new BooleanResult(flagCohortMember && showFlag, this));

			if (isSelfEvaluation) {
				// Mother is the cohort member — propagate the same flag to her children under 24 months
				for (org.openmrs.Person child : EmrUtils.getPersonChildren(patient)) {
					Date childBirthDate = child.getBirthdate();
					int childAgeMonths = (childBirthDate != null) ? Months.monthsBetween(new DateTime(childBirthDate), new DateTime(now)).getMonths() : Integer.MAX_VALUE;
					if (childAgeMonths < 24) {
						ret.put(child.getPersonId(), new BooleanResult(showFlag, this));
					}
				}
			} else {
				// Infant is the cohort member — flag each linked mother/guardian only if child is under 24 months
				for (Integer motherId : motherIds) {
					ret.put(motherId, new BooleanResult(childUnder24Months && showFlag, this));
				}
			}
		}

		return ret;
	}

}
