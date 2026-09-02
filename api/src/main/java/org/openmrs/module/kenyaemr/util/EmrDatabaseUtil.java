/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.util;

import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
public class EmrDatabaseUtil {

    private static final Logger log = LoggerFactory.getLogger(EmrDatabaseUtil.class);

    private static final String DEFAULT_SCHEMA = "openmrs";
    private static final String PREFIX = "openmrs_";
    private static String cachedSchema;
    public static String getCurrentOpenmrsSchema() {
        if (cachedSchema != null) {
            return cachedSchema;
        }

        try {
            Properties props = Context.getRuntimeProperties();
            String connectionUrl = props.getProperty("connection.url");

            if (connectionUrl != null && connectionUrl.contains("/")) {
                String schema = connectionUrl.substring(connectionUrl.lastIndexOf("/") + 1);
                if (schema.contains("?")) {
                    schema = schema.substring(0, schema.indexOf("?"));
                }

                if (schema != null && !schema.trim().isEmpty()) {
                    cachedSchema = schema.trim();
                    log.info("KenyaEMR detected schema from runtime properties: " + cachedSchema);
                    return cachedSchema;
                }
            }
        } catch (Exception e) {
            log.warn("Unable to parse schema from connection URL, falling back to default", e);
        }

        cachedSchema = DEFAULT_SCHEMA;
        return cachedSchema;
    }

    public static String getTenantSuffix() {
        String schema = getCurrentOpenmrsSchema();

        if (DEFAULT_SCHEMA.equals(schema)) {
            return "";
        }

        if (schema.startsWith(PREFIX)) {
            return schema.substring(PREFIX.length());
        }

        return "";
    }

    public static String getEtlDatabaseName() {
        String suffix = getTenantSuffix();
        String etlSchema = suffix.isEmpty() ? "kenyaemr_etl" : "kenyaemr_etl_" + suffix;
        System.out.println("[KenyaEMR] Active ETL Schema: " + etlSchema);
        return etlSchema;
    }

    public static String getDataToolsSchema() {
        String suffix = getTenantSuffix();
        return suffix.isEmpty() ? "kenyaemr_datatools" : "kenyaemr_datatools_" + suffix;
    }
}