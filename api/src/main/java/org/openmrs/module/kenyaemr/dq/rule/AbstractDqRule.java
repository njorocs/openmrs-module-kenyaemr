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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared helpers for building results
 */
public abstract class AbstractDqRule implements DqRule {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/** Tolerance for threshold comparisons, so that a ratio of exactly 1.0 is not failed by rounding */
	protected static final double EPSILON = 1e-9;

	private final DqConfig.RuleDefinition definition;

	protected AbstractDqRule(DqConfig.RuleDefinition definition) {
		this.definition = definition;
	}

	@Override
	public String getId() {
		return definition.getId();
	}

	@Override
	public String getProgram() {
		return definition.getProgramCode();
	}

	@Override
	public DqConfig.RuleDefinition getDefinition() {
		return definition;
	}

	@Override
	public Date checkAfter(DqConfig config, Date month) {
		return DqPeriods.addMonths(DqPeriods.monthStart(month), 1);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.REPORT;
	}

	/**
	 * @return the reports of the indicators the rule names
	 */
	@Override
	public Set<String> getReports(DqConfig config) {
		Set<String> reports = new LinkedHashSet<String>();
		for (String code : definition.getIndicatorCodes()) {
			DqConfig.Indicator indicator = config.getIndicator(code);
			if (indicator != null) {
				reports.addAll(indicator.getReports());
			}
		}
		return reports;
	}

	/**
	 * @return the rule's settings, with an indicator's own settings for this rule layered over them
	 * @param indicator the indicator, or null for the rule's settings only
	 */
	protected DqRuleSettings settings(DqConfig.Indicator indicator) {
		Map<String, Object> merged = new LinkedHashMap<String, Object>(definition.getValues());
		if (indicator != null && indicator.getRules().get(getId()) != null) {
			merged.putAll(indicator.getRules().get(getId()));
		}
		return new DqRuleSettings(merged);
	}

	protected DqRuleSettings settings() {
		return settings(null);
	}

	/**
	 * @return true when the run's report is one of the indicator's reports
	 */
	protected boolean readsRun(DqRuleContext context, DqConfig.Indicator indicator) {
		return indicator != null && context.getRun() != null
				&& indicator.getReports().contains(context.getRun().getReportName());
	}

	protected DqResult result(DqRuleContext context, String indicatorCode, DqOutcome outcome, String message) {
		DqResult result = new DqResult();
		result.setRuleId(getId());
		result.setProgramCode(getProgram());
		result.setParams(json(definition.getValues()));
		result.setIndicatorCode(indicatorCode);
		result.setReportRun(context.getRun());
		result.setPeriodStart(context.getPeriodStart());
		result.setPeriodEnd(context.getPeriodEnd());
		result.setOutcome(outcome);
		result.setMessage(message);
		result.setEvaluatedAt(new Date());
		return result;
	}

	protected static String json(Map<String, Object> detail) {
		try {
			return objectMapper.writeValueAsString(detail);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	protected static String format(Double value) {
		return value == null ? "-" : new DecimalFormat("#,##0.##").format(value);
	}

	protected static String percent(double fraction) {
		return new DecimalFormat("0.#").format(fraction * 100) + "%";
	}
}
