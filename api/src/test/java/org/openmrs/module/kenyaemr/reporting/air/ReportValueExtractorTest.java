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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link ReportValueExtractor}. The sendable cells must be exactly what the ADX writer
 * has always sent; the others are kept for the data quality checks.
 */
public class ReportValueExtractorTest {

	@Test
	public void extractDataSet_shouldResolveMappedCellsWithPrefixAndCombo() {
		AdxConfiguration config = config("MOH 706", "DHIS_");
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH706", "BSRT", "CceLQEL5Q4p|TZ7UnG134RW");

		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("MOH706", "BSRT", 12), mapping, "MOH 706", config));

		Assert.assertTrue(cell.isMapped());
		Assert.assertTrue(cell.isSendable());
		Assert.assertEquals("DHIS_CceLQEL5Q4p", cell.getDataElement());
		Assert.assertEquals("TZ7UnG134RW", cell.getCategoryOptionCombo());
		Assert.assertEquals("12", cell.getValue());
	}

	@Test
	public void extractDataSet_shouldKeepZeroCellsButNotSendThem() {
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH706", "BSRT", "CceLQEL5Q4p|TZ7UnG134RW");

		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("MOH706", "BSRT", 0), mapping, "MOH 706", null));

		Assert.assertTrue(cell.isMapped());
		Assert.assertFalse(cell.isSendable());
		Assert.assertEquals("0", cell.getValue());
		Assert.assertEquals("CceLQEL5Q4p", cell.getDataElement());
	}

	@Test
	public void extractDataSet_shouldKeepUnmappedColumnsOfMappedDatasetsButNotSendThem() {
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH706", "BSRT", "CceLQEL5Q4p|TZ7UnG134RW");

		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("MOH706", "BST-01", 7), mapping, "MOH 706", null));

		Assert.assertFalse(cell.isMapped());
		Assert.assertFalse(cell.isSendable());
		Assert.assertNull(cell.getDataElement());
		Assert.assertEquals("7", cell.getValue());
	}

	@Test
	public void extractDataSet_shouldNotSendPlaceholderMappings() {
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH706", "BSRT", "|");

		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("MOH706", "BSRT", 3), mapping, "MOH 706", null));

		Assert.assertFalse(cell.isMapped());
		Assert.assertFalse(cell.isSendable());
	}

	@Test
	public void extractDataSet_shouldOnlyKeepMonthlyTotalsForMoh705() {
		String report = "MOH 705A Outpatient summary";
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH705A", "TMC-32", "siOyOiOJpI8|lHl2rmXpHse");
		Map<String, Object> cells = new LinkedHashMap<String, Object>();
		cells.put("TMC-1", 2);
		cells.put("TMC-32", 9);

		List<ReportCellValue> extracted = ReportValueExtractor.extractDataSet(dataset("MOH705A", cells), mapping, report, null);

		ReportCellValue cell = single(extracted);
		Assert.assertEquals("TMC-32", cell.getColumnName());
		Assert.assertEquals("siOyOiOJpI8", cell.getDataElement());
		Assert.assertTrue(cell.isSendable());
	}

	@Test
	public void extractDataSet_shouldUseCompositeKeyForMoh743() {
		String report = "MOH-743 Report";
		AdxConfiguration.AdxDatasetMapping mapping = mapping("Positive Result", "Positive Result.microscopy", "E64sUrp6DrX|QKy6jjocOLj");

		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("Positive Result", "microscopy", 4), mapping, report, null));

		Assert.assertEquals("Positive Result.microscopy", cell.getMappingKey());
		Assert.assertEquals("E64sUrp6DrX", cell.getDataElement());
		Assert.assertTrue(cell.isSendable());
	}

	@Test
	public void extractDataSet_shouldSendUnmappedMoh731ColumnsUnderTheirOwnName() {
		ReportCellValue cell = single(ReportValueExtractor.extractDataSet(dataset("ART", "HV03-01", 5), null,
				ReportValueExtractor.MOH_731_REPORT_NAME, null));

		Assert.assertFalse(cell.isMapped());
		Assert.assertTrue(cell.isSendable());
		Assert.assertEquals("HV03-01", cell.getDataElement());
	}

	@Test
	public void extractDataSet_shouldSkipExcludedColumns() {
		AdxConfiguration.AdxDatasetMapping mapping = mapping("MOH706", "BSRT", "CceLQEL5Q4p|TZ7UnG134RW");
		mapping.getExcludeColumns().add("BSRT");

		Assert.assertTrue(ReportValueExtractor.extractDataSet(dataset("MOH706", "BSRT", 3), mapping, "MOH 706", null).isEmpty());
	}

	@Test
	public void extractAll_shouldIncludeDatasetsWithoutMapping() {
		AdxConfiguration config = config("MOH-743 Report", null);
		config.getDatasets().add(mapping("Positive Result", "Positive Result.microscopy", "E64sUrp6DrX|QKy6jjocOLj"));

		ReportDefinition definition = new ReportDefinition();
		definition.setName("MOH-743 Report");
		ReportData reportData = new ReportData();
		reportData.setDefinition(definition);
		Map<String, DataSet> dataSets = new LinkedHashMap<String, DataSet>();
		dataSets.put("Positive Result", dataset("Positive Result", "microscopy", 4));
		dataSets.put("SUSPECTED_MALARIA_AGE", dataset("SUSPECTED_MALARIA_AGE", "MICRO_LESS_5_POS", 2));
		reportData.setDataSets(dataSets);

		List<ReportCellValue> cells = ReportValueExtractor.extractAll(reportData, config);

		Assert.assertEquals(2, cells.size());
		Assert.assertTrue(cells.get(0).isSendable());
		Assert.assertEquals("SUSPECTED_MALARIA_AGE", cells.get(1).getDatasetName());
		Assert.assertEquals("2", cells.get(1).getValue());
		Assert.assertFalse(cells.get(1).isMapped());
	}

	private static ReportCellValue single(List<ReportCellValue> cells) {
		Assert.assertEquals(1, cells.size());
		return cells.get(0);
	}

	private static AdxConfiguration config(String reportName, String prefix) {
		AdxConfiguration config = new AdxConfiguration();
		config.setReportName(reportName);
		config.setPrefix(prefix);
		config.setDatasets(new ArrayList<AdxConfiguration.AdxDatasetMapping>());
		return config;
	}

	private static AdxConfiguration.AdxDatasetMapping mapping(String name, String key, String target) {
		AdxConfiguration.AdxDatasetMapping mapping = new AdxConfiguration.AdxDatasetMapping();
		mapping.setName(name);
		mapping.getFieldMappings().put(key, target);
		return mapping;
	}

	private static DataSet dataset(String name, String column, Object value) {
		Map<String, Object> cells = new LinkedHashMap<String, Object>();
		cells.put(column, value);
		return dataset(name, cells);
	}

	private static DataSet dataset(String name, Map<String, Object> cells) {
		SqlDataSetDefinition definition = new SqlDataSetDefinition();
		definition.setName(name);
		SimpleDataSet dataSet = new SimpleDataSet(definition, null);
		for (Map.Entry<String, Object> cell : cells.entrySet()) {
			dataSet.addColumnValue(0, new DataSetColumn(cell.getKey(), cell.getKey(), Object.class), cell.getValue());
		}
		return dataSet;
	}
}
