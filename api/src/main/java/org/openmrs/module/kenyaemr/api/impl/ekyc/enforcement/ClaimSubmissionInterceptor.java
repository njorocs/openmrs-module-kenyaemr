/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl.ekyc.enforcement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.stream.Collectors;

@Component
public class ClaimSubmissionInterceptor implements HandlerInterceptor {

    @Autowired
    private EkycVerificationEnforcementService enforcement;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler)
            throws Exception {

        if (req.getRequestURI().contains("/ws/rest/v1/claim/submit") && "POST".equalsIgnoreCase(req.getMethod())) {//TODO confirm endpoint for claims processing

            String body = readBody(req);
            String patientUuid = JsonUtil.extractPatientUuid(body);

            if (patientUuid == null) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"patientUuid missing in request\"}");
                return false;
            }

            try {
                enforcement.assertClaimVerified(patientUuid);
            } catch (VerificationException ex) {
                resp.setStatus(403);
                resp.getWriter().write("{\"error\":\" + ex.getMessage() + \"}");
                return false;
            }
        }
        return true;
    }

    private String readBody(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader();
            return reader.lines().collect(Collectors.joining());
        } catch (Exception ex) {
            return null;
        }
    }
}
