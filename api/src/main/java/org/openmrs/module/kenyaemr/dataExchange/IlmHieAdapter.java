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
 * Parses the DHA HIE middleware facility search response (JSON array).
 * Endpoint: GET /api/v1/facilities/search?identifier=&identifier-type=registration-number
 */
public class IlmHieAdapter implements FacilityStatusAdapter {

    @Override
    public Map<String, String> parse(String responseBody) {
        Map<String, String> statusMap = defaultMap();
        try {
            JSONArray array = new JSONArray(responseBody);
            if (array.length() == 0) return statusMap;

            JSONObject f = array.getJSONObject(0);

            statusMap.put("registrationNumber", f.optString("registrationNumber", "--"));
            statusMap.put("officialName", f.optString("officialName", "--"));
            statusMap.put("facilityType", f.optString("facilityType", "--"));
            statusMap.put("facilityOwnership", f.optString("facilityOwnership", "--"));
            statusMap.put("kephLevel", f.optString("kephLevel", "--"));
            statusMap.put("isHub", String.valueOf(f.optBoolean("isHub", false)));
            statusMap.put("fidCode", f.optString("fidCode", "--"));
            statusMap.put("facilityRegistryCode", f.optString("frCode", "--"));
            statusMap.put("facilityLicenseStatus", f.optString("facilityLicenseStatus", "--"));
            statusMap.put("shaFacilityLicenseNumber", f.optString("licenseNumber", "--"));
            statusMap.put("facilityLicenseStartDate", f.optString("facilityLicenseStartDate", "--"));
            statusMap.put("shaFacilityExpiryDate", f.optString("facilityLicenseEndDate", "--"));
            statusMap.put("regulatoryBody", f.optString("regulatoryBody", "--").toUpperCase());
            statusMap.put("shaContractStatus", f.optString("shaContractStatus", "--"));
            statusMap.put("shaContractStartDate", f.optString("shaConstractStartDate", "--"));
            statusMap.put("shaContractEndDate", f.optString("shaConstractEndDate", "--"));
            JSONObject shaOps = f.optJSONObject("SHAOperationStatus");
            JSONObject regOps = f.optJSONObject("regulatoryOperationalStatus");
            String ops = "--";
            if (shaOps != null && !shaOps.optString("operationalStatus", "").isEmpty()) {
                ops = shaOps.optString("operationalStatus", "--");
            } else if (regOps != null) {
                ops = regOps.optString("operationalStatus", "--");
            }
            statusMap.put("operationalStatus", ops);
            statusMap.put("facilityPhoneNumber", f.optString("facilityPhoneNumber", "--"));
            statusMap.put("facilityEmail", f.optString("facilityEmail", "--"));
            statusMap.put("facilityAdministratorName", f.optString("facilityAdministratorName", "--"));
            statusMap.put("facilityAdministratorPhone", f.optString("facilityAdministratorPhone", "--"));
            statusMap.put("facilityAdministratorEmail", f.optString("facilityAdministratorEmail", "--"));
            JSONObject address = f.optJSONObject("address");
            if (address != null) {
                statusMap.put("county", address.optString("county", "--"));
                statusMap.put("subCounty", address.optString("subCounty", "--"));
                statusMap.put("town", address.optString("town", "--"));
                statusMap.put("physicalLocation", address.optString("physicalLocation", "--"));
                statusMap.put("postalAddress", address.optString("postalAddress", "--"));
                statusMap.put("latitude", address.optString("latitude", "--"));
                statusMap.put("longitude", address.optString("longitude", "--"));
            }
            JSONObject beds = f.optJSONObject("bedOccupancy");
            if (beds != null) {
                statusMap.put("totalBeds", String.valueOf(beds.optInt("totalBeds", 0)));
                statusMap.put("normalBeds", String.valueOf(beds.optInt("normalBeds", 0)));
                statusMap.put("icuBeds", String.valueOf(beds.optInt("icuBeds", 0)));
                statusMap.put("hduBeds", String.valueOf(beds.optInt("hduBeds", 0)));
                statusMap.put("dialysisBeds", String.valueOf(beds.optInt("dialysisBeds", 0)));
            }
            JSONArray services = f.optJSONArray("shaContractedServices");
            statusMap.put("shaContractedServices", services != null ? services.toString() : "[]");
            statusMap.put("mflCode", "--");
            statusMap.put("approved", f.optString("facilityLicenseStatus", "--"));
            statusMap.put("shaFacilityId", f.optString("fidCode", "--"));

        } catch (Exception e) {
            System.err.println("IlmHieAdapter parse error: " + e.getMessage());
            e.printStackTrace();
        }
        return statusMap;
    }

    private Map<String, String> defaultMap() {
        Map<String, String> map = new HashMap<>();
        String[] keys = {
            "operationalStatus", "kephLevel", "approved", "shaFacilityExpiryDate",
            "shaFacilityId", "shaFacilityLicenseNumber", "mflCode", "facilityRegistryCode",
            "registrationNumber", "officialName", "facilityType", "facilityOwnership",
            "facilityLicenseStatus", "facilityLicenseStartDate", "regulatoryBody",
            "shaContractStatus", "shaContractStartDate", "shaContractEndDate",
            "facilityPhoneNumber", "facilityEmail", "facilityAdministratorName",
            "facilityAdministratorPhone", "facilityAdministratorEmail", "isHub", "fidCode",
            "county", "subCounty", "town", "physicalLocation", "postalAddress",
            "latitude", "longitude", "totalBeds", "normalBeds", "icuBeds",
            "hduBeds", "dialysisBeds", "shaContractedServices"
        };
        for (String key : keys) map.put(key, "--");
        map.put("shaContractedServices", "[]");
        return map;
    }
}
