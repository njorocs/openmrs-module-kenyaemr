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
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            // NOTE: do not require obs created "today" so recent triage entries trigger the flag.

            // Criterion 1: temperature >= 38 °C
            Concept temperatureConcept = Dictionary.getConcept(Dictionary.TEMPERATURE);
            double temperature = 0.0;
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().equals(temperatureConcept) && obs.getValueNumeric() != null) {
                    temperature = obs.getValueNumeric();
                    break;
                }
            }

            // Criterion 2: travel history (any value) OR contact with suspected case (yes/no)
            Concept travelHistoryConcept = Dictionary.getConcept(Dictionary.TRAVEL_HISTORY);
            boolean hasTravelHistory = false;
            Date triageDate = lastTriageEncounter.getEncounterDatetime();
            // check travel obs on the last triage encounter (or any travel obs within 21 days prior)
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().equals(travelHistoryConcept) && obs.getValueCoded() != null) {
                    // if travel recorded on this encounter treat as within window; if travel obs from earlier encounters
                    Date obsDate = obs.getObsDatetime() != null ? obs.getObsDatetime() : obs.getEncounter().getEncounterDatetime();
                    long diff = Math.abs(triageDate.getTime() - obsDate.getTime());
                    long days = diff / (1000L * 60 * 60 * 24);
                    if (days <= 21) {
                        hasTravelHistory = true;
                        break;
                    }
                }
            }
            // check contact with suspected case within last 21 days across encounters
            boolean hasContactWithCase = false;
            Concept contactConcept = Dictionary.getConcept(Dictionary.CONTACT_WITH_SUSPECTED_CASE);
            if (contactConcept != null) {
                // scan patient's encounters in the 21 days prior to triage for a yes answer
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
                // also check last triage encounter directly as a fallback
                if (!hasContactWithCase && EmrUtils.hasYesAnswer(lastTriageEncounter, Dictionary.CONTACT_WITH_SUSPECTED_CASE)) {
                    hasContactWithCase = true;
                }
            }

            boolean meetsAlertCriteria = temperature >= 38.0 && (hasTravelHistory || hasContactWithCase);

            if (!meetsAlertCriteria) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            // Patient meets suspect criteria (fever + travel/contact within window) -> move to isolation
            suspected = true;
            flagMessage = EBOLA_FLAG_LABEL;

            // Criterion 3: presenting symptoms
            Concept screeningQuestion = Dictionary.getConcept(Dictionary.PRESENTING_SYMPTOMS_SCREENING_QUESTION);

            boolean hasHeadache      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.HEADACHE));
            boolean hasLethargy      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.LETHARGY));
            boolean hasBodyAche      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.BODY_ACHE));
            boolean hasAbdominalPain = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.ABDOMINAL_PAIN));
            boolean hasVomiting      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.VOMITING));
            boolean hasDiarrhea      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.DIARRHEA));
            boolean hasBleeding      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.BLEEDING));

            int symptomCount = 0;
            if (hasHeadache) symptomCount++;
            if (hasLethargy) symptomCount++;
            if (hasBodyAche) symptomCount++;
            if (hasAbdominalPain) symptomCount++;
            if (hasVomiting) symptomCount++;
            if (hasDiarrhea) symptomCount++;
            if (hasBleeding) symptomCount++;

            // symptomCount used below to determine escalation

            // Criterion 4: high-risk occupation
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

            // If patient presents with >= 3 key symptoms escalate to suspected case (isolate)
            if (symptomCount >= 3) {
                flagMessage = EBOLA_FLAG_LABEL;
                suspected = true;
            } else if (hasHighRiskOccupation) {
                // keep isolation message but note high risk occupation
                flagMessage = EBOLA_FLAG_LABEL;
            }

            // Persist a short-lived flag as an obs on the triage encounter (expires after 24 hours)
            try {
                // Prefer a configured dedicated concept for storing Ebola flags. Fallback to FREE_TEXT_GENERAL.
                String gpFlagConceptUuid = Context.getAdministrationService().getGlobalProperty("kenyaemr.ebola.flag.concept.uuid");
                Concept flagConcept = null;
                if (gpFlagConceptUuid != null && !gpFlagConceptUuid.trim().isEmpty()) {
                    try {
                        flagConcept = Dictionary.getConcept(gpFlagConceptUuid.trim());
                    } catch (Exception ignored) {}
                }
                if (flagConcept == null) {
                    flagConcept = Dictionary.getConcept(Dictionary.FREE_TEXT_GENERAL); // FREE_TEXT_GENERAL
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
                                    // void expired flag
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
                // don't let persistence errors stop the calculation
                System.err.println("Failed to persist Ebola flag obs: " + e.getMessage());
            }

            ret.put(ptId, new BooleanResult(suspected, this));
        }

        return ret;
    }
}
