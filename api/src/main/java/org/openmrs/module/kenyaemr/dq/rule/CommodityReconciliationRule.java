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
 * Rule type RECONCILIATION: a commodity consumed should match the patients treated with it, within
 * ±tolerance. For malaria, T22-05 (the same check as G-RPT-03, implemented once): ACT dispensed in
 * MOH 643 against patients given an ACT in MOH 743, within 10%.
 * <p>
 * Settings: pairs (a list of {consumed, treated} indicator codes, e.g. one per ACT product),
 * tolerance (default 0.10), severity (default WARN).
 */
public class CommodityReconciliationRule extends AbstractDqRule {

	public CommodityReconciliationRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		DqConfig config = context.getConfig();
		for (Map<String, Object> pair : settings().getObjectList("pairs")) {
			if (readsRun(context, config.getIndicator(String.valueOf(pair.get("consumed"))))
					|| readsRun(context, config.getIndicator(String.valueOf(pair.get("treated"))))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		double tolerance = settings.getDouble("tolerance", 0.10);
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.WARN);

		List<DqResult> results = new ArrayList<DqResult>();
		RatioCheck check = new RatioCheck(this);
		for (Map<String, Object> pair : settings.getObjectList("pairs")) {
			results.add(check.check(context, String.valueOf(pair.get("consumed")), String.valueOf(pair.get("treated")),
					1 - tolerance, 1 + tolerance, severity));
		}
		return results;
	}
}
