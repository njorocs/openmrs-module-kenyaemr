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
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
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
        long now = System.currentTimeMillis();

        // 1. If current access token is still valid, return it immediately
        if (TokenCache.isValid()) {
            return TokenCache.getAccessToken();
        }

        // 2. If we have a refresh token, attempt refresh first
        String refreshToken = TokenCache.getRefreshToken();
        if (refreshToken != null) {
            try {
                return refreshWithRefreshToken(refreshToken);
            } catch (Exception ex) {
                // Refresh token invalid or expired → fallback to client_credentials
            }
        }

        // 3. Refresh token missing or failed → use client_credentials
        return fetchNewTokenClientCredentials();
    }

    private String refreshWithRefreshToken(String refreshToken) throws Exception {

        GlobalProperty ekycTokenUrl = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_TOKEN_URL);
        GlobalProperty ekycClientId = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_CLIENT_ID);
        GlobalProperty ekycClientSecret = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_CLIENT_SECRET);

        String tokenUrl = ekycTokenUrl.getPropertyValue();
        String clientId = ekycClientId.getPropertyValue();
        String clientSecret = ekycClientSecret.getPropertyValue();

        JSONObject payload = new JSONObject();
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("grant_type", "refresh_token");
        payload.put("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> resp =
                rest.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Refresh token failed: " + resp.getStatusCode());
        }

        JSONObject json = new JSONObject(resp.getBody());
        storeToken(json);
        return json.getString("access_token");
    }

    private String fetchNewTokenClientCredentials() throws Exception {
        GlobalProperty ekycTokenUrl = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_TOKEN_URL);
        GlobalProperty ekycClientId = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_CLIENT_ID);
        GlobalProperty ekycClientSecret = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_CLIENT_SECRET);

        String tokenUrl = ekycTokenUrl.getPropertyValue();
        String clientId = ekycClientId.getPropertyValue();
        String clientSecret = ekycClientSecret.getPropertyValue();
        JSONObject payload = new JSONObject();
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

        ResponseEntity<String> resp =
                rest.exchange(tokenUrl, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Client credentials flow failed: " + resp.getStatusCode());
        }

        JSONObject json = new JSONObject(resp.getBody());
        storeToken(json);
        return json.getString("access_token");
    }

    private void storeToken(JSONObject json) {
        GlobalProperty ekycTokenRefreshMargin = Context.getAdministrationService()
                .getGlobalPropertyObject(CommonMetadata.GP_EKYC_TOKEN_REFRESH_MARGIN_SECONDS);
        int tokenRefreshMargin = Integer.parseInt(ekycTokenRefreshMargin.getPropertyValue());
        String accessToken = json.getString("access_token");
        String tokenType = json.optString("token_type", "Bearer"); // Safe fallback
        String refreshToken = json.optString("refresh_token", null);

        int expiresIn = json.getInt("expires_in"); // Provided by eCitizen

        long expiryEpoch = System.currentTimeMillis() + ((expiresIn - tokenRefreshMargin) * 1000L);

        TokenCache.store(
                accessToken,
                refreshToken,
                tokenType,
                expiryEpoch
        );
    }
}
