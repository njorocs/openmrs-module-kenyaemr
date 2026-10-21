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

import org.openmrs.module.kenyaemr.model.dq.DqOutcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Typed access to the settings of one rule, with defaults for anything M&E has not configured
 */
public class DqRuleSettings {

	private final Map<String, Object> values;

	public DqRuleSettings(Map<String, Object> values) {
		this.values = values != null ? values : Collections.<String, Object>emptyMap();
	}

	/**
	 * @return false only when the rule is explicitly disabled
	 */
	public boolean isEnabled() {
		return getBoolean("enabled", true);
	}

	public boolean has(String key) {
		return values.get(key) != null;
	}

	public Double getDouble(String key, Double defaultValue) {
		Object value = values.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.valueOf((String) value);
			}
			catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	public Integer getInteger(String key, Integer defaultValue) {
		Double value = getDouble(key, null);
		return value != null ? Integer.valueOf(value.intValue()) : defaultValue;
	}

	public String getString(String key, String defaultValue) {
		Object value = values.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		Object value = values.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}
		return defaultValue;
	}

	/**
	 * @return the configured severity outcome, e.g. "WARN", or the default when unset or invalid
	 */
	public DqOutcome getOutcome(String key, DqOutcome defaultValue) {
		String value = getString(key, null);
		if (value == null) {
			return defaultValue;
		}
		try {
			return DqOutcome.valueOf(value.trim().toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/**
	 * @return the configured list, or an empty list when unset or not a list
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getList(String key) {
		Object value = values.get(key);
		return value instanceof List ? (List<Object>) value : Collections.emptyList();
	}

	/**
	 * @return a list of objects, such as indicator pairs, or an empty list
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getObjectList(String key) {
		Object value = values.get(key);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		if (value instanceof List) {
			for (Object item : (List<Object>) value) {
				if (item instanceof Map) {
					list.add((Map<String, Object>) item);
				}
			}
		}
		return list;
	}
}
