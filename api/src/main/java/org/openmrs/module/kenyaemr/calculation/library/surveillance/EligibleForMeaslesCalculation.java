/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.surveillance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Calculates the eligibility for Measles screening flag for  patients
 *
 * @should calculate Fever
 * @should calculate Generalised Rash
 * @should calculate Coryza
 * @should calculate Conjuctivitis( red eyes)
 * @should calculate Cough
 */
public class EligibleForMeaslesCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {
    protected static final Log log = LogFactory.getLog(EligibleForMeaslesCalculation.class);

    public static final EncounterType consultationEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);
    public static final Form clinicalEncounterForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.CLINICAL_ENCOUNTER);

    public static final EncounterType greenCardEncType = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION);
    public static final Form greenCardForm = MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD);

    @Override
    public String getFlagMessage() {
        return "Suspected Measles case";
    }

    Integer FEVER = 140238;
    Integer RASH = 512;
    Integer CORYZA = 106;
    Integer CONJUCTIVITIS = 127777;
    Integer COUGH = 143264;
    Integer ONSET_DATE = 159948;
    Integer SCREENING_QUESTION = 5219;

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

        Set<Integer> alive = Filters.alive(cohort, context);
        PatientService patientService = Context.getPatientService();
        CalculationResultMap ret = new CalculationResultMap();

        for (Integer ptId : alive) {
            boolean eligible = false;
            Date currentDate = new Date();
            Integer greenCardDateDifference = 0;
            Integer clinicalEncounterDateDifference = 0;
            Date greenCardOnsetDate = null;
            Date clinicalEnounterOnsetDate = null;
            Date dateCreated = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayDate = dateFormat.format(currentDate);
            Patient patient = patientService.getPatient(ptId);

            Encounter lastHivFollowUpEncounter = EmrUtils.lastEncounter(patient, greenCardEncType, greenCardForm);   //last greencard followup form
            Encounter lastClinicalEncounter = EmrUtils.lastEncounter(patient, consultationEncType, clinicalEncounterForm);   //last clinical encounter form

            ConceptService cs = Context.getConceptService();
            Concept feverResult = cs.getConcept(FEVER);
            Concept rashResult = cs.getConcept(RASH);
            Concept coryzaResult = cs.getConcept(CORYZA);
            Concept coughResult = cs.getConcept(COUGH);
            Concept conjuctivitisResult = cs.getConcept(CONJUCTIVITIS);
            Concept screeningQuestion = cs.getConcept(SCREENING_QUESTION);

            boolean patientFeverResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, feverResult) : false;
            boolean patientRashResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, rashResult) : false;
            boolean patientCoryzaResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, coryzaResult) : false;
            boolean patientCoughResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, coughResult) : false;
            boolean patientConjuctivitisResultGreenCard = lastHivFollowUpEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastHivFollowUpEncounter, screeningQuestion, conjuctivitisResult) : false;
            boolean patientFeverResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, feverResult) : false;
            boolean patientRashResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, rashResult) : false;
            boolean patientCoryzaResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, coryzaResult) : false;
            boolean patientCoughResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, coughResult) : false;
            boolean patientConjuctivitisResultClinical = lastClinicalEncounter != null ? EmrUtils.encounterThatPassCodedAnswer(lastClinicalEncounter, screeningQuestion, conjuctivitisResult) : false;

            if (lastHivFollowUpEncounter != null) {
                if (patientFeverResultGreenCard && patientRashResultGreenCard && patientCoryzaResultGreenCard && patientCoughResultGreenCard && patientConjuctivitisResultGreenCard) {
                    for (Obs obs : lastHivFollowUpEncounter.getObs()) {
                        dateCreated = obs.getDateCreated();
                        if (obs.getConcept().getConceptId().equals(ONSET_DATE)) {
                            greenCardOnsetDate = obs.getValueDatetime();
                            greenCardDateDifference = daysBetween(currentDate, greenCardOnsetDate);
                        }
                        if (dateCreated != null) {
                            String createdDate = dateFormat.format(dateCreated);
                            if (greenCardDateDifference <= 10) {
                                if (createdDate != null && createdDate.equals(todayDate)) {
                                    eligible = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (lastClinicalEncounter != null) {
                if (patientFeverResultClinical && patientRashResultClinical && patientCoryzaResultClinical && patientCoughResultClinical && patientConjuctivitisResultClinical) {
                    for (Obs obs : lastClinicalEncounter.getObs()) {
                        dateCreated = obs.getDateCreated();
                        if (obs.getConcept().getConceptId().equals(ONSET_DATE)) {
                            clinicalEnounterOnsetDate = obs.getValueDatetime();
                            clinicalEncounterDateDifference = daysBetween(currentDate, clinicalEnounterOnsetDate);
                        }
                        if (dateCreated != null) {
                            String createdDate = dateFormat.format(dateCreated);
                            if (clinicalEncounterDateDifference <= 10) {
                                if (createdDate != null && createdDate.equals(todayDate)) {
                                    eligible = true;
                                }
                                break;
                            }
                        }
                    }
                }
            }
            ret.put(ptId, new BooleanResult(eligible, this));
        }

        return ret;
    }

    private int daysBetween(Date date1, Date date2) {
        DateTime d1 = new DateTime(date1.getTime());
        DateTime d2 = new DateTime(date2.getTime());
        return Math.abs(Days.daysBetween(d1, d2).getDays());
    }
}
