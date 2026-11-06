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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
		// MOH 505 is left out of the checks; MOH 643 is checked only for T22-05
		Assert.assertNull(config.getExpectedReport("MOH 505 IDSR Epidemic Case Monitoring"));
		Assert.assertEquals(Double.valueOf(0.10), config.getRuleSettings("T22-05", null).getDouble("tolerance", null));
		Assert.assertEquals("KHIS", config.getExpectedReport("MOH 643").getTarget());
		Assert.assertFalse(config.getRuleSettings("G-RPT-01", null).getReportNames().contains("MOH 643"));

		DqConfig.Program malaria = config.getProgram("malaria");
		Assert.assertEquals("Malaria", malaria.getName());
		Assert.assertEquals(9, malaria.getRules().size());
		Assert.assertTrue(malaria.getDashboard().getCrossChecks().isEmpty());
		DqConfig.Cascade cascade = malaria.getDashboard().getCascade();
		List<String> steps = new java.util.ArrayList<String>();
		for (DqConfig.CascadeStep step : cascade.getSteps()) {
			steps.add(step.getIndicator());
		}
		Assert.assertEquals(Arrays.asList("MAL_SUSPECTED_705", "MAL_TESTS_706", "MAL_POSITIVE_706", "MAL_TREATED_743"), steps);
		Assert.assertEquals("X-RPT-01", cascade.getRuleId());
		Assert.assertEquals("MAL_CONFIRMED_705", malaria.getDashboard().getConsistency().getIndicator());
		Assert.assertTrue(config.getReports(malaria).contains("MOH 711"));
		Assert.assertTrue(config.isTracked(config.getIndicator("MAL_CONFIRMED_705")));
		Assert.assertFalse(config.isTracked(config.getIndicator("MAL_TESTS_706")));
	}

	@Test
	public void merge_shouldLetAProgramFileReplaceReportsIndicatorsAndPrograms() {
		DqConfig config = new DqConfig();
		DqConfig.ExpectedReport knlms = new DqConfig.ExpectedReport();
		knlms.setReport("MOH 643");
		knlms.setTarget("KNLMS");
		config.getExpectedReports().add(knlms);
		DqConfig.Indicator old = new DqConfig.Indicator();
		old.setCode("MAL_TESTED_705");
		config.getIndicators().add(old);

		DqConfigStore.ProgramFile file = new DqConfigStore.ProgramFile();
		DqConfig.ExpectedReport khis = new DqConfig.ExpectedReport();
		khis.setReport("MOH 643");
		file.getExpectedReports().add(khis);
		DqConfig.Indicator updated = new DqConfig.Indicator();
		updated.setCode("MAL_TESTED_705");
		updated.setName("updated");
		file.getIndicators().add(updated);
		DqConfig.Program program = new DqConfig.Program();
		program.setCode("TB");
		file.setProgram(program);

		DqConfigStore.merge(config, file);
		DqConfigStore.merge(config, file);

		Assert.assertEquals(1, config.getExpectedReports().size());
		Assert.assertEquals("KHIS", config.getExpectedReport("MOH 643").getTarget());
		Assert.assertEquals("updated", config.getIndicator("MAL_TESTED_705").getName());
		Assert.assertEquals(1, config.getIndicators().size());
		Assert.assertEquals(1, config.getPrograms().size());
	}

	@Test
	public void validate_shouldFindProgramMistakes() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqConfig.Program malaria = config.getProgram("MALARIA");
		Map<String, Object> duplicate = new HashMap<String, Object>(malaria.getRules().get(0));
		duplicate.put("type", "NOT_A_TYPE");
		duplicate.put("indicators", Arrays.asList("MAL_TYPO"));
		malaria.getRules().add(duplicate);
		Map<String, Object> gated = new HashMap<String, Object>();
		gated.put("id", "X-GATED");
		gated.put("type", "RATIO");
		gated.put("gates", "MOH 706");
		malaria.getRules().add(gated);
		malaria.getIndicators().add("MAL_OTHER_TYPO");
		config.getRules().put("T22-04", new HashMap<String, Object>());

		String problems = DqConfigStore.validate(config).toString();

		Assert.assertTrue(problems, problems.contains("rule id T22-03 is used more than once"));
		Assert.assertTrue(problems, problems.contains("has type NOT_A_TYPE"));
		Assert.assertTrue(problems, problems.contains("refers to unknown indicator MAL_TYPO"));
		Assert.assertTrue(problems, problems.contains("watches unknown indicator MAL_OTHER_TYPO"));
		Assert.assertTrue(problems, problems.contains("gates MOH 706, which program MALARIA does not own"));
		Assert.assertTrue(problems, problems.contains("still has a \"rules\" section"));
	}

	@Test
	public void validate_shouldFindToleranceThatIsNotAFraction() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		List<Map<String, Object>> rules = config.getProgram("MALARIA").getRules();
		for (Map<String, Object> rule : rules) {
			if ("G-RPT-02".equals(rule.get("id"))) {
				rule.put("tolerance", 5);
			} else if ("VF-01".equals(rule.get("id"))) {
				rule.put("tolerance", "10%");
			}
		}

		String problems = DqConfigStore.validate(config).toString();

		Assert.assertTrue(problems, problems.contains("G-RPT-02 of program MALARIA has tolerance 5; write it as a fraction"));
		Assert.assertTrue(problems, problems.contains("VF-01 of program MALARIA has tolerance 10%"));
		Assert.assertFalse(problems, problems.contains("X-RPT-01"));
	}

	@Test
	public void validate_shouldFindUnknownIndicatorsAndReports() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		config.getExpectedReports().remove(config.getExpectedReport("MOH 706"));
		Map<String, Object> rule = new HashMap<String, Object>();
		rule.put("id", "X-RPT-02");
		rule.put("type", "RATIO");
		rule.put("numerator", "NO_SUCH_INDICATOR");
		config.getProgram("MALARIA").getRules().add(rule);

		String problems = DqConfigStore.validate(config).toString();

		Assert.assertTrue(problems, problems.contains("MAL_TESTS_706 reads from MOH 706, which is not an expected report"));
		Assert.assertTrue(problems, problems.contains("rule X-RPT-02 of program MALARIA refers to unknown indicator NO_SUCH_INDICATOR"));
	}

	@Test
	public void getRuleSettings_shouldLayerIndicatorSettingsOverGlobalOnes() {
		DqConfig config = DqConfigStore.getDefaultConfig();
		DqConfig.Indicator indicator = config.getIndicator("MAL_CONFIRMED_705");
		Map<String, Object> own = new HashMap<String, Object>();
		own.put("band", 0.35);
		indicator.getRules().put("T22-03", own);

		DqRuleSettings settings = config.getRuleSettings("T22-03", indicator);

		Assert.assertEquals(Double.valueOf(0.35), settings.getDouble("band", null));
		Assert.assertEquals(Double.valueOf(0.8), settings.getDouble("ratioMin", null));
		Assert.assertEquals(Double.valueOf(0.20), config.getRuleSettings("T22-03", null).getDouble("band", null));
	}
}
