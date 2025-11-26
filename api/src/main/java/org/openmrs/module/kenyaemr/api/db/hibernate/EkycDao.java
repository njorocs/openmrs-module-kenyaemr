/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.db.hibernate;

import org.openmrs.module.kenyaemr.api.model.BiometricVerification;
import org.openmrs.module.kenyaemr.api.model.OtpUseRequest;

import java.util.List;

public interface EkycDao {
    void saveVerification(BiometricVerification v);
    void updateVerification(BiometricVerification v);
    BiometricVerification getById(Integer id);
    BiometricVerification getByRequestId(String requestId);
    List<BiometricVerification> getAllVerifications();

    void saveOtpRequest(OtpUseRequest r);
    OtpUseRequest getOtpRequestById(Integer id);
    List<OtpUseRequest> getOtpRequestsByVerificationId(Integer verificationId);
    List<OtpUseRequest> getAllOtpRequests();
}
