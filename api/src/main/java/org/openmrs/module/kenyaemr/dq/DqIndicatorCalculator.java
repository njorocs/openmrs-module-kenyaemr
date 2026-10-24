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

import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds up an indicator from stored report values
 */
public final class DqIndicatorCalculator {

	private DqIndicatorCalculator() {
	}

	/**
	 * @param indicator the indicator definition
	 * @param values stored values for one period; may span several reports
	 * @return the indicator value, with the columns that were not found
	 */
	public static IndicatorValue calculate(DqConfig.Indicator indicator, Collection<DqValue> values) {
		// A dataset with several rows (e.g. one per unit of issue) has the same column more than once
		Map<String, List<DqValue>> byKey = new HashMap<String, List<DqValue>>();
		Set<String> reports = new HashSet<String>();
		for (DqValue value : values) {
			String key = key(value.getReportRun().getReportName(), value.getDatasetName(), value.getColumnName());
			if (!byKey.containsKey(key)) {
				byKey.put(key, new ArrayList<DqValue>());
			}
			byKey.get(key).add(value);
			reports.add(value.getReportRun().getReportName());
		}

		double sum = 0;
		int found = 0;
		List<String> missing = new ArrayList<String>();
		for (DqConfig.Part part : indicator.getParts()) {
			for (String column : part.getColumns()) {
				List<DqValue> cells = byKey.get(key(part.getReport(), part.getDataset(), column));
				if (cells == null && part.isZeroWhenEmpty() && reports.contains(part.getReport())) {
					found++;
					continue;
				}
				if (cells == null) {
					missing.add(part.getReport() + " / " + part.getDataset() + " / " + column);
					continue;
				}
				found++;
				for (DqValue value : cells) {
					// An empty cell counts as zero; a non-numeric one is reported as missing
					if (value.getValueNumeric() != null) {
						sum += value.getValueNumeric();
					} else if (value.getValueText() != null && !value.getValueText().trim().isEmpty()) {
						missing.add(part.getReport() + " / " + part.getDataset() + " / " + column + " (not a number)");
					}
				}
			}
		}
		return new IndicatorValue(indicator.getCode(), found > 0 ? Double.valueOf(sum) : null, missing);
	}

	/**
	 * @return the numeric value of a report cell, or null when it is empty or not a number
	 */
	public static Double parseNumber(String text) {
		if (text == null || text.trim().isEmpty()) {
			return null;
		}
		try {
			return Double.valueOf(text.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	private static String key(String report, String dataset, String column) {
		return report + "\u0000" + dataset + "\u0000" + column;
	}

	/**
	 * The value of one indicator for one period
	 */
	public static class IndicatorValue {

		private final String code;

		private final Double value;

		private final List<String> missingColumns;

		public IndicatorValue(String code, Double value, List<String> missingColumns) {
			this.code = code;
			this.value = value;
			this.missingColumns = missingColumns;
		}

		public String getCode() {
			return code;
		}

		/**
		 * @return the sum of the columns found, or null when none of the indicator's columns were found
		 */
		public Double getValue() {
			return value;
		}

		public List<String> getMissingColumns() {
			return missingColumns;
		}

		public boolean isComplete() {
			return value != null && missingColumns.isEmpty();
		}
	}
}
