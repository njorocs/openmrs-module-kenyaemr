/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl.token;

import org.json.JSONObject;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenServiceImpl implements TokenService {

    private final RestTemplate rest = new RestTemplate();

    @Override
    public synchronized String getValidToken() throws Exception {

        if (TokenCache.isValid()) {
            return TokenCache.getAccessToken();
        }

        String refreshToken = TokenCache.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                return refreshWithRefreshToken(refreshToken);
            } catch (Exception ex) {
                // failed to refresh - clear cache and fallback to client_credentials
                TokenCache.clear();
            }
        }

        return fetchNewTokenClientCredentials();
    }

    private String refreshWithRefreshToken(String refreshToken) throws Exception {
        String ekycTokenUrl = EmrUtils.getGlobalPropertyValue("kenyaemre.kyc.tokenUrl");
        String ekycClientId = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.clientId");
        String ekycClientSecret = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.clientSecret");

        JSONObject payload = new JSONObject();
        payload.put("client_id", ekycClientId);
        payload.put("client_secret", ekycClientSecret);
        payload.put("grant_type", "refresh_token");
        payload.put("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> resp =
                rest.exchange(ekycTokenUrl, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Refresh token failed: " + resp.getStatusCode());
        }

        JSONObject json = new JSONObject(resp.getBody());
        storeToken(json);
        return json.getString("access_token");
    }

    private String fetchNewTokenClientCredentials() throws Exception {
        String ekycTokenUrl = EmrUtils.getGlobalPropertyValue("kenyaemre.kyc.tokenUrl");
        String ekycClientId = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.clientId");
        String ekycClientSecret = EmrUtils.getGlobalPropertyValue("kenyaemr.ekyc.clientSecret");

        JSONObject payload = new JSONObject();
        payload.put("client_id", ekycClientId);
        payload.put("client_secret", ekycClientSecret);
        payload.put("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> resp =
                rest.exchange(ekycTokenUrl, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Client credentials flow failed: " + resp.getStatusCode());
        }

        JSONObject json = new JSONObject(resp.getBody());
        storeToken(json);
        return json.getString("access_token");
    }

    private void storeToken(JSONObject json) {
        String ekycTokenRefreshMargin = EmrUtils.getGlobalPropertyValue("kenyaemre.kyc.tokenRefreshMarginSeconds");
        int tokenRefreshMargin = Integer.parseInt(ekycTokenRefreshMargin);
        String accessToken = json.getString("access_token");
        String tokenType = json.optString("token_type", "Bearer"); // Safe fallback
        String refreshToken = json.optString("refresh_token", null);

        int expiresIn = json.optInt("expires_in", 3600); // Provided by eCitizen or defaults to 3600

        long expiryEpoch = System.currentTimeMillis() + Math.max(0, (expiresIn - tokenRefreshMargin)) * 1000L;

        TokenCache.store(accessToken, refreshToken, tokenType, expiryEpoch);
    }
}
