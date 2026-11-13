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
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The JSON payload must hold what the ADX payload holds: unmapped datasets stay out
 */
public class ConfigurableJsonWriterTest {

	private static final String MOH_743 = "MOH-743 Report";

	@Test
	public void writeJsonDataElements_shouldLeaveOutUnmappedDatasets() throws Exception {
		AdxConfiguration config = new AdxConfiguration();
		config.setReportName(MOH_743);
		config.setOutputFormat("json");
		AdxConfiguration.AdxDatasetMapping mapping = new AdxConfiguration.AdxDatasetMapping();
		mapping.setName("Positive Result");
		mapping.setDhisName("Hq1V5o9zqxc");
		mapping.getFieldMappings().put("Positive Result.microscopy", "E64sUrp6DrX|QKy6jjocOLj");
		config.getDatasets().add(mapping);

		// The unmapped dataset comes first, so it would also have named the payload's dataSet
		Map<String, DataSet> dataSets = new LinkedHashMap<String, DataSet>();
		dataSets.put("SUSPECTED_MALARIA_AGE", dataset("SUSPECTED_MALARIA_AGE", "MICRO_LESS_5_POS", 2));
		dataSets.put("Positive Result", dataset("Positive Result", "microscopy", 4));

		JsonNode payload = new ObjectMapper().readTree(write(config, MOH_743, dataSets));

		Assert.assertEquals("Hq1V5o9zqxc", payload.path("dataSet").asText());
		Assert.assertEquals("202609", payload.path("period").asText());
		Assert.assertEquals("12345", payload.path("orgUnit").asText());
		JsonNode values = payload.path("dataValues");
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("E64sUrp6DrX", values.get(0).path("dataElement").asText());
		Assert.assertEquals("QKy6jjocOLj", values.get(0).path("categoryOptionCombo").asText());
		Assert.assertEquals("4", values.get(0).path("value").asText());
	}

	@Test
	public void writeJsonDataElements_shouldStillSendUnmappedMoh731Datasets() throws Exception {
		AdxConfiguration config = new AdxConfiguration();
		config.setReportName(ReportValueExtractor.MOH_731_REPORT_NAME);
		config.setOutputFormat("json");

		Map<String, DataSet> dataSets = new LinkedHashMap<String, DataSet>();
		dataSets.put("ART", dataset("ART", "HV03-01", 5));

		JsonNode payload = new ObjectMapper().readTree(write(config, ReportValueExtractor.MOH_731_REPORT_NAME, dataSets));

		Assert.assertEquals(1, payload.path("dataValues").size());
		Assert.assertEquals("HV03-01", payload.path("dataValues").get(0).path("dataElement").asText());
	}

	private static String write(final AdxConfiguration config, String reportName, Map<String, DataSet> dataSets)
			throws Exception {
		ConfigurableAdxGenerationStrategy strategy = new ConfigurableAdxGenerationStrategy() {

			@Override
			public AdxConfiguration getConfigurationForReport(String name) {
				return config;
			}

			@Override
			public AdxMetadata getAdxMetadata(ReportData reportData) {
				AdxMetadata metadata = new AdxMetadata();
				metadata.setOrgUnit("12345");
				return metadata;
			}
		};

		ReportDefinition definition = new ReportDefinition();
		definition.setName(reportName);
		// A real EvaluationContext needs a logged-in user
		EvaluationContext context = mock(EvaluationContext.class);
		when(context.getParameterValue("startDate")).thenReturn(new GregorianCalendar(2026, Calendar.SEPTEMBER, 1).getTime());
		when(context.getParameterValue("endDate")).thenReturn(new GregorianCalendar(2026, Calendar.SEPTEMBER, 30).getTime());
		ReportData reportData = new ReportData();
		reportData.setDefinition(definition);
		reportData.setContext(context);
		reportData.setDataSets(dataSets);

		StringWriter out = new StringWriter();
		strategy.writeJsonDataElements(out, reportData);
		return out.toString();
	}

	private static DataSet dataset(String name, String column, Object value) {
		SqlDataSetDefinition definition = new SqlDataSetDefinition();
		definition.setName(name);
		SimpleDataSet dataSet = new SimpleDataSet(definition, null);
		dataSet.addColumnValue(0, new DataSetColumn(column, column, Object.class), value);
		return dataSet;
	}
}
