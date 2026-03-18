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

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.NCDMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Calculates whether patients are eligible for Telehealth Consultation
 * * Eligibility: Enrolled into NCD
 * * and have had an NCD Initial encounter
 */

public class EligibleForTeleHealthConsultationFormCalculation extends AbstractPatientCalculation {

    public static final EncounterType ncdInitialType = MetadataUtils.existing(EncounterType.class,
            NCDMetadata._EncounterType.NCD_INITIAL);
    public static final Form NCD_INITIAL_FORM = MetadataUtils.existing(Form.class, NCDMetadata._Form.NCD_INITIAL_FORM);

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> params,
            PatientCalculationContext context) {

        // Patients enrolled in the NCD program at the evaluation date
        Set<Integer> alive = Filters.alive(cohort, context);
        Program ncdProgram = MetadataUtils.existing(Program.class, NCDMetadata._Program.NCD);
        Set<Integer> inNcdProgram = Filters.inProgram(ncdProgram, alive, context);
        PatientService patientService = Context.getPatientService();

        CalculationResultMap ret = new CalculationResultMap();
        for (Integer ptId : cohort) {
            boolean eligible = false;
            Patient patient = patientService.getPatient(ptId);

            Encounter lastNcdInitialEnc = EmrUtils.lastEncounter(patient, ncdInitialType, NCD_INITIAL_FORM);

            if (inNcdProgram.contains(ptId) && lastNcdInitialEnc != null) {
                eligible = true;
            }

            ret.put(ptId, new BooleanResult(eligible, this));
        }
        return ret;
    }
}