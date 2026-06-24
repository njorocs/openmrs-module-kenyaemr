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

            if (!EmrUtils.encounterHasObsCreatedToday(lastTriageEncounter)) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

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
            for (Obs obs : lastTriageEncounter.getAllObs()) {
                if (obs.getConcept().equals(travelHistoryConcept) && obs.getValueCoded() != null) {
                    hasTravelHistory = true;
                    break;
                }
            }
            boolean hasContactWithCase = EmrUtils.hasYesAnswer(lastTriageEncounter, Dictionary.CONTACT_WITH_SUSPECTED_CASE);

            boolean meetsAlertCriteria = temperature >= 38.0 && (hasTravelHistory || hasContactWithCase);

            if (!meetsAlertCriteria) {
                ret.put(ptId, new BooleanResult(false, this));
                continue;
            }

            suspected = true;
            flagMessage = "Suspected Ebola Case";

            // Criterion 3: presenting symptoms
            Concept screeningQuestion = Dictionary.getConcept(Dictionary.PRESENTING_SYMPTOMS_SCREENING_QUESTION);

            boolean hasHeadache      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.HEADACHE));
            boolean hasLethargy      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.LETHARGY));
            boolean hasBodyAche      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.BODY_ACHE));
            boolean hasAbdominalPain = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.ABDOMINAL_PAIN));
            boolean hasVomiting      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.VOMITING));
            boolean hasDiarrhea      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.DIARRHEA));
            boolean hasBleeding      = EmrUtils.encounterThatPassCodedAnswer(lastTriageEncounter, screeningQuestion, Dictionary.getConcept(Dictionary.BLEEDING));

            boolean hasAnySymptom = hasHeadache || hasLethargy || hasBodyAche
                    || hasAbdominalPain || hasVomiting || hasDiarrhea || hasBleeding;

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

            if (hasAnySymptom || hasHighRiskOccupation) {
                flagMessage = "Suspected Ebola Case";
            }

            ret.put(ptId, new BooleanResult(suspected, this));
        }

        return ret;
    }
}
