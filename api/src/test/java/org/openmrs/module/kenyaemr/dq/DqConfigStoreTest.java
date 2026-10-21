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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link DqConfigStore} and {@link DqConfig}
 */
public class DqConfigStoreTest {

	@Test
	public void getDefaultConfig_shouldBeValid() {
		DqConfig config = DqConfigStore.getDefaultConfig();

		Assert.assertEquals(Collections.<String>emptyList(), DqConfigStore.validate(config));
		Assert.assertNotNull(config.getIndicator("MAL_CONFIRMED_743"));
		Assert.assertNotNull(config.getExpectedReport("MOH-743 Report"));
		Assert.assertFalse(config.getOverrideReasons().isEmpty());
		Assert.assertEquals(DqOutcome.BLOCK, config.getRuleSettings("T22-04", null).getOutcome("severity", DqOutcome.WARN));
		Assert.assertFalse(config.getRuleSettings("T22-05", null).isEnabled());
	}

	@Test
	public void validate_shouldFindUnknownIndicatorsAndReports() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		config.getExpectedReports().remove(config.getExpectedReport("MOH 706"));
		Map<String, Object> rule = new HashMap<String, Object>();
		rule.put("numerator", "NO_SUCH_INDICATOR");
		config.getRules().put("X-RPT-01", rule);

		String problems = DqConfigStore.validate(config).toString();

		Assert.assertTrue(problems, problems.contains("MAL_TESTS_706 reads from MOH 706, which is not an expected report"));
		Assert.assertTrue(problems, problems.contains("rule X-RPT-01 refers to unknown indicator NO_SUCH_INDICATOR"));
	}

	@Test
	public void getRuleSettings_shouldLayerIndicatorSettingsOverGlobalOnes() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqConfig.Indicator indicator = config.getIndicator("MAL_CONFIRMED_743");
		Map<String, Object> own = new HashMap<String, Object>();
		own.put("band", 0.35);
		indicator.getRules().put("T22-03", own);

		DqRuleSettings settings = config.getRuleSettings("T22-03", indicator);

		Assert.assertEquals(Double.valueOf(0.35), settings.getDouble("band", null));
		Assert.assertEquals(Double.valueOf(0.8), settings.getDouble("ratioMin", null));
		Assert.assertEquals(Double.valueOf(0.20), config.getRuleSettings("T22-03", null).getDouble("band", null));
	}
}
