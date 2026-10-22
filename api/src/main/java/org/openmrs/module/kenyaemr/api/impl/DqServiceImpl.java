/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.api.db.DqDao;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqReportGenerator;
import org.openmrs.module.kenyaemr.dq.ServiceDqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.DqRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleEngine;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportPayload;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link DqService}
 */
public class DqServiceImpl extends BaseOpenmrsService implements DqService {

	/** The override reason that needs a free-text note */
	static final String OTHER_REASON = "OTHER";

	private DqDao dqDao;

	public void setDqDao(DqDao dqDao) {
		this.dqDao = dqDao;
	}

	@Override
	public DqReportRun generateReportRun(String reportName, Date periodStart) {
		return generateReportRun(reportName, periodStart, true);
	}

	@Override
	public DqReportRun generateReportRun(String reportName, Date periodStart, boolean evaluateRules) {
		DqReportGenerator.GeneratedRun generated = new DqReportGenerator().generate(reportName, periodStart);
		DqReportRun run = generated.getRun();

		DqReportRun previous = dqDao.getLatestReportRun(reportName, run.getPeriodStart());
		if (previous != null && previous.getStatus() != DqRunStatus.SUPERSEDED) {
			previous.setStatus(DqRunStatus.SUPERSEDED);
			dqDao.saveReportRun(previous);
		}

		run.setVersion(previous != null ? previous.getVersion() + 1 : 1);
		// Left empty when the scheduled task generates the run
		if (Context.isAuthenticated() && !Daemon.isDaemonThread()) {
			run.setGeneratedBy(Context.getAuthenticatedUser());
		}
		dqDao.saveReportRun(run);
		dqDao.saveValues(generated.getValues());
		if (generated.getPayload() != null) {
			dqDao.savePayload(generated.getPayload());
		}
		if (evaluateRules) {
			evaluateReportRun(run);
		}
		return run;
	}

	@Override
	public List<DqResult> evaluateReportRun(DqReportRun run) {
		if (run.getStatus() != DqRunStatus.GENERATED) {
			throw new APIException("Version " + run.getVersion() + " of " + run.getReportName() + " has already been "
					+ "checked (" + run.getStatus() + "). Regenerate the report to check it again.");
		}
		DqConfig config = DqConfigStore.getConfig();
		DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(), run.getPeriodEnd());
		List<DqResult> results = DqRuleEngine.evaluate(context, DqRuleEngine.monthlyRules());
		saveResultsAndTickets(results, config);

		run.setStatus(DqRuleEngine.statusFor(results));
		dqDao.saveReportRun(run);
		return results;
	}

	@Override
	public DqReportRun approveReportRun(DqReportRun run, List<DqOverride> overrides) {
		if (run.getStatus() == DqRunStatus.BLOCKED) {
			List<String> blocks = new ArrayList<String>();
			for (DqResult result : dqDao.getResults(run)) {
				if (result.getOutcome() == DqOutcome.BLOCK) {
					blocks.add(result.getRuleId() + ": " + result.getMessage());
				}
			}
			throw new APIException("This report cannot be approved while a hard block is open. Correct the records and "
					+ "regenerate it. " + StringUtils.join(blocks, " "));
		}
		if (run.getStatus() != DqRunStatus.AWAITING_APPROVAL) {
			throw new APIException("Version " + run.getVersion() + " of " + run.getReportName() + " cannot be approved: it is "
					+ run.getStatus() + ".");
		}

		DqConfig config = DqConfigStore.getConfig();
		Set<String> overridden = new HashSet<String>();
		for (DqOverride existing : dqDao.getOverrides(run)) {
			overridden.add(existing.getResult().getUuid());
		}
		List<DqOverride> toSave = new ArrayList<DqOverride>();
		for (DqOverride override : overrides != null ? overrides : Collections.<DqOverride>emptyList()) {
			DqResult result = override.getResult();
			if (result == null || result.getReportRun() == null || !run.getId().equals(result.getReportRun().getId())) {
				throw new APIException("An override refers to a result that does not belong to this report.");
			}
			if (result.getOutcome() != DqOutcome.WARN) {
				throw new APIException("Only warnings can be overridden; " + result.getRuleId() + " is " + result.getOutcome() + ".");
			}
			if (!config.getOverrideReasons().contains(override.getReasonCode())) {
				throw new APIException("Reason " + override.getReasonCode() + " is not one of " + config.getOverrideReasons() + ".");
			}
			if (OTHER_REASON.equals(override.getReasonCode()) && StringUtils.isBlank(override.getNote())) {
				throw new APIException("A note is required when the reason is " + OTHER_REASON + ".");
			}
			if (overridden.add(result.getUuid())) {
				toSave.add(override);
			}
		}

		List<String> unanswered = new ArrayList<String>();
		for (DqResult result : dqDao.getResults(run)) {
			if (result.getOutcome() == DqOutcome.WARN && !overridden.contains(result.getUuid())) {
				unanswered.add(result.getRuleId() + (result.getIndicatorCode() != null ? " (" + result.getIndicatorCode() + ")" : ""));
			}
		}
		if (!unanswered.isEmpty()) {
			throw new APIException("Give a reason for each warning before approving: " + StringUtils.join(unanswered, ", ") + ".");
		}

		Date now = new Date();
		for (DqOverride override : toSave) {
			override.setCreator(Context.getAuthenticatedUser());
			override.setDateCreated(now);
			dqDao.saveOverride(override);
		}
		run.setStatus(DqRunStatus.APPROVED);
		run.setApprovedBy(Context.getAuthenticatedUser());
		run.setApprovedAt(now);
		run.setApprovalMode(DqReportRun.APPROVAL_MODE_MANUAL);
		return dqDao.saveReportRun(run);
	}

	@Override
	public List<DqResult> evaluateWeek(Date anyDateInWeek) {
		Date weekStart = DqPeriods.weekStart(anyDateInWeek);
		List<DqResult> existing = new ArrayList<DqResult>();
		for (DqRule rule : DqRuleEngine.weeklyRules()) {
			existing.addAll(dqDao.getResults(rule.getId(), weekStart, weekStart));
		}
		if (!existing.isEmpty()) {
			return existing;
		}
		DqConfig config = DqConfigStore.getConfig();
		DqRuleContext context = new ServiceDqRuleContext(this, config, null, weekStart, DqPeriods.weekEnd(weekStart));
		List<DqResult> results = DqRuleEngine.evaluate(context, DqRuleEngine.weeklyRules());
		saveResultsAndTickets(results, config);
		return results;
	}

	private void saveResultsAndTickets(List<DqResult> results, DqConfig config) {
		for (DqResult result : results) {
			dqDao.saveResult(result);
		}
		for (DqTicket ticket : DqRuleEngine.ticketsFor(results, config)) {
			dqDao.saveTicket(ticket);
		}
	}

	@Override
	public DqReportRun saveReportRun(DqReportRun run) {
		return dqDao.saveReportRun(run);
	}

	@Override
	public DqReportRun getReportRunByUuid(String uuid) {
		return dqDao.getReportRunByUuid(uuid);
	}

	@Override
	public DqReportRun getLatestReportRun(String reportName, Date periodStart) {
		return dqDao.getLatestReportRun(reportName, periodStart);
	}

	@Override
	public List<DqReportRun> getLatestReportRuns(Date periodStart) {
		return dqDao.getLatestReportRuns(periodStart);
	}

	@Override
	public List<DqReportRun> getLatestReportRuns(String reportName, Date fromPeriod, Date toPeriod) {
		return dqDao.getLatestReportRuns(reportName, fromPeriod, toPeriod);
	}

	@Override
	public void saveValues(Collection<DqValue> values) {
		dqDao.saveValues(values);
	}

	@Override
	public List<DqValue> getValues(DqReportRun run) {
		return dqDao.getValues(run);
	}

	@Override
	public DqReportPayload getPayload(DqReportRun run) {
		return dqDao.getPayload(run);
	}

	@Override
	public List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod) {
		return dqDao.getLatestValues(reportNames, fromPeriod, toPeriod);
	}

	@Override
	public DqResult saveResult(DqResult result) {
		return dqDao.saveResult(result);
	}

	@Override
	public DqResult getResultByUuid(String uuid) {
		return dqDao.getResultByUuid(uuid);
	}

	@Override
	public List<DqResult> getResults(DqReportRun run) {
		return dqDao.getResults(run);
	}

	@Override
	public List<DqResult> getResults(String ruleId, Date fromPeriod, Date toPeriod) {
		return dqDao.getResults(ruleId, fromPeriod, toPeriod);
	}

	@Override
	public DqOverride saveOverride(DqOverride override) {
		return dqDao.saveOverride(override);
	}

	@Override
	public List<DqOverride> getOverrides(DqReportRun run) {
		return dqDao.getOverrides(run);
	}

	@Override
	public DqTicket saveTicket(DqTicket ticket) {
		return dqDao.saveTicket(ticket);
	}

	@Override
	public DqTicket getTicketByUuid(String uuid) {
		return dqDao.getTicketByUuid(uuid);
	}

	@Override
	public List<DqTicket> getTickets(DqTicketStatus status) {
		return dqDao.getTickets(status);
	}

	@Override
	public List<Object[]> executeSqlQuery(String sql, Map<String, Object> parameters) {
		return dqDao.executeSqlQuery(sql, parameters);
	}
}
