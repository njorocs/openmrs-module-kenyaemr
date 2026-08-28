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
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.InProgramCalculation;
import org.openmrs.module.kenyaemr.calculation.library.IsPregnantCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.GreenCardVelocityCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.LastReturnVisitDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.mchcs.InfantMotherGuardianRelationsCalculation;
import org.openmrs.module.kenyaemr.util.EmrUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flags a mother-to-child-transmission (MTCT) risk on:
 *  - a pregnant woman, evaluated on her own since the baby is not yet born; or
 *  - a mother and her born child, paired together, where the child is under 24 months and still
 *    active in the HEI (Child Welfare Clinic / MCH-CS) program.
 */
public class MotherToBabyPairRiskCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {

	private static final String HIGH_RISK_MESSAGE = "High MTCT Risk";
	private static final String LOW_RISK_MESSAGE = "Low MTCT Risk";

	private String mtctFlagMessage = "";

	@Override
	public String getFlagMessage() {
		return mtctFlagMessage;
	}

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		Date now = context.getNow();
		CalculationResultMap ret = new CalculationResultMap();
		Map<Integer, Patient> patientsById = fetchPatients(cohort);

		// Split the cohort into infants linked to a mother/guardian via a relationship, and candidate
		// mothers: female cohort members with no such relationship (either pregnant, or with a born baby).
		CalculationResultMap infantRelationsMap = calculate(new InfantMotherGuardianRelationsCalculation(), cohort, context);
		Map<Integer, Set<Integer>> linkedMotherIdsByInfant = new HashMap<Integer, Set<Integer>>();
		Set<Integer> candidateMotherIds = new HashSet<Integer>();
		for (Integer ptId : cohort) {
			Set<Integer> linkedMotherIds = getSetValue(infantRelationsMap, ptId);
			if (!linkedMotherIds.isEmpty()) {
				linkedMotherIdsByInfant.put(ptId, linkedMotherIds);
			} else if (isFemale(patientsById.get(ptId))) {
				candidateMotherIds.add(ptId);
			}
		}

		Map<Integer, List<Person>> childrenByMother = fetchChildren(candidateMotherIds, patientsById);

		// HEI (MCH-CS) program membership is needed for: infants linked via a relationship, and the born
		// children of candidate mothers — batched into a single lookup up front.
		Set<Integer> heiCandidateIds = new HashSet<Integer>(linkedMotherIdsByInfant.keySet());
		for (List<Person> children : childrenByMother.values()) {
			for (Person child : children) {
				heiCandidateIds.add(child.getPersonId());
			}
		}
		CalculationResultMap inHeiProgramMap = calculateHeiProgramMembership(heiCandidateIds, context);
		CalculationResultMap isPregnantMap = calculate(new IsPregnantCalculation(), candidateMotherIds, context);

		// ---- Cohort 1: mothers paired with a born child under 24 months, still active in the HEI program ----
		Map<Integer, List<Person>> pairedChildrenByMother = new HashMap<Integer, List<Person>>();
		// ---- Cohort 2: pregnant mothers, evaluated alone since the baby is not yet born ----
		Set<Integer> pregnantMotherIds = new HashSet<Integer>();

		for (Integer motherId : candidateMotherIds) {
			List<Person> eligibleChildren = isUnder24Months(patientsById.get(motherId), now)
					? Collections.<Person>emptyList()
					: findEligibleHeiChildren(childrenByMother.get(motherId), inHeiProgramMap, now);

			if (!eligibleChildren.isEmpty()) {
				pairedChildrenByMother.put(motherId, eligibleChildren);
			} else if (isTrue(isPregnantMap, motherId)) {
				pregnantMotherIds.add(motherId);
			}
			// else: neither pregnant nor paired with an eligible born child — not evaluated for this flag.
		}

		// ---- Cohort 3: infants/babies in the cohort, linked to mother(s)/guardian(s), still under 24 months and in the HEI program ----
		Map<Integer, Set<Integer>> eligibleInfantMotherIds = new HashMap<Integer, Set<Integer>>();
		for (Map.Entry<Integer, Set<Integer>> entry : linkedMotherIdsByInfant.entrySet()) {
			Integer infantId = entry.getKey();
			if (isUnder24Months(patientsById.get(infantId), now) && isTrue(inHeiProgramMap, infantId)) {
				eligibleInfantMotherIds.put(infantId, entry.getValue());
			}
		}

		// Batch-fetch MTCT risk factors only for the mothers who will actually be assessed above.
		Set<Integer> motherIdsToAssess = new HashSet<Integer>();
		motherIdsToAssess.addAll(pregnantMotherIds);
		motherIdsToAssess.addAll(pairedChildrenByMother.keySet());
		for (Set<Integer> linkedMotherIds : eligibleInfantMotherIds.values()) {
			motherIdsToAssess.addAll(linkedMotherIds);
		}
		MotherRiskMaps riskMaps = fetchMotherRiskMaps(motherIdsToAssess, context);

		// ---- Evaluate pregnant mothers: the flag applies to the mother only ----
		for (Integer motherId : pregnantMotherIds) {
			String riskMessage = assessMtctRisk(Collections.singleton(motherId), now, riskMaps);
			ret.put(motherId, flagResult(riskMessage));
		}

		// ---- Evaluate mother-baby pairs (mother is the cohort member): the flag applies to the mother and her paired child(ren) ----
		for (Map.Entry<Integer, List<Person>> entry : pairedChildrenByMother.entrySet()) {
			Integer motherId = entry.getKey();
			List<Person> children = entry.getValue();
			String riskMessage = assessMtctRisk(Collections.singleton(motherId), now, riskMaps);
			BooleanResult result = flagResult(riskMessage);
			ret.put(motherId, result);
			for (Person child : children) {
				ret.put(child.getPersonId(), result);
			}
		}

		// ---- Evaluate mother-baby pairs (infant is the cohort member): the flag applies to the infant and linked mother(s)/guardian(s) ----
		for (Map.Entry<Integer, Set<Integer>> entry : eligibleInfantMotherIds.entrySet()) {
			Integer infantId = entry.getKey();
			Set<Integer> linkedMotherIds = entry.getValue();
			String riskMessage = assessMtctRisk(linkedMotherIds, now, riskMaps);
			BooleanResult result = flagResult(riskMessage);
			ret.put(infantId, result);
			for (Integer motherId : linkedMotherIds) {
				ret.put(motherId, result);
			}
		}

		// ---- Everyone else in the cohort does not qualify for this flag ----
		for (Integer ptId : cohort) {
			if (!ret.containsKey(ptId)) {
				ret.put(ptId, new BooleanResult(false, this));
			}
		}

		return ret;
	}

	private BooleanResult flagResult(String riskMessage) {
		mtctFlagMessage = riskMessage != null ? riskMessage : "";
		return new BooleanResult(riskMessage != null, this);
	}

	/**
	 * Aggregates MTCT risk across one or more mothers/guardians (an infant may be linked to more than
	 * one), preferring the highest risk level found: high beats low beats none.
	 */
	private String assessMtctRisk(Set<Integer> motherIds, Date now, MotherRiskMaps maps) {
		boolean anyHighRisk = false;
		boolean anyLowRisk = false;
		for (Integer motherId : motherIds) {
			String risk = assessSingleMotherRisk(motherId, now, maps);
			if (HIGH_RISK_MESSAGE.equals(risk)) {
				anyHighRisk = true;
			} else if (LOW_RISK_MESSAGE.equals(risk)) {
				anyLowRisk = true;
			}
		}
		if (anyHighRisk) {
			return HIGH_RISK_MESSAGE;
		}
		if (anyLowRisk) {
			return LOW_RISK_MESSAGE;
		}
		return null;
	}

	private String assessSingleMotherRisk(Integer motherId, Date now, MotherRiskMaps maps) {
		Patient mother = Context.getPatientService().getPatient(motherId);
		int motherAgeYears = (mother != null && mother.getAge() != null) ? mother.getAge() : Integer.MAX_VALUE;
		boolean motherAgeUnder19 = motherAgeYears < 19;

		String greenCardResult = "";
		if (maps.greenCardMap.get(motherId) != null && maps.greenCardMap.get(motherId).getValue() != null) {
			greenCardResult = maps.greenCardMap.get(motherId).getValue().toString();
		}

		double vlResult = EmrUtils.parseKeyValueDouble(greenCardResult, "vlResult");
		String ldlResult = EmrUtils.parseKeyValueString(greenCardResult, "ldlResult");
		String firstIllnessControl = EmrUtils.parseKeyValueString(greenCardResult, "firstChronicIllnessControl");
		String secondIllnessControl = EmrUtils.parseKeyValueString(greenCardResult, "secondChronicIllnessControl");

		Obs adherenceObs = EmrCalculationUtils.obsResultForPatient(maps.lastAdherenceMap, motherId);
		boolean goodAdherence = false;
		if (adherenceObs != null && maps.goodAdherenceAnswerConcept != null) {
			goodAdherence = maps.goodAdherenceAnswerConcept.equals(adherenceObs.getValueCoded());
		}
		boolean vlAvailable = "LDL".equals(ldlResult) || vlResult > 0.0;
		boolean isVlSuppressed = "LDL".equals(ldlResult) || (vlResult > 0.0 && vlResult <= 50.0);
		boolean comorbidityControlled = (firstIllnessControl.isEmpty() || "Controlled".equalsIgnoreCase(firstIllnessControl))
				&& (secondIllnessControl.isEmpty() || "Controlled".equalsIgnoreCase(secondIllnessControl));

		Date returnVisitDate = null;
		if (maps.lastReturnVisitMap.get(motherId) != null && maps.lastReturnVisitMap.get(motherId).getValue() != null) {
			returnVisitDate = (Date) maps.lastReturnVisitMap.get(motherId).getValue();
		}

		boolean isRtc = false;
		if (returnVisitDate != null) {
			int daysPastTca = Days.daysBetween(new DateTime(returnVisitDate), new DateTime(now)).getDays();
			boolean missedAppointment = daysPastTca >= 30;

			Encounter lastEncounter = EmrCalculationUtils.encounterResultForPatient(maps.lastEncounterMap, motherId);
			Date lastActualReturnDate = lastEncounter != null ? lastEncounter.getEncounterDatetime() : null;
			boolean hasReturnedSinceMissedVisit = lastActualReturnDate != null && lastActualReturnDate.after(returnVisitDate);

			isRtc = missedAppointment && hasReturnedSinceMissedVisit;
		}

		Date artStartDate = null;
		if (maps.artStartDateMap.get(motherId) != null && maps.artStartDateMap.get(motherId).getValue() != null) {
			artStartDate = (Date) maps.artStartDateMap.get(motherId).getValue();
		}

		boolean newOnArt = false;
		if (artStartDate != null) {
			int daysSinceArtStart = Days.daysBetween(new DateTime(artStartDate), new DateTime(now)).getDays();
			newOnArt = daysSinceArtStart < 180;
		}

		Obs stiObs = EmrCalculationUtils.obsResultForPatient(maps.lastStiMap, motherId);
		boolean hasRecentSti = false;
		if (stiObs != null) {
			int daysSinceSti = Math.abs(Days.daysBetween(new DateTime(stiObs.getObsDatetime()), new DateTime(now)).getDays());
			hasRecentSti = daysSinceSti <= 180;
		}

		Concept keyPopulation = EmrCalculationUtils.codedObsResultForPatient(maps.lastKeyPopulationMap, motherId);
		boolean isFswOrPwid = keyPopulation != null && (keyPopulation.equals(maps.fswConcept)
				|| keyPopulation.equals(maps.peopleWhoInjectDrugsConcept) || keyPopulation.equals(maps.usedAlcoholOrSubstanceUseConcept));

		boolean motherHighRisk = false;
		motherHighRisk = motherAgeUnder19
				|| (vlAvailable && !isVlSuppressed)
				|| !goodAdherence
				|| hasRecentSti
				|| isFswOrPwid
				|| newOnArt;

		boolean motherLowRisk = isVlSuppressed && goodAdherence && comorbidityControlled;

		if (motherHighRisk) {
			return HIGH_RISK_MESSAGE;
		}
		if (motherLowRisk) {
			return LOW_RISK_MESSAGE;
		}
		return null;
	}

	private MotherRiskMaps fetchMotherRiskMaps(Set<Integer> motherIds, PatientCalculationContext context) {
		CalculationResultMap greenCardMap = calculate(new GreenCardVelocityCalculation(), motherIds, context);

		Concept stiScreeningConcept = Dictionary.getConcept(Dictionary.STI_SCREENING);
		// KEY POP - PWID, FSW
		Concept keyPopulationConcept = Context.getConceptService().getConceptByUuid(Dictionary.HIV_RISK_FACTOR);
		Concept fswConcept = Context.getConceptService().getConceptByUuid(Dictionary.FEMALE_SEX_WORKER);
		Concept peopleWhoInjectDrugsConcept = Context.getConceptService().getConceptByUuid(Dictionary.PEOPLE_WHO_INJECT_DRUGS);
		Concept usedAlcoholOrSubstanceUseConcept = Context.getConceptService().getConceptByUuid(Dictionary.PEOPLE_WHO_USE_SUBSTANCES);
		// Adherence: question concept 1658AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA, good-adherence answer 159405AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
		Concept adherenceQuestionConcept = Context.getConceptService().getConceptByUuid(Dictionary.ARV_ADHERENCE);
		Concept goodAdherenceAnswerConcept = Context.getConceptService().getConceptByUuid(Dictionary.GOOD_ADHERENCE);

		CalculationResultMap lastReturnVisitMap = calculate(new LastReturnVisitDateCalculation(), motherIds, context);
		CalculationResultMap lastEncounterMap = Calculations.lastEncounter(null, motherIds, context);
		CalculationResultMap artStartDateMap = calculate(new InitialArtStartDateCalculation(), motherIds, context);
		CalculationResultMap lastStiMap = Calculations.lastObs(stiScreeningConcept, motherIds, context);
		CalculationResultMap lastKeyPopulationMap = Calculations.lastObs(keyPopulationConcept, motherIds, context);
		CalculationResultMap lastAdherenceMap = Calculations.lastObs(adherenceQuestionConcept, motherIds, context);

		return new MotherRiskMaps(greenCardMap, lastReturnVisitMap, lastEncounterMap, artStartDateMap, lastStiMap,
				lastKeyPopulationMap, lastAdherenceMap, fswConcept, peopleWhoInjectDrugsConcept,
				usedAlcoholOrSubstanceUseConcept, goodAdherenceAnswerConcept);
	}

	private CalculationResultMap calculateHeiProgramMembership(Set<Integer> patientIds, PatientCalculationContext context) {
		Program heiProgram = Context.getProgramWorkflowService().getProgramByUuid(Metadata.Program.MCH_CS);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("program", heiProgram);
		return new InProgramCalculation().evaluate(patientIds, params, context);
	}

	private static List<Person> findEligibleHeiChildren(List<Person> children, CalculationResultMap inHeiProgramMap, Date now) {
		List<Person> eligibleChildren = new ArrayList<Person>();
		if (children == null) {
			return eligibleChildren;
		}
		for (Person child : children) {
			Date childBirthDate = child.getBirthdate();
			if (childBirthDate == null) continue;
			int childAgeMonths = Months.monthsBetween(new DateTime(childBirthDate), new DateTime(now)).getMonths();
			if (childAgeMonths < 24 && isTrue(inHeiProgramMap, child.getPersonId())) {
				eligibleChildren.add(child);
			}
		}
		return eligibleChildren;
	}

	private Map<Integer, Patient> fetchPatients(Collection<Integer> ids) {
		Map<Integer, Patient> patientsById = new HashMap<Integer, Patient>();
		for (Integer id : ids) {
			patientsById.put(id, Context.getPatientService().getPatient(id));
		}
		return patientsById;
	}

	private Map<Integer, List<Person>> fetchChildren(Collection<Integer> motherIds, Map<Integer, Patient> patientsById) {
		Map<Integer, List<Person>> childrenByMother = new HashMap<Integer, List<Person>>();
		for (Integer motherId : motherIds) {
			childrenByMother.put(motherId, EmrUtils.getPersonChildren(patientsById.get(motherId)));
		}
		return childrenByMother;
	}

	private static boolean isFemale(Patient patient) {
		return patient != null && "F".equalsIgnoreCase(patient.getGender());
	}

	private static boolean isUnder24Months(Patient patient, Date now) {
		Date birthDate = patient != null ? patient.getBirthdate() : null;
		int ageMonths = (birthDate != null) ? Months.monthsBetween(new DateTime(birthDate), new DateTime(now)).getMonths() : 0;
		return ageMonths < 24;
	}

	@SuppressWarnings("unchecked")
	private static Set<Integer> getSetValue(CalculationResultMap map, Integer ptId) {
		if (map.get(ptId) != null && map.get(ptId).getValue() != null) {
			return (Set<Integer>) map.get(ptId).getValue();
		}
		return Collections.emptySet();
	}

	private static boolean isTrue(CalculationResultMap map, Integer patientId) {
		return map.get(patientId) != null && Boolean.TRUE.equals(map.get(patientId).getValue());
	}

	/**
	 * Batched MTCT risk-factor lookups for a set of mothers, plus the concepts compared against them.
	 */
	private static class MotherRiskMaps {
		final CalculationResultMap greenCardMap;
		final CalculationResultMap lastReturnVisitMap;
		final CalculationResultMap lastEncounterMap;
		final CalculationResultMap artStartDateMap;
		final CalculationResultMap lastStiMap;
		final CalculationResultMap lastKeyPopulationMap;
		final CalculationResultMap lastAdherenceMap;
		final Concept fswConcept;
		final Concept peopleWhoInjectDrugsConcept;
		final Concept usedAlcoholOrSubstanceUseConcept;
		final Concept goodAdherenceAnswerConcept;

		MotherRiskMaps(CalculationResultMap greenCardMap, CalculationResultMap lastReturnVisitMap, CalculationResultMap lastEncounterMap,
				CalculationResultMap artStartDateMap, CalculationResultMap lastStiMap, CalculationResultMap lastKeyPopulationMap,
				CalculationResultMap lastAdherenceMap, Concept fswConcept, Concept peopleWhoInjectDrugsConcept,
				Concept usedAlcoholOrSubstanceUseConcept, Concept goodAdherenceAnswerConcept) {
			this.greenCardMap = greenCardMap;
			this.lastReturnVisitMap = lastReturnVisitMap;
			this.lastEncounterMap = lastEncounterMap;
			this.artStartDateMap = artStartDateMap;
			this.lastStiMap = lastStiMap;
			this.lastKeyPopulationMap = lastKeyPopulationMap;
			this.lastAdherenceMap = lastAdherenceMap;
			this.fswConcept = fswConcept;
			this.peopleWhoInjectDrugsConcept = peopleWhoInjectDrugsConcept;
			this.usedAlcoholOrSubstanceUseConcept = usedAlcoholOrSubstanceUseConcept;
			this.goodAdherenceAnswerConcept = goodAdherenceAnswerConcept;
		}
	}

}
