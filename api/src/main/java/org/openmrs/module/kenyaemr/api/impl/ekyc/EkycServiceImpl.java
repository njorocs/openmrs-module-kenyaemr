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

import org.json.JSONObject;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.db.hibernate.EkycDao;
import org.openmrs.module.kenyaemr.api.events.EventsBroadcaster;
import org.openmrs.module.kenyaemr.api.impl.token.TokenCache;
import org.openmrs.module.kenyaemr.api.impl.token.TokenService;
import org.openmrs.module.kenyaemr.api.model.BiometricVerification;
import org.openmrs.module.kenyaemr.api.model.OtpUseRequest;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Service
public class EkycServiceImpl implements EkycService {

    @Autowired private EkycDao dao;
    @Autowired private TokenService tokenService;
    @Autowired private EventsBroadcaster events;

    private final RestTemplate rest = new RestTemplate();

    @Override
    public BiometricVerification startVerification(String patientUuid, String subjectId, String subjectIdType, String agentId, String agentIdType, String reason, String locationName) throws Exception {
        // Global Properties
        String maxAttempts = EmrUtils.getGlobalPropertyValue(CommonMetadata.GP_EKYC_MAX_ATTEMPTS);
        String requestUrl = EmrUtils.getGlobalPropertyValue(CommonMetadata.GP_EKYC_REQUEST_URL);
        String callbackUrl = EmrUtils.getGlobalPropertyValue(CommonMetadata.GP_EKYC_CALLBACK_URL);

        int maxAttemptsVal;
        try {
            maxAttemptsVal = Integer.parseInt(maxAttempts);
        } catch (Exception ex) {
            maxAttemptsVal = 4;
        }
        // ===== Create DB record first =====
        BiometricVerification biometricVerification = new BiometricVerification();
        biometricVerification.setPatientUuid(patientUuid);
        biometricVerification.setAttemptsUsed(0);
        biometricVerification.setTotalAttempts(maxAttemptsVal);
        biometricVerification.setCompleted(false);
        biometricVerification.setDateCreated(new Date());
        dao.saveVerification(biometricVerification);

        // ===== Fetch OAuth Token =====
        String accessToken = tokenService.getValidToken();
        String tokenType = TokenCache.getTokenType();  // Bearer or other type

        // ===== Build request payload =====
        JSONObject payload = new JSONObject();
        payload.put("notification_callback_url", callbackUrl);
        payload.put("reason", reason);
        payload.put("relying_party_agent_id_number", agentId);
        payload.put("relying_party_agent_id_type", agentIdType);
        payload.put("subject_id_number", subjectId);
        payload.put("subject_id_type", subjectIdType);
        payload.put("relying_party_request_id", biometricVerification.getId().toString());
        payload.put("expiry_in_seconds", 3600);
        payload.put("service_id", "medical-care");
        payload.put("total_attempts", maxAttempts);
        payload.put("location_name", locationName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", tokenType + " " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> resp = rest.postForEntity(requestUrl, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("eKYC request failed: " + resp.getStatusCode());
        }
        JSONObject obj = new JSONObject(resp.getBody());
        biometricVerification.setRequestId(obj.getString("request_id"));
        biometricVerification.setRelyingPartyRequestId(payload.getString("relying_party_request_id"));
        biometricVerification.setStatus("pending");
        biometricVerification.setDateUpdated(new Date());
        dao.updateVerification(biometricVerification);

        return biometricVerification;
    }

    @Override
    public void handleCallback(String callbackJson) {
        JSONObject o = new JSONObject(callbackJson);
        
        String requestId = o.optString("request_id");
        String status = o.optString("status");
        String result = o.optString("result");

        BiometricVerification biometricVerification = dao.getByRequestId(requestId);
        if (biometricVerification == null) return;

        biometricVerification.setLastCallbackRaw(callbackJson);
        biometricVerification.setStatus(status);
        biometricVerification.setResult(result);
        biometricVerification.setDateUpdated(new Date());

        boolean isFinal = false;

        // Successful match
        if ("match".equals(result) && "accepted".equals(status)) {
            isFinal = true;
            biometricVerification.setFinalResult("match");
            biometricVerification.setCompleted(true);
        }
        // Biometrics not found
        else if ("biometrics_not_found".equals(result)) {
            isFinal = true;
            biometricVerification.setFinalResult("biometrics_not_found");
            biometricVerification.setCompleted(true);
        }
        // No match, but may have retries
        else if ("no_match".equals(result)) {
            int used = (biometricVerification.getAttemptsUsed() == null ? 0 : biometricVerification.getAttemptsUsed()) + 1;
            biometricVerification.setAttemptsUsed(used);
            if (used < biometricVerification.getTotalAttempts()) {
                isFinal = false;
            } else {
                isFinal = true;
                biometricVerification.setFinalResult("no_match");
                biometricVerification.setCompleted(true);
            }
        }
        // Terminal statuses
        else if (Arrays.asList("capture_failed", "expired", "failed", "canceled").contains(status)) {
            isFinal = true;
            biometricVerification.setFinalResult(status);
            biometricVerification.setCompleted(true);
        }
        // Captured (for storage)
        else if ("captured".equals(status)) {
            isFinal = true;
            biometricVerification.setFinalResult("captured");
            biometricVerification.setCompleted(true);
        }
        dao.updateVerification(biometricVerification);

        // Emit SSE payload
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", biometricVerification.getRequestId());
        data.put("status", biometricVerification.getStatus());
        data.put("result", biometricVerification.getResult());
        data.put("attemptsUsed", biometricVerification.getAttemptsUsed());
        data.put("totalAttempts", biometricVerification.getTotalAttempts());
        data.put("completed", biometricVerification.getCompleted());
        data.put("finalResult", biometricVerification.getFinalResult());

        String eventName = isFinal ? "final-status" : "status-update";
        events.sendStatusEvent(biometricVerification.getRequestId(), data, eventName);
    }

    // OTP Fallback request
    @Override
    public void requestOtpUse(String requestId, String reason, String requestedBy) {
        BiometricVerification biometricVerification = dao.getByRequestId(requestId);
        if (biometricVerification == null) {
            throw new IllegalArgumentException("Unknown requestId: " + requestId);
        }
        OtpUseRequest otpUseRequest = new OtpUseRequest();
        otpUseRequest.setVerificationId(biometricVerification.getId());
        otpUseRequest.setPatientUuid(biometricVerification.getPatientUuid());
        otpUseRequest.setReason(reason);
        otpUseRequest.setRequestedBy(requestedBy);
        otpUseRequest.setApproved(false);
        otpUseRequest.setDateRequested(new Date());

        dao.saveOtpRequest(otpUseRequest);

        // Notify frontend
        Map<String, Object> otpStatusMap = new HashMap<>();
        otpStatusMap.put("requestId", requestId);
        otpStatusMap.put("otpRequestId", otpUseRequest.getId());
        otpStatusMap.put("status", "otp_requested");

        events.sendStatusEvent(requestId, otpStatusMap, "otp-requested");
    }

}
