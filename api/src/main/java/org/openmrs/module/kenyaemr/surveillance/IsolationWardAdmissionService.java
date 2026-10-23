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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Isolation Ward: suspected-Ebola queue, admission-request write, and bed list
 * used by the Isolation awaiting-admission tab.
 */
public class IsolationWardAdmissionService {

	private static final Log log = LogFactory.getLog(IsolationWardAdmissionService.class);

	public static final String ISOLATION_WARD_UUID = CommonMetadata._LocationTag.ISOLATION_WARD;
	public static final String ADMISSION_REQUEST_FORM = "0bae7646-b079-4cbb-8130-b24274fc16f7";
	public static final String ADMISSION_REQUEST_ENCOUNTER_TYPE = "b2c4d5e6-7f8a-4e9b-8c1d-2e3f8e4a3b8f";
	public static final String UNKNOWN_ENCOUNTER_ROLE = "a0b03050-c99b-11e0-9572-0800200c9a66";

	private static final String DISPOSITION_CONSTRUCT = "169405AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DISPOSITION = "169402AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String DISPOSITION_ADMIT = "168619AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String ADMISSION_LOCATION = "169403AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final int DISPOSITION_CONSTRUCT_ID = 169405;
	private static final int DISPOSITION_ID = 169402;
	private static final int DISPOSITION_ADMIT_ID = 168619;
	private static final int ADMISSION_LOCATION_ID = 169403;

	public List<Visit> findSuspectedOpenVisits() {
		String sql = "SELECT v.visit_id FROM visit v "
				+ "WHERE v.voided = 0 AND v.date_stopped IS NULL "
				+ "AND EXISTS ( "
				+ "  SELECT 1 FROM encounter e "
				+ "  JOIN obs t ON t.encounter_id = e.encounter_id AND t.voided = 0 "
				+ "  JOIN concept c ON c.concept_id = t.concept_id "
				+ "  WHERE e.visit_id = v.visit_id AND e.voided = 0 "
				+ "    AND t.value_numeric >= 38 "
				+ "    AND (c.uuid = '" + EbolaCaseDefinition.TEMPERATURE + "' OR t.concept_id = "
				+ EbolaCaseDefinition.TEMPERATURE_CONCEPT_ID + ") "
				+ ")";
		List<List<Object>> rows = Context.getAdministrationService().executeSQL(sql, true);
		List<Visit> suspected = new ArrayList<Visit>();
		if (rows == null || rows.isEmpty()) {
			return suspected;
		}
		Set<String> alreadyAdmitted = isolationBedPatientUuids();
		for (List<Object> row : rows) {
			if (row == null || row.isEmpty() || row.get(0) == null) {
				continue;
			}
			Visit visit = Context.getVisitService().getVisit(((Number) row.get(0)).intValue());
			if (visit == null || visit.getPatient() == null) {
				continue;
			}
			if (visit.getPatient().getUuid() != null && alreadyAdmitted.contains(visit.getPatient().getUuid())) {
				continue;
			}
			try {
				if (EbolaCaseDefinition.isSuspectedCase(visit, loadVisitEncounters(visit))) {
					suspected.add(visit);
				}
			} catch (Exception e) {
				log.warn("Could not evaluate suspected Ebola rule for visit " + visit.getUuid()
						+ ": " + e.getMessage(), e);
			}
		}
		return suspected;
	}

	public List<Encounter> loadVisitEncounters(Visit visit) {
		if (visit == null) {
			return Collections.emptyList();
		}
		List<Encounter> encounters = Context.getEncounterService().getEncountersByVisit(visit, false);
		return encounters == null ? Collections.<Encounter>emptyList() : encounters;
	}

	public boolean queueForIsolation(Patient patient, Visit visit, Location preferredWard) {
		if (patient == null || visit == null) {
			return false;
		}
		Location isolationWard = resolveIsolationWard(preferredWard);
		if (isolationWard == null) {
			log.warn("Isolation ward was not found for uuid " + ISOLATION_WARD_UUID);
			return false;
		}
		if (hasPendingAdmissionRequest(visit, isolationWard)) {
			return false;
		}
		return createAdmissionRequest(patient, visit, isolationWard, new Date());
	}

	public Location resolveIsolationWard(Location preferredWard) {
		if (isIsolationLocation(preferredWard)) {
			return preferredWard;
		}
		LocationService locationService = Context.getLocationService();
		Location byUuid = locationService.getLocationByUuid(ISOLATION_WARD_UUID);
		if (byUuid != null && !Boolean.TRUE.equals(byUuid.getRetired())) {
			return byUuid;
		}
		List<Location> tagged = locationsTaggedIsolation();
		return tagged.isEmpty() ? null : tagged.get(0);
	}

	public List<Map<String, Object>> listBeds(Location preferredWard) {
		List<Integer> locationIds = isolationLocationIds(preferredWard);
		StringBuilder where = new StringBuilder("WHERE blm.bed_id IS NOT NULL AND (LOWER(l.name) LIKE '%isolation%'");
		if (!locationIds.isEmpty()) {
			where.append(" OR l.location_id IN (");
			for (int i = 0; i < locationIds.size(); i++) {
				if (i > 0) {
					where.append(',');
				}
				where.append(locationIds.get(i));
			}
			where.append(')');
		}
		where.append(')');
		String sql = "SELECT b.bed_id, b.uuid, b.bed_number, b.status, "
				+ "bt.uuid, bt.name, bt.display_name, l.name, "
				+ "blm.bed_row_number, blm.bed_column_number, p.uuid "
				+ "FROM bed_location_map blm "
				+ "INNER JOIN bed b ON b.bed_id = blm.bed_id AND b.voided = 0 "
				+ "INNER JOIN location l ON l.location_id = blm.location_id AND (l.retired = 0 OR l.retired IS NULL) "
				+ "LEFT JOIN bed_type bt ON bt.bed_type_id = b.bed_type_id "
				+ "LEFT JOIN bed_patient_assignment_map bpam ON bpam.bed_id = b.bed_id "
				+ "  AND bpam.date_stopped IS NULL AND (bpam.voided = 0 OR bpam.voided IS NULL) "
				+ "LEFT JOIN person p ON p.person_id = bpam.patient_id AND p.voided = 0 "
				+ where + " ORDER BY l.name, b.bed_number";
		try {
			List<List<Object>> rows = Context.getAdministrationService().executeSQL(sql, true);
			Map<Integer, Map<String, Object>> bedsById = new LinkedHashMap<Integer, Map<String, Object>>();
			if (rows == null) {
				return new ArrayList<Map<String, Object>>();
			}
			for (List<Object> row : rows) {
				Map<String, Object> bed = toBed(row);
				if (bed == null) {
					continue;
				}
				Integer bedId = (Integer) bed.get("bedId");
				Map<String, Object> existing = bedsById.get(bedId);
				if (existing == null) {
					bedsById.put(bedId, bed);
					existing = bed;
				}
				addOccupant(existing, stringVal(row, 10));
			}
			return new ArrayList<Map<String, Object>>(bedsById.values());
		} catch (Exception e) {
			log.warn("Could not list isolation beds: " + e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	public boolean hasPendingAdmissionRequest(Visit visit, Location isolationWard) {
		for (Encounter encounter : loadVisitEncounters(visit)) {
			if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
				continue;
			}
			Set<Obs> obs = encounter.getAllObs(false);
			if (obs == null) {
				continue;
			}
			for (Obs observation : obs) {
				if (isAdmitToLocation(observation, isolationWard)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean createAdmissionRequest(Patient patient, Visit visit, Location isolationWard, Date when) {
		EncounterService encounterService = Context.getEncounterService();
		EncounterType encounterType = encounterService.getEncounterTypeByUuid(ADMISSION_REQUEST_ENCOUNTER_TYPE);
		Form form = Context.getFormService().getFormByUuid(ADMISSION_REQUEST_FORM);
		if (encounterType == null) {
			log.warn("Transfer Request encounter type " + ADMISSION_REQUEST_ENCOUNTER_TYPE + " is missing");
			return false;
		}
		Concept construct = ciel(DISPOSITION_CONSTRUCT_ID, DISPOSITION_CONSTRUCT);
		Concept dispositionQuestion = ciel(DISPOSITION_ID, DISPOSITION);
		Concept admit = ciel(DISPOSITION_ADMIT_ID, DISPOSITION_ADMIT);
		Concept admitToLocation = ciel(ADMISSION_LOCATION_ID, ADMISSION_LOCATION);
		if (construct == null || dispositionQuestion == null || admit == null || admitToLocation == null) {
			log.warn("Disposition concepts (CIEL 169405/169402/168619/169403) are missing");
			return false;
		}

		Encounter encounter = new Encounter();
		encounter.setPatient(patient);
		encounter.setVisit(visit);
		encounter.setEncounterType(encounterType);
		if (form != null) {
			encounter.setForm(form);
		}
		encounter.setEncounterDatetime(when);
		encounter.setLocation(visit.getLocation() != null ? visit.getLocation() : isolationWard);
		addEncounterProvider(encounter, encounterService);

		Obs group = obs(patient, encounter, construct, when);
		Obs disposition = obs(patient, encounter, dispositionQuestion, when);
		disposition.setValueCoded(admit);
		disposition.setObsGroup(group);
		Obs locationObs = obs(patient, encounter, admitToLocation, when);
		locationObs.setValueText(isolationWard.getLocationId() != null
				? isolationWard.getLocationId().toString() : ISOLATION_WARD_UUID);
		locationObs.setObsGroup(group);
		group.addGroupMember(disposition);
		group.addGroupMember(locationObs);
		encounter.addObs(group);
		encounterService.saveEncounter(encounter);
		return true;
	}

	private boolean isIsolationLocation(Location location) {
		if (location == null || Boolean.TRUE.equals(location.getRetired())) {
			return false;
		}
		if (ISOLATION_WARD_UUID.equalsIgnoreCase(location.getUuid())) {
			return true;
		}
		if (location.getName() != null && location.getName().toLowerCase().contains("isolation")) {
			return true;
		}
		LocationTag tag = Context.getLocationService().getLocationTagByUuid(ISOLATION_WARD_UUID);
		return tag != null && tag.getName() != null && location.hasTag(tag.getName());
	}

	private List<Location> locationsTaggedIsolation() {
		LocationTag tag = Context.getLocationService().getLocationTagByUuid(ISOLATION_WARD_UUID);
		if (tag == null) {
			return Collections.emptyList();
		}
		List<Location> tagged = Context.getLocationService().getLocationsByTag(tag);
		if (tagged == null) {
			return Collections.emptyList();
		}
		List<Location> active = new ArrayList<Location>();
		for (Location location : tagged) {
			if (location != null && !Boolean.TRUE.equals(location.getRetired())) {
				active.add(location);
			}
		}
		return active;
	}

	private Set<String> isolationBedPatientUuids() {
		Set<String> uuids = new LinkedHashSet<String>();
		try {
			for (Map<String, Object> bed : listBeds(null)) {
				Object patients = bed.get("patients");
				if (!(patients instanceof List)) {
					continue;
				}
				for (Object occupant : (List<?>) patients) {
					if (occupant instanceof Map && ((Map<?, ?>) occupant).get("uuid") != null) {
						uuids.add(((Map<?, ?>) occupant).get("uuid").toString());
					}
				}
			}
		} catch (Exception e) {
			log.warn("Could not load isolation bed occupants for the awaiting list: " + e.getMessage(), e);
		}
		return uuids;
	}

	private List<Integer> isolationLocationIds(Location preferredWard) {
		Set<Integer> ids = new LinkedHashSet<Integer>();
		addLocationAndChildren(ids, resolveIsolationWard(preferredWard));
		addLocationAndChildren(ids, preferredWard);
		addLocationAndChildren(ids, Context.getLocationService().getLocationByUuid(ISOLATION_WARD_UUID));
		for (Location tagged : locationsTaggedIsolation()) {
			addLocationAndChildren(ids, tagged);
		}
		return new ArrayList<Integer>(ids);
	}

	private static void addLocationAndChildren(Set<Integer> ids, Location location) {
		if (location == null || Boolean.TRUE.equals(location.getRetired()) || location.getLocationId() == null) {
			return;
		}
		ids.add(location.getLocationId());
		Set<Location> children = location.getChildLocations();
		if (children == null) {
			return;
		}
		for (Location child : children) {
			if (child != null && !Boolean.TRUE.equals(child.getRetired()) && child.getLocationId() != null) {
				ids.add(child.getLocationId());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void addOccupant(Map<String, Object> bed, String patientUuid) {
		if (patientUuid == null) {
			return;
		}
		List<Map<String, Object>> patients = (List<Map<String, Object>>) bed.get("patients");
		for (Map<String, Object> occupant : patients) {
			if (patientUuid.equals(occupant.get("uuid"))) {
				return;
			}
		}
		Map<String, Object> occupant = new LinkedHashMap<String, Object>();
		occupant.put("uuid", patientUuid);
		patients.add(occupant);
		bed.put("status", "OCCUPIED");
	}

	private static Map<String, Object> toBed(List<Object> row) {
		if (row == null || row.size() < 3 || row.get(0) == null) {
			return null;
		}
		Map<String, Object> bed = new LinkedHashMap<String, Object>();
		bed.put("bedId", ((Number) row.get(0)).intValue());
		bed.put("bedUuid", stringVal(row, 1));
		bed.put("bedNumber", stringVal(row, 2));
		bed.put("status", stringVal(row, 3) == null ? "AVAILABLE" : stringVal(row, 3));
		bed.put("rowNumber", intVal(row, 8, 1));
		bed.put("columnNumber", intVal(row, 9, 1));
		bed.put("location", stringVal(row, 7));
		bed.put("patients", new ArrayList<Map<String, Object>>());
		Map<String, Object> bedType = new LinkedHashMap<String, Object>();
		bedType.put("uuid", stringVal(row, 4));
		bedType.put("name", stringVal(row, 5));
		String displayName = stringVal(row, 6);
		bedType.put("displayName", displayName != null ? displayName : stringVal(row, 5));
		bed.put("bedType", bedType);
		return bed;
	}

	private static String stringVal(List<Object> row, int index) {
		if (index >= row.size() || row.get(index) == null) {
			return null;
		}
		return row.get(index).toString();
	}

	private static int intVal(List<Object> row, int index, int fallback) {
		if (index >= row.size() || row.get(index) == null || !(row.get(index) instanceof Number)) {
			return fallback;
		}
		return ((Number) row.get(index)).intValue();
	}

	private void addEncounterProvider(Encounter encounter, EncounterService encounterService) {
		try {
			EncounterRole role = encounterService.getEncounterRoleByUuid(UNKNOWN_ENCOUNTER_ROLE);
			if (role == null || Context.getAuthenticatedUser() == null
					|| Context.getAuthenticatedUser().getPerson() == null) {
				return;
			}
			ProviderService providerService = Context.getProviderService();
			Collection<Provider> providers = providerService.getProvidersByPerson(
					Context.getAuthenticatedUser().getPerson());
			if (providers == null || providers.isEmpty()) {
				return;
			}
			encounter.addProvider(role, providers.iterator().next());
		} catch (Exception e) {
			log.debug("Could not set provider on isolation admission request: " + e.getMessage());
		}
	}

	private static boolean isAdmitToLocation(Obs obs, Location isolationWard) {
		if (obs == null || obs.getConcept() == null || isolationWard == null) {
			return false;
		}
		if (!matchesConcept(obs.getConcept(), ADMISSION_LOCATION, ADMISSION_LOCATION_ID)) {
			return false;
		}
		if (isolationWard.getUuid() != null && isolationWard.getUuid().equals(obs.getValueText())) {
			return true;
		}
		if (isolationWard.getId() != null && String.valueOf(isolationWard.getId()).equals(obs.getValueText())) {
			return true;
		}
		return isolationWard.getId() != null && obs.getValueNumeric() != null
				&& isolationWard.getId().equals(obs.getValueNumeric().intValue());
	}

	private static Obs obs(Patient patient, Encounter encounter, Concept concept, Date when) {
		Obs observation = new Obs();
		observation.setPerson(patient);
		observation.setEncounter(encounter);
		observation.setConcept(concept);
		observation.setObsDatetime(when);
		observation.setLocation(encounter.getLocation());
		return observation;
	}

	private static Concept ciel(int conceptId, String uuid) {
		ConceptService conceptService = Context.getConceptService();
		Concept mapped = conceptService.getConceptByMapping(String.valueOf(conceptId), "CIEL");
		if (mapped != null) {
			return mapped;
		}
		Concept byUuid = conceptService.getConceptByUuid(uuid);
		return byUuid != null ? byUuid : conceptService.getConcept(conceptId);
	}

	private static boolean matchesConcept(Concept concept, String uuid, int conceptId) {
		if (concept == null) {
			return false;
		}
		if (uuid.equalsIgnoreCase(concept.getUuid())) {
			return true;
		}
		return concept.getConceptId() != null && concept.getConceptId() == conceptId;
	}
}
