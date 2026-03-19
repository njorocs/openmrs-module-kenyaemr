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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses the legacy FHIR Organization bundle response.
 * Retained as fallback in case the upstream reverts to FHIR format.
 */
public class FhirOrganizationAdapter implements FacilityStatusAdapter {

    @Override
    public Map<String, String> parse(String responseBody) {
        Map<String, String> statusMap = defaultMap();
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray entries = jsonResponse.optJSONArray("entry");
            if (entries == null || entries.isEmpty()) return statusMap;

            JSONObject resource = entries.getJSONObject(0).optJSONObject("resource");
            if (resource == null || !"Organization".equals(resource.optString("resourceType")))
                return statusMap;

            statusMap.put("shaFacilityId", resource.optString("id", "--"));

            // Extensions
            JSONArray extensions = resource.optJSONArray("extension");
            if (extensions != null) {
                for (int i = 0; i < extensions.length(); i++) {
                    JSONObject ext = extensions.optJSONObject(i);
                    String url = ext.optString("url");
                    switch (url) {
                        case "https://fr-kenyahie/StructureDefinition/license-status":
                            statusMap.put("operationalStatus", ext.optString("valueString", "--"));
                            break;
                        case "https://fr-kenyahie/StructureDefinition/keph-level":
                            JSONObject vcc = ext.optJSONObject("valueCodeableConcept");
                            if (vcc != null) {
                                JSONArray coding = vcc.optJSONArray("coding");
                                if (coding != null && !coding.isEmpty())
                                    statusMap.put("kephLevel", coding.getJSONObject(0).optString("display", "--"));
                            }
                            break;
                        case "https://shr.tiberbuapps.com/fhir/StructureDefinition/approved":
                            JSONObject vc = ext.optJSONObject("valueCoding");
                            if (vc != null) statusMap.put("approved", vc.optString("display", "--"));
                            break;
                    }
                }
            }

            // Identifiers
            JSONArray identifiers = resource.optJSONArray("identifier");
            if (identifiers != null) {
                for (int i = 0; i < identifiers.length(); i++) {
                    JSONObject identifier = identifiers.getJSONObject(i);
                    JSONObject type = identifier.optJSONObject("type");
                    if (type == null) continue;
                    JSONArray codingArray = type.optJSONArray("coding");
                    if (codingArray != null) {
                        for (int j = 0; j < codingArray.length(); j++) {
                            String code = codingArray.getJSONObject(j).optString("code");
                            switch (code) {
                                case "license-number":
                                    statusMap.put("shaFacilityLicenseNumber", identifier.optString("value", "--"));
                                    break;
                                case "mfl-code":
                                    statusMap.put("mflCode", identifier.optString("value", "--"));
                                    break;
                                case "fid":
                                    statusMap.put("shaFacilityId", identifier.optString("value", "--"));
                                    break;
                                case "fr-code":
                                    statusMap.put("facilityRegistryCode", identifier.optString("value", "--"));
                                    break;
                                case "registration-number":
                                    statusMap.put("registrationNumber", identifier.optString("value", "--"));
                                    break;
                            }
                        }
                    }
                    JSONObject period = identifier.optJSONObject("period");
                    if (period != null)
                        statusMap.put("shaFacilityExpiryDate", period.optString("end", "--"));
                }
            }
        } catch (Exception e) {
            System.err.println("FhirOrganizationAdapter parse error: " + e.getMessage());
            e.printStackTrace();
        }
        return statusMap;
    }

    private Map<String, String> defaultMap() {
        Map<String, String> map = new HashMap<>();
        map.put("operationalStatus", "--");
        map.put("kephLevel", "--");
        map.put("approved", "--");
        map.put("shaFacilityExpiryDate", "--");
        map.put("shaFacilityId", "--");
        map.put("shaFacilityLicenseNumber", "--");
        map.put("mflCode", "--");
        map.put("facilityRegistryCode", "--");
        map.put("registrationNumber", "--");
        return map;
    }
}
