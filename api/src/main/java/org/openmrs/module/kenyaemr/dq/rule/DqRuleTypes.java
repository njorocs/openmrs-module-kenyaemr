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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The rule types a program's rules can use. Each type is written in code; a program's file says
 * which indicators each instance checks and with what thresholds.
 */
public final class DqRuleTypes {

	public static final List<String> TYPES = Arrays.asList(DqConfig.RuleDefinition.TYPE_RATIO,
			DqConfig.RuleDefinition.TYPE_EXPECTED_RANGE, DqConfig.RuleDefinition.TYPE_RECONCILIATION,
			DqConfig.RuleDefinition.TYPE_REPEAT_EPISODE, DqConfig.RuleDefinition.TYPE_CHANGE,
			DqConfig.RuleDefinition.TYPE_TIMELINESS, DqConfig.RuleDefinition.TYPE_KHIS_PARITY);

	private DqRuleTypes() {
	}

	public static boolean isKnown(String type) {
		return type != null && TYPES.contains(type);
	}

	/**
	 * @return the rule for a definition, or null when its type is unknown
	 */
	public static DqRule create(DqConfig.RuleDefinition definition) {
		String type = definition.getType();
		if (DqConfig.RuleDefinition.TYPE_RATIO.equals(type)) {
			return new IndicatorRatioRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_EXPECTED_RANGE.equals(type)) {
			return new ConsistencyOverTimeRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_RECONCILIATION.equals(type)) {
			return new CommodityReconciliationRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_REPEAT_EPISODE.equals(type)) {
			return new DuplicateEpisodeRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_CHANGE.equals(type)) {
			return new PositivityChangeRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_TIMELINESS.equals(type)) {
			return new TimelinessRule(definition);
		}
		if (DqConfig.RuleDefinition.TYPE_KHIS_PARITY.equals(type)) {
			return new KhisParityRule(definition);
		}
		return null;
	}

	/**
	 * @return the rules of every enabled program, of every known type, checked on the schedule; a
	 * rule switched off with "enabled": false is left out
	 */
	public static List<DqRule> rules(DqConfig config, DqRule.Schedule schedule) {
		List<DqRule> rules = new ArrayList<DqRule>();
		for (DqConfig.RuleDefinition definition : config.getRuleDefinitions()) {
			if (!definition.getSettings().isEnabled()) {
				continue;
			}
			DqRule rule = create(definition);
			if (rule != null && rule.getSchedule() == schedule) {
				rules.add(rule);
			}
		}
		return rules;
	}

	/**
	 * @return the rule with this id, enabled or not, or null when no program defines it
	 */
	public static DqRule rule(DqConfig config, String ruleId) {
		DqConfig.RuleDefinition definition = config.getRuleDefinition(ruleId);
		return definition != null ? create(definition) : null;
	}
}
