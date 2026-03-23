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

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.config.ReportDescriptor;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.renderer.RenderingException;
import org.openmrs.module.reporting.report.renderer.ReportDesignRenderer;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Configurable JSON Report Renderer for KHIS format
 */
@Handler
public class ConfigurableJsonReportRenderer extends ReportDesignRenderer {
    private static final Log log = LogFactory.getLog(ConfigurableJsonReportRenderer.class);

    private ConfigurableAdxGenerationStrategy strategy;

    public ConfigurableJsonReportRenderer() {
        this.strategy = new ConfigurableAdxGenerationStrategy();
    }

    @Override
    public String getFilename(ReportRequest request) {
        return getFilenameBase(request) + ".json";
    }

    @Override
    public String getRenderedContentType(ReportRequest request) {
        return "application/json";
    }

    @Override
    public void render(ReportData reportData, String argument, OutputStream out) throws IOException, RenderingException {

        if (reportData == null) {
            throw new RenderingException("ReportData cannot be null");
        }

        if (reportData.getDefinition() == null) {
            throw new RenderingException("Report definition cannot be null");
        }

        ReportDescriptor descriptor = new ReportDescriptor();
        descriptor.setName(reportData.getDefinition().getName());

        if (!strategy.canHandle(descriptor)) {
            throw new RenderingException("No ADX configuration found for report: " + reportData.getDefinition().getName());
        }

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out, "UTF-8");

            strategy.writeJsonDataElements(writer, reportData);

            writer.flush();
        } catch (IOException e) {
            log.error("Error rendering JSON for report: " + reportData.getDefinition().getName(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error rendering JSON for report: " + reportData.getDefinition().getName(), e);
            throw new RenderingException("Error rendering JSON: " + e.getMessage(), e);
        }
    }
}
