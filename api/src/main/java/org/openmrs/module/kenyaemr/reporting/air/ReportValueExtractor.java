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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.ReportData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns an evaluated report into cells resolved against the KHIS mapping. The ADX and JSON writers
 * send the sendable cells; the data quality checks store every cell, including zeros and unmapped
 * columns, so that what is checked is exactly what is sent.
 */
public final class ReportValueExtractor {

	static final String MOH_731_REPORT_NAME = "Revised MOH 731";

	private static final String MOH_743_REPORT_NAME = "MOH-743 Report";

	private ReportValueExtractor() {
	}

	/**
	 * Extracts every dataset of a report, whether or not the dataset is mapped
	 *
	 * @param reportData the evaluated report
	 * @param config the report's KHIS mapping, or null when it has none
	 * @return the cells in dataset, row and column order
	 */
	public static List<ReportCellValue> extractAll(ReportData reportData, AdxConfiguration config) {
		List<ReportCellValue> cells = new ArrayList<ReportCellValue>();
		String reportName = reportData.getDefinition().getName();
		for (Map.Entry<String, DataSet> entry : reportData.getDataSets().entrySet()) {
			AdxConfiguration.AdxDatasetMapping datasetMapping = findDatasetMapping(config, entry.getKey());
			cells.addAll(extractDataSet(entry.getValue(), datasetMapping, reportName, config));
		}
		return cells;
	}

	/**
	 * Extracts one dataset, applying the same filtering, mapping and transformation rules the ADX
	 * writer has always applied
	 */
	public static List<ReportCellValue> extractDataSet(DataSet dataset, AdxConfiguration.AdxDatasetMapping datasetMapping,
	                                                   String reportName, AdxConfiguration config) {
		List<ReportCellValue> cells = new ArrayList<ReportCellValue>();
		List<DataSetColumn> columns = dataset.getMetaData().getColumns();
		String dsDefinitionName = dataset.getDefinition().getName();
		boolean moh705 = isMoh705(reportName);
		boolean moh731 = MOH_731_REPORT_NAME.equals(reportName);

		for (DataSetRow row : dataset) {
			for (DataSetColumn column : columns) {
				String columnName = column.getName();

				if (datasetMapping != null && datasetMapping.getExcludeColumns().contains(columnName)) {
					continue;
				}

				// MOH 705A/B are reported as monthly totals only (columns ending with -32)
				if (moh705 && !columnName.endsWith("-32")) {
					continue;
				}

				Object rawValue = row.getColumnValue(column);
				boolean hasValue = rawValue != null && StringUtils.isNotBlank(rawValue.toString())
						&& !"0".equals(rawValue.toString());

				// For MOH 743, try the composite key (datasetName.columnName) first
				String mappingKey = columnName;
				if (MOH_743_REPORT_NAME.equals(reportName)) {
					String compositeKey = dsDefinitionName + "." + columnName;
					if (datasetMapping != null && datasetMapping.getFieldMappings().containsKey(compositeKey)) {
						mappingKey = compositeKey;
					}
				}

				boolean hasMapping = datasetMapping != null && datasetMapping.getFieldMappings().containsKey(mappingKey);
				String dataElementName = mappingKey;
				String categoryOptionCombo = null;
				boolean mapped = false;
				boolean sendable = hasValue;

				if (hasMapping) {
					String[] resolved = resolveMappedDataElement(datasetMapping.getFieldMappings().get(mappingKey));
					if (resolved == null) {
						// A placeholder such as "|" for an indicator that has no KHIS code yet
						sendable = false;
					} else {
						dataElementName = resolved[0];
						categoryOptionCombo = resolved[1];
						mapped = true;
					}
				} else if (datasetMapping != null && !moh731) {
					sendable = false;
				}

				if ("date".equals(mappingKey) && !moh731) {
					sendable = false;
				}

				if (moh705 && dataElementName.endsWith("-32")) {
					dataElementName = dataElementName.substring(0, dataElementName.length() - 3);
				}

				String value = rawValue != null ? rawValue.toString() : null;
				if (value != null && datasetMapping != null && datasetMapping.getTransformations().containsKey(mappingKey)) {
					value = applyTransformation(value, datasetMapping.getTransformations().get(mappingKey));
				}

				String dataElement = null;
				if (mapped || sendable) {
					dataElement = getColumnPrefix(config) + dataElementName;
				}

				cells.add(new ReportCellValue(dsDefinitionName, columnName, mappingKey, dataElement,
						mapped ? categoryOptionCombo : null, value, mapped, sendable));
			}
		}
		return cells;
	}

	/**
	 * Resolves a configured field mapping of the form {@code dataElement} or
	 * {@code dataElement|categoryOptionCombo}. Placeholders that carry no data element - such as
	 * {@code "|"}, used for indicators that have no KHIS code yet - resolve to null so that the
	 * column is skipped instead of breaking the whole payload.
	 *
	 * @return {dataElement, categoryOptionCombo or null}, or null when there is no usable data element
	 */
	static String[] resolveMappedDataElement(String mappedValue) {
		if (StringUtils.isBlank(mappedValue)) {
			return null;
		}

		String dataElement = mappedValue;
		String categoryOptionCombo = null;

		int separator = mappedValue.indexOf('|');
		if (separator >= 0) {
			dataElement = mappedValue.substring(0, separator);
			categoryOptionCombo = mappedValue.substring(separator + 1);
		}

		dataElement = StringUtils.trimToNull(dataElement);
		if (dataElement == null) {
			return null;
		}

		return new String[] { dataElement, StringUtils.trimToNull(categoryOptionCombo) };
	}

	static AdxConfiguration.AdxDatasetMapping findDatasetMapping(AdxConfiguration config, String datasetName) {
		if (config == null || config.getDatasets() == null) {
			return null;
		}
		for (AdxConfiguration.AdxDatasetMapping mapping : config.getDatasets()) {
			if (datasetName.equals(mapping.getName())) {
				return mapping;
			}
		}
		return null;
	}

	static String getColumnPrefix(AdxConfiguration config) {
		return config != null && StringUtils.isNotBlank(config.getPrefix()) ? config.getPrefix() : "";
	}

	static String applyTransformation(String value, String transformation) {
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

	private static boolean isMoh705(String reportName) {
		return "MOH 705A Outpatient summary".equals(reportName) || "MOH705A".equals(reportName)
				|| "MOH 705B Outpatient summary".equals(reportName) || "MOH705B".equals(reportName);
	}
}
