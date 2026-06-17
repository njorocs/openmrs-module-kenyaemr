/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.mchcs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Months;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.*;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;

import java.util.*;

public class PendingHBVTestCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {
    protected static final Log log = LogFactory.getLog(PendingHBVTestCalculation.class);

    StringBuilder flagMsg = new StringBuilder();

    @Override
    public String getFlagMessage() {
        return flagMsg.toString();
    }

    public static final String IMMUNIZATION_ENCOUNTER_TYPE_UUID = "29c02aff-9a93-46c9-bf6f-48b552fcb1fa";

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

        Program mchcsProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHCS);

        Set<Integer> alive = Filters.alive(cohort, context);
        Set<Integer> inMchcsProgram = Filters.inProgram(mchcsProgram, alive, context);

        CalculationResultMap lastChildHivStatus = Calculations.lastObs(Dictionary.getConcept(Dictionary.CHILDS_CURRENT_HIV_STATUS), cohort, context);

        Concept hivExposed = Dictionary.getConcept(Dictionary.EXPOSURE_TO_HIV);
        Concept hivExposedUnknown = Dictionary.getConcept(Dictionary.UNKNOWN);
        Concept hbvConcept = Dictionary.getConcept(Dictionary.HBV_IMMUNIZATION_CONCEPT_UUID);
        Concept reactiveHbvConcept = Dictionary.getConcept(Dictionary.POSITIVE);

        EncounterType immunizationEncType = Context.getEncounterService().getEncounterTypeByUuid(IMMUNIZATION_ENCOUNTER_TYPE_UUID);

        CalculationResultMap ret = new CalculationResultMap();

        for (Integer ptId : cohort) {
            boolean pendingHbvTest = false;

            if (inMchcsProgram.contains(ptId)) {
                Person person = Context.getPersonService().getPerson(ptId);
                Integer ageInMonths = getAgeInMonths(person.getBirthdate(), context.getNow());

                if (ageInMonths >= 9) {
                    Obs hivStatusObs = EmrCalculationUtils.obsResultForPatient(lastChildHivStatus, ptId);

                    if (hivStatusObs != null && (hivStatusObs.getValueCoded().equals(hivExposed) || hivStatusObs.getValueCoded().equals(hivExposedUnknown))) {

                        if (immunizationEncType != null) {
                            Patient patient = Context.getPatientService().getPatient(ptId);
                            List<Encounter> immunizationEncounters = Context.getEncounterService().getEncounters(
                                    new EncounterSearchCriteriaBuilder()
                                            .setPatient(patient)
                                            .setEncounterTypes(Collections.singletonList(immunizationEncType))
                                            .setIncludeVoided(false)
                                            .createEncounterSearchCriteria()
                            );

                            if (!immunizationEncounters.isEmpty()) {
                                boolean hbvObsFound = false;
                                outer:
                                for (Encounter enc : immunizationEncounters) {
                                    for (Obs obs : enc.getObs()) {
                                        if (obs.getConcept().equals(hbvConcept) && obs.getValueCoded() != null) {
                                            if (obs.getValueCoded().equals(reactiveHbvConcept)) {
                                                pendingHbvTest = true;
                                                flagMsg.setLength(0);
                                                flagMsg.append("Positive HBV");
                                            }
                                            hbvObsFound = true;
                                            break outer;
                                        }
                                    }
                                }
                                if (!hbvObsFound) {
                                    pendingHbvTest = true;
                                    flagMsg.setLength(0);
                                    flagMsg.append("Due For HBV Test");
                                }
                            }
                        }
                    }
                }
            }

            ret.put(ptId, new BooleanResult(pendingHbvTest, this, context));
        }

        return ret;
    }

    private Integer getAgeInMonths(Date birthDate, Date now) {
        DateTime d1 = new DateTime(birthDate.getTime());
        DateTime d2 = new DateTime(now.getTime());
        return Months.monthsBetween(d1, d2).getMonths();
    }
}
