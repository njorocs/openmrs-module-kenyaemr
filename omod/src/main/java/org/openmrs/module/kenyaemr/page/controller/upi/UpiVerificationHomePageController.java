/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.page.controller.upi;

import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;

import java.util.ArrayList;
import java.util.List;

@AppPage(EmrConstants.APP_UPI_VERIFICATION)
public class UpiVerificationHomePageController {

    public void get(@SpringBean KenyaUiUtils kenyaUi, UiUtils ui, PageModel model) {


        List<SimpleObject> pendingVerification = new ArrayList<SimpleObject>();
        List<SimpleObject> verified = new ArrayList<SimpleObject>();
        List<SimpleObject> verifiedOnART = new ArrayList<SimpleObject>();
        PersonAttributeType verificationStatusPA = Context.getPersonService().getPersonAttributeTypeByUuid(CommonMetadata._PersonAttributeType.VERIFICATION_STATUS_WITH_NATIONAL_REGISTRY);
        PersonAttributeType verificationMessagePA = Context.getPersonService().getPersonAttributeTypeByUuid(CommonMetadata._PersonAttributeType.VERIFICATION_MESSAGE_WITH_NATIONAL_REGISTRY);
        List<Patient> allPatients = Context.getPatientService().getAllPatients();
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);

        for (Patient patient : allPatients) {
            if (patient.getAttribute(verificationStatusPA) != null) {
                String networkError = "";
                if(patient.getAttribute(verificationMessagePA) != null) {
                    networkError = patient.getAttribute(verificationMessagePA).getValue().trim();
                }
                // Has attempted verification
                if (patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Pending")) {
                    // Has attempted verification but has not received NUPI
                    SimpleObject patientPendingObject = SimpleObject.create("id", patient.getId(), "uuid", patient.getUuid(), "givenName", patient
                            .getGivenName(), "middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "", "familyName", patient.getFamilyName(), "birthdate", kenyaUi.formatDate(patient.getBirthdate()), "gender", patient.getGender(), "error", networkError != null ? networkError : "-");
                    pendingVerification.add(patientPendingObject);
                }
                // Has attempted verification and has received NUPI
                if (patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Yes") || patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Verified") || patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Verified Elsewhere")) {
                    SimpleObject patientVerifiedObject = SimpleObject.create("id", patient.getId(), "uuid", patient.getUuid(), "givenName", patient
                            .getGivenName(), "middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "", "familyName", patient.getFamilyName(), "birthdate", kenyaUi.formatDate(patient.getBirthdate()), "gender", patient.getGender(), "error", networkError != null ? networkError : "-");
                    verified.add(patientVerifiedObject);
                }
                // Has successful verification and has received NUPI and in HIV program
                ProgramWorkflowService pwfservice = Context.getProgramWorkflowService();
                List<PatientProgram> programs = pwfservice.getPatientPrograms(patient, hivProgram, null, null, null,null, true);
                if (programs.size() > 0) {
                    if (patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Yes") || patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Verified") || patient.getAttribute(verificationStatusPA).getValue().trim().equalsIgnoreCase("Verified Elsewhere")) {
                        SimpleObject patientVerifiedObject = SimpleObject.create("id", patient.getId(), "uuid", patient.getUuid(), "givenName", patient
                                .getGivenName(), "middleName", patient.getMiddleName() != null ? patient.getMiddleName() : "", "familyName", patient.getFamilyName(), "birthdate", kenyaUi.formatDate(patient.getBirthdate()), "gender", patient.getGender(), "error", networkError != null ? networkError : "-");
                        verifiedOnART.add(patientVerifiedObject);
                    }
                }
            }
        }
        model.put("patientPendingList", ui.toJson(pendingVerification));
        model.put("patientPendingListSize", pendingVerification.size());
        model.put("patientVerifiedList", ui.toJson(verified));
        model.put("patientVerifiedListSize", verified.size());
        model.put("patientVerifiedOnARTListSize", verifiedOnART.size());
        model.put("totalAttemptedVerification", pendingVerification.size() + verified.size());
    }
}
