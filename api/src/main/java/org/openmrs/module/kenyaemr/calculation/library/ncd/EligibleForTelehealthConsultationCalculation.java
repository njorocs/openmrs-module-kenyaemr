/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.ncd;

import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Filters;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Calculates whether patients are eligible for Telehealth Consultation
 *  * Eligibility: Enrolled into NCD
 */

public class EligibleForTelehealthProgramCalculation extends AbstractPatientCalculation {

    private static final Program NCD_PROGRAM = Context.getProgramWorkflowService()
            .getProgramByUuid(NCDMetadata.class);

    private static final EncounterType NCD_INITIAL = Context.getEncounterWorkflowService()
            .getEncounterTypeByUuid(_EncounterType.NCD_INITIAl);

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> params, PatientCalculationContext context) {

        // Patients enrolled in the NCD program at the evaluation date
        Set<Integer> inProgram = Filters.inProgram(cohort, NCD, context);
        Set<Integer> inProgram = Filters.EncounterType(NCD_INITIAL, context);

        CalculationResultMap ret = new CalculationResultMap();
        for (Integer ptId : cohort) {
            ret.put(ptId, new BooleanResult(inProgram.contains(ptId), this));
        }
        return ret;
    }
}