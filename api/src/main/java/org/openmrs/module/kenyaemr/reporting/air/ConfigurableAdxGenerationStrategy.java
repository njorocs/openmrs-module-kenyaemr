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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.facilityreporting.api.FacilityreportingService;
import org.openmrs.module.facilityreporting.api.models.FacilityReportDataset;
import org.openmrs.module.facilityreporting.api.restUtil.DatasetIndicatorDetails;
import org.openmrs.module.facilityreporting.api.restUtil.FacilityReporting;
import org.openmrs.module.facilityreporting.api.restUtil.ReportDatasetValueEntryMapper;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.wrapper.Facility;
import org.openmrs.module.reporting.config.ReportDescriptor;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.ReportData;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Configurable ADX generation strategy that reads configuration from global properties
 */
    @Component
    public class ConfigurableAdxGenerationStrategy implements AdxGenerationStrategy {
    private static final Log log = LogFactory.getLog(ConfigurableAdxGenerationStrategy.class);

    private static final String MOH_731_REPORT_NAME = "Revised MOH 731";
    private static final String MONTHLY_REPORT_NAME = "Monthly report";
    private static final String MOH_743_REPORT_NAME = "MOH-743 Report";
    private static final String MOH_711_REPORT_NAME = "MOH 711";

    private final DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    private final DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canHandle(ReportDescriptor reportDescriptor) {
        try {
            AdxConfiguration config = getConfigurationForReport(reportDescriptor.getName());
            return config != null;
        } catch (Exception e) {
            log.error("Error checking if strategy can handle report: " + reportDescriptor.getName(), e);
            return false;
        }
    }

    @Override
    public String getDataSetIdentifier(ReportData reportData) {
        try {
            AdxConfiguration config = getConfigurationForReport(reportData.getDefinition().getName());
            if (config != null && StringUtils.isNotBlank(config.getPrefix())) {
                return config.getPrefix() + reportData.getDefinition().getName().replace(" ", "_");
            }
            return reportData.getDefinition().getName().replace(" ", "_");
        } catch (Exception e) {
            log.error("Error getting dataset identifier for report: " + reportData.getDefinition().getName(), e);
            return reportData.getDefinition().getName().replace(" ", "_");
        }
    }

    @Override
    public AdxMetadata getAdxMetadata(ReportData reportData) {
        try {
            String reportName = reportData.getDefinition().getName();

            AdxConfiguration config = getConfigurationForReport(reportName);

            AdministrationService adminService = Context.getAdministrationService();
            LocationService locationService = Context.getLocationService();

            String defaultLocationId = adminService.getGlobalProperty(EmrConstants.GP_DEFAULT_LOCATION);
            String mflCode = getMflCode(adminService, locationService, defaultLocationId);

            // Build endpoint URL dynamically based on field mappings
            String endpointUrl = buildEndpointUrl(config, reportName);

            AdxMetadata metadata = new AdxMetadata();
            metadata.setOrgUnit(mflCode);
            metadata.setEndpointUrl(endpointUrl);

            if (config != null) {
                metadata.setReportName(config.getReportName());
                metadata.setPrefix(config.getPrefix());
                metadata.setCustomNamespace(config.getCustomNamespace());
                boolean hasFieldMappings = hasFieldMappings(config);
                metadata.setHasFieldMappings(hasFieldMappings);

            } else {
                metadata.setHasFieldMappings(false);
            }

            return metadata;

        } catch (Exception e) {
            log.error("Error generating ADX metadata for report: " +
                    (reportData.getDefinition() != null ? reportData.getDefinition().getName() : "Unknown"), e);

            // Return minimal metadata with fallback endpoint to prevent complete failure
            AdxMetadata metadata = new AdxMetadata();
            metadata.setOrgUnit("Unknown");
            String fallbackEndpoint = getBaseUrl() + "/api/dataValueSets?dataElementIdScheme=code&orgUnitIdScheme=code&dataSetIdScheme=uid";
            metadata.setEndpointUrl(fallbackEndpoint);
            metadata.setHasFieldMappings(false);

          //  log.warn("Using fallback ADX metadata - EndpointURL: '{}'", fallbackEndpoint);
            return metadata;
        }
    }

    @Override
    public void writeDataElements(Writer writer, ReportData reportData) throws IOException {
        try {
            Date reportDate = (Date) reportData.getContext().getParameterValue("startDate");
            Date endDate = (Date) reportData.getContext().getParameterValue("endDate");
            String reportName = reportData.getDefinition().getName();

            AdxMetadata metadata = getAdxMetadata(reportData);
            AdxConfiguration config = getConfigurationForReport(reportName);

            if (config == null) {
                log.warn("No configuration found for report: " + reportName + ", using default behavior");
            }

            if (reportDate == null) {
                reportDate = new Date();
            }
            if (endDate == null) {
                endDate = new Date();
            }

            // Write regular report datasets first
            for (String dsKey : reportData.getDataSets().keySet()) {
                AdxConfiguration.AdxDatasetMapping datasetMapping = findDatasetMapping(config, dsKey);
                String datasetName = getDatasetName(datasetMapping, dsKey, reportName);

                if (StringUtils.isBlank(datasetName)) {
                    log.warn("Skipping dataset "+dsKey+" because no ADX dataset name could be resolved for report " +reportName);
                    continue;
                }

                writer.write("<group orgUnit=\"" + escapeXml(metadata.getOrgUnit()) +
                        "\" period=\"" + isoDateFormat.format(reportDate) + "/P1M\"" +
                        " completeDate=\"" + isoDateFormat.format(new Date()) + "\"" +
                        " dataSet=\"" + escapeXml(datasetName) + "\">\n");

                writeDataSet(writer, reportData.getDataSets().get(dsKey),
                        datasetMapping, reportData, config);

                writer.write("</group>\n");
            }

            // Write facility reporting data if configured - THIS IS THE CRITICAL ADDITION
            if (config != null && reportDate != null && endDate != null) {
                writeFacilityReportingDataForReport(writer, reportDate, endDate, metadata, config, reportName);
            } else {
                log.debug("Skipping facility reporting data - missing configuration or dates");
            }

        } catch (Exception e) {
            log.error("Error writing ADX data elements for report: " +
                    (reportData.getDefinition() != null ? reportData.getDefinition().getName() : "Unknown"), e);
            throw new IOException("Error writing ADX data elements: " + e.getMessage(), e);
        }
    }
    private void writeFacilityReportingData(Writer writer, Date reportDate, Date endDate,
                                            AdxMetadata metadata, AdxConfiguration config) throws IOException {

        log.warn("Using deprecated writeFacilityReportingData method. Consider updating to use writeFacilityReportingDataForReport.");

        // For backward compatibility, try to determine report name from metadata or config
        String reportName = "MOH 731"; // Default fallback
        if (config != null && config.getReportName() != null) {
            reportName = config.getReportName();
        }

        // Delegate to the new configurable method
        writeFacilityReportingDataForReport(writer, reportDate, endDate, metadata, config, reportName);
    }
    @Override
    public String getCustomNamespace() {
        return "urn:ihe:qrph:adx:2015";
    }

    /**
     * Writes facility reporting data for reports that have DOM datasets configured with proper error handling
     */
    private void writeFacilityReportingDataForReport(Writer writer, Date reportDate, Date endDate,
                                                     AdxMetadata metadata, AdxConfiguration config, String reportName) throws IOException {
        if (writer == null) {
            throw new IOException("Writer cannot be null");
        }

        if (reportDate == null || endDate == null) {
            throw new IOException("Report dates cannot be null");
        }

        if (metadata == null) {
            throw new IOException("Metadata cannot be null");
        }

        if (config == null) {
            log.error("Configuration is null, cannot write facility reporting data for report: " + reportName);
            return;
        }

        if (reportName == null || reportName.trim().isEmpty()) {
            log.error("Report name is null or empty, cannot write facility reporting data");
            return;
        }

        try {
            // Get facility report ID from configuration
            Integer facilityReportId = getFacilityReportIdForReport(reportName);
            if (facilityReportId == null) {
                log.info("No facility reporting configured for report: " + reportName);
                return;
            }

            log.info("Processing facility reporting data for report: " + reportName + " with facility ID: " + facilityReportId);

            // Get facility reporting service
            FacilityreportingService facilityreportingService = Context.getService(FacilityreportingService.class);
            if (facilityreportingService == null) {
                log.error("FacilityreportingService is not available");
                return;
            }

            // Fetch facility data for the specified period
            String startDateStr = isoDateFormat.format(reportDate);
            String endDateStr = isoDateFormat.format(endDate);

            List<ReportDatasetValueEntryMapper> facilityData = getFacilityReportData(
                    facilityReportId, startDateStr, endDateStr);

            log.info("Retrieved " + (facilityData != null ? facilityData.size() : 0) +
                    " facility data entries for " + reportName);

            if (facilityData == null || facilityData.isEmpty()) {
                log.info("No facility data found for report: " + reportName + " between " +
                        startDateStr + " and " + endDateStr);
                return;
            }

            // Process each facility dataset entry
            int processedDatasets = 0;
            int totalIndicators = 0;

            for (ReportDatasetValueEntryMapper entry : facilityData) {
                if (entry == null) {
                    log.warn("Encountered null facility data entry, skipping");
                    continue;
                }

                if (entry.getDatasetID() == null || entry.getDatasetID().trim().isEmpty()) {
                    log.warn("Facility data entry has null or empty dataset ID, skipping");
                    continue;
                }

                try {
                    Integer datasetId = Integer.parseInt(entry.getDatasetID().trim());
                    FacilityReportDataset dataset = facilityreportingService.getDatasetById(datasetId);

                    if (dataset == null) {
                        log.warn("Could not find facility dataset with ID: " + datasetId);
                        continue;
                    }

                    String datasetMapping = dataset.getMapping();
                    if (datasetMapping == null || datasetMapping.trim().isEmpty()) {
                        log.warn("Dataset mapping is null or empty for dataset ID: " + datasetId);
                        continue;
                    }

                    log.debug("Processing facility dataset: " + datasetId + " with mapping: " + datasetMapping);

                    // Check if this dataset should be included for the current report
                    if (!shouldIncludeFacilityDataset(config, datasetMapping, reportName)) {
                        log.debug("Skipping facility dataset " + datasetMapping +
                                " - not configured for inclusion in " + reportName);
                        continue;
                    }

                    // Get dataset name for ADX output
                    String adxDatasetName = getDatasetNameForFacilityData(config, datasetMapping, reportName);

                    // Write group start for facility data
                    writer.write("<group orgUnit=\"" + escapeXml(metadata.getOrgUnit()) +
                            "\" period=\"" + startDateStr + "/P1M\"");

                    // Add completeDate if configured
                    String includeCompleteDate = config.getMetadata().get("includeCompleteDate");
                    if (!"false".equalsIgnoreCase(includeCompleteDate)) {
                        writer.write(" completeDate=\"" + isoDateTimeFormat.format(new Date()) + "\"");
                    }

                    writer.write(" dataSet=\"" + escapeXml(adxDatasetName) + "\">\n");

                    // Write data values for this facility dataset
                    int indicatorCount = 0;
                    if (entry.getIndicators() != null && !entry.getIndicators().isEmpty()) {
                        for (DatasetIndicatorDetails indicator : entry.getIndicators()) {
                            if (indicator == null) {
                                log.debug("Encountered null indicator, skipping");
                                continue;
                            }

                            if (!isValidIndicatorValue(indicator.getValue())) {
                                log.debug("Invalid indicator value for " + indicator.getName() +
                                        ": " + indicator.getValue());
                                continue;
                            }

                            try {
                                // Apply any configured transformations to the indicator name
                                String indicatorName = indicator.getName();
                                if (indicatorName == null || indicatorName.trim().isEmpty()) {
                                    log.debug("Indicator name is null or empty, skipping");
                                    continue;
                                }

                                String elementName = buildElementName(config, indicatorName.trim());
                                String value = transformIndicatorValue(config, indicator.getValue());

                                writer.write("<dataValue dataElement=\"" + escapeXml(elementName) +
                                        "\" value=\"" + escapeXml(value) + "\"/>\n");
                                indicatorCount++;
                                totalIndicators++;

                            } catch (Exception e) {
                                log.error("Error processing indicator " + indicator.getName() +
                                        " for dataset " + datasetMapping, e);
                                // Continue with next indicator rather than failing the entire dataset
                            }
                        }
                    } else {
                        log.debug("No indicators found for facility dataset: " + datasetMapping);
                    }

                    writer.write("</group>\n");
                    processedDatasets++;

                    log.debug("Successfully wrote facility dataset " + datasetMapping +
                            " with " + indicatorCount + " indicators");

                } catch (NumberFormatException e) {
                    log.error("Invalid dataset ID format: " + entry.getDatasetID(), e);
                    continue;
                } catch (Exception e) {
                    log.error("Error processing facility dataset entry with ID: " + entry.getDatasetID(), e);
                    continue;
                }
            }

            log.info("Successfully processed " + processedDatasets + " facility datasets with " +
                    totalIndicators + " total indicators for " + reportName + " report");

        } catch (Exception e) {
            log.error("Error writing facility reporting data for " + reportName, e);
            throw new IOException("Error processing facility reporting data for " + reportName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Determines facility report ID based on report name
     */
    private Integer getFacilityReportIdForReport(String reportName) {
        if (reportName == null || reportName.trim().isEmpty()) {
            log.error("Report name cannot be null or empty");
            return null;
        }
        try {
            AdxConfiguration config = getConfigurationForReport(reportName);
            if (config == null) {
                log.warn("No configuration found for report: " + reportName);
                return null;
            }

            // Check if facility ID is configured in metadata
            String facilityIdStr = config.getMetadata().get("facilityReportId");
            if (facilityIdStr != null && !facilityIdStr.trim().isEmpty()) {
                try {
                    return Integer.valueOf(facilityIdStr.trim());
                } catch (NumberFormatException e) {
                    log.error("Invalid facility report ID format in configuration for report '" + reportName + "': " + facilityIdStr, e);
                    return null;
                }
            }

            // Fallback: determine based on report name pattern matching
            String normalizedReportName = reportName.toLowerCase().trim();

            if (normalizedReportName.contains("731") || normalizedReportName.contains("revised moh 731")) {
                return 1; // MOH 731 facility ID
            } else if (normalizedReportName.contains("711") || normalizedReportName.contains("moh 711")) {
                return 2; // MOH 711 facility ID
            } else if (normalizedReportName.contains("743") || normalizedReportName.contains("moh-743")) {
                // Add appropriate facility ID for MOH 743 if needed
                log.warn("No specific facility ID configured for MOH 743 report, using default");
                return null;
            } else {
                log.warn("Unknown report type, cannot determine facility report ID: " + reportName);
                return null;
            }

        } catch (Exception e) {
            log.error("Error determining facility report ID for report: " + reportName, e);
            return null;
        }
    }

    /**
     * Enhanced logic for determining if facility dataset should be included with proper error handling
     */
    private boolean shouldIncludeFacilityDataset(AdxConfiguration config, String datasetMapping, String reportName) {
        try {
            if (config == null) {
                log.warn("Configuration is null, cannot determine if facility dataset should be included");
                return false;
            }

            if (reportName == null || reportName.trim().isEmpty()) {
                log.warn("Report name is null or empty, cannot determine if facility dataset should be included");
                return false;
            }

            // Check configuration metadata for explicit facility dataset inclusion flag
            String includeFacilityDataset = config.getMetadata().get("includeFacilityDataset");
            if ("true".equalsIgnoreCase(includeFacilityDataset)) {
                return true;
            } else if ("false".equalsIgnoreCase(includeFacilityDataset)) {
                return false;
            }

            // Check if this specific dataset mapping has field mappings
            AdxConfiguration.AdxDatasetMapping mappingConfig = findDatasetMapping(config, datasetMapping);
            if (mappingConfig != null && mappingConfig.getFieldMappings() != null && !mappingConfig.getFieldMappings().isEmpty()) {
                log.debug("Including facility dataset '{}' because it has field mappings");
                return true;
            }

            // Fallback logic based on report name
            return shouldIncludeFacilityDatasetForReport(config, datasetMapping, reportName);

        } catch (Exception e) {
            log.error("Error determining if facility dataset should be included for report: " + reportName, e);
            return false;
        }
    }

    /**
     * Gets the appropriate dataset name for facility data based on configuration
     */
    private String getDatasetNameForFacilityData(AdxConfiguration config, String datasetMapping, String reportName) {
        if (config == null || datasetMapping == null) {
            return datasetMapping != null ? datasetMapping : "Unknown";
        }

        // Check if there's a specific mapping configured for this dataset
        AdxConfiguration.AdxDatasetMapping mappingConfig = findDatasetMapping(config, datasetMapping);
        if (mappingConfig != null) {
            // Use DHIS name if available, otherwise use the original mapping
            String dhisName = mappingConfig.getDhisName();
            if (dhisName != null && !dhisName.trim().isEmpty()) {
                return dhisName.trim();
            }
        }

        // Use original dataset mapping as fallback
        return datasetMapping;
    }

    /**
     * Builds the element name with proper prefix and transformations
     */
    private String buildElementName(AdxConfiguration config, String indicatorName) {
        if (indicatorName == null || indicatorName.trim().isEmpty()) {
            return "";
        }

        String elementName = indicatorName.trim();

        // Apply prefix if configured
        String prefix = getColumnPrefix(config);
        if (prefix != null && !prefix.isEmpty() && !elementName.startsWith(prefix)) {
            elementName = prefix + elementName;
        }

        // Apply any name transformations
        String transformation = null;
        if (config != null && config.getDatasets() != null) {
            for (AdxConfiguration.AdxDatasetMapping dataset : config.getDatasets()) {
                if (dataset.getTransformations() != null && dataset.getTransformations().containsKey(indicatorName)) {
                    transformation = dataset.getTransformations().get(indicatorName);
                    break;
                }
            }
        }

        if (transformation != null && !transformation.trim().isEmpty()) {
            elementName = applyTransformation(elementName, transformation);
        }

        return elementName;
    }

    /**
     * Transforms indicator value based on configuration
     */
    private String transformIndicatorValue(AdxConfiguration config, Object value) {
        if (value == null) {
            return "";
        }

        String stringValue = value.toString().trim();

        // Apply global value transformations if configured
        if (config != null && config.getMetadata() != null) {
            String valueTransformation = config.getMetadata().get("valueTransformation");
            if (valueTransformation != null && !valueTransformation.trim().isEmpty()) {
                stringValue = applyTransformation(stringValue, valueTransformation);
            }
        }

        return stringValue;
    }
    private void writeDataSet(Writer writer, DataSet dataset, AdxConfiguration.AdxDatasetMapping datasetMapping,
                              ReportData reportData, AdxConfiguration config) throws IOException {
        List<DataSetColumn> columns = dataset.getMetaData().getColumns();
        String reportName = reportData.getDefinition().getName();
        String datasetName = dataset.getDefinition().getName();

        for (DataSetRow row : dataset) {
            for (DataSetColumn column : columns) {
                String columnName = column.getName();

                // Skip excluded columns
                if (datasetMapping != null && datasetMapping.getExcludeColumns().contains(columnName)) {
                    continue;
                }

                // Special filtering for MOH 705A - only include totals (ending with -32)
                if ("MOH 705A Outpatient summary".equals(reportName) || "MOH705A".equals(reportName)) {
                    if (!columnName.endsWith("-32")) {
                        continue; // Skip non-total columns
                    }
                }

                // Special filtering for MOH 705B - only include totals (ending with -32)
                if ("MOH 705B Outpatient summary".equals(reportName) || "MOH705B".equals(reportName)) {
                    if (!columnName.endsWith("-32")) {
                        continue; // Skip non-total columns
                    }
                }

                Object value = row.getColumnValue(column);

                // Universal zero value exclusion for ALL reports
                if (value == null || "0".equals(value.toString()) || StringUtils.isBlank(value.toString())) {
                    continue;
                }

                // Determine the mapping key to use
                String mappingKey = columnName;
                if ("MOH-743 Report".equals(reportName)) {
                    // For MOH 743, try composite key first (datasetName.columnName)
                    String compositeKey = datasetName + "." + columnName;
                    if (datasetMapping != null && datasetMapping.getFieldMappings().containsKey(compositeKey)) {
                        mappingKey = compositeKey;
                    }
                    // Otherwise fall back to direct column name mapping
                }

                // Apply field mapping if configured
                String dataElementName = mappingKey;
                String categoryOptionCombo = null;

                if (datasetMapping != null && datasetMapping.getFieldMappings().containsKey(mappingKey)) {
                    String mappedValue = datasetMapping.getFieldMappings().get(mappingKey);
                    // Check if mapping contains categoryOptionCombo (format: dataElement|categoryOptionCombo)
                    if (mappedValue.contains("|")) {
                        String[] parts = mappedValue.split("\\|");
                        dataElementName = parts[0];
                        categoryOptionCombo = parts[1];
                    } else {
                        dataElementName = mappedValue;
                    }
                }

                // Special processing for MOH 705A - remove -32 suffix from totals
                if (("MOH 705A Outpatient summary".equals(reportName) || "MOH705A".equals(reportName)) &&
                        dataElementName.endsWith("-32")) {
                    dataElementName = dataElementName.substring(0, dataElementName.length() - 3);
                }

                // Special processing for MOH 705B - remove -32 suffix from totals
                if (("MOH 705B Outpatient summary".equals(reportName) || "MOH705B".equals(reportName)) &&
                        dataElementName.endsWith("-32")) {
                    dataElementName = dataElementName.substring(0, dataElementName.length() - 3);
                }

                // Apply value transformations if configured
                String valueStr = value.toString();
                if (datasetMapping != null && datasetMapping.getTransformations().containsKey(mappingKey)) {
                    valueStr = applyTransformation(valueStr, datasetMapping.getTransformations().get(mappingKey));
                }

                // Add prefix for data element name
                String prefix = getColumnPrefix(config);
                if (prefix != null && !prefix.isEmpty()) {
                    dataElementName = prefix + dataElementName;
                }

                // Escape XML special characters
                valueStr = escapeXml(valueStr);
                dataElementName = escapeXml(dataElementName);

                // Write dataValue with or without categoryOptionCombo
                if (categoryOptionCombo != null && !categoryOptionCombo.isEmpty()) {
                    writer.write("<dataValue dataElement=\"" + dataElementName + "\" value=\"" + valueStr +
                            "\" categoryOptionCombo=\"" + escapeXml(categoryOptionCombo) + "\"/>\n");
                } else {
                    writer.write("<dataValue dataElement=\"" + dataElementName + "\" value=\"" + valueStr + "\"/>\n");
                }
            }
        }
    }

    /**
     * Gets the appropriate dataset name based on report type
     */
    private String getDatasetName(AdxConfiguration.AdxDatasetMapping datasetMapping, String dsKey, String reportName) {
        if (datasetMapping != null) {
            if (MOH_731_REPORT_NAME.equals(reportName) || MOH_743_REPORT_NAME.equals(reportName)) {
                return datasetMapping.getDhisName();
            } else if (MONTHLY_REPORT_NAME.equals(reportName)) {
                return datasetMapping.get3pmName();
            } else {
                // For other reports, use dhisName as default
                return datasetMapping.getDhisName();
            }
        }
        return dsKey; // Fallback to original key
    }

    /**
     * Gets the MFL code for the facility
     */
    private String getMflCode(AdministrationService adminService, LocationService locationService, String defaultLocationId) {
        String mfl = "Unknown";

        // First try to get from global property
        String gpMflCode = adminService.getGlobalProperty("facility.mflcode");
        if (StringUtils.isNotBlank(gpMflCode)) {
            return gpMflCode;
        }

        // Then try to get from location
        if (StringUtils.isNotBlank(defaultLocationId)) {
            try {
                Integer locationId = Integer.parseInt(defaultLocationId);
                Location location = locationService.getLocation(locationId);
                if (location != null) {
                    mfl = new Facility(location).getMflCode();
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid default location ID: " + defaultLocationId);
            }
        }

        return mfl;
    }
    /**
     * Gets the base URL for DHIS2 API from the existing ilServer.address global property
     */
    private String getBaseUrl() {
        AdministrationService adminService = Context.getAdministrationService();
        String baseUrl = adminService.getGlobalProperty("ilServer.address");

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.warn("ilServer.address global property is not set or empty, using default: https://test.hiskenya.org");
            return "https://test.hiskenya.org";
        }

        String trimmedUrl = baseUrl.trim();
        return trimmedUrl;
    }
    /**
     * Builds the complete endpoint URL with query parameters based on field mappings
     */
    private String buildEndpointUrl(AdxConfiguration config, String reportName) {
        try {
            String baseUrl = getBaseUrl();
            if (baseUrl == null) {
                throw new IllegalStateException("Missing IL server address configuration (ilServer.address)");
            }

            // Remove trailing slash if present
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
              //  log.debug("Removed trailing slash from base URL: {}", baseUrl);
            }

            // Check if any dataset has field mappings
            boolean hasFieldMappings = hasFieldMappings(config);

            StringBuilder urlBuilder = new StringBuilder(baseUrl)
                    .append("/api/dataValueSets");

            if (hasFieldMappings) {
                // Use UID-based parameters for reports with field mappings
                urlBuilder.append("?dataElementIdScheme=uid")
                        .append("&orgUnitIdScheme=code")
                        .append("&dataSetIdScheme=uid")
                        .append("&categoryOptionComboIdScheme=uid");

            } else {
                // Use code-based parameters for reports without field mappings
                urlBuilder.append("?dataElementIdScheme=code")
                        .append("&orgUnitIdScheme=code")
                        .append("&dataSetIdScheme=uid");
            }

            String finalUrl = urlBuilder.toString();

            return finalUrl;

        } catch (Exception e) {
            log.error("Error building endpoint URL for report: " + reportName, e);
            String fallbackUrl = getBaseUrl() + "/api/dataValueSets?dataElementIdScheme=code&orgUnitIdScheme=code&dataSetIdScheme=uid";
          //  log.warn("Using fallback endpoint URL: {}", fallbackUrl);
            return fallbackUrl;
        }
    }
    /**
     * Checks if the configuration has any field mappings across all datasets
     */
    private boolean hasFieldMappings(AdxConfiguration config) {
        if (config == null || config.getDatasets() == null) {
            return false;
        }

        for (AdxConfiguration.AdxDatasetMapping dataset : config.getDatasets()) {
            if (dataset != null && dataset.getFieldMappings() != null && !dataset.getFieldMappings().isEmpty()) {
                log.debug("Found field mappings in dataset: {}");
                return true;
            }
        }

        log.debug("No field mappings found in configuration");
        return false;
    }
    /**
     * Searches both DHIS2 and 3PM global properties for report configuration
     */
    private AdxConfiguration getConfigurationForReport(String reportName) {
        try {
            // First check DHIS2 ADX mapping
            String dhisConfigJson = EmrUtils.getGlobalPropertyValue(EmrConstants.GP_DHIS2_DATASET_MAPPING);
            AdxConfiguration config = findConfigInJson(dhisConfigJson, reportName);

            if (config != null) {
                log.debug("Found configuration for report '" + reportName + "' in DHIS2 mapping");
                return config;
            }

            // If not found, check 3PM ADX mapping
            String threePmConfigJson = EmrUtils.getGlobalPropertyValue(EmrConstants.GP_3PM_DATASET_MAPPING);
            config = findConfigInJson(threePmConfigJson, reportName);

            if (config != null) {
                log.debug("Found configuration for report '" + reportName + "' in 3PM mapping");
                return config;
            }

            log.debug("No configuration found for report: " + reportName);

        } catch (Exception e) {
            log.error("Error parsing ADX configuration for report: " + reportName, e);
        }

        return null;
    }

    private AdxConfiguration findConfigInJson(String configJson, String reportName) {
        try {
            if (StringUtils.isBlank(configJson)) {
                return null;
            }

            // Parse JSON array of configurations
            JsonNode rootNode = objectMapper.readTree(configJson);
            if (rootNode.isArray()) {
                for (JsonNode configNode : rootNode) {
                    AdxConfiguration config = objectMapper.treeToValue(configNode, AdxConfiguration.class);
                    if (reportName.equals(config.getReportName())) {
                        return config;
                    }
                }
            } else {
                log.warn("ADX configuration is not a JSON array: " + configJson);
            }

        } catch (Exception e) {
            log.error("Error parsing JSON configuration: " + configJson, e);
        }

        return null;
    }

    private AdxConfiguration.AdxDatasetMapping findDatasetMapping(AdxConfiguration config, String datasetName) {
        if (config == null || config.getDatasets() == null) {
            return null;
        }

        return config.getDatasets().stream()
                .filter(mapping -> datasetName.equals(mapping.getName()))
                .findFirst()
                .orElse(null);
    }
    private boolean shouldIncludeFacilityDatasetForReport(AdxConfiguration config, String datasetMapping, String reportName) {
        if (StringUtils.isBlank(datasetMapping)) {
            log.debug("Dataset mapping is blank, excluding");
            return false;
        }

        // For MOH 731, include ALL facility datasets unless specifically configured otherwise
        if (MOH_731_REPORT_NAME.equals(reportName)) {
            // If no specific configuration exists, include ALL facility datasets for MOH 731
            if (config == null || config.getDatasets() == null || config.getDatasets().isEmpty()) {
                log.debug("No dataset configuration found for MOH 731, including all facility datasets");
                return true;
            }

            // If configuration exists, check if this dataset is explicitly configured
            boolean isConfigured = config.getDatasets().stream()
                    .anyMatch(mapping -> datasetMapping.equals(mapping.getDhisName()));

            if (isConfigured) {
                log.debug("Dataset " + datasetMapping + " is explicitly configured for MOH 731, including");
                return true;
            } else {
                // if not explicitly configured, include for MOH 731 (backward compatibility)
                log.debug("Dataset " + datasetMapping + " not explicitly configured but including for MOH 731 backward compatibility");
                return true;
            }
        }

        // For other reports, use configured mapping
        if (config != null && config.getDatasets() != null) {
            boolean shouldInclude = config.getDatasets().stream()
                    .anyMatch(mapping -> {
                        String targetMapping = null;
                        if (MONTHLY_REPORT_NAME.equals(reportName)) {
                            targetMapping = mapping.get3pmName();
                        } else {
                            // For MOH 711 and other reports, use dhisName
                            targetMapping = mapping.getDhisName();
                        }
                        return datasetMapping.equals(targetMapping);
                    });

            log.debug("Dataset " + datasetMapping + " configured check result for " + reportName + ": " + shouldInclude);
            return shouldInclude;
        }

        log.debug("No configuration found for " + reportName + ", excluding dataset " + datasetMapping);
        return false;
    }
    /**
     * Gets facility report data for the specified period
     */
    private List<ReportDatasetValueEntryMapper> getFacilityReportData(Integer reportID, String startDate, String endDate) {
        try {
            log.debug("Fetching facility report data for reportID: " + reportID +
                    ", startDate: " + startDate + ", endDate: " + endDate);

            List<ReportDatasetValueEntryMapper> data = FacilityReporting.getReportDataForPeriod(reportID, startDate, endDate);

            if (data != null && !data.isEmpty()) {
                log.info("Retrieved " + data.size() + " facility reporting entries");
                // Log first entry details for debugging
                ReportDatasetValueEntryMapper firstEntry = data.get(0);
                log.debug("First entry - Dataset ID: " + firstEntry.getDatasetID() +
                        ", Indicators count: " + (firstEntry.getIndicators() != null ? firstEntry.getIndicators().size() : 0));
            } else {
                log.warn("No facility reporting data found for reportID: " + reportID +
                        " between " + startDate + " and " + endDate);
            }

            return data != null ? data : new ArrayList<>();
        } catch (ParseException e) {
            log.error("Error parsing dates for facility report data: startDate=" + startDate + ", endDate=" + endDate, e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error retrieving facility report data for reportID: " + reportID, e);
            return new ArrayList<>();
        }
    }

    /**
     * Validates if the indicator value is valid for inclusion
     */
    private boolean isValidIndicatorValue(Object value) {
        return value != null &&
                !"0".equals(value.toString()) &&
                !"".equals(value.toString()) &&
                StringUtils.isNotEmpty(value.toString());
    }

    /**
     * Gets the column prefix from configuration
     */
    private String getColumnPrefix(AdxConfiguration config) {
        return config != null && StringUtils.isNotBlank(config.getPrefix()) ?
                config.getPrefix() : "";
    }

    private String applyTransformation(String value, String transformation) {
        // Simple transformation logic - can be extended
        switch (transformation.toLowerCase()) {
            case "uppercase":
                return value.toUpperCase();
            case "lowercase":
                return value.toLowerCase();
            case "trim":
                return value.trim();
            default:
                return value;
        }
    }

    private String escapeXml(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
