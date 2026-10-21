/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.model.dq;

import java.util.Date;

/**
 * One cell of a generated report: a dataset column for one report run. When the column is mapped
 * to KHIS it also carries the data element and category option combo. Indicators are sums of these
 * values, so they are never stored separately.
 *
 * The KHIS fields are filled by the submission pipeline and read-back in later phases.
 */
public class DqValue {

	public static final String SOURCE_REPORT = "REPORT";

	public static final String SOURCE_FACILITY_REPORTING = "FACILITY_REPORTING";

	private Integer id;

	private DqReportRun reportRun;

	private String datasetName;

	private String columnName;

	/** Key looked up in the KHIS mapping, e.g. "TMC-32" or "Positive Result.microscopy" */
	private String mappingKey;

	private String dataElement;

	private String categoryOptionCombo;

	private String valueText;

	/** Null when the value is not a number */
	private Double valueNumeric;

	private String source;

	/** EMR value recalculated at read-back, to show data entered late */
	private Double emrValueNow;

	private Double khisValue;

	private Date khisLastUpdated;

	/** saved / ignored / conflict, from the KHIS import summary */
	private String importOutcome;

	private String conflictText;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public DqReportRun getReportRun() {
		return reportRun;
	}

	public void setReportRun(DqReportRun reportRun) {
		this.reportRun = reportRun;
	}

	public String getDatasetName() {
		return datasetName;
	}

	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getMappingKey() {
		return mappingKey;
	}

	public void setMappingKey(String mappingKey) {
		this.mappingKey = mappingKey;
	}

	public String getDataElement() {
		return dataElement;
	}

	public void setDataElement(String dataElement) {
		this.dataElement = dataElement;
	}

	public String getCategoryOptionCombo() {
		return categoryOptionCombo;
	}

	public void setCategoryOptionCombo(String categoryOptionCombo) {
		this.categoryOptionCombo = categoryOptionCombo;
	}

	public String getValueText() {
		return valueText;
	}

	public void setValueText(String valueText) {
		this.valueText = valueText;
	}

	public Double getValueNumeric() {
		return valueNumeric;
	}

	public void setValueNumeric(Double valueNumeric) {
		this.valueNumeric = valueNumeric;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Double getEmrValueNow() {
		return emrValueNow;
	}

	public void setEmrValueNow(Double emrValueNow) {
		this.emrValueNow = emrValueNow;
	}

	public Double getKhisValue() {
		return khisValue;
	}

	public void setKhisValue(Double khisValue) {
		this.khisValue = khisValue;
	}

	public Date getKhisLastUpdated() {
		return khisLastUpdated;
	}

	public void setKhisLastUpdated(Date khisLastUpdated) {
		this.khisLastUpdated = khisLastUpdated;
	}

	public String getImportOutcome() {
		return importOutcome;
	}

	public void setImportOutcome(String importOutcome) {
		this.importOutcome = importOutcome;
	}

	public String getConflictText() {
		return conflictText;
	}

	public void setConflictText(String conflictText) {
		this.conflictText = conflictText;
	}
}
