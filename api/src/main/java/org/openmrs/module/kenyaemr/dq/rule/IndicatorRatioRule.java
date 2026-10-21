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

import java.util.Collections;
import java.util.List;

/**
 * A rule that bounds the ratio of two configured indicators. Used for:
 * <ul>
 * <li>T22-04: patients treated with an ACT ÷ confirmed cases must not exceed 1 (hard block)</li>
 * <li>X-RPT-01: suspected cases tested (MOH 705A/B) ÷ tests performed (MOH 706) within 90-110%</li>
 * </ul>
 * Settings: numerator, denominator (indicator codes), min, max, severity.
 */
public class IndicatorRatioRule extends AbstractDqRule {

	private final DqOutcome defaultSeverity;

	public IndicatorRatioRule(String id, DqOutcome defaultSeverity) {
		super(id);
		this.defaultSeverity = defaultSeverity;
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		if (!settings.isEnabled()) {
			return false;
		}
		DqConfig config = context.getConfig();
		return readsRun(context, config.getIndicator(settings.getString("numerator", null)))
				|| readsRun(context, config.getIndicator(settings.getString("denominator", null)));
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings(context, null);
		return Collections.singletonList(new RatioCheck(this).check(context,
				settings.getString("numerator", null), settings.getString("denominator", null),
				settings.getDouble("min", null), settings.getDouble("max", null),
				settings.getOutcome("severity", defaultSeverity)));
	}
}
