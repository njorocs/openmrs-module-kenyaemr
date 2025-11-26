/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl.ekyc;

import org.openmrs.module.kenyaemr.api.model.BiometricVerification;

public interface EkycService {

    /**
     * Start biometric verification by calling eKYC remote endpoint.
     */
    BiometricVerification startVerification(
            String patientUuid,
            String subjectId,
            String subjectIdType,
            String agentId,
            String agentIdType,
            String reason,
            String locationName
    ) throws Exception;

    /**
     * Handle callback from the eKYC service.
     */
    void handleCallback(String callbackJson);

    /**
     * Request OTP fallback permission after biometric no-match or missing biometrics.
     */
    void requestOtpUse(String requestId, String reason, String requestedBy);
}

