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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link DqRuleThresholds} and {@link DqConfigStore#findRule}
 */
public class DqRuleThresholdsTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ObjectNode rule(String json) throws Exception {
		return (ObjectNode) objectMapper.readTree(json);
	}

	private static Map<String, Object> changes(Object... keysAndValues) {
		Map<String, Object> changes = new HashMap<String, Object>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			changes.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return changes;
	}

	@Test
	public void apply_shouldSetTheThresholdsAndLeaveTheRestOfTheRule() throws Exception {
		ObjectNode rule = rule("{\"id\":\"T22-03\",\"type\":\"EXPECTED_RANGE\",\"indicators\":[\"MAL_CONFIRMED_705\"],"
				+ "\"baseline\":\"SAME_MONTH_LAST_YEAR\",\"ratioMin\":0.8,\"ratioMax\":1.2,\"severity\":\"TICKET\"}");

		List<String> problems = DqRuleThresholds.apply(rule, changes("ratioMin", 0.7, "ratioMax", "1.3",
				"minHistoryMonths", 6, "baseline", "ROLLING_12_MONTHS"));

		Assert.assertEquals(Collections.<String>emptyList(), problems);
		Assert.assertEquals(0.7, rule.get("ratioMin").doubleValue(), 0);
		Assert.assertEquals(1.3, rule.get("ratioMax").doubleValue(), 0);
		Assert.assertEquals(6, rule.get("minHistoryMonths").intValue());
		Assert.assertTrue(rule.get("minHistoryMonths").isInt());
		Assert.assertEquals("ROLLING_12_MONTHS", rule.get("baseline").asText());
		Assert.assertEquals("TICKET", rule.get("severity").asText());
		Assert.assertEquals("MAL_CONFIRMED_705", rule.get("indicators").get(0).asText());
	}

	@Test
	public void apply_shouldRefuseAnythingButTheTypesThresholds() throws Exception {
		ObjectNode rule = rule("{\"id\":\"T22-03\",\"type\":\"EXPECTED_RANGE\",\"severity\":\"TICKET\",\"ratioMin\":0.8}");

		List<String> problems = DqRuleThresholds.apply(rule, changes("severity", "WARN", "ratioMin", 0.7));

		Assert.assertEquals(1, problems.size());
		Assert.assertTrue(problems.get(0), problems.get(0).startsWith("severity is not a threshold"));
		// Nothing changes when anything is refused
		Assert.assertEquals("TICKET", rule.get("severity").asText());
		Assert.assertEquals(0.8, rule.get("ratioMin").doubleValue(), 0);
	}

	@Test
	public void apply_shouldRefuseValuesOutsideWhatTheThresholdMayTake() throws Exception {
		ObjectNode reconciliation = rule("{\"id\":\"T22-05\",\"type\":\"RECONCILIATION\",\"tolerance\":0.1}");
		Assert.assertFalse(DqRuleThresholds.apply(reconciliation, changes("tolerance", 1.5)).isEmpty());
		Assert.assertFalse(DqRuleThresholds.apply(reconciliation, changes("tolerance", "ten")).isEmpty());
		Assert.assertFalse(DqRuleThresholds.apply(reconciliation, changes("tolerance", null)).isEmpty());
		Assert.assertEquals(0.1, reconciliation.get("tolerance").doubleValue(), 0);

		ObjectNode change = rule("{\"id\":\"T22-08\",\"type\":\"CHANGE\",\"minDenominator\":20}");
		Assert.assertFalse(DqRuleThresholds.apply(change, changes("minDenominator", 2.5)).isEmpty());
		Assert.assertFalse(DqRuleThresholds.apply(change, changes("minDenominator", 0)).isEmpty());

		ObjectNode range = rule("{\"id\":\"T22-03\",\"type\":\"EXPECTED_RANGE\",\"ratioMin\":0.8,\"ratioMax\":1.2}");
		Assert.assertFalse(DqRuleThresholds.apply(range, changes("ratioMin", 1.1)).isEmpty());
		Assert.assertFalse(DqRuleThresholds.apply(range, changes("baseline", "LAST_WEEK")).isEmpty());
	}

	@Test
	public void apply_shouldLetARatioRuleHaveOneBoundButNotNone() throws Exception {
		ObjectNode rule = rule("{\"id\":\"X-RPT-01\",\"type\":\"RATIO\",\"min\":0.9,\"max\":1.1}");

		Assert.assertEquals(Collections.<String>emptyList(), DqRuleThresholds.apply(rule, changes("min", null)));
		Assert.assertFalse(rule.has("min"));
		Assert.assertFalse(DqRuleThresholds.apply(rule, changes("max", "")).isEmpty());
		Assert.assertEquals(1.1, rule.get("max").doubleValue(), 0);
		Assert.assertFalse(DqRuleThresholds.apply(rule, changes("min", 1.2)).isEmpty());
	}

	@Test
	public void apply_shouldKeepATypedPercentageExact() throws Exception {
		ObjectNode rule = rule("{\"id\":\"VF-01\",\"type\":\"VERIFICATION\",\"margin\":0.1}");

		DqRuleThresholds.apply(rule, changes("margin", 15 / 100.0 + 1e-12));

		Assert.assertEquals("0.15", rule.get("margin").asText());
	}

	@Test
	public void fields_shouldCoverEveryThresholdOfTheBundledMalariaRules() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream(DqConfigStore.programResource("malaria"));
		JsonNode file = objectMapper.readTree(in);
		for (JsonNode rule : file.path("program").path("rules")) {
			for (DqRuleThresholds.Field field : DqRuleThresholds.fields(rule.path("type").asText())) {
				JsonNode value = rule.get(field.getKey());
				if (value != null) {
					Object typed = value.isNumber() ? (Object) value.doubleValue() : value.asText();
					Assert.assertNull(rule.path("id").asText() + " " + field.getKey(), DqRuleThresholds.check(field, typed));
				}
			}
		}
		Assert.assertNotNull(DqConfigStore.findRule(file, "T22-03"));
		Assert.assertNull(DqConfigStore.findRule(file, "T22-99"));
		Assert.assertTrue(DqRuleThresholds.fields("TIMELINESS").isEmpty());
	}
}
