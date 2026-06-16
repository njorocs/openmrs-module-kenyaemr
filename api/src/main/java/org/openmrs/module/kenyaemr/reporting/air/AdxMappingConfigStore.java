/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.air;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source of truth for the ADX dataset/field mapping configuration.
 *
 * <p>The mappings used to be stored in the {@code kenyaemr.adxDatasetMapping} and
 * {@code kenyakeypop.adx3pmDatasetMapping} global properties, but the DHIS2 mapping grew large
 * enough to exceed the {@code global_property.property_value} column size. The configuration is
 * therefore kept as JSON files on disk, under the OpenMRS application data directory, where it is
 * not bound by any DB column limit and can be edited by an administrator.</p>
 *
 * <p>Reads are served from a static in-memory cache keyed by file path. The parsed configuration is
 * only re-read and re-parsed when the file's last-modified timestamp changes, so repeated lookups
 * are cheap and edits to the file are picked up without restarting OpenMRS.</p>
 *
 * <p>On first use, a missing file is seeded from (1) the legacy global property if it still holds a
 * non-empty value (migration for existing deployments), otherwise (2) a default JSON resource
 * bundled in the module.</p>
 */
public final class AdxMappingConfigStore {

    private static final Log log = LogFactory.getLog(AdxMappingConfigStore.class);

    private static final String ADX_DIR = OpenmrsUtil.getApplicationDataDirectory()
            + File.separator + "kenyaemr" + File.separator + "khis";

    private static final String DHIS2_FILE = ADX_DIR + File.separator + "dhis2_dataset_mapping.json";
    private static final String THREE_PM_FILE = ADX_DIR + File.separator + "3pm_dataset_mapping.json";

    private static final String DHIS2_DEFAULT_RESOURCE = "khis/khis_dataset_mapping.default.json";
    private static final String THREE_PM_DEFAULT_RESOURCE = "khis/3pm_dataset_mapping.default.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<String, Cached>();

    private AdxMappingConfigStore() {
    }

    /**
     * @return the DHIS2 ADX configurations (never null; empty list when nothing is configured)
     */
    public static List<AdxConfiguration> getDhis2Configurations() {
        return getConfigurations(DHIS2_FILE, DHIS2_DEFAULT_RESOURCE, EmrConstants.GP_DHIS2_DATASET_MAPPING);
    }

    /**
     * @return the 3PM ADX configurations (never null; empty list when nothing is configured)
     */
    public static List<AdxConfiguration> getThreePmConfigurations() {
        return getConfigurations(THREE_PM_FILE, THREE_PM_DEFAULT_RESOURCE, EmrConstants.GP_3PM_DATASET_MAPPING);
    }

    private static List<AdxConfiguration> getConfigurations(String filePath, String defaultResource, String legacyGpKey) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                seedFile(file, defaultResource, legacyGpKey);
            }
            if (!file.exists()) {
                return Collections.emptyList();
            }

            long lastModified = file.lastModified();
            Cached cached = CACHE.get(filePath);
            if (cached != null && cached.lastModified == lastModified) {
                return cached.configurations;
            }

            List<AdxConfiguration> configurations = parse(objectMapper.readTree(file));
            CACHE.put(filePath, new Cached(lastModified, configurations));
            return configurations;
        } catch (Exception e) {
            log.error("Error loading ADX mapping config from " + filePath, e);
            Cached cached = CACHE.get(filePath);
            return cached != null ? cached.configurations : Collections.<AdxConfiguration>emptyList();
        }
    }

    private static List<AdxConfiguration> parse(JsonNode rootNode) {
        List<AdxConfiguration> configurations = new ArrayList<AdxConfiguration>();
        if (rootNode != null && rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                try {
                    configurations.add(objectMapper.treeToValue(node, AdxConfiguration.class));
                } catch (Exception e) {
                    log.error("Skipping invalid ADX configuration entry: " + node, e);
                }
            }
        } else {
            log.warn("ADX configuration is not a JSON array; treating as empty");
        }
        return configurations;
    }

    private static synchronized void seedFile(File file, String defaultResource, String legacyGpKey) {
        if (file.exists()) {
            return;
        }
        ensureDirectoryExists(file);
        try {
            JsonNode seed = readSeedNode(defaultResource, legacyGpKey);
            if (seed == null) {
                return;
            }
            objectMapper.writeValue(file, seed);
            log.info("Seeded ADX mapping file: " + file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to seed ADX mapping file: " + file.getAbsolutePath(), e);
        }
    }

    private static JsonNode readSeedNode(String defaultResource, String legacyGpKey) {
        // 1. Migrate from the legacy global property if it still holds a usable value.
        String gpValue = readLegacyGlobalProperty(legacyGpKey);
        if (StringUtils.isNotBlank(gpValue)) {
            try {
                return objectMapper.readTree(gpValue);
            } catch (Exception e) {
                log.warn("Legacy global property '" + legacyGpKey
                        + "' is not valid JSON; falling back to bundled default", e);
            }
        }

        // 2. Fall back to the default resource bundled in the module.
        InputStream in = AdxMappingConfigStore.class.getClassLoader().getResourceAsStream(defaultResource);
        if (in == null) {
            log.error("Bundled ADX default resource not found: " + defaultResource);
            return null;
        }
        try {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            log.error("Failed to read bundled ADX default resource: " + defaultResource, e);
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String readLegacyGlobalProperty(String key) {
        try {
            String value = Context.getAdministrationService().getGlobalProperty(key);
            return value != null ? value.trim() : null;
        } catch (Exception e) {
            log.warn("Could not read legacy global property: " + key, e);
            return null;
        }
    }

    private static void ensureDirectoryExists(File file) {
        File dir = file.getParentFile();
        if (dir == null || dir.exists()) {
            return;
        }
        if (dir.mkdirs()) {
            try {
                Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (Exception e) {
                log.warn("Failed to set permissions on directory: " + dir.getAbsolutePath(), e);
            }
        } else {
            log.error("Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    private static final class Cached {

        private final long lastModified;

        private final List<AdxConfiguration> configurations;

        private Cached(long lastModified, List<AdxConfiguration> configurations) {
            this.lastModified = lastModified;
            this.configurations = configurations;
        }
    }
}
