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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The thresholds of each rule type that a facility may set from the settings screen, with the
 * values each may take. Everything else about a rule (its indicators, reports, severity) stays in
 * the file, set by the program.
 */
public final class DqRuleThresholds {

	/** How a threshold is written and shown */
	public enum Kind {
		/** A fraction from 0 to 1, shown as a percentage: 0.2 is 20% */
		PERCENT,
		/** A ratio, shown as it is: 0.8 */
		RATIO,
		/** A whole number of days, months or tests */
		COUNT,
		/** One of a list of values */
		CHOICE
	}

	/** One threshold of a rule type */
	public static final class Field {

		private final String key;

		private final Kind kind;

		private final Object fallback;

		private final double min;

		private final double max;

		private final boolean optional;

		private final List<String> options;

		Field(String key, Kind kind, Object fallback, double min, double max, boolean optional, List<String> options) {
			this.key = key;
			this.kind = kind;
			this.fallback = fallback;
			this.min = min;
			this.max = max;
			this.optional = optional;
			this.options = options;
		}

		public String getKey() {
			return key;
		}

		public Kind getKind() {
			return kind;
		}

		/**
		 * @return the value the rule uses when the file does not set it; null when there is none
		 */
		public Object getFallback() {
			return fallback;
		}

		public double getMin() {
			return min;
		}

		public double getMax() {
			return max;
		}

		/**
		 * @return whether the threshold may be left out, which turns it off
		 */
		public boolean isOptional() {
			return optional;
		}

		public List<String> getOptions() {
			return options;
		}
	}

	private static final Map<String, List<Field>> FIELDS = new HashMap<String, List<Field>>();

	static {
		FIELDS.put(DqConfig.RuleDefinition.TYPE_EXPECTED_RANGE, Arrays.asList(
				choice("baseline", ConsistencyOverTimeRule.BASELINE_BOTH, ConsistencyOverTimeRule.BASELINE_LAST_YEAR,
						ConsistencyOverTimeRule.BASELINE_ROLLING, ConsistencyOverTimeRule.BASELINE_BOTH),
				ratio("ratioMin", 0.8, false), ratio("ratioMax", 1.2, false),
				percent("band", 0.2, 0.01, 1), count("minHistoryMonths", 3, 1, 12)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_RATIO, Arrays.asList(ratio("min", null, true), ratio("max", null, true)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_RECONCILIATION, Collections.singletonList(percent("tolerance", 0.10, 0, 0.99)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_REPEAT_EPISODE, Collections.singletonList(count("windowDays", 14, 1, 90)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_CHANGE, Arrays.asList(percent("changeThreshold", 0.5, 0.01, 10),
				count("minDenominator", 20, 1, 10000)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_KHIS_PARITY, Collections.singletonList(percent("tolerance", 0.05, 0, 0.99)));
		FIELDS.put(DqConfig.RuleDefinition.TYPE_VERIFICATION, Collections.singletonList(percent("margin", 0.1, 0.01, 0.99)));
	}

	private DqRuleThresholds() {
	}

	/**
	 * @return the thresholds a rule of the type has, in the order they are shown; empty when it has
	 * none (G-RPT-01 follows the report deadlines)
	 */
	public static List<Field> fields(String type) {
		List<Field> fields = type != null ? FIELDS.get(type) : null;
		return fields != null ? fields : Collections.<Field>emptyList();
	}

	/**
	 * Sets a rule's thresholds in the rule's entry of its program file. Keys that are not thresholds
	 * of the rule's type are refused, so the rest of the rule cannot be changed from here.
	 *
	 * @param rule the rule's entry in the file, changed in place only when there is no problem
	 * @param changes threshold to value; a null value removes an optional threshold
	 * @return a description of each problem; when not empty, nothing was changed
	 */
	public static List<String> apply(ObjectNode rule, Map<String, Object> changes) {
		String type = rule.path("type").asText(null);
		List<Field> fields = fields(type);
		List<String> problems = new ArrayList<String>();
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (Field field : fields) {
			JsonNode current = rule.get(field.getKey());
			values.put(field.getKey(), current == null || current.isNull() ? null
					: current.isNumber() ? (Object) current.doubleValue() : current.asText());
		}
		Map<String, Object> parsed = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Object> change : changes.entrySet()) {
			Field field = field(fields, change.getKey());
			if (field == null) {
				problems.add(change.getKey() + " is not a threshold of a " + type + " rule");
				continue;
			}
			String problem = check(field, change.getValue());
			if (problem != null) {
				problems.add(problem);
				continue;
			}
			Object value = parse(field, change.getValue());
			parsed.put(field.getKey(), value);
			values.put(field.getKey(), value);
		}
		problems.addAll(checkTogether(type, values));
		if (!problems.isEmpty()) {
			return problems;
		}
		for (Map.Entry<String, Object> value : parsed.entrySet()) {
			Object v = value.getValue();
			if (v == null) {
				rule.remove(value.getKey());
			} else if (v instanceof Integer) {
				rule.put(value.getKey(), (Integer) v);
			} else if (v instanceof Double) {
				rule.put(value.getKey(), (Double) v);
			} else {
				rule.put(value.getKey(), v.toString());
			}
		}
		return problems;
	}

	private static Field field(List<Field> fields, String key) {
		for (Field field : fields) {
			if (field.getKey().equals(key)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * @return why the value cannot be used for the threshold, or null when it can
	 */
	static String check(Field field, Object value) {
		if (value == null || value.toString().trim().isEmpty()) {
			return field.isOptional() ? null : field.getKey() + " cannot be left empty";
		}
		if (field.getKind() == Kind.CHOICE) {
			return field.getOptions().contains(value.toString()) ? null
					: field.getKey() + " must be one of " + field.getOptions();
		}
		Double number = number(value);
		if (number == null || number.isNaN() || number.isInfinite()) {
			return field.getKey() + " must be a number";
		}
		if (field.getKind() == Kind.COUNT && number != Math.rint(number)) {
			return field.getKey() + " must be a whole number";
		}
		if (number < field.getMin() || number > field.getMax()) {
			return field.getKey() + " must be from " + show(field, field.getMin()) + " to " + show(field, field.getMax());
		}
		return null;
	}

	private static List<String> checkTogether(String type, Map<String, Object> values) {
		List<String> problems = new ArrayList<String>();
		if (DqConfig.RuleDefinition.TYPE_EXPECTED_RANGE.equals(type)) {
			Double low = number(values.get("ratioMin"));
			Double high = number(values.get("ratioMax"));
			if (low != null && high != null && !(low < 1 && 1 < high)) {
				problems.add("the lowest ratio must be below 1 and the highest above 1, or every month would be outside");
			}
		}
		if (DqConfig.RuleDefinition.TYPE_RATIO.equals(type)) {
			Double low = number(values.get("min"));
			Double high = number(values.get("max"));
			if (low == null && high == null) {
				problems.add("set a lowest or a highest ratio, or both");
			} else if (low != null && high != null && low >= high) {
				problems.add("the lowest ratio must be below the highest");
			}
		}
		return problems;
	}

	private static Object parse(Field field, Object value) {
		if (value == null || value.toString().trim().isEmpty()) {
			return null;
		}
		switch (field.getKind()) {
			case CHOICE:
				return value.toString();
			case COUNT:
				return (int) Math.rint(number(value));
			default:
				// Keeps a typed 0.1 as 0.1, not 0.1000000000000001 from a percentage divided by 100
				return Math.round(number(value) * 10000) / 10000.0;
		}
	}

	private static Double number(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value == null) {
			return null;
		}
		try {
			return Double.valueOf(value.toString().trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	private static String show(Field field, double value) {
		if (field.getKind() == Kind.PERCENT) {
			return Math.round(value * 100) + "%";
		}
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

	private static Field percent(String key, double fallback, double min, double max) {
		return new Field(key, Kind.PERCENT, fallback, min, max, false, null);
	}

	private static Field ratio(String key, Double fallback, boolean optional) {
		return new Field(key, Kind.RATIO, fallback, 0, 100, optional, null);
	}

	private static Field count(String key, int fallback, int min, int max) {
		return new Field(key, Kind.COUNT, fallback, min, max, false, null);
	}

	private static Field choice(String key, String fallback, String... options) {
		return new Field(key, Kind.CHOICE, fallback, 0, 0, false, Arrays.asList(options));
	}
}
