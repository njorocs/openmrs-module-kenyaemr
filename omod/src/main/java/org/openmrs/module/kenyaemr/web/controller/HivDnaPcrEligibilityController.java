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
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculation;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.calculation.library.IsBreastFeedingCalculation;
import org.openmrs.module.kenyaemr.calculation.library.IsPregnantCalculation;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr")
public class HivDnaPcrEligibilityController extends BaseRestController {

    @GetMapping("/hiv-dna-pcr-eligibility")
    public ResponseEntity<SimpleObject> getHivDnaPcrEligibility(@RequestParam("patientUuid") String patientUuid) {
        if (StringUtils.isBlank(patientUuid)) {
            return new ResponseEntity<>(new SimpleObject(), HttpStatus.BAD_REQUEST);
        }

        Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
        if (patient == null) {
            return new ResponseEntity<>(new SimpleObject(), HttpStatus.NOT_FOUND);
        }

        EncounterType mchMotherConsultationType = Context.getEncounterService()
                .getEncounterTypeByUuid(MchMetadata._EncounterType.MCHMS_CONSULTATION);
        Concept finalHivTestResultConcept = Context.getConceptService()
                .getConceptByUuid(Metadata.Concept.RESULT_OF_HIV_TEST);
        if (mchMotherConsultationType == null || finalHivTestResultConcept == null) {
            return new ResponseEntity<>(new SimpleObject(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Integer patientId = patient.getPatientId();
        boolean pregnant = evaluateCalculation(patientId, new IsPregnantCalculation());
        boolean breastfeeding = evaluateCalculation(patientId, new IsBreastFeedingCalculation());

        Obs latestResult = findLatestFinalHivResult(patient, mchMotherConsultationType, finalHivTestResultConcept);
        boolean hasFinalResult = latestResult != null;
        boolean hasInconclusiveResult = hasFinalResult
                && latestResult.getValueCoded() != null
                && Metadata.Concept.INDETERMINATE.equals(latestResult.getValueCoded().getUuid());

        SimpleObject response = new SimpleObject()
                .add("eligible", (pregnant || breastfeeding) && hasInconclusiveResult)
                .add("pregnant", pregnant)
                .add("breastfeeding", breastfeeding)
                .add("hasFinalResult", hasFinalResult)
                .add("hasInconclusiveResult", hasInconclusiveResult);
        return ResponseEntity.ok(response);
    }

    private boolean evaluateCalculation(Integer patientId, PatientCalculation calculation) {
        CalculationResult result = Context.getService(PatientCalculationService.class).evaluate(patientId, calculation);
        return result != null && Boolean.TRUE.equals(result.getValue());
    }

    private Obs findLatestFinalHivResult(Patient patient, EncounterType encounterType, Concept finalHivTestResultConcept) {
        List<Obs> obsList = Context.getObsService()
                .getObservationsByPersonAndConcept(patient, finalHivTestResultConcept);

        Obs latest = null;
        for (Obs obs : obsList) {
            Encounter encounter = obs.getEncounter();
            boolean skip = encounter == null
                    || !encounterType.equals(encounter.getEncounterType())
                    || obs.getObsDatetime() == null;
            if (!skip && (latest == null || obs.getObsDatetime().after(latest.getObsDatetime()))) {
                latest = obs;
            }
        }
        return latest;
    }
}
