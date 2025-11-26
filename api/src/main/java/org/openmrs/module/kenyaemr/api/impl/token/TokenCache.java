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

public class TokenCache {

    private static volatile String accessToken;
    private static volatile String refreshToken;
    private static volatile String tokenType;
    private static volatile long expiryEpochMs;

    public static synchronized void store(String at, String rt, String tt, long expiry) {
        accessToken = at;
        refreshToken = rt;
        tokenType = tt;
        expiryEpochMs = expiry;
    }

    public static synchronized String getAccessToken() {
        return accessToken;
    }

    public static synchronized String getRefreshToken() {
        return refreshToken;
    }

    public static synchronized String getTokenType() {
        return tokenType == null ? "Bearer" : tokenType;
    }

    public static synchronized long getExpiryEpochMs() {
        return expiryEpochMs;
    }

    public static synchronized boolean isValid() {
        return accessToken != null && System.currentTimeMillis() < expiryEpochMs;
    }

    public static synchronized void clear() {
        accessToken = null;
        refreshToken = null;
        tokenType = null;
        expiryEpochMs = 0;
    }
}