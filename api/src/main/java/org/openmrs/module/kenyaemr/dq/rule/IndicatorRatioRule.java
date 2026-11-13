/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq.rule;

import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rule type RATIO: the ratio of two indicators must lie within bounds. For malaria:
 * <ul>
 * <li>T22-04: patients treated (MOH 743) ÷ confirmed cases (MOH 705A/B) must not exceed 1</li>
 * <li>X-RPT-01: suspected cases (MOH 705A/B) against tests in MOH 706 and in MOH 505, 90-110%</li>
 * </ul>
 * Settings: numerator and denominator (indicator codes), or "pairs", a list of {numerator,
 * denominator} each checked on its own; min, max, severity (default WARN).
 */
public class IndicatorRatioRule extends AbstractDqRule {

	public IndicatorRatioRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		DqConfig config = context.getConfig();
		for (String[] pair : pairs()) {
			if (readsRun(context, config.getIndicator(pair[0])) || readsRun(context, config.getIndicator(pair[1]))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		List<DqResult> results = new ArrayList<DqResult>();
		RatioCheck check = new RatioCheck(this);
		for (String[] pair : pairs()) {
			results.add(check.check(context, pair[0], pair[1], settings.getDouble("min", null),
					settings.getDouble("max", null), settings.getOutcome("severity", DqOutcome.WARN)));
		}
		return results;
	}

	/**
	 * @return {numerator, denominator} codes: the "pairs" list, or the single numerator and denominator
	 */
	public List<String[]> pairs() {
		DqRuleSettings settings = settings();
		List<String[]> pairs = new ArrayList<String[]>();
		for (Map<String, Object> pair : settings.getObjectList("pairs")) {
			pairs.add(new String[] { String.valueOf(pair.get("numerator")), String.valueOf(pair.get("denominator")) });
		}
		if (pairs.isEmpty() && settings.has("numerator")) {
			pairs.add(new String[] { settings.getString("numerator", null), settings.getString("denominator", null) });
		}
		return pairs;
	}
}
