/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.surveillance.EbolaCaseDefinition;
import org.openmrs.module.kenyaemr.surveillance.IsolationWardAdmissionService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Isolation Ward list of suspected Ebola cases, plus the same admission-request
 * write the In-Patient Admission Request form would save.
 */
@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr")
public class EbolaIsolationRestController extends BaseRestController {

	private static final Log log = LogFactory.getLog(EbolaIsolationRestController.class);

	@GetMapping("/ebola-isolation-beds")
	public ResponseEntity<SimpleObject> listBeds(
			@RequestParam(value = "locationUuid", required = false) String locationUuid) {
		IsolationWardAdmissionService service = new IsolationWardAdmissionService();
		Location preferred = StringUtils.isBlank(locationUuid) ? null
				: Context.getLocationService().getLocationByUuid(locationUuid);
		SimpleObject response = new SimpleObject();
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (Map<String, Object> bed : service.listBeds(preferred)) {
			results.add(toSimple(bed));
		}
		response.add("results", results);
		Location isolation = service.resolveIsolationWard(preferred);
		if (isolation != null) {
			response.add("locationUuid", isolation.getUuid());
			response.add("locationName", isolation.getName());
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/ebola-isolation-suspects")
	public ResponseEntity<SimpleObject> listSuspects() {
		SimpleObject response = new SimpleObject();
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		try {
			IsolationWardAdmissionService service = new IsolationWardAdmissionService();
			List<Visit> visits = service.findSuspectedOpenVisits();
			for (Visit visit : visits) {
				try {
					results.add(toSuspect(visit));
				} catch (Exception e) {
					log.warn("Could not serialize suspected Ebola visit "
							+ (visit != null ? visit.getUuid() : null) + ": " + e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			log.error("Failed to list suspected Ebola isolation cases: " + e.getMessage(), e);
		}
		response.add("results", results);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/ebola-isolation-admission-request")
	public ResponseEntity<SimpleObject> createAdmissionRequest(@RequestParam("patientUuid") String patientUuid,
			@RequestParam("visitUuid") String visitUuid,
			@RequestParam(value = "locationUuid", required = false) String locationUuid) {
		if (StringUtils.isBlank(patientUuid) || StringUtils.isBlank(visitUuid)) {
			return new ResponseEntity<SimpleObject>(new SimpleObject(), HttpStatus.BAD_REQUEST);
		}
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		Visit visit = Context.getVisitService().getVisitByUuid(visitUuid);
		if (patient == null || visit == null) {
			return new ResponseEntity<SimpleObject>(new SimpleObject(), HttpStatus.NOT_FOUND);
		}

		IsolationWardAdmissionService service = new IsolationWardAdmissionService();
		List<Encounter> encounters = service.loadVisitEncounters(visit);
		if (!EbolaCaseDefinition.isSuspectedCase(visit, encounters)) {
			SimpleObject rejected = new SimpleObject();
			rejected.add("queued", false);
			rejected.add("reason", "Visit does not meet the suspected Ebola case rule");
			return new ResponseEntity<SimpleObject>(rejected, HttpStatus.BAD_REQUEST);
		}

		Location preferred = StringUtils.isBlank(locationUuid) ? null
				: Context.getLocationService().getLocationByUuid(locationUuid);
		boolean queued = service.queueForIsolation(patient, visit, preferred);
		Location isolation = service.resolveIsolationWard(preferred);
		boolean pending = isolation != null && service.hasPendingAdmissionRequest(visit, isolation);
		if (!queued && !pending) {
			SimpleObject failed = new SimpleObject();
			failed.add("queued", false);
			failed.add("reason", "Could not create isolation admission request");
			return new ResponseEntity<SimpleObject>(failed, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		SimpleObject ok = new SimpleObject();
		ok.add("queued", queued);
		ok.add("alreadyQueued", pending && !queued);
		ok.add("dispositionType", "ADMIT");
		ok.add("dispositionLocation", isolation != null ? isolation.getUuid() : IsolationWardAdmissionService.ISOLATION_WARD_UUID);
		return ResponseEntity.ok(ok);
	}

	private static SimpleObject toSuspect(Visit visit) {
		Patient patient = visit.getPatient();
		PersonName name = patient.getPersonName();
		SimpleObject person = new SimpleObject();
		person.add("uuid", patient.getPerson() != null ? patient.getPerson().getUuid() : patient.getUuid());
		person.add("display", name != null ? name.getFullName() : "");
		person.add("gender", patient.getGender());
		person.add("age", patient.getAge());

		List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
		if (patient.getActiveIdentifiers() != null) {
			for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
				if (identifier == null || identifier.getIdentifierType() == null) {
					continue;
				}
				SimpleObject type = new SimpleObject();
				type.add("uuid", identifier.getIdentifierType().getUuid());
				SimpleObject id = new SimpleObject();
				id.add("identifier", identifier.getIdentifier());
				id.add("identifierType", type);
				identifiers.add(id);
			}
		}

		SimpleObject patientObj = new SimpleObject();
		patientObj.add("uuid", patient.getUuid());
		patientObj.add("identifiers", identifiers);
		patientObj.add("person", person);

		SimpleObject visitObj = new SimpleObject();
		visitObj.add("uuid", visit.getUuid());
		if (visit.getStartDatetime() != null) {
			visitObj.add("startDatetime", iso(visit.getStartDatetime()));
		}

		SimpleObject row = new SimpleObject();
		row.add("patient", patientObj);
		row.add("visit", visitObj);
		row.add("dispositionType", "ADMIT");
		row.add("dateQueued", iso(visit.getStartDatetime()));
		return row;
	}

	private static SimpleObject toSimple(Map<String, Object> map) {
		SimpleObject object = new SimpleObject();
		if (map == null) {
			return object;
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			object.add(entry.getKey(), toSimpleValue(entry.getValue()));
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	private static Object toSimpleValue(Object value) {
		if (value instanceof Map) {
			return toSimple((Map<String, Object>) value);
		}
		if (value instanceof List) {
			List<Object> converted = new ArrayList<Object>();
			for (Object item : (List<?>) value) {
				converted.add(toSimpleValue(item));
			}
			return converted;
		}
		return value;
	}

	private static String iso(Date date) {
		if (date == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
	}
}
