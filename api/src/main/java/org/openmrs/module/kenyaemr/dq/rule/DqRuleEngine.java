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
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Runs the rules of every program that apply to a report run, a week, a month or a KHIS read-back
 */
public final class DqRuleEngine {

	private static final Log log = LogFactory.getLog(DqRuleEngine.class);

	public static final String T22_04 = "T22-04";

	public static final String X_RPT_01 = "X-RPT-01";

	private DqRuleEngine() {
	}

	/**
	 * Evaluates every rule that applies. A rule that fails with an error gives a NOT_APPLICABLE
	 * result naming the error, so one broken rule does not stop the others. On a report run, a
	 * block that the rule may not impose on that report is shown as a warning.
	 */
	public static List<DqResult> evaluate(DqRuleContext context, List<DqRule> rules) {
		List<DqResult> results = new ArrayList<DqResult>();
		for (DqRule rule : rules) {
			try {
				if (rule.appliesTo(context)) {
					List<DqResult> ruleResults = rule.evaluate(context);
					if (context.getRun() != null) {
						limitBlocks(context, rule, ruleResults);
					}
					results.addAll(ruleResults);
				}
			}
			catch (RuntimeException e) {
				log.error("Data quality rule " + rule.getId() + " failed", e);
				DqResult result = new DqResult();
				result.setRuleId(rule.getId());
				result.setProgramCode(rule.getProgram());
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
	 * @return why the rule may not block the report, or null when it may. A program blocks only the
	 * reports it owns; and a rule that reads several reports blocks only the one it names in
	 * "gates", since no report is the superior one by default.
	 */
	public static String blockNotAllowed(DqRule rule, DqConfig config, String reportName) {
		DqConfig.Program program = rule.getDefinition().getProgram();
		if (program == null || !program.getOwns().contains(reportName)) {
			return (program != null ? program.getName() : "This program") + " does not own " + reportName;
		}
		Set<String> reports = rule.getReports(config);
		boolean crossReport = reports.size() > 1 || (reports.size() == 1 && !reports.contains(reportName));
		if (crossReport && !reportName.equals(rule.getDefinition().getGates())) {
			return "this check compares several reports and is not set to hold back " + reportName;
		}
		return null;
	}

	private static void limitBlocks(DqRuleContext context, DqRule rule, List<DqResult> results) {
		String reason = null;
		for (DqResult result : results) {
			if (result.getOutcome() != DqOutcome.BLOCK) {
				continue;
			}
			if (reason == null) {
				reason = blockNotAllowed(rule, context.getConfig(), context.getRun().getReportName());
				if (reason == null) {
					return;
				}
			}
			result.setOutcome(DqOutcome.WARN);
			result.setMessage(result.getMessage() + " (A warning, not a block: " + reason + ".)");
		}
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
	 * "ticket": true. Tickets go to the rule's assignee role, else the program owner's.
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
			DqConfig.RuleDefinition definition = config.getRuleDefinition(result.getRuleId());
			String type = definition != null ? definition.getType() : null;
			DqTicket ticket = new DqTicket();
			ticket.setResult(result);
			ticket.setRuleId(result.getRuleId());
			ticket.setProgramCode(result.getProgramCode());
			ticket.setIndicatorCode(result.getIndicatorCode());
			ticket.setPeriodStart(result.getPeriodStart());
			ticket.setEmrValue(result.getNumerator());
			if (DqConfig.RuleDefinition.TYPE_KHIS_PARITY.equals(type)) {
				ticket.setKhisValue(result.getDenominator());
				if (result.getMeasureValue() != null) {
					ticket.setVariancePct(result.getMeasureValue() * 100);
				}
				ticket.setSuggestedCause("Data entered or corrected after sending, or a value KHIS did not import; "
						+ "compare the cells and resend the corrected report");
			} else if (result.getMeasureValue() != null && (DqConfig.RuleDefinition.TYPE_RATIO.equals(type)
					|| DqConfig.RuleDefinition.TYPE_RECONCILIATION.equals(type))) {
				ticket.setVariancePct((result.getMeasureValue() - 1) * 100);
			}
			ticket.setAssigneeRole(definition != null ? definition.getAssigneeRole() : DqTicket.ROLE_HRIO);
			ticket.setStatus(DqTicketStatus.OPEN);
			ticket.setOpenedAt(new Date());
			tickets.add(ticket);
		}
		return tickets;
	}
}
