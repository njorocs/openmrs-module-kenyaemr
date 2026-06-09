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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Allergen;
import org.openmrs.Allergy;
import org.openmrs.AllergyReaction;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Condition;
import org.openmrs.Diagnosis;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Visit;
import org.openmrs.api.ConditionService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.context.Context;
import org.openmrs.module.orderexpansion.api.ProcedureService;
import org.openmrs.module.orderexpansion.api.model.Procedure;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.VisitSummaryPdfGenerator;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.ui.framework.SimpleObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr")
public class KenyaemrVisitSummaryRestController extends BaseRestController {

	protected final Log log = LogFactory.getLog(getClass());
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	// ── Complaint obs concepts ─────────────────────────────────────────────────
	private static final String COMPLAINT_GROUP_CONCEPT = "160531AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String COMPLAINT_CONCEPT       = "5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String COMPLAINT_DURATION      = "159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String COMPLAINT_ONSET_STATUS  = "d7a3441d-6aeb-49be-b7d6-b2a3bb39e78d";
	private static final String COMPLAINT_OTHER_TEXT    = "20395601-257c-490c-86c2-acffb627f91f";

	// ── Vital-signs concept UUIDs ──────────────────────────────────────────────
	private static final String WEIGHT           = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String HEIGHT           = "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String TEMPERATURE      = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String PULSE_RATE       = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String BP_SYSTOLIC      = "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String BP_DIASTOLIC     = "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String RESPIRATORY_RATE = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String OXYGEN_SAT       = "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	private static final String MUAC             = "1343AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

	// ── Vital-signs response keys ──────────────────────────────────────────────
	private static final String K_WEIGHT       = "weight";
	private static final String K_HEIGHT       = "height";
	private static final String K_TEMPERATURE  = "temperature";
	private static final String K_PULSE        = "pulse";
	private static final String K_BP_SYSTOLIC  = "bpSystolic";
	private static final String K_BP_DIASTOLIC = "bpDiastolic";
	private static final String K_RESP_RATE    = "respiratoryRate";
	private static final String K_O2_SAT       = "oxygenSaturation";
	private static final String K_MUAC         = "muac";

	// ── FHIR interpretation ────────────────────────────────────────────────────
	private static final String FHIR_INTERP_SYSTEM =
		"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";
	private static final String K_INTERPRETATION = "interpretation";
	private static final String K_VALUE          = "value";
	private static final String K_UNIT           = "unit";
	private static final String K_RESULTS        = "results";


	@GetMapping("/visitSummary")
	public Object getVisitSummary(@RequestParam("visitUuid") String visitUuid) {
		Visit visit = Context.getVisitService().getVisitByUuid(visitUuid);
		if (visit == null) {
			return new ResponseEntity<>("Visit not found", new HttpHeaders(), HttpStatus.NOT_FOUND);
		}

		Patient patient = visit.getPatient();
		Date visitEnd = visit.getStopDatetime() != null ? visit.getStopDatetime() : new Date();
		ConceptService cs = Context.getConceptService();

		SimpleObject summary = new SimpleObject();
		summary.put("visitUuid", visit.getUuid());
		summary.put("visitDate", formatDate(visit.getStartDatetime()));
		summary.put("visitStopDate", formatDate(visit.getStopDatetime()));
		summary.put("visitType", visit.getVisitType() != null ? visit.getVisitType().getName() : "");
		summary.put("location", visit.getLocation() != null ? visit.getLocation().getName() : "");

		summary.put("vitals", buildVitals(visit, visitEnd, cs));
		summary.put("complaints", buildComplaints(visit, cs));
		summary.put("conditions", buildConditions(visit, visitEnd, patient));
		summary.put("allergies", buildAllergies(visit, visitEnd, patient));
		summary.put("diagnoses", buildDiagnoses(visit));
		summary.put("medications", buildMedications(visit));
		summary.put("clinicalNotes", buildClinicalNotes(visit));
		summary.put("procedures", buildProcedures(visit));
		summary.put("imaging", buildImaging(visit));
		summary.put("labResults", buildLabResults(visit, cs));

		return summary;
	}

	@GetMapping("/visitSummary/pdf")
	public ResponseEntity<byte[]> getVisitSummaryPdf(@RequestParam("visitUuid") String visitUuid) {
		Visit visit = Context.getVisitService().getVisitByUuid(visitUuid);
		if (visit == null) {
			return new ResponseEntity<>("Visit not found".getBytes(), new HttpHeaders(), HttpStatus.NOT_FOUND);
		}
		try {
			byte[] pdfBytes = new VisitSummaryPdfGenerator().generate(visit);
			String fileName = "visit_summary_" + visitUuid + ".pdf";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDisposition(ContentDisposition.builder("inline").filename(fileName).build());
			headers.setContentLength(pdfBytes.length);
			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		} catch (IOException e) {
			log.error("Failed to generate visit summary PDF for visit " + visitUuid, e);
			return new ResponseEntity<>(("PDF generation failed: " + e.getMessage()).getBytes(),
				new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ── Section builders ──────────────────────────────────────────────────────

	private SimpleObject buildVitals(Visit visit, Date visitEnd, ConceptService cs) {
		List<Concept> vitalConcepts = conceptList(cs, WEIGHT, HEIGHT, TEMPERATURE, PULSE_RATE,
			BP_SYSTOLIC, BP_DIASTOLIC, RESPIRATORY_RATE, OXYGEN_SAT, MUAC);

		List<Obs> obsList = Context.getObsService().getObservations(
			Arrays.asList(Context.getPersonService().getPerson(visit.getPatient().getPersonId())),
			null, vitalConcepts, null, null, null,
			Arrays.asList("obsId"), null, null,
			visit.getStartDatetime(), visitEnd, false
		);

		Map<Concept, String> keyMap = buildVitalConceptKeyMap(cs);
		Map<String, Obs> obsMap = new HashMap<>();
		for (Obs obs : obsList) {
			String key = keyMap.get(obs.getConcept());
			if (key != null && !obsMap.containsKey(key)) {
				obsMap.put(key, obs);
			}
		}

		SimpleObject vitals = new SimpleObject();
		vitals.put(K_WEIGHT,        vitalEntryFromObs(obsMap.get(K_WEIGHT),       "kg",          cs));
		vitals.put(K_HEIGHT,        vitalEntryFromObs(obsMap.get(K_HEIGHT),       "cm",          cs));
		vitals.put(K_TEMPERATURE,   vitalEntryFromObs(obsMap.get(K_TEMPERATURE),  "°C",          cs));
		vitals.put(K_PULSE,         vitalEntryFromObs(obsMap.get(K_PULSE),        "bpm",         cs));
		vitals.put(K_BP_SYSTOLIC,   vitalEntryFromObs(obsMap.get(K_BP_SYSTOLIC),  "mmHg",        cs));
		vitals.put(K_BP_DIASTOLIC,  vitalEntryFromObs(obsMap.get(K_BP_DIASTOLIC), "mmHg",        cs));
		vitals.put("bloodPressure", buildBpEntry(obsMap.get(K_BP_SYSTOLIC), obsMap.get(K_BP_DIASTOLIC), cs));
		vitals.put(K_RESP_RATE,     vitalEntryFromObs(obsMap.get(K_RESP_RATE),    "breaths/min", cs));
		vitals.put(K_O2_SAT,        vitalEntryFromObs(obsMap.get(K_O2_SAT),       "%",           cs));
		vitals.put(K_MUAC,          vitalEntryFromObs(obsMap.get(K_MUAC),         "cm",          cs));
		return vitals;
	}

	private Map<Concept, String> buildVitalConceptKeyMap(ConceptService cs) {
		Map<Concept, String> map = new HashMap<>();
		putConcept(map, cs, WEIGHT, K_WEIGHT);
		putConcept(map, cs, HEIGHT, K_HEIGHT);
		putConcept(map, cs, TEMPERATURE, K_TEMPERATURE);
		putConcept(map, cs, PULSE_RATE, K_PULSE);
		putConcept(map, cs, BP_SYSTOLIC, K_BP_SYSTOLIC);
		putConcept(map, cs, BP_DIASTOLIC, K_BP_DIASTOLIC);
		putConcept(map, cs, RESPIRATORY_RATE, K_RESP_RATE);
		putConcept(map, cs, OXYGEN_SAT, K_O2_SAT);
		putConcept(map, cs, MUAC, K_MUAC);
		return map;
	}

	private SimpleObject vitalEntryFromObs(Obs obs, String defaultUnit, ConceptService cs) {
		SimpleObject entry = new SimpleObject();
		if (obs == null || obs.getValueNumeric() == null) {
			entry.put(K_VALUE, null);
			entry.put(K_UNIT, defaultUnit);
			entry.put(K_INTERPRETATION, null);
			return entry;
		}
		double value = obs.getValueNumeric();
		entry.put(K_VALUE, value);
		if (obs.getConcept().isNumeric()) {
			ConceptNumeric cn = cs.getConceptNumeric(obs.getConcept().getConceptId());
			String unit = (cn != null && cn.getUnits() != null) ? cn.getUnits() : defaultUnit;
			entry.put(K_UNIT, unit);
			if (cn != null && (cn.getLowNormal() != null || cn.getHiNormal() != null)) {
				entry.put(K_INTERPRETATION, fhirInterpretation(
					value, cn.getLowCritical(), cn.getLowNormal(), cn.getHiNormal(), cn.getHiCritical()));
			} else {
				entry.put(K_INTERPRETATION, null);
			}
		} else {
			entry.put(K_UNIT, defaultUnit);
			entry.put(K_INTERPRETATION, null);
		}
		return entry;
	}

	private SimpleObject buildBpEntry(Obs sbpObs, Obs dbpObs, ConceptService cs) {
		SimpleObject entry = new SimpleObject();
		entry.put(K_UNIT, "mmHg");
		if (sbpObs == null || dbpObs == null
				|| sbpObs.getValueNumeric() == null || dbpObs.getValueNumeric() == null) {
			entry.put(K_VALUE, null);
			entry.put(K_INTERPRETATION, null);
			return entry;
		}
		entry.put(K_VALUE, sbpObs.getValueNumeric().intValue() + "/" + dbpObs.getValueNumeric().intValue());
		String bpCode = worstCode(obsInterpCode(sbpObs, cs), obsInterpCode(dbpObs, cs));
		entry.put(K_INTERPRETATION, bpCode != null ? fhirInterpretationFromCode(bpCode) : null);
		return entry;
	}

	// Returns the FHIR interpretation code string for an obs based on its ConceptNumeric ranges,
	// or null if the concept has no reference range configured.
	private String obsInterpCode(Obs obs, ConceptService cs) {
		if (!obs.getConcept().isNumeric() || obs.getValueNumeric() == null) {
			return null;
		}
		ConceptNumeric cn = cs.getConceptNumeric(obs.getConcept().getConceptId());
		if (cn == null || (cn.getLowNormal() == null && cn.getHiNormal() == null)) {
			return null;
		}
		return interpCode(obs.getValueNumeric(),
			cn.getLowCritical(), cn.getLowNormal(), cn.getHiNormal(), cn.getHiCritical());
	}

	private String worstCode(String a, String b) {
		if (a == null) return b;
		if (b == null) return a;
		return interpSeverity(a) >= interpSeverity(b) ? a : b;
	}

	private int interpSeverity(String code) {
		if ("LL".equals(code)) return 5;
		if ("HH".equals(code)) return 4;
		if ("L".equals(code))  return 3;
		if ("H".equals(code))  return 2;
		return 1;
	}

	private void putConcept(Map<Concept, String> map, ConceptService cs, String uuid, String key) {
		Concept c = cs.getConceptByUuid(uuid);
		if (c != null) {
			map.put(c, key);
		}
	}

	private List<SimpleObject> buildComplaints(Visit visit, ConceptService cs) {
		List<SimpleObject> complaints = new ArrayList<>();
		EncounterType triageType = Context.getEncounterService()
			.getEncounterTypeByUuid(CommonMetadata._EncounterType.TRIAGE);

		Concept groupConcept   = cs.getConceptByUuid(COMPLAINT_GROUP_CONCEPT);
		Concept complaintCon   = cs.getConceptByUuid(COMPLAINT_CONCEPT);
		Concept durationCon    = cs.getConceptByUuid(COMPLAINT_DURATION);
		Concept onsetStatusCon = cs.getConceptByUuid(COMPLAINT_ONSET_STATUS);
		Concept otherTextCon   = cs.getConceptByUuid(COMPLAINT_OTHER_TEXT);

		if (groupConcept == null || complaintCon == null) {
			return complaints;
		}

		for (Encounter enc : visit.getEncounters()) {
			boolean active = !Boolean.TRUE.equals(enc.getVoided());
			boolean matchesType = triageType == null || enc.getEncounterType().equals(triageType);
			if (active && matchesType) {
				collectComplaintsFromEncounter(enc, groupConcept, complaintCon,
					durationCon, onsetStatusCon, otherTextCon, complaints);
			}
		}
		return complaints;
	}

	private void collectComplaintsFromEncounter(Encounter enc, Concept groupConcept, Concept complaintCon,
			Concept durationCon, Concept onsetStatusCon, Concept otherTextCon, List<SimpleObject> complaints) {
		for (Obs topObs : enc.getObsAtTopLevel(false)) {
			if (topObs.getConcept().equals(groupConcept) && topObs.hasGroupMembers()) {
				SimpleObject entry = buildComplaintEntry(topObs, complaintCon, durationCon, onsetStatusCon, otherTextCon);
				if (!entry.isEmpty()) {
					complaints.add(entry);
				}
			}
		}
	}

	private SimpleObject buildComplaintEntry(Obs groupObs, Concept complaintCon, Concept durationCon,
			Concept onsetStatusCon, Concept otherTextCon) {
		SimpleObject complaint = new SimpleObject();
		for (Obs member : groupObs.getGroupMembers(false)) {
			Concept mc = member.getConcept();
			if (mc.equals(complaintCon)) {
				String val = member.getValueCoded() != null
					? member.getValueCoded().getName().getName() : member.getValueText();
				complaint.put("complaint", val);
			} else if (durationCon != null && mc.equals(durationCon) && member.getValueNumeric() != null) {
				complaint.put("duration", member.getValueNumeric().intValue() + " days");
			} else if (onsetStatusCon != null && mc.equals(onsetStatusCon) && member.getValueCoded() != null) {
				complaint.put("onsetStatus", member.getValueCoded().getName().getName());
			} else if (otherTextCon != null && mc.equals(otherTextCon) && member.getValueText() != null) {
				complaint.put("otherComplaints", member.getValueText());
			}
		}
		return complaint;
	}

	private List<SimpleObject> buildConditions(Visit visit, Date visitEnd, Patient patient) {
		List<SimpleObject> conditions = new ArrayList<>();
		List<Condition> active = Context.getService(ConditionService.class).getActiveConditions(patient);
		for (Condition condition : active) {
			Date onset = condition.getOnsetDate();
			Date ref   = onset != null ? onset : condition.getDateCreated();
			if (ref != null && !ref.before(visit.getStartDatetime()) && !ref.after(visitEnd)) {
				conditions.add(buildConditionEntry(condition, onset));
			}
		}
		return conditions;
	}

	private SimpleObject buildConditionEntry(Condition condition, Date onset) {
		String name = condition.getCondition().getCoded() != null
			? condition.getCondition().getCoded().getName().getName()
			: condition.getCondition().getNonCoded();
		SimpleObject obj = new SimpleObject();
		obj.put("name", name != null ? name : "");
		obj.put("onsetDate", formatDate(onset));
		obj.put("status", condition.getClinicalStatus() != null ? condition.getClinicalStatus().toString() : "");
		return obj;
	}

	private List<SimpleObject> buildAllergies(Visit visit, Date visitEnd, Patient patient) {
		List<SimpleObject> allergies = new ArrayList<>();
		List<Allergy> all = Context.getPatientService().getAllergies(patient);
		for (Allergy allergy : all) {
			Date recorded = allergy.getDateCreated();
			if (recorded != null && !recorded.before(visit.getStartDatetime()) && !recorded.after(visitEnd)) {
				allergies.add(buildAllergyEntry(allergy));
			}
		}
		return allergies;
	}

	private SimpleObject buildAllergyEntry(Allergy allergy) {
		Allergen allergen = allergy.getAllergen();
		String allergenName = allergen.getCodedAllergen() != null
			? allergen.getCodedAllergen().getName().getName() : allergen.getNonCodedAllergen();
		String allergenType = allergen.getAllergenType() != null ? allergen.getAllergenType().toString() : "";
		String severity = allergy.getSeverity() != null ? allergy.getSeverity().getName().getName() : "";

		SimpleObject obj = new SimpleObject();
		obj.put("allergen", allergenName);
		obj.put("allergenType", allergenType);
		obj.put("severity", severity);
		obj.put("reactions", collectReactionNames(allergy));
		return obj;
	}

	private List<String> collectReactionNames(Allergy allergy) {
		List<String> names = new ArrayList<>();
		if (allergy.getReactions() != null) {
			for (AllergyReaction r : allergy.getReactions()) {
				if (r.getReaction() != null && r.getReaction().getName() != null) {
					names.add(r.getReaction().getName().getName());
				}
			}
		}
		return names;
	}

	private List<SimpleObject> buildDiagnoses(Visit visit) {
		List<SimpleObject> diagnoses = new ArrayList<>();
		List<Diagnosis> visitDiagnoses = Context.getService(DiagnosisService.class)
			.getDiagnosesByVisit(visit, false, false);
		for (Diagnosis diagnosis : visitDiagnoses) {
			diagnoses.add(buildDiagnosisEntry(diagnosis));
		}
		return diagnoses;
	}

	private SimpleObject buildDiagnosisEntry(Diagnosis diagnosis) {
		String name = diagnosis.getDiagnosis().getCoded() != null
			? diagnosis.getDiagnosis().getCoded().getName().getName()
			: diagnosis.getDiagnosis().getNonCoded();
		String date = diagnosis.getEncounter() != null
			? formatDate(diagnosis.getEncounter().getEncounterDatetime()) : "";

		SimpleObject obj = new SimpleObject();
		obj.put("diagnosis", name != null ? name : "");
		obj.put("certainty", diagnosis.getCertainty() != null ? diagnosis.getCertainty().toString() : "");
		obj.put("rank", diagnosis.getRank());
		obj.put("date", date);
		return obj;
	}

	private List<SimpleObject> buildMedications(Visit visit) {
		List<SimpleObject> medications = new ArrayList<>();
		for (Encounter enc : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(enc.getVoided())) {
				for (Order order : enc.getOrders()) {
					if (!Boolean.TRUE.equals(order.getVoided()) && order instanceof DrugOrder) {
						medications.add(buildMedicationEntry((DrugOrder) order));
					}
				}
			}
		}
		return medications;
	}

	private SimpleObject buildMedicationEntry(DrugOrder d) {
		String drugName = resolveDrugName(d);
		String dose = buildDoseString(d);
		String duration = buildDurationString(d);
		String route = d.getRoute() != null ? d.getRoute().getName().getName() : "";
		String frequency = d.getFrequency() != null ? d.getFrequency().getName() : "";

		SimpleObject obj = new SimpleObject();
		obj.put("drug", drugName);
		obj.put("dose", dose);
		obj.put("frequency", frequency);
		obj.put("duration", duration);
		obj.put("route", route);
		obj.put("dateActivated", formatDate(d.getDateActivated()));
		obj.put("autoExpireDate", formatDate(d.getAutoExpireDate()));
		return obj;
	}

	private String resolveDrugName(DrugOrder d) {
		if (d.getDrug() != null) return d.getDrug().getName();
		if (d.getConcept() != null) return d.getConcept().getName().getName();
		return "";
	}

	private String buildDoseString(DrugOrder d) {
		if (d.getDose() == null) return "";
		String units = d.getDoseUnits() != null ? " " + d.getDoseUnits().getName().getName() : "";
		return d.getDose() + units;
	}

	private String buildDurationString(DrugOrder d) {
		if (d.getDuration() == null) return "";
		String units = d.getDurationUnits() != null ? " " + d.getDurationUnits().getName().getName() : "";
		return d.getDuration() + units;
	}

	private List<SimpleObject> buildClinicalNotes(Visit visit) {
		List<SimpleObject> notes = new ArrayList<>();
		Set<String> noteTypes = new HashSet<>(Arrays.asList(
			CommonMetadata._EncounterType.CONSULTATION,
			CommonMetadata._EncounterType.NURSING_CARE_PLAN
		));
		for (Encounter enc : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(enc.getVoided()) && noteTypes.contains(enc.getEncounterType().getUuid())) {
				String text = collectNoteText(enc);
				if (!text.isEmpty()) {
					SimpleObject obj = new SimpleObject();
					obj.put("encounterType", enc.getEncounterType().getName());
					obj.put("date", formatDate(enc.getEncounterDatetime()));
					obj.put("note", text);
					notes.add(obj);
				}
			}
		}
		return notes;
	}

	private String collectNoteText(Encounter enc) {
		StringBuilder text = new StringBuilder();
		for (Obs obs : enc.getObs()) {
			String val = obs.getValueText();
			if (!Boolean.TRUE.equals(obs.getVoided()) && val != null && !val.isEmpty()) {
				if (text.length() > 0) {
					text.append("\n");
				}
				text.append(obs.getConcept().getName().getName()).append(": ").append(val);
			}
		}
		return text.toString();
	}

	// Concept class UUIDs that represent imaging and procedure result concepts
	private static final String PROCEDURE_CONCEPT_CLASS_UUID = "8d490bf4-c2cc-11de-8d13-0010c6dffd0f";
	private static final String IMAGING_CONCEPT_CLASS_UUID   = "8caa332c-efe4-4025-8b18-3398328e1323";

	// UUID of the procedure order type (matches orderTypes param used by the SPA)
	private static final String PROCEDURE_ORDER_TYPE_UUID = "b4a7c280-369e-4d12-9ce8-18e36783fed6";

	private Set<Obs> collectAllVisitObs(Visit visit) {
		Set<Obs> allVisitObs = new HashSet<>();
		for (Encounter enc : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(enc.getVoided())) {
				allVisitObs.addAll(enc.getObs());
			}
		}
		return allVisitObs;
	}

	private boolean isImagingOrder(Order order) {
		return order.getConcept() != null
			&& order.getConcept().getConceptClass() != null
			&& IMAGING_CONCEPT_CLASS_UUID.equals(order.getConcept().getConceptClass().getUuid());
	}

	private List<SimpleObject> buildProcedures(Visit visit) {
		Set<Obs> allVisitObs = collectAllVisitObs(visit);
		List<SimpleObject> procedures = new ArrayList<>();
		for (Encounter enc : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(enc.getVoided())) {
				for (Order order : enc.getOrders()) {
					if (!Boolean.TRUE.equals(order.getVoided())
							&& order.getOrderType() != null
							&& PROCEDURE_ORDER_TYPE_UUID.equals(order.getOrderType().getUuid())
							&& !isImagingOrder(order)) {
						procedures.add(buildProcedureEntry(order, allVisitObs));
					}
				}
			}
		}
		return procedures;
	}

	private List<SimpleObject> buildImaging(Visit visit) {
		Set<Obs> allVisitObs = collectAllVisitObs(visit);
		List<SimpleObject> imaging = new ArrayList<>();
		for (Encounter enc : visit.getEncounters()) {
			if (!Boolean.TRUE.equals(enc.getVoided())) {
				for (Order order : enc.getOrders()) {
					if (!Boolean.TRUE.equals(order.getVoided())
							&& order.getOrderType() != null
							&& PROCEDURE_ORDER_TYPE_UUID.equals(order.getOrderType().getUuid())
							&& isImagingOrder(order)) {
						imaging.add(buildImagingEntry(order, allVisitObs));
					}
				}
			}
		}
		return imaging;
	}

	private SimpleObject buildProcedureEntry(Order order, Set<Obs> allVisitObs) {
		Procedure procedure = getLinkedProcedure(order);
		SimpleObject obj = new SimpleObject();
		obj.put("procedure", order.getConcept().getName().getName());
		obj.put("procedureUuid", order.getConcept().getUuid());
		obj.put("orderNumber", order.getOrderNumber());
		obj.put("action", order.getAction() != null ? order.getAction().name() : "");
		obj.put("urgency", order.getUrgency() != null ? order.getUrgency().name() : "");
		obj.put("status", order.getFulfillerStatus() != null ? order.getFulfillerStatus().name() : "");
		obj.put("orderedDate", formatDate(order.getDateActivated()));
		obj.put("instructions", order.getInstructions() != null ? order.getInstructions() : "");
		obj.put("orderer", order.getOrderer() != null ? order.getOrderer().getName() : "");
		obj.put("procedureReport", procedure != null && procedure.getProcedureReport() != null ? procedure.getProcedureReport() : "");
		obj.put(K_RESULTS, collectOrderResults(order, allVisitObs));
		return obj;
	}

	private SimpleObject buildImagingEntry(Order order, Set<Obs> allVisitObs) {
		Procedure procedure = getLinkedProcedure(order);
		SimpleObject obj = new SimpleObject();
		obj.put("procedure", order.getConcept().getName().getName());
		obj.put("procedureUuid", order.getConcept().getUuid());
		obj.put("orderNumber", order.getOrderNumber());
		obj.put("action", order.getAction() != null ? order.getAction().name() : "");
		obj.put("urgency", order.getUrgency() != null ? order.getUrgency().name() : "");
		obj.put("status", order.getFulfillerStatus() != null ? order.getFulfillerStatus().name() : "");
		obj.put("orderedDate", formatDate(order.getDateActivated()));
		obj.put("instructions", order.getInstructions() != null ? order.getInstructions() : "");
		obj.put("orderer", order.getOrderer() != null ? order.getOrderer().getName() : "");
		obj.put("procedureReport", procedure != null && procedure.getProcedureReport() != null ? procedure.getProcedureReport() : "");
		obj.put("impression", procedure != null && procedure.getImpressions() != null ? procedure.getImpressions() : "");
		obj.put(K_RESULTS, collectOrderResults(order, allVisitObs));
		return obj;
	}

	private Procedure getLinkedProcedure(Order order) {
		List<Procedure> procedures = Context.getService(ProcedureService.class)
			.getProceduresByOrderUuid(order.getUuid());
		return procedures.isEmpty() ? null : procedures.get(0);
	}

	private List<SimpleObject> collectOrderResults(Order order, Set<Obs> allVisitObs) {
		List<SimpleObject> results = new ArrayList<>();
		for (Obs obs : allVisitObs) {
			if (!Boolean.TRUE.equals(obs.getVoided()) && order.equals(obs.getOrder())) {
				String conceptClassUuid = obs.getConcept().getConceptClass() != null
					? obs.getConcept().getConceptClass().getUuid() : "";
				boolean isProcedureResult = PROCEDURE_CONCEPT_CLASS_UUID.equals(conceptClassUuid)
					|| IMAGING_CONCEPT_CLASS_UUID.equals(conceptClassUuid)
					|| order.getConcept().equals(obs.getConcept());
				if (isProcedureResult) {
					SimpleObject result = new SimpleObject();
					result.put("finding", obs.getConcept().getName().getName());
					result.put(K_VALUE, obsValueAsString(obs));
					result.put("date", formatDate(obs.getObsDatetime()));
					results.add(result);
				}
			}
		}
		return results;
	}

	// UUID of the "Laboratory Order" concept set — parent of all lab panels
	private static final String LAB_ORDER_CONCEPT_UUID = "9a6f10d6-7fc5-4fb7-9428-24ef7b8d01f7";

	private List<SimpleObject> buildLabResults(Visit visit, ConceptService cs) {
		Concept labOrderConcept = cs.getConceptByUuid(LAB_ORDER_CONCEPT_UUID);
		if (labOrderConcept == null) {
			return new ArrayList<>();
		}
		List<Concept> labConcepts = labOrderConcept.getSetMembers();
		if (labConcepts.isEmpty()) {
			return new ArrayList<>();
		}
		Date endDate = visit.getStopDatetime() != null ? visit.getStopDatetime() : new Date();
		List<Person> persons = Collections.singletonList(
			Context.getPersonService().getPerson(visit.getPatient().getPersonId()));

		List<Obs> allLabObs = Context.getObsService().getObservations(
			persons, null, labConcepts, null, null, null, null,
			null, null, visit.getStartDatetime(), endDate, false);

		List<SimpleObject> results = new ArrayList<>();
		for (Obs obs : allLabObs) {
			if (!Boolean.TRUE.equals(obs.getVoided())) {
				if (obs.hasGroupMembers()) {
					collectPanelEntry(obs, cs, results);
				} else if (obs.getObsGroup() == null) {
					// standalone result not belonging to any panel
					results.add(buildStandaloneLabEntry(obs, cs));
				}
			}
		}
		return results;
	}

	private void collectPanelEntry(Obs panel, ConceptService cs, List<SimpleObject> results) {
		Set<Obs> members = panel.getGroupMembers(false);
		if (members == null || members.isEmpty()) {
			return;
		}
		List<SimpleObject> panelResults = new ArrayList<>();
		for (Obs child : members) {
			if (!Boolean.TRUE.equals(child.getVoided())) {
				panelResults.add(buildLabResultEntry(child, cs));
			}
		}
		SimpleObject panelObj = new SimpleObject();
		panelObj.put("panel", panel.getConcept().getName().getName());
		panelObj.put("panelUuid", panel.getConcept().getUuid());
		panelObj.put("isPanel", true);
		panelObj.put(K_RESULTS, panelResults);
		results.add(panelObj);
	}

	private SimpleObject buildStandaloneLabEntry(Obs obs, ConceptService cs) {
		SimpleObject entry = new SimpleObject();
		entry.put("panel", null);
		entry.put("panelUuid", null);
		entry.put("isPanel", false);
		entry.put(K_RESULTS, Collections.singletonList(buildLabResultEntry(obs, cs)));
		return entry;
	}

	private SimpleObject buildLabResultEntry(Obs obs, ConceptService cs) {
		SimpleObject obj = new SimpleObject();
		obj.put("test", obs.getConcept().getName().getName());
		obj.put("testUuid", obs.getConcept().getUuid());
		obj.put(K_VALUE, obsValueAsString(obs));
		obj.put("date", formatDate(obs.getObsDatetime()));
		obj.put("units", "");
		obj.put("lowNormal", null);
		obj.put("hiNormal", null);
		obj.put("lowCritical", null);
		obj.put("hiCritical", null);
		obj.put(K_INTERPRETATION, null);
		if (obs.getConcept().isNumeric()) {
			ConceptNumeric cn = cs.getConceptNumeric(obs.getConcept().getConceptId());
			if (cn != null) {
				obj.put("units", cn.getUnits() != null ? cn.getUnits() : "");
				obj.put("lowNormal", cn.getLowNormal());
				obj.put("hiNormal", cn.getHiNormal());
				obj.put("lowCritical", cn.getLowCritical());
				obj.put("hiCritical", cn.getHiCritical());
				if (obs.getValueNumeric() != null && (cn.getLowNormal() != null || cn.getHiNormal() != null)) {
					obj.put(K_INTERPRETATION, fhirInterpretation(
						obs.getValueNumeric(), cn.getLowCritical(), cn.getLowNormal(),
						cn.getHiNormal(), cn.getHiCritical()));
				}
			}
		}
		return obj;
	}

	private SimpleObject fhirInterpretation(double value, Double ll, Double l, Double h, Double hh) {
		return fhirInterpretationFromCode(interpCode(value, ll, l, h, hh));
	}

	private String interpCode(double value, Double ll, Double l, Double h, Double hh) {
		if (ll != null && value < ll) return "LL";
		if (l  != null && value < l)  return "L";
		if (hh != null && value > hh) return "HH";
		if (h  != null && value > h)  return "H";
		return "N";
	}

	private SimpleObject fhirInterpretationFromCode(String code) {
		SimpleObject interp = new SimpleObject();
		interp.put("system", FHIR_INTERP_SYSTEM);
		interp.put("code", code);
		interp.put("display", interpDisplay(code));
		return interp;
	}

	private String interpDisplay(String code) {
		if ("LL".equals(code)) return "Critical Low";
		if ("L".equals(code))  return "Low";
		if ("HH".equals(code)) return "Critical High";
		if ("H".equals(code))  return "High";
		return "Normal";
	}

	// ── Utilities ─────────────────────────────────────────────────────────────

	private String formatDate(Date date) {
		return date == null ? "" : dateFormat.format(date);
	}

	private String obsValueAsString(Obs obs) {
		if (obs.getValueNumeric() != null)  return String.valueOf(obs.getValueNumeric());
		if (obs.getValueCoded() != null)    return obs.getValueCoded().getName().getName();
		if (obs.getValueText() != null)     return obs.getValueText();
		if (obs.getValueDate() != null)     return formatDate(obs.getValueDate());
		if (obs.getValueDatetime() != null) return formatDate(obs.getValueDatetime());
		return "";
	}

	private List<Concept> conceptList(ConceptService cs, String... uuids) {
		List<Concept> list = new ArrayList<>();
		for (String uuid : uuids) {
			Concept c = cs.getConceptByUuid(uuid);
			if (c != null) {
				list.add(c);
			}
		}
		return list;
	}
}
