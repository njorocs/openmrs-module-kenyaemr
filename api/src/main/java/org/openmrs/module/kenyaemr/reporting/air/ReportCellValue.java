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

/**
 * One cell of an evaluated report, resolved against the KHIS mapping
 */
public class ReportCellValue {

	private final String datasetName;

	private final String columnName;

	private final String mappingKey;

	private final String dataElement;

	private final String categoryOptionCombo;

	private final String value;

	private final boolean mapped;

	private final boolean sendable;

	public ReportCellValue(String datasetName, String columnName, String mappingKey, String dataElement,
	                       String categoryOptionCombo, String value, boolean mapped, boolean sendable) {
		this.datasetName = datasetName;
		this.columnName = columnName;
		this.mappingKey = mappingKey;
		this.dataElement = dataElement;
		this.categoryOptionCombo = categoryOptionCombo;
		this.value = value;
		this.mapped = mapped;
		this.sendable = sendable;
	}

	/**
	 * @return the name of the dataset definition the cell came from
	 */
	public String getDatasetName() {
		return datasetName;
	}

	public String getColumnName() {
		return columnName;
	}

	/**
	 * @return the key looked up in the field mappings, e.g. "TMC-32" or "Positive Result.microscopy"
	 */
	public String getMappingKey() {
		return mappingKey;
	}

	/**
	 * @return the data element as written to ADX (prefix applied), or null when the cell is neither
	 * mapped nor sent
	 */
	public String getDataElement() {
		return dataElement;
	}

	public String getCategoryOptionCombo() {
		return categoryOptionCombo;
	}

	/**
	 * @return the value with configured transformations applied, or null when the cell is empty
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return true when the field mappings resolve this cell to a KHIS data element
	 */
	public boolean isMapped() {
		return mapped;
	}

	/**
	 * @return true when the ADX payload includes this cell; zero, empty and unmapped cells are not sent
	 */
	public boolean isSendable() {
		return sendable;
	}
}
