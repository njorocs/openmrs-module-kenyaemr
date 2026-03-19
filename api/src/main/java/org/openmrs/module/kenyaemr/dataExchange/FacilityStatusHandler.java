/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dataExchange;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.JsonNode;
import org.json.JSONObject;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.openmrs.util.OpenmrsUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import static org.openmrs.module.kenyaemr.util.EmrUtils.getGlobalPropertyValue;

public class FacilityStatusHandler extends DataHandler {

    private static final String LOCAL_FILE_PATH = OpenmrsUtil.getApplicationDataDirectory() + "/sha/sha_facility_status.json";
    private static final Logger log = LoggerFactory.getLogger(FacilityStatusHandler.class);

    private static final String BASE_URL_KEY = CommonMetadata.GP_HIE_BASE_END_POINT_URL;
    private static final String FACILITY_REGISTRATION_NUMBER = CommonMetadata.GP_SHA_FACILITY_REGISTRATION_NUMBER;

    public FacilityStatusHandler() {
        super(LOCAL_FILE_PATH);
    }

    @Override
    protected JsonNode fetchRemoteData() {
        try {
            String response = extractFacilityStatus().getBody();
            if (response != null) {
                return objectMapper.readTree(response);
            }
        } catch (Exception e) {
            log.error("Error fetching remote facility data", e);
        }
        return null;
    }

    /**
     * Calls the DHA HIE middleware facility search endpoint.
     * Base URL GP: kenyaemr.sha.facilityregistry.get.api
     * Auth: mediator client_credentials via kenyaemr.sha.jwt.auth.mode = mediator
     */
    public static ResponseEntity<String> getFacilityStatus() throws IOException {
        String bearerToken = DataHandler.getBearerToken();
        if (bearerToken == null || bearerToken.isEmpty()) {
            log.error("Bearer token is missing");
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": \"Error\", \"message\": \"Bearer token is missing\"}");
        }

        String facilityRegistrationNumber = getGlobalPropertyValue(FACILITY_REGISTRATION_NUMBER).trim();
        if (facilityRegistrationNumber.isEmpty()) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": \"Error\", \"message\": \"Facility registration number is missing\"}");
        }

        String baseUrl = getGlobalPropertyValue(BASE_URL_KEY).trim();
        String url = baseUrl + "/api/v1/facilities/search"
                + "?identifier=" + facilityRegistrationNumber
                + "&identifier-type=registration-number";

        log.info("Fetching facility status from: {}", url);

        try {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(createSslConnectionFactory())
                    .build();
            HttpGet getRequest = new HttpGet(url);
            getRequest.setHeader("Authorization", "Bearer " + bearerToken);

            HttpResponse response = httpClient.execute(getRequest);
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(createSuccessResponse(response));
            } else {
                log.error("HIE facility registry returned HTTP {}", responseCode);
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\": \"Error\", \"code\": " + responseCode + "}");
            }
        } catch (Exception ex) {
            log.error("Error fetching facility status from HIE middleware", ex);
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\": \"Error\", \"message\": \"" + ex.getMessage() + "\"}");
        }
    }

    /**
     * Fetches facility status and routes to the correct adapter based on response shape:
     *   "[" → IlmHieAdapter          (new DHA HIE middleware — JSON array)
     *   "{" → FhirOrganizationAdapter (legacy FHIR bundle — JSON object)
     */
    private static ResponseEntity<String> extractFacilityStatus() throws IOException {
        ResponseEntity<String> apiResponse = getFacilityStatus();
        String body = apiResponse.getBody();

        if (body == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Empty response from facility registry\"}");
        }

        FacilityStatusAdapter adapter;
        if (body.trim().startsWith("[")) {
            log.info("Routing to IlmHieAdapter (JSON array response)");
            adapter = new IlmHieAdapter();
        } else {
            log.info("Routing to FhirOrganizationAdapter (FHIR bundle response)");
            adapter = new FhirOrganizationAdapter();
        }

        Map<String, String> statusMap = adapter.parse(body);

        // Persist facilityRegistryCode (frCode) as GP
        String frCode = statusMap.get("facilityRegistryCode");
        if (frCode != null && !frCode.equals("--") && !frCode.isEmpty()) {
            try {
                GlobalProperty facilityRegistryCodeGP = Context.getAdministrationService()
                        .getGlobalPropertyObject(CommonMetadata.GP_SHA_FACILITY_REGISTRY_CODE);
                if (facilityRegistryCodeGP != null) {
                    facilityRegistryCodeGP.setPropertyValue(frCode);
                    Context.getAdministrationService().saveGlobalProperty(facilityRegistryCodeGP);
                    log.info("Persisted facilityRegistryCode GP: {}", frCode);
                }
            } catch (Exception e) {
                log.error("Failed to persist facilityRegistryCode GP", e);
            }
        }

        return ResponseEntity.ok(new JSONObject(statusMap).toString());
    }
}
