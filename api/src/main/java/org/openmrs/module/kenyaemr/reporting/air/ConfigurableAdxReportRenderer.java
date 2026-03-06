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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Configurable ADX Report Renderer
 */
@Handler
public class ConfigurableAdxReportRenderer extends ReportDesignRenderer {
    private static final Log log = LogFactory.getLog(ConfigurableAdxReportRenderer.class);

    private ConfigurableAdxGenerationStrategy strategy;

    public ConfigurableAdxReportRenderer() {
        this.strategy = new ConfigurableAdxGenerationStrategy();
    }

    @Override
    public String getFilename(ReportRequest request) {
        return getFilenameBase(request) + ".xml";
    }

    @Override
    public String getRenderedContentType(ReportRequest request) {
        return "text/xml";
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

            // Write ADX header
            writeAdxHeader(writer, strategy);

            // Use strategy to write data
            strategy.writeDataElements(writer, reportData);

            // Write ADX footer
            writeAdxFooter(writer);

            writer.flush();
        } catch (IOException e) {
            log.error("Error rendering ADX for report: " + reportData.getDefinition().getName(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error rendering ADX for report: " + reportData.getDefinition().getName(), e);
            throw new RenderingException("Error rendering ADX: " + e.getMessage(), e);
        }
    }

    private void writeAdxHeader(Writer writer, AdxGenerationStrategy strategy) throws IOException {
        DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<adx xmlns=\"" + strategy.getCustomNamespace() + "\"\n" +
                "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "     xsi:schemaLocation=\"" + strategy.getCustomNamespace() + " ../schema/adx_loose.xsd\"\n" +
                "     exported=\"" + isoDateTimeFormat.format(new Date()) + "\">\n");
    }

    private void writeAdxFooter(Writer writer) throws IOException {
        writer.write("</adx>\n");
    }
}
