/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.surveillance;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Suspected Ebola case rule: temperature ≥ 38°C, travel to DRC/Uganda/Other or contact
 * with a suspected case, and at least three Ebola symptoms on presenting complaints.
 */
public final class EbolaCaseDefinition {

	public static final String TEMPERATURE = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final int TEMPERATURE_CONCEPT_ID = 5088;
	public static final double TEMPERATURE_THRESHOLD = 38.0;

	public static final String SCREENING_QUESTION = "5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

	public static final String CONTACT_WITH_SUSPECTED_CASE = "eb42f103-6d87-4c1e-a5f8-1c74dff5840d";
	public static final int CONTACT_WITH_SUSPECTED_CASE_CONCEPT_ID = 2032545;
	public static final String YES_ANSWER = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final int YES_CONCEPT_ID = 1065;

	public static final String TRAVEL_DRC = "165748AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String TRAVEL_UGANDA = "162884AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String TRAVEL_OTHER = "5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final int TRAVEL_DRC_ID = 165748;
	public static final int TRAVEL_UGANDA_ID = 162884;
	public static final int TRAVEL_OTHER_SPECIFY_ID = 2032318;
	public static final int TRAVEL_OTHER_ID = 5622;

	public static final String EBOLA_HEADACHE = "139084AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_LETHARGY = "116334AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_BODY_ACHE = "cbe63c88-0bee-4a32-85dc-2c378a306731";
	public static final String EBOLA_MUSCLE_PAIN = "133632AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_MYALGIA = "121AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_ABDOMINAL_PAIN = "151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_VOMITING = "122983AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_DIARRHEA = "142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	public static final String EBOLA_BLEEDING = "d5305f4f-745a-45ff-87ac-3d1fbad7d339";
	public static final String EBOLA_BLEEDING_TENDENCIES = "162628AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	/** Kenya custom "body ache" used on ETL / some live forms. */
	public static final int EBOLA_BODY_ACHE_CONCEPT_ID = 2032542;

	private static final Set<String> TRAVEL_ANSWER_UUIDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			TRAVEL_DRC, TRAVEL_UGANDA, TRAVEL_OTHER)));

	private static final Set<Integer> TRAVEL_ANSWER_IDS = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(
			TRAVEL_DRC_ID, TRAVEL_UGANDA_ID, TRAVEL_OTHER_SPECIFY_ID, TRAVEL_OTHER_ID)));

	private static final Set<String> EBOLA_SYMPTOM_UUIDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			EBOLA_HEADACHE, EBOLA_LETHARGY, EBOLA_BODY_ACHE, EBOLA_MUSCLE_PAIN, EBOLA_MYALGIA,
			EBOLA_ABDOMINAL_PAIN, EBOLA_VOMITING, EBOLA_DIARRHEA, EBOLA_BLEEDING, EBOLA_BLEEDING_TENDENCIES)));

	private static final Set<Integer> EBOLA_SYMPTOM_IDS = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(
			139084, 116334, EBOLA_BODY_ACHE_CONCEPT_ID, 133632, 121, 151, 122983, 142412, 162628)));

	private static final int MIN_SYMPTOMS = 3;

	private EbolaCaseDefinition() {
	}

	public static boolean isSuspectedCase(Visit visit, Collection<Encounter> extraEncounters) {
		Set<Obs> obs = collectVisitObs(visit, extraEncounters);
		if (obs.isEmpty()) {
			return false;
		}
		return temperatureAtLeast(obs, TEMPERATURE_THRESHOLD)
				&& (hasPositiveTravel(obs) || hasContactWithSuspectedCase(obs))
				&& ebolaSymptomCount(obs) >= MIN_SYMPTOMS;
	}

	static Set<Obs> collectVisitObs(Visit visit, Collection<Encounter> extraEncounters) {
		Set<Obs> obs = new HashSet<Obs>();
		Set<Encounter> encounters = new HashSet<Encounter>();
		if (visit != null) {
			try {
				if (visit.getEncounters() != null) {
					encounters.addAll(visit.getEncounters());
				}
			} catch (Exception ignored) {
				// visit collection may not be initialized; extraEncounters is the source of truth
			}
		}
		if (extraEncounters != null) {
			encounters.addAll(extraEncounters);
		}
		for (Encounter encounter : encounters) {
			if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
				continue;
			}
			if (encounter.getObs() != null) {
				obs.addAll(encounter.getObs());
			}
			if (encounter.getAllObs(false) != null) {
				for (Obs observation : encounter.getAllObs(false)) {
					addObsAndMembers(obs, observation);
				}
			}
		}
		return obs;
	}

	private static void addObsAndMembers(Set<Obs> obs, Obs observation) {
		if (observation == null || Boolean.TRUE.equals(observation.getVoided())) {
			return;
		}
		if (!obs.add(observation)) {
			return;
		}
		if (observation.getGroupMembers(false) == null) {
			return;
		}
		for (Obs member : observation.getGroupMembers(false)) {
			addObsAndMembers(obs, member);
		}
	}

	static boolean temperatureAtLeast(Set<Obs> obs, double threshold) {
		Double latest = null;
		for (Obs observation : obs) {
			if (!isTemperature(observation) || observation.getValueNumeric() == null) {
				continue;
			}
			if (latest == null || observation.getValueNumeric() > latest) {
				latest = observation.getValueNumeric();
			}
		}
		return latest != null && latest >= threshold;
	}

	static boolean hasPositiveTravel(Set<Obs> obs) {
		for (Obs observation : obs) {
			if (isPositiveTravelAnswer(observation)) {
				return true;
			}
		}
		return false;
	}

	static boolean hasContactWithSuspectedCase(Set<Obs> obs) {
		for (Obs observation : obs) {
			if (isContactQuestion(observation) && isYesAnswer(observation)) {
				return true;
			}
		}
		return false;
	}

	static int ebolaSymptomCount(Set<Obs> obs) {
		Set<String> found = new HashSet<String>();
		for (Obs observation : obs) {
			if (!isScreeningQuestion(observation) || observation.getValueCoded() == null) {
				continue;
			}
			String uuid = observation.getValueCoded().getUuid();
			Integer id = observation.getValueCoded().getConceptId();
			if ((uuid != null && EBOLA_SYMPTOM_UUIDS.contains(uuid))
					|| (id != null && EBOLA_SYMPTOM_IDS.contains(id))) {
				found.add(uuid != null ? uuid : String.valueOf(id));
			}
		}
		return found.size();
	}

	private static boolean isTemperature(Obs obs) {
		return matchesQuestion(obs, TEMPERATURE, TEMPERATURE_CONCEPT_ID);
	}

	private static boolean isContactQuestion(Obs obs) {
		return matchesQuestion(obs, CONTACT_WITH_SUSPECTED_CASE, CONTACT_WITH_SUSPECTED_CASE_CONCEPT_ID);
	}

	private static boolean isScreeningQuestion(Obs obs) {
		return matchesQuestion(obs, SCREENING_QUESTION, 5219);
	}

	private static boolean matchesQuestion(Obs obs, String uuid, int conceptId) {
		if (obs == null || obs.getConcept() == null) {
			return false;
		}
		if (uuid.equalsIgnoreCase(obs.getConcept().getUuid())) {
			return true;
		}
		return obs.getConcept().getConceptId() != null && obs.getConcept().getConceptId() == conceptId;
	}

	private static boolean isPositiveTravelAnswer(Obs obs) {
		if (obs.getValueCoded() == null) {
			return false;
		}
		if (obs.getValueCoded().getUuid() != null && TRAVEL_ANSWER_UUIDS.contains(obs.getValueCoded().getUuid())) {
			return true;
		}
		return obs.getValueCoded().getConceptId() != null
				&& TRAVEL_ANSWER_IDS.contains(obs.getValueCoded().getConceptId());
	}

	private static boolean isYesAnswer(Obs obs) {
		if (obs.getValueCoded() == null) {
			return false;
		}
		if (YES_ANSWER.equalsIgnoreCase(obs.getValueCoded().getUuid())) {
			return true;
		}
		return obs.getValueCoded().getConceptId() != null && obs.getValueCoded().getConceptId() == YES_CONCEPT_ID;
	}
}
