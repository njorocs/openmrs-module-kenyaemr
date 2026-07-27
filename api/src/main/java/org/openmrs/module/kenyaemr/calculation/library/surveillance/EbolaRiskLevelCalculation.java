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

import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.*;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.*;
import java.text.SimpleDateFormat;

public class EbolaRiskLevelCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {

    public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);
    public static final Form triageScreeningForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.TRIAGE);

    private static final List<String> HIGH_RISK_OCCUPATION_UUIDS = Arrays.asList(
            Dictionary.FARMER,
            Dictionary.STUDENT,
            Dictionary.ANIMAL_HANDLER,
            Dictionary.HANDLER_OF_DEAD_BODIES,
            Dictionary.COMMERCIAL_SEX_WORKER,
            Dictionary.FOREST_RANGER,
            Dictionary.CAVER
    );

    private static final String EBOLA_FLAG_LABEL = "Suspected Ebola Case";
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

        for (Integer ptId : alive) {
            boolean suspected = false;
            flagMessage = "";

            Patient patient = patientService.getPatient(ptId);
            List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);

            if (activeVisits.isEmpty()) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            Encounter lastTriageEncounter = EmrUtils.lastEncounter(patient, triageEncType, triageScreeningForm);
            if (lastTriageEncounter == null) {
                lastTriageEncounter = EmrUtils.lastEncounter(patient, triageEncType);
            }

            if (lastTriageEncounter == null) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }
            Concept temperatureConcept = Dictionary.getConcept(Dictionary.TEMPERATURE);
            double temperature = 0.0;
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().equals(temperatureConcept) && obs.getValueNumeric() != null) {
                    temperature = obs.getValueNumeric();
                    break;
                }
            }
            Concept travelHistoryConcept = Dictionary.getConcept(Dictionary.TRAVEL_HISTORY);
            boolean hasTravelHistory = false;
            Date triageDate = lastTriageEncounter.getEncounterDatetime();
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().equals(travelHistoryConcept) && obs.getValueCoded() != null) {
                    Date obsDate = obs.getObsDatetime() != null ? obs.getObsDatetime() : obs.getEncounter().getEncounterDatetime();
                    long diff = Math.abs(triageDate.getTime() - obsDate.getTime());
                    long days = diff / (1000L * 60 * 60 * 24);
                    if (days <= 21) {
                        hasTravelHistory = true;
                        break;
                    }
                }
            }
            boolean hasContactWithCase = false;
            Concept contactConcept = Dictionary.getConcept(Dictionary.CONTACT_WITH_SUSPECTED_CASE);
            if (contactConcept != null) {
                Date windowStart = new Date(triageDate.getTime() - (21L * 24 * 60 * 60 * 1000));
                List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(patient);
                for (Encounter e : encs) {
                    Date encDate = e.getEncounterDatetime();
                    if (encDate != null && !encDate.before(windowStart) && !encDate.after(triageDate)) {
                        if (EmrUtils.hasYesAnswer(e, Dictionary.CONTACT_WITH_SUSPECTED_CASE)) {
                            hasContactWithCase = true;
                            break;
                        }
                    }
                }
                if (!hasContactWithCase && EmrUtils.hasYesAnswer(lastTriageEncounter, Dictionary.CONTACT_WITH_SUSPECTED_CASE)) {
                    hasContactWithCase = true;
                }
            }

            boolean meetsAlertCriteria = temperature >= 38.0 && (hasTravelHistory || hasContactWithCase);

            if (!meetsAlertCriteria) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            suspected = true;
            flagMessage = EBOLA_FLAG_LABEL;
            Concept screeningQuestion = Dictionary.getConcept(Dictionary.PRESENTING_SYMPTOMS_SCREENING_QUESTION);

            // Count each Ebola symptom type at most once (Headache x3 still counts as 1).
            Set<Integer> ebolaSymptomConceptIds = new HashSet<Integer>();
            for (Concept symptomConcept : Arrays.asList(
                    Dictionary.getConcept(Dictionary.HEADACHE),
                    Dictionary.getConcept(Dictionary.LETHARGY),
                    Dictionary.getConcept(Dictionary.BODY_ACHE),
                    Dictionary.getConcept(Dictionary.ABDOMINAL_PAIN),
                    Dictionary.getConcept(Dictionary.VOMITING),
                    Dictionary.getConcept(Dictionary.DIARRHEA),
                    Dictionary.getConcept(Dictionary.BLEEDING))) {
                if (symptomConcept != null && symptomConcept.getConceptId() != null) {
                    ebolaSymptomConceptIds.add(symptomConcept.getConceptId());
                }
            }
            Set<Integer> uniqueSymptomsPresent = new HashSet<Integer>();
            for (Obs obs : lastTriageEncounter.getAllObs(false)) {
                if (obs.getConcept() != null && screeningQuestion != null
                        && screeningQuestion.equals(obs.getConcept())
                        && obs.getValueCoded() != null
                        && obs.getValueCoded().getConceptId() != null
                        && ebolaSymptomConceptIds.contains(obs.getValueCoded().getConceptId())) {
                    uniqueSymptomsPresent.add(obs.getValueCoded().getConceptId());
                }
            }
            int symptomCount = uniqueSymptomsPresent.size();

            boolean hasHighRiskOccupation = false;
            Concept occupationConcept = Dictionary.getConcept(Dictionary.OCCUPATION);

            if (occupationConcept != null) {
                for (Obs obs : lastTriageEncounter.getAllObs()) {
                    if (obs.getConcept().equals(occupationConcept) && obs.getValueCoded() != null) {
                        if (HIGH_RISK_OCCUPATION_UUIDS.contains(obs.getValueCoded().getUuid())) {
                            hasHighRiskOccupation = true;
                            break;
                        }
                    }
                }
            }

            if (symptomCount >= 3) {
                flagMessage = EBOLA_FLAG_LABEL;
                suspected = true;
            } else if (hasHighRiskOccupation) {
                flagMessage = EBOLA_FLAG_LABEL;
            }
            try {
                String gpFlagConceptUuid = Context.getAdministrationService().getGlobalProperty("kenyaemr.ebola.flag.concept.uuid");
                Concept flagConcept = null;
                if (gpFlagConceptUuid != null && !gpFlagConceptUuid.trim().isEmpty()) {
                    try {
                        flagConcept = Dictionary.getConcept(gpFlagConceptUuid.trim());
                    } catch (Exception ignored) {}
                }
                if (flagConcept == null) {
                    flagConcept = Dictionary.getConcept(Dictionary.FREE_TEXT_GENERAL);
                }
                boolean existingUnexpired = false;
                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                for (Obs o : lastTriageEncounter.getAllObs()) {
                    if (o.getConcept().equals(flagConcept) && o.getValueText() != null && o.getValueText().startsWith("ebola_flag:")) {
                        String val = o.getValueText();
                        String[] parts = val.split("expires=");
                        if (parts.length == 2) {
                            String ts = parts[1].split(";")[0];
                            try {
                                Date expiry = sdf.parse(ts);
                                if (expiry.after(now)) {
                                    existingUnexpired = true;
                                } else {
                                    o.setVoided(true);
                                    o.setVoidReason("expired flag");
                                    Context.getObsService().saveObs(o, "void expired");
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                }

                if (suspected && !existingUnexpired) {
                    Date expiryDate = new Date(triageDate.getTime() + 24L * 60 * 60 * 1000);
                    String valueText = "ebola_flag:" + flagMessage + ";expires=" + sdf.format(expiryDate) + ";source=calculation";
                    Obs obs = new Obs(patient, flagConcept, new Date(), lastTriageEncounter.getLocation());
                    obs.setValueText(valueText);
                    obs.setEncounter(lastTriageEncounter);
                    Context.getObsService().saveObs(obs, "created by ebola calculation");
                }
            } catch (Exception e) {
                System.err.println("Failed to persist Ebola flag obs: " + e.getMessage());
            }

            ret.put(ptId, new BooleanResult(suspected, this));
        }

        return ret;
    }
}
