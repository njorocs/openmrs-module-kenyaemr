/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
/**
 * @author steve
 * @created 02/03/2026
 */

package org.openmrs.module.kenyaemr.reporting.air;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for ADX dataset mappings
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdxConfiguration {
    @JsonProperty("reportName")
    private String reportName;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("datasets")
    private List<AdxDatasetMapping> datasets;

    @JsonProperty("customNamespace")
    private String customNamespace;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    public AdxConfiguration() {
        this.datasets = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public List<AdxDatasetMapping> getDatasets() { return datasets; }
    public void setDatasets(List<AdxDatasetMapping> datasets) { this.datasets = datasets; }

    public String getCustomNamespace() { return customNamespace; }
    public void setCustomNamespace(String customNamespace) { this.customNamespace = customNamespace; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdxDatasetMapping {

        @JsonProperty("name")
        private String name;

        @JsonProperty("dhisName")
        private String dhisName;

        @JsonProperty("threePmName")
        private String threePmName;

        @JsonProperty("fieldMappings")
        private Map<String, String> fieldMappings;

        @JsonProperty("excludeColumns")
        private List<String> excludeColumns;

        @JsonProperty("transformations")
        private Map<String, String> transformations;

        public AdxDatasetMapping() {
            this.fieldMappings = new HashMap<>();
            this.excludeColumns = new ArrayList<>();
            this.transformations = new HashMap<>();
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDhisName() { return dhisName; }
        public void setDhisName(String dhisName) { this.dhisName = dhisName; }

        public String get3pmName() { return threePmName; }
        public void set3pmName(String threePmName) { this.threePmName = threePmName; }

        public Map<String, String> getFieldMappings() { return fieldMappings; }
        public void setFieldMappings(Map<String, String> fieldMappings) { this.fieldMappings = fieldMappings; }

        public List<String> getExcludeColumns() { return excludeColumns; }
        public void setExcludeColumns(List<String> excludeColumns) { this.excludeColumns = excludeColumns; }

        public Map<String, String> getTransformations() { return transformations; }
        public void setTransformations(Map<String, String> transformations) { this.transformations = transformations; }
    }
}