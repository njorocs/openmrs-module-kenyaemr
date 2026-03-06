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

import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.config.ReportDescriptor;

import java.io.IOException;
import java.io.Writer;

/**
 * Strategy interface for different ADX generation approaches
 */
public interface AdxGenerationStrategy {
    /**
     * Get the ADX dataset identifier for this report
     */
    String getDataSetIdentifier(ReportData reportData);

    /**
     * Get report-specific metadata for ADX
     */
    AdxMetadata getAdxMetadata(ReportData reportData);

    /**
     * Transform report data to ADX format
     */
    void writeDataElements(Writer writer, ReportData reportData) throws IOException;

    /**
     * Validate if this strategy can handle the given report
     */
    boolean canHandle(ReportDescriptor reportDescriptor);

    /**
     * Get custom ADX namespace if needed
     */
    default String getCustomNamespace() {
        return "urn:ihe:qrph:adx:2015";
    }
}
