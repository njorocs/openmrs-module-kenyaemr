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

import java.util.HashMap;
import java.util.Map;
/**
 * Encapsulates ADX metadata for a report
 */
public class AdxMetadata {
    private String orgUnit;
    private String reportName;
    private String prefix;
    private String customNamespace;
    private String endpointUrl;
    private boolean hasFieldMappings;

    public AdxMetadata() {}

    // Getters and setters
    public String getOrgUnit() { return orgUnit; }
    public void setOrgUnit(String orgUnit) { this.orgUnit = orgUnit; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getCustomNamespace() { return customNamespace; }
    public void setCustomNamespace(String customNamespace) { this.customNamespace = customNamespace; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public boolean isHasFieldMappings() { return hasFieldMappings; }
    public void setHasFieldMappings(boolean hasFieldMappings) { this.hasFieldMappings = hasFieldMappings; }
}