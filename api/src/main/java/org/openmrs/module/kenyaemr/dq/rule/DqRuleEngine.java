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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Runs the rules that apply to a report run (monthly) or to a week (T22-08)
 */
public final class DqRuleEngine {

	private static final Log log = LogFactory.getLog(DqRuleEngine.class);

	public static final String T22_04 = "T22-04";

	public static final String X_RPT_01 = "X-RPT-01";

	private DqRuleEngine() {
	}

	/**
	 * @return the rules checked when a report is generated, hard blocks first
	 */
	public static List<DqRule> monthlyRules() {
		return Arrays.<DqRule>asList(
				new IndicatorRatioRule(T22_04, DqOutcome.BLOCK),
				new ConsistencyOverTimeRule(),
				new CommodityReconciliationRule(),
				new IndicatorRatioRule(X_RPT_01, DqOutcome.WARN),
				new DuplicateEpisodeRule());
	}

	public static List<DqRule> weeklyRules() {
		return Collections.<DqRule>singletonList(new PositivityChangeRule());
	}

	/**
	 * Evaluates every rule that applies. A rule that fails with an error gives a NOT_APPLICABLE
	 * result naming the error, so one broken rule does not stop the others.
	 */
	public static List<DqResult> evaluate(DqRuleContext context, List<DqRule> rules) {
		List<DqResult> results = new ArrayList<DqResult>();
		for (DqRule rule : rules) {
			try {
				if (rule.appliesTo(context)) {
					results.addAll(rule.evaluate(context));
				}
			}
			catch (RuntimeException e) {
				log.error("Data quality rule " + rule.getId() + " failed", e);
				DqResult result = new DqResult();
				result.setRuleId(rule.getId());
				result.setReportRun(context.getRun());
				result.setPeriodStart(context.getPeriodStart());
				result.setPeriodEnd(context.getPeriodEnd());
				result.setOutcome(DqOutcome.NOT_APPLICABLE);
				result.setMessage("The check could not run: " + e.getMessage());
				result.setEvaluatedAt(new Date());
				results.add(result);
			}
		}
		return results;
	}

	/**
	 * @return BLOCKED when any result is a hard block, otherwise AWAITING_APPROVAL
	 */
	public static DqRunStatus statusFor(List<DqResult> results) {
		for (DqResult result : results) {
			if (result.getOutcome() == DqOutcome.BLOCK) {
				return DqRunStatus.BLOCKED;
			}
		}
		return DqRunStatus.AWAITING_APPROVAL;
	}

	/**
	 * @return a ticket for every TICKET result, and for every failed result of a rule configured with
	 * "ticket": true
	 */
	public static List<DqTicket> ticketsFor(List<DqResult> results, DqConfig config) {
		List<DqTicket> tickets = new ArrayList<DqTicket>();
		for (DqResult result : results) {
			DqOutcome outcome = result.getOutcome();
			boolean failed = outcome == DqOutcome.WARN || outcome == DqOutcome.BLOCK;
			boolean wanted = outcome == DqOutcome.TICKET
					|| (failed && config.getRuleSettings(result.getRuleId(), config.getIndicator(result.getIndicatorCode()))
					.getBoolean("ticket", false));
			if (!wanted) {
				continue;
			}
			DqTicket ticket = new DqTicket();
			ticket.setResult(result);
			ticket.setRuleId(result.getRuleId());
			ticket.setIndicatorCode(result.getIndicatorCode());
			ticket.setPeriodStart(result.getPeriodStart());
			ticket.setEmrValue(result.getNumerator());
			if (result.getMeasureValue() != null && (X_RPT_01.equals(result.getRuleId())
					|| CommodityReconciliationRule.ID.equals(result.getRuleId()))) {
				ticket.setVariancePct((result.getMeasureValue() - 1) * 100);
			}
			ticket.setAssigneeRole(PositivityChangeRule.ID.equals(result.getRuleId()) ? DqTicket.ROLE_SCHRIO : DqTicket.ROLE_HRIO);
			ticket.setStatus(DqTicketStatus.OPEN);
			ticket.setOpenedAt(new Date());
			tickets.add(ticket);
		}
		return tickets;
	}
}
