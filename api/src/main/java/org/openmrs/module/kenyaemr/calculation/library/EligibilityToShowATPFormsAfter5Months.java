/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

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
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

/**
 * Calculates whether patients are eligible to be shown ATP forms i.e Disclosure
 * Readiness Assessment,and Transition Readiness Assessment
 */
public class EligibilityToShowATPFormsAfter5Months extends AbstractPatientCalculation {
    public static final EncounterType atpDisclosureEncType = MetadataUtils.existing(EncounterType.class,
            CommonMetadata._EncounterType.ATP_DISCLOSURE_READINESS_ASSESSMENT);
    public static final Form atpDisclosureForm = MetadataUtils.existing(Form.class,
            CommonMetadata._Form.ATP__DISCLOSURE_READINESS_ASSESSMENT_FORM);
    public static final EncounterType atpTransitionEncType = MetadataUtils.existing(EncounterType.class,
            CommonMetadata._EncounterType.ATP_TRANSITION_READINESS);
    public static final Form atpTransitionForm = MetadataUtils.existing(Form.class,
            CommonMetadata._Form.ATP_TRANSITION_READINESS_ASS_FORM);

    /**
     * @see org.openmrs.calculation.patient.PatientCalculation#evaluate(Collection,
     *      Map, PatientCalculationContext)
     */
    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> params,
            PatientCalculationContext context) {
        Set<Integer> alive = Filters.alive(cohort, context);
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        Set<Integer> inHivProgram = Filters.inProgram(hivProgram, alive, context);
        PatientService patientService = Context.getPatientService();

        CalculationResultMap ret = new CalculationResultMap();
        for (Integer ptId : cohort) {

            boolean eligible = false;
            Patient patient = patientService.getPatient(ptId);

            if (inHivProgram.contains(ptId) && patient.getAge() < 25) {

                boolean disclosureReadinessEligible = eligibleAfterFiveMonths(patient,
                        atpDisclosureEncType, atpDisclosureForm);

                boolean transitionEligible = eligibleAfterFiveMonths(patient,
                        atpTransitionEncType, atpTransitionForm);

                eligible = disclosureReadinessEligible || transitionEligible;
            }

            ret.put(ptId, new BooleanResult(eligible, this));
        }
        return ret;
    }

    private boolean eligibleAfterFiveMonths(Patient patient, EncounterType encType, Form form) {

        Encounter lastEnc = EmrUtils.lastEncounter(patient, encType, form);

        if (lastEnc == null) {
            return true; // form never filled
        }

        Date lastDate = lastEnc.getEncounterDatetime();

        Calendar cal = Calendar.getInstance();
        cal.setTime(lastDate);
        cal.add(Calendar.MONTH, 5);

        Date nextEligibleDate = cal.getTime();

        return !new Date().before(nextEligibleDate);
    }

}
