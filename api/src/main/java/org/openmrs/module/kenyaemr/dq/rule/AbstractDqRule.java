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
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

/**
 * Shared helpers for building results
 */
public abstract class AbstractDqRule implements DqRule {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/** Tolerance for threshold comparisons, so that a ratio of exactly 1.0 is not failed by rounding */
	protected static final double EPSILON = 1e-9;

	private final String id;

	protected AbstractDqRule(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	protected DqRuleSettings settings(DqRuleContext context, DqConfig.Indicator indicator) {
		return context.getConfig().getRuleSettings(id, indicator);
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
		result.setRuleId(id);
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
