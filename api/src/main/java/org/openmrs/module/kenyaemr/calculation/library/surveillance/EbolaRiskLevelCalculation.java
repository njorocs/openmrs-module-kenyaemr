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
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.*;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.text.SimpleDateFormat;
import java.util.*;

public class EbolaRiskLevelCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {
    protected static final Log log = LogFactory.getLog(EbolaRiskLevelCalculation.class);

    public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
    public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);

    // Vitals / epidemiological questions
    public static final String TEMPERATURE_CONCEPT_UUID       = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String TRAVEL_HISTORY_CONCEPT_UUID    = "155deedc-3623-4fb1-a3ef-88bfe2b95ecc";
    public static final String CONTACT_WITH_CASE_CONCEPT_UUID = "eb42f103-6d87-4c1e-a5f8-1c74dff5840d";
    public static final String YES_CONCEPT_UUID               = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // Screening question (parent for coded symptom answers)
    public static final String SCREENING_QUESTION_UUID = "5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // Symptom concept UUIDs
    public static final String FEVER_CONCEPT_UUID           = "140238AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String VOMITING_CONCEPT_UUID        = "122983AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String DIARRHEA_CONCEPT_UUID        = "142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String ABDOMINAL_PAIN_CONCEPT_UUID  = "151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String HEADACHE_CONCEPT_UUID        = "139084AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String BODY_ACHE_CONCEPT_UUID       = "63c88-0bee-4a32-85dc-2c378a306731";
    public static final String LETHARGY_CONCEPT_UUID        = "116334AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String BLEEDING_CONCEPT_UUID        = "d5305f4f-745a-45ff-87ac-3d1fbad7d339";
    public static final String SYMPTOM_DURATION_CONCEPT_UUID = "159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // Occupation
    public static final String OCCUPATION_CONCEPT_UUID = "1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // High-risk occupation answer concept UUIDs
    private static final List<String> HIGH_RISK_OCCUPATION_UUIDS = Arrays.asList(
            "1538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Healthcare worker
            "159465AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Hunter
            "162944AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Animal handler
            "162940AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Handler of dead bodies
            "160579AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Commercial sex worker
            "163559AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",  // Forest ranger
            "163560AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"   // Caver
    );

    private String flagMessage = "";

    @Override
    public String getFlagMessage() {
        return flagMessage;
    }

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
        CalculationResultMap ret = new CalculationResultMap();

        Set<Integer> alive = Filters.alive(cohort, context);
        PatientService patientService = Context.getPatientService();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todayDate = dateFormat.format(new Date());

        System.out.println("===== EbolaRiskLevelCalculation START | cohort=" + cohort.size()
                + " alive=" + alive.size() + " today=" + todayDate + " =====");

        for (Integer ptId : alive) {
            boolean suspected = false;
            flagMessage = "";

            System.out.println("--- EbolaRiskLevel evaluating patient=" + ptId + " ---");

            Patient patient = patientService.getPatient(ptId);
            List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);

            System.out.println("EbolaRiskLevel [patient=" + ptId + "] activeVisits=" + activeVisits.size());

            if (activeVisits.isEmpty()) {
                System.out.println("EbolaRiskLevel [patient=" + ptId + "] SKIP: no active visit");
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            Encounter lastTriageEncounter = EmrUtils.lastEncounter(patient, triageEncType, triageScreeningForm);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] lastTriageEncounter="
                    + (lastTriageEncounter != null ? "id=" + lastTriageEncounter.getId()
                        + " date=" + dateFormat.format(lastTriageEncounter.getEncounterDatetime()) : "null"));

            if (lastTriageEncounter == null) {
                System.out.println("EbolaRiskLevel [patient=" + ptId + "] SKIP: no triage encounter found"
                        + " (encounterType=" + triageEncType.getName() + ", form=" + triageScreeningForm.getName() + ")");
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            boolean todayCheck = isToday(lastTriageEncounter, todayDate, dateFormat);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] isTodayEncounter=" + todayCheck
                    + " (encounter has " + lastTriageEncounter.getAllObs().size() + " obs)");

            if (!todayCheck) {
                System.out.println("EbolaRiskLevel [patient=" + ptId + "] SKIP: triage encounter is not from today");
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            // Dump all obs in the encounter for full visibility
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] ALL OBS IN TRIAGE ENCOUNTER:");
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                String value = obs.getValueCoded() != null
                        ? "coded=" + obs.getValueCoded().getUuid() + " (" + obs.getValueCoded().getName() + ")"
                        : obs.getValueNumeric() != null ? "numeric=" + obs.getValueNumeric()
                        : obs.getValueText() != null ? "text=" + obs.getValueText()
                        : "valueDatetime=" + obs.getValueDatetime();
                System.out.println("  obs[" + obs.getId() + "] concept=" + obs.getConcept().getUuid()
                        + " (" + obs.getConcept().getName() + ") => " + value
                        + " created=" + dateFormat.format(obs.getDateCreated()));
            }

            ConceptService cs = Context.getConceptService();

            // --- Criterion 1: temperature >= 38 °C ---
            double temperature = 0.0;
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().getUuid().equals(TEMPERATURE_CONCEPT_UUID) && obs.getValueNumeric() != null) {
                    temperature = obs.getValueNumeric();
                    break;
                }
            }
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] temperature=" + temperature
                    + " (threshold=38.0, passes=" + (temperature >= 38.0) + ")");

            // --- Criterion 2: travel history OR contact with suspected case ---
            boolean hasTravelHistory   = hasYesAnswer(lastTriageEncounter, TRAVEL_HISTORY_CONCEPT_UUID);
            boolean hasContactWithCase = hasYesAnswer(lastTriageEncounter, CONTACT_WITH_CASE_CONCEPT_UUID);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] travelHistory="
                    + hasTravelHistory + " (concept=" + TRAVEL_HISTORY_CONCEPT_UUID + ")"
                    + " | contactWithCase=" + hasContactWithCase + " (concept=" + CONTACT_WITH_CASE_CONCEPT_UUID + ")");

            boolean meetsAlertCriteria = temperature >= 38.0 && (hasTravelHistory || hasContactWithCase);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] Level1 meetsAlertCriteria=" + meetsAlertCriteria
                    + " (temp>=38=" + (temperature >= 38.0) + " AND (travel=" + hasTravelHistory + " OR contact=" + hasContactWithCase + "))");

            if (!meetsAlertCriteria) {
                System.out.println("EbolaRiskLevel [patient=" + ptId + "] STOP: Level1 not met - flag will not show");
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            suspected = true;
            flagMessage = "Suspected Ebola - Move to Isolation";
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] Level1 PASSED => \"" + flagMessage + "\"");

            // --- Criterion 3: presenting symptoms ---
            Concept screeningQuestion = cs.getConceptByUuid(SCREENING_QUESTION_UUID);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] screeningQuestion resolved="
                    + (screeningQuestion != null ? screeningQuestion.getName() : "NULL - check UUID " + SCREENING_QUESTION_UUID));

            boolean hasHeadache      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(HEADACHE_CONCEPT_UUID));
            boolean hasLethargy      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(LETHARGY_CONCEPT_UUID));
            boolean hasBodyAche      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(BODY_ACHE_CONCEPT_UUID));
            boolean hasAbdominalPain = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(ABDOMINAL_PAIN_CONCEPT_UUID));
            boolean hasVomiting      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(VOMITING_CONCEPT_UUID));
            boolean hasDiarrhea      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(DIARRHEA_CONCEPT_UUID));
            boolean hasBleeding      = safeCheck(lastTriageEncounter, screeningQuestion, cs.getConceptByUuid(BLEEDING_CONCEPT_UUID));

            boolean hasAnySymptom = hasHeadache || hasLethargy || hasBodyAche
                    || hasAbdominalPain || hasVomiting || hasDiarrhea || hasBleeding;

            System.out.println("EbolaRiskLevel [patient=" + ptId + "] SYMPTOMS:"
                    + " headache=" + hasHeadache
                    + " lethargy=" + hasLethargy
                    + " bodyAche=" + hasBodyAche
                    + " abdominalPain=" + hasAbdominalPain
                    + " vomiting=" + hasVomiting
                    + " diarrhea=" + hasDiarrhea
                    + " bleeding=" + hasBleeding
                    + " => hasAnySymptom=" + hasAnySymptom);

            // --- Criterion 4: high-risk occupation ---
            boolean hasHighRiskOccupation = false;
            String occupationValue = "not recorded";
            Concept occupationConcept = cs.getConceptByUuid(OCCUPATION_CONCEPT_UUID);
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] occupationConcept resolved="
                    + (occupationConcept != null ? occupationConcept.getName() : "NULL - check UUID " + OCCUPATION_CONCEPT_UUID));

            if (occupationConcept != null) {
                for (Obs obs : lastTriageEncounter.getAllObs()) {
                    if (obs.getConcept().equals(occupationConcept) && obs.getValueCoded() != null) {
                        occupationValue = obs.getValueCoded().getUuid() + " (" + obs.getValueCoded().getName() + ")";
                        boolean isHighRisk = HIGH_RISK_OCCUPATION_UUIDS.contains(obs.getValueCoded().getUuid());
                        System.out.println("EbolaRiskLevel [patient=" + ptId + "] occupation found: " + occupationValue
                                + " isHighRisk=" + isHighRisk);
                        if (isHighRisk) {
                            hasHighRiskOccupation = true;
                            break;
                        }
                    }
                }
                if (occupationValue.equals("not recorded")) {
                    System.out.println("EbolaRiskLevel [patient=" + ptId + "] occupation obs NOT found in encounter");
                }
            }

            System.out.println("EbolaRiskLevel [patient=" + ptId + "] hasHighRiskOccupation=" + hasHighRiskOccupation);

            // Level 2 – Suspected Case: alert criteria + (any symptom OR high-risk occupation)
            if (hasAnySymptom || hasHighRiskOccupation) {
                flagMessage = "Suspected Ebola - Move to Isolation";
            }
            System.out.println("EbolaRiskLevel [patient=" + ptId + "] ===== FINAL FLAG: \"" + flagMessage
                    + "\" suspected=" + suspected + " =====");

            ret.put(ptId, new BooleanResult(suspected, this));
        }

        System.out.println("===== EbolaRiskLevelCalculation END =====");

        return ret;
    }

    /** Returns true when the obs for this Yes/No question has a YES answer. */
    private boolean hasYesAnswer(Encounter enc, String questionConceptUuid) {
        for (Obs obs : enc.getAllObs()) {
            if (obs.getConcept().getUuid().equals(questionConceptUuid) && obs.getValueCoded() != null) {
                return obs.getValueCoded().getUuid().equals(YES_CONCEPT_UUID);
            }
        }
        return false;
    }

    /** Null-safe wrapper around EmrUtils.encounterThatPassCodedAnswer. */
    private boolean safeCheck(Encounter enc, Concept question, Concept answer) {
        if (question == null || answer == null) return false;
        return EmrUtils.encounterThatPassCodedAnswer(enc, question, answer);
    }

    /** Returns true when at least one obs in the encounter was created today. */
    private boolean isToday(Encounter enc, String todayDate, SimpleDateFormat dateFormat) {
        for (Obs obs : enc.getAllObs()) {
            if (obs.getDateCreated() != null && dateFormat.format(obs.getDateCreated()).equals(todayDate)) {
                return true;
            }
        }
        return false;
    }
}
