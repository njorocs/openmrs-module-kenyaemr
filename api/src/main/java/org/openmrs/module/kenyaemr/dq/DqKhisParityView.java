/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EMR against KHIS for a report version read back from KHIS (G-RPT-02), for the page: one row per
 * KHIS data element the version sent, its EMR and KHIS totals over its category option combos and
 * the variance, compared as {@link org.openmrs.module.kenyaemr.dq.rule.KhisParityRule} compares
 * them; and the values KHIS holds that the version did not send.
 */
public final class DqKhisParityView {

	private static final double EPSILON = 1e-9;

	/** One report cell: a data element and category option combo */
	public static class Cell {

		private final String column;

		private final String label;

		private final String categoryOptionCombo;

		private double emr;

		private Double khis;

		private String categoryOptionComboName;

		Cell(String column, String label, String categoryOptionCombo) {
			this.column = column;
			this.label = label;
			this.categoryOptionCombo = categoryOptionCombo;
		}

		public String getColumn() {
			return column;
		}

		public String getLabel() {
			return label;
		}

		public String getCategoryOptionCombo() {
			return categoryOptionCombo;
		}

		public double getEmr() {
			return emr;
		}

		/** Null when KHIS holds no value for the cell, or the version was not read back */
		public Double getKhis() {
			return khis;
		}

		/** KHIS's name for the category option combo, e.g. "<5 years, Female"; null when not read */
		public String getCategoryOptionComboName() {
			return categoryOptionComboName;
		}
	}

	/** One data element: the indicator compared */
	public static class Row {

		private final String dataElement;

		private final List<Cell> cells = new ArrayList<Cell>();

		private double emr;

		private Double khis;

		private Double variance;

		private Boolean withinTolerance;

		private DqResult result;

		private String khisName;

		Row(String dataElement) {
			this.dataElement = dataElement;
		}

		public String getDataElement() {
			return dataElement;
		}

		/** The labels of its report columns, e.g. "Confirmed Malaria (Total)" */
		public String getLabel() {
			List<String> labels = new ArrayList<String>();
			for (Cell cell : cells) {
				String label = StringUtils.defaultIfBlank(cell.getLabel(), cell.getColumn());
				if (!labels.contains(label)) {
					labels.add(label);
				}
			}
			return StringUtils.join(labels, ", ");
		}

		public List<String> getColumns() {
			List<String> columns = new ArrayList<String>();
			for (Cell cell : cells) {
				if (!columns.contains(cell.getColumn())) {
					columns.add(cell.getColumn());
				}
			}
			return columns;
		}

		/** KHIS's name for the data element, e.g. "MOH 705A Rev 2020_Deworming"; null when not read */
		public String getKhisName() {
			return khisName;
		}

		public List<Cell> getCells() {
			return cells;
		}

		public double getEmr() {
			return emr;
		}

		/** Null until the version is read back */
		public Double getKhis() {
			return khis;
		}

		/** (KHIS - EMR) / EMR; null until read back */
		public Double getVariance() {
			return variance;
		}

		public Boolean getWithinTolerance() {
			return withinTolerance;
		}

		/** The G-RPT-02 result saved for the data element, when the rule compares every value */
		public DqResult getResult() {
			return result;
		}
	}

	/** A value KHIS holds for the report's datasets that the version did not send */
	public static class KhisOnly {

		private final String dataElement;

		private final String categoryOptionCombo;

		private final String value;

		private final String storedBy;

		private final String lastUpdated;

		private String dataElementName;

		private String categoryOptionComboName;

		KhisOnly(String dataElement, String categoryOptionCombo, String value, String storedBy, String lastUpdated) {
			this.dataElement = dataElement;
			this.categoryOptionCombo = categoryOptionCombo;
			this.value = value;
			this.storedBy = storedBy;
			this.lastUpdated = lastUpdated;
		}

		public String getDataElement() {
			return dataElement;
		}

		public String getCategoryOptionCombo() {
			return categoryOptionCombo;
		}

		public String getValue() {
			return value;
		}

		public String getStoredBy() {
			return storedBy;
		}

		public String getLastUpdated() {
			return lastUpdated;
		}

		/** KHIS's name for the data element, or null when it was not read */
		public String getDataElementName() {
			return dataElementName;
		}

		public String getCategoryOptionComboName() {
			return categoryOptionComboName;
		}
	}

	private DqKhisParityView() {
	}

	/**
	 * @param values the version's stored cells; those without a KHIS mapping are left out
	 * @param labels column label by "dataset\u0000column"
	 * @param readBack whether the version was read back, so that a cell KHIS has no value for counts
	 *        as 0 there, as the rule counts it
	 * @param tolerance the rule's tolerance, e.g. 0.05
	 * @param results the version's G-RPT-02 results, matched to a row by data element
	 * @param names KHIS's names by uid as JSON, read back with the values; may be null
	 */
	public static List<Row> rows(List<DqValue> values, Map<String, String> labels, boolean readBack, double tolerance,
	                             List<DqResult> results, String names) {
		Map<String, String> named = names(names);
		Map<String, Row> rows = new LinkedHashMap<String, Row>();
		// Cells sharing a data element and category option combo are added up, as the rule does
		Map<String, Cell> cells = new HashMap<String, Cell>();
		for (DqValue value : values) {
			if (value.getDataElement() == null) {
				continue;
			}
			Row row = rows.get(value.getDataElement());
			if (row == null) {
				row = new Row(value.getDataElement());
				rows.put(value.getDataElement(), row);
			}
			String key = value.getDataElement() + "|" + value.getCategoryOptionCombo();
			Cell cell = cells.get(key);
			if (cell == null) {
				cell = new Cell(value.getColumnName(), labels.get(value.getDatasetName() + "\u0000" + value.getColumnName()),
						value.getCategoryOptionCombo());
				cells.put(key, cell);
				row.cells.add(cell);
				cell.khis = value.getKhisValue();
			}
			cell.emr += value.getValueNumeric() != null ? value.getValueNumeric() : 0;
		}
		Map<String, DqResult> byCode = new HashMap<String, DqResult>();
		for (DqResult result : results) {
			if (result.getIndicatorCode() != null) {
				byCode.put(result.getIndicatorCode(), result);
			}
		}
		for (Row row : rows.values()) {
			double khis = 0;
			for (Cell cell : row.cells) {
				row.emr += cell.emr;
				khis += cell.khis != null ? cell.khis : 0;
			}
			if (readBack) {
				row.khis = khis;
				row.variance = row.emr == 0 ? (khis == 0 ? 0 : 1) : (khis - row.emr) / row.emr;
				row.withinTolerance = Math.abs(row.variance) <= tolerance + EPSILON;
			}
			row.result = byCode.get(row.dataElement);
			row.khisName = named.get(row.dataElement);
			for (Cell cell : row.cells) {
				cell.categoryOptionComboName = cell.categoryOptionCombo != null ? named.get(cell.categoryOptionCombo) : null;
			}
		}
		return new ArrayList<Row>(rows.values());
	}

	/**
	 * @param reply KHIS's dataValueSets reply as read back (JSON)
	 * @param sent the "dataElement|categoryOptionCombo" the version sent
	 * @param names KHIS's names by uid as JSON, read back with the values; may be null
	 * @return the values in the reply that the version did not send, e.g. entered in KHIS by hand;
	 *         empty when the reply cannot be read
	 */
	public static List<KhisOnly> khisOnly(String reply, Set<String> sent, String names) {
		Map<String, String> named = names(names);
		List<KhisOnly> found = new ArrayList<KhisOnly>();
		if (StringUtils.isBlank(reply)) {
			return found;
		}
		JsonNode root;
		try {
			root = new ObjectMapper().readTree(reply);
		}
		catch (Exception e) {
			return found;
		}
		for (JsonNode value : root.path("dataValues")) {
			String dataElement = value.path("dataElement").asText();
			String combo = value.path("categoryOptionCombo").asText();
			if (!sent.contains(dataElement + "|" + combo)) {
				KhisOnly extra = new KhisOnly(dataElement, combo, value.path("value").asText(null),
						value.path("storedBy").asText(null), value.path("lastUpdated").asText(null));
				extra.dataElementName = named.get(dataElement);
				extra.categoryOptionComboName = named.get(combo);
				found.add(extra);
			}
		}
		return found;
	}

	/** @return the names by uid; empty when there are none or they cannot be read */
	static Map<String, String> names(String json) {
		Map<String, String> names = new HashMap<String, String>();
		if (StringUtils.isBlank(json)) {
			return names;
		}
		try {
			JsonNode root = new ObjectMapper().readTree(json);
			java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				names.put(field.getKey(), field.getValue().asText());
			}
		}
		catch (Exception e) {
			names.clear();
		}
		return names;
	}
}
