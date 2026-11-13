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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.api.db.DqDao;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqReportGenerator;
import org.openmrs.module.kenyaemr.dq.DqSendGate;
import org.openmrs.module.kenyaemr.dq.DqSendGates;
import org.openmrs.module.kenyaemr.dq.DqAccess;
import org.openmrs.module.kenyaemr.dq.ServiceDqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleEngine;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleTypes;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportPayload;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.module.reporting.report.ReportData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link DqService}
 */
public class DqServiceImpl extends BaseOpenmrsService implements DqService {

	private static final Log log = LogFactory.getLog(DqServiceImpl.class);

	/** The override reason that needs a free-text note */
	static final String OTHER_REASON = "OTHER";

	/** Recorded on warnings that were never answered when a report is approved automatically */
	public static final String UNREVIEWED_REASON = "UNREVIEWED";

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
		return saveGeneratedRun(new DqReportGenerator().generate(reportName, periodStart), evaluateRules);
	}

	/** The reports and months being checked after a run on the reports page, see {@link #getSendGate} */
	private static final Set<String> CAPTURING = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	/** How long after a report finishes its checks are taken to be starting */
	static final long CHECKS_START_WITHIN_MS = 2 * 60 * 1000L;

	@Override
	public DqReportRun captureReportRun(String reportDefinitionUuid, ReportData reportData, Date start, Date end) {
		Date now = new Date();
		// Cheap date checks first, as reading the figures takes longer
		if (!DqSendGates.isChecked(start, end, now) && !DqSendGates.isCheckedWeek(start, end, now)) {
			return null;
		}
		DqReportGenerator.GeneratedRun generated = new DqReportGenerator().fromReportData(reportDefinitionUuid, reportData,
				start, end);
		DqConfig.ExpectedReport expected = generated != null ? expectedReport(generated.getRun().getReportName()) : null;
		boolean month = DqSendGates.isChecked(start, end, now);
		if (expected == null || (!month && !expected.isWeekly())) {
			return null;
		}
		DqReportRun run = generated.getRun();
		if (!month) {
			// A week is stored by its days, so it is found again whatever time the end date had
			run.setPeriodEnd(DqPeriods.dayOf(end));
		}
		String key = captureKey(run.getReportName(), run.getPeriodStart(), run.getPeriodEnd());
		CAPTURING.add(key);
		try {
			DqReportRun latest = latestOf(run.getReportName(), run.getPeriodStart(), run.getPeriodEnd());
			if (latest != null && StringUtils.equals(latest.getValuesHash(), run.getValuesHash())) {
				// The same figures, stored unchecked by a backfill, are checked now, as they may be sent
				if (latest.getStatus() == DqRunStatus.GENERATED) {
					evaluateReportRun(latest);
					return latest;
				}
				return null;
			}
			return saveGeneratedRun(generated, true);
		}
		finally {
			CAPTURING.remove(key);
		}
	}

	private static String captureKey(String reportName, Date periodStart, Date periodEnd) {
		return reportName + "|" + periodStart.getTime() + "|" + DqPeriods.dayOf(periodEnd).getTime();
	}

	/**
	 * @return the latest version for the month, or for the week when the dates are a week
	 */
	private DqReportRun latestOf(String reportName, Date periodStart, Date periodEnd) {
		return DqPeriods.isWholeMonth(periodStart, periodEnd) ? dqDao.getLatestReportRun(reportName, periodStart)
				: dqDao.getLatestReportRun(reportName, periodStart, DqPeriods.dayOf(periodEnd));
	}

	@Override
	public DqSendGate getSendGate(String reportName, Date periodStart, Date periodEnd, String valuesHash, Date completedAt) {
		DqConfig.ExpectedReport expected = expectedReport(reportName);
		if (expected == null) {
			return DqSendGate.notCheckedHere();
		}
		Date now = new Date();
		boolean month = DqSendGates.isChecked(periodStart, periodEnd, now);
		boolean week = expected.isWeekly() && DqSendGates.isCheckedWeek(periodStart, periodEnd, now);
		if (!month && !week) {
			// A report sent a week at a time is held whatever its dates; the others as before
			return expected.isWeekly() ? DqSendGates.wrongPeriod(reportName, periodStart, periodEnd, now)
					: DqSendGate.notCheckedHere();
		}
		DqReportRun latest = latestOf(reportName, periodStart, periodEnd);
		boolean same = latest != null && StringUtils.equals(latest.getValuesHash(), valuesHash);
		boolean starting = completedAt != null && now.getTime() - completedAt.getTime() < CHECKS_START_WITHIN_MS;
		List<DqResult> results = same && latest.getStatus() != DqRunStatus.GENERATED
				? dqDao.getResults(latest)
				: Collections.<DqResult>emptyList();
		DqSendGate gate = DqSendGates.gate(latest, same,
				CAPTURING.contains(captureKey(reportName, periodStart, periodEnd)) || starting, results);
		return gate.withResolvedTickets(resolvedTickets(results));
	}

	/**
	 * @return the gate messages of the TICKET results whose ticket has been resolved: a ticket was
	 * raised for the finding and none of its tickets is still open or in progress
	 */
	private List<String> resolvedTickets(List<DqResult> results) {
		List<String> resolved = new ArrayList<String>();
		for (DqResult result : results) {
			if (result.getOutcome() != DqOutcome.TICKET) {
				continue;
			}
			boolean raised = false;
			boolean open = false;
			for (DqTicket ticket : dqDao.getTickets(result.getRuleId(), result.getPeriodStart())) {
				if (isFinding(ticket, result.getProgramCode(), result.getRuleId(), result.getIndicatorCode(),
						result.getPeriodStart())) {
					raised = true;
					open |= ticket.getStatus() != DqTicketStatus.RESOLVED;
				}
			}
			if (raised && !open) {
				resolved.add(DqSendGates.message(result));
			}
		}
		return resolved;
	}

	@Override
	public DqReportRun getLatestReportRun(String reportName, Date periodStart, Date periodEnd) {
		return dqDao.getLatestReportRun(reportName, periodStart, DqPeriods.dayOf(periodEnd));
	}

	@Override
	public List<DqReportRun> getLatestWeeklyRuns(Date fromPeriod, Date toPeriod) {
		return dqDao.getLatestWeeklyRuns(fromPeriod, toPeriod);
	}

	private static DqConfig.ExpectedReport expectedReport(String reportName) {
		for (DqConfig.ExpectedReport report : DqConfigStore.getConfig().getExpectedReports()) {
			if (report.getReport().equals(reportName)
					&& DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency())) {
				return report;
			}
		}
		return null;
	}

	private DqReportRun saveGeneratedRun(DqReportGenerator.GeneratedRun generated, boolean evaluateRules) {
		DqReportRun run = generated.getRun();

		DqReportRun previous = latestOf(run.getReportName(), run.getPeriodStart(), run.getPeriodEnd());
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
		// A month already verified (VF-01) is verified again against the new version
		evaluateMonth(run.getPeriodStart(), new Date(), true);
		recheckReading(run);
		return run;
	}

	/**
	 * Checks again, with the latest values, every result the new version's values feed, so that
	 * the run review, the tickets and the trends, which work their figures out from the latest
	 * values, give an indicator the same figures:
	 * <ul>
	 * <li>a month: the same month's checks saved with its other reports that read this one, e.g. a
	 * cross check of MOH 705A against MOH 706 saved with the MOH 706 version; and T22-03 in the 12
	 * months after, whose 12-month average or same month of the previous year this month is</li>
	 * <li>a week: the week's checks (T22-08) and the next week's, which compares with this one</li>
	 * </ul>
	 * Versions not yet checked are left to their own check. The month's MONTH checks (VF-01) are
	 * checked again by {@link #evaluateMonth(Date, Date, boolean)}.
	 */
	private void recheckReading(DqReportRun generated) {
		DqConfig config = DqConfigStore.getConfig();
		String report = generated.getReportName();
		if (DqPeriods.isWholeWeek(generated.getPeriodStart(), generated.getPeriodEnd())) {
			Date week = DqPeriods.weekStart(generated.getPeriodStart());
			evaluateWeek(week, true);
			evaluateWeek(DqPeriods.addDays(week, 7), true);
			// The week's other versions, and the next week's, as their run reviews show them. A
			// week's version raises no tickets, so none are raised or closed here.
			for (DqReportRun run : dqDao.getLatestWeeklyRuns(week, DqPeriods.addDays(week, 7))) {
				if (run.getUuid().equals(generated.getUuid()) || run.getStatus() == DqRunStatus.GENERATED
						|| run.getStatus() == DqRunStatus.SUPERSEDED) {
					continue;
				}
				List<DqRule> reading = new ArrayList<DqRule>();
				Set<String> ruleIds = new HashSet<String>();
				for (DqRule rule : weekRules(config, run.getReportName())) {
					if (rule.readsReportValues() && rule.getReports(config).contains(report)) {
						reading.add(rule);
						ruleIds.add(rule.getId());
					}
				}
				List<DqResult> previous = readingReport(dqDao.getResults(run), ruleIds, config, report);
				if (previous.isEmpty()) {
					continue;
				}
				DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(),
						DqPeriods.weekEnd(run.getPeriodStart()));
				List<DqResult> results = readingReport(DqRuleEngine.evaluate(context, reading), ruleIds, config, report);
				for (DqResult result : replace(results, previous, new ArrayList<DqResult>())) {
					dqDao.saveResult(result);
				}
			}
			return;
		}
		List<DqRule> rules = new ArrayList<DqRule>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.REPORT)) {
			// A rule checked with each version reads the values of the reports of its indicators
			if (rule.getReports(config).contains(report)) {
				rules.add(rule);
			}
		}
		Date month = DqPeriods.monthStart(generated.getPeriodStart());
		for (int i = 0; i <= 12; i++) {
			// Only T22-03 reads the months before
			List<DqRule> reading = new ArrayList<DqRule>();
			for (DqRule rule : rules) {
				if (i == 0 || rule instanceof ConsistencyOverTimeRule) {
					reading.add(rule);
				}
			}
			if (reading.isEmpty()) {
				return;
			}
			Set<String> ruleIds = new HashSet<String>();
			for (DqRule rule : reading) {
				ruleIds.add(rule.getId());
			}
			for (DqReportRun run : dqDao.getLatestReportRuns(DqPeriods.addMonths(month, i))) {
				if (run.getUuid().equals(generated.getUuid()) || run.getStatus() == DqRunStatus.GENERATED
						|| run.getStatus() == DqRunStatus.SUPERSEDED) {
					continue;
				}
				List<DqResult> previous = readingReport(dqDao.getResults(run), ruleIds, config, report);
				if (previous.isEmpty()) {
					continue;
				}
				DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(), run.getPeriodEnd());
				recheck(readingReport(DqRuleEngine.evaluate(context, reading), ruleIds, config, report), previous, config);
			}
		}
	}

	@Override
	public int refreshOpenTickets() {
		DqConfig config = DqConfigStore.getConfig();
		Map<String, DqRule> reportRules = new HashMap<String, DqRule>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.REPORT)) {
			// The rules checked with each version that read the values of their indicators' reports
			if (!rule.getReports(config).isEmpty()) {
				reportRules.put(rule.getId(), rule);
			}
		}
		Set<String> weekRules = new HashSet<String>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.WEEK)) {
			weekRules.add(rule.getId());
		}
		Date now = new Date();
		int refreshed = 0;
		Set<String> monthsChecked = new HashSet<String>();
		for (DqTicket ticket : dqDao.getTickets(null)) {
			if (ticket.getStatus() == DqTicketStatus.RESOLVED || ticket.getResult() == null) {
				continue;
			}
			String gone = noLongerChecked(ticket, config);
			if (gone != null) {
				dqDao.saveTicket(close(ticket, now, "Closed automatically: " + gone + ", so it is no longer checked"));
				refreshed++;
				continue;
			}
			DqRule rule = reportRules.get(ticket.getRuleId());
			if (rule == null) {
				// A month's or week's checks are checked again when their reports have a newer version
				String key = ticket.getRuleId() + "@" + ticket.getPeriodStart().getTime();
				if (monthsChecked.add(key)) {
					if (weekRules.contains(ticket.getRuleId())) {
						evaluateWeek(ticket.getPeriodStart(), true);
					} else {
						evaluateMonth(ticket.getPeriodStart(), now, true);
					}
				}
				continue;
			}
			if (refreshTicket(ticket, rule, config)) {
				refreshed++;
			}
		}
		return refreshed;
	}

	/**
	 * Checks a report rule's ticket again with the latest values, in every checked latest version of
	 * the rule's reports for the month, so that each version's run review shows the same figures;
	 * with the version the ticket's result belongs to when none of them is checked
	 *
	 * @return true when the ticket's finding was checked again
	 */
	private boolean refreshTicket(DqTicket ticket, DqRule rule, DqConfig config) {
		DqResult raised = ticket.getResult();
		Set<String> reports = rule.getReports(config);
		List<DqReportRun> runs = new ArrayList<DqReportRun>();
		for (DqReportRun candidate : dqDao.getLatestReportRuns(DqPeriods.monthStart(ticket.getPeriodStart()))) {
			if (reports.contains(candidate.getReportName()) && candidate.getStatus() != DqRunStatus.GENERATED
					&& candidate.getStatus() != DqRunStatus.SUPERSEDED) {
				runs.add(candidate);
			}
		}
		if (runs.isEmpty() && raised.getReportRun() != null) {
			runs.add(raised.getReportRun());
		}
		List<DqResult> saved = new ArrayList<DqResult>();
		for (DqReportRun run : runs) {
			if (DqPeriods.isWholeWeek(run.getPeriodStart(), run.getPeriodEnd())) {
				continue;
			}
			List<DqResult> previous = new ArrayList<DqResult>();
			for (DqResult result : dqDao.getResults(run)) {
				if (rule.getId().equals(result.getRuleId())) {
					previous.add(result);
				}
			}
			DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(), run.getPeriodEnd());
			saved.addAll(recheck(DqRuleEngine.evaluate(context, Collections.singletonList(rule)), previous, config));
		}
		// The ticket's own finding, which may be held by another version than the one checked now
		for (DqResult result : saved) {
			if (!isFinding(ticket, result.getProgramCode(), result.getRuleId(), result.getIndicatorCode(),
					result.getPeriodStart()) || !StringUtils.equals(baselineOf(result), baselineOf(raised))) {
				continue;
			}
			if (ticket.getStatus() == DqTicketStatus.RESOLVED) {
				return true;
			}
			if (result.getOutcome() == DqOutcome.PASS) {
				dqDao.saveTicket(closeAsPassed(ticket, new Date()));
			} else {
				List<DqTicket> asRaised = DqRuleEngine.ticketsFor(Collections.singletonList(result), config);
				if (asRaised.isEmpty()) {
					// Still outside the range but no longer ticketed, e.g. the rule's severity changed
					ticket.setResult(result);
					ticket.setEmrValue(result.getNumerator());
					dqDao.saveTicket(ticket);
				} else {
					dqDao.saveTicket(mergeInto(ticket, asRaised.get(0)));
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Puts the results checked again in place of the previous ones and saves them; raises or closes
	 * the tickets of those whose outcome changed, and gives the open tickets of the others the new
	 * figures
	 *
	 * @return the results saved
	 */
	private List<DqResult> recheck(List<DqResult> results, List<DqResult> previous, DqConfig config) {
		List<DqResult> changed = new ArrayList<DqResult>();
		List<DqResult> saved = replace(results, previous, changed);
		for (DqResult result : saved) {
			dqDao.saveResult(result);
		}
		followUp(changed, config);
		List<DqResult> unchanged = new ArrayList<DqResult>(saved);
		unchanged.removeAll(changed);
		for (DqTicket raised : DqRuleEngine.ticketsFor(unchanged, config)) {
			// Only a ticket still open: one resolved stays resolved
			DqTicket open = sameFinding(dqDao.getUnresolvedTickets(raised.getRuleId(), raised.getPeriodStart()), raised);
			if (open != null) {
				dqDao.saveTicket(mergeInto(open, raised));
			}
		}
		return saved;
	}

	/**
	 * @return the results of the rules about an indicator counted from the report; a cross check's
	 * indicator, numerator/denominator, when either is
	 */
	static List<DqResult> readingReport(List<DqResult> results, Set<String> ruleIds, DqConfig config, String reportName) {
		List<DqResult> reading = new ArrayList<DqResult>();
		for (DqResult result : results) {
			if (!ruleIds.contains(result.getRuleId()) || result.getIndicatorCode() == null) {
				continue;
			}
			for (String code : result.getIndicatorCode().split("/")) {
				DqConfig.Indicator indicator = config.getIndicator(code);
				if (indicator != null && indicator.getReports().contains(reportName)) {
					reading.add(result);
					break;
				}
			}
		}
		return reading;
	}

	@Override
	public List<DqResult> evaluateReportRun(DqReportRun run) {
		if (run.getStatus() != DqRunStatus.GENERATED) {
			throw new APIException("Version " + run.getVersion() + " of " + run.getReportName() + " has already been "
					+ "checked (" + run.getStatus() + "). Regenerate the report to check it again.");
		}
		DqConfig config = DqConfigStore.getConfig();
		List<DqResult> results;
		if (DqPeriods.isWholeWeek(run.getPeriodStart(), run.getPeriodEnd())) {
			// A week is checked by the week's checks of the programs that read the report. Their tickets
			// are raised once, by the daily task's own check of the week, so none are raised here.
			DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(),
					DqPeriods.weekEnd(run.getPeriodStart()));
			results = DqRuleEngine.evaluate(context, weekRules(config, run.getReportName()));
			for (DqResult result : results) {
				dqDao.saveResult(result);
			}
		} else {
			DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(), run.getPeriodEnd());
			results = DqRuleEngine.evaluate(context, DqRuleTypes.rules(config, DqRule.Schedule.REPORT));
			saveResultsAndTickets(results, config);
		}

		run.setStatus(DqRuleEngine.statusFor(results));
		dqDao.saveReportRun(run);
		return results;
	}

	/**
	 * @return the WEEK rules of the programs that read the report
	 */
	static List<DqRule> weekRules(DqConfig config, String reportName) {
		Set<String> programs = new HashSet<String>();
		for (DqConfig.Program program : config.getPrograms()) {
			if (config.getReports(program).contains(reportName)) {
				programs.add(program.getCode());
			}
		}
		List<DqRule> rules = new ArrayList<DqRule>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.WEEK)) {
			if (programs.contains(rule.getProgram())) {
				rules.add(rule);
			}
		}
		return rules;
	}

	@Override
	public DqReportRun approveReportRun(DqReportRun run, List<DqOverride> overrides) {
		if (!DqAccess.mayApprove()) {
			throw new APIAuthenticationException("Approving a report needs the \"" + PRIVILEGE_APPROVE
					+ "\" privilege or one of the roles " + Arrays.toString(RUN_ROLES) + ".");
		}
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
	public List<DqReportRun> autoApproveDueReports(Date now) {
		Date month = DqPeriods.previousMonth(now);
		List<DqReportRun> approved = new ArrayList<DqReportRun>();
		for (DqConfig.ExpectedReport report : DqConfigStore.getConfig().getExpectedReports()) {
			if (!isDeadlineReached(report, month, now)) {
				continue;
			}
			DqReportRun run = dqDao.getLatestReportRun(report.getReport(), month);
			if (run == null || run.getStatus() != DqRunStatus.AWAITING_APPROVAL) {
				continue;
			}
			approved.add(autoApprove(run, now));
		}
		return approved;
	}

	/**
	 * @return true from the start of a monthly report's deadline day for that month
	 */
	static boolean isDeadlineReached(DqConfig.ExpectedReport report, Date month, Date now) {
		return DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency()) && report.getDeadlineDay() != null
				&& !now.before(DqPeriods.deadline(month, report.getDeadlineDay()));
	}

	private DqReportRun autoApprove(DqReportRun run, Date now) {
		Set<String> overridden = new HashSet<String>();
		for (DqOverride existing : dqDao.getOverrides(run)) {
			overridden.add(existing.getResult().getUuid());
		}
		for (DqResult result : dqDao.getResults(run)) {
			if (result.getOutcome() == DqOutcome.WARN && !overridden.contains(result.getUuid())) {
				DqOverride override = new DqOverride();
				override.setResult(result);
				override.setReasonCode(UNREVIEWED_REASON);
				override.setNote("Approved automatically on the deadline; nobody reviewed this warning.");
				override.setCreator(Context.getAuthenticatedUser());
				override.setDateCreated(now);
				dqDao.saveOverride(override);
			}
		}
		run.setStatus(DqRunStatus.APPROVED);
		run.setApprovedAt(now);
		run.setApprovalMode(DqReportRun.APPROVAL_MODE_AUTO);
		return dqDao.saveReportRun(run);
	}

	@Override
	public List<DqResult> evaluateWeek(Date anyDateInWeek) {
		return evaluateWeek(anyDateInWeek, false);
	}

	/**
	 * Checks the week's WEEK rules not yet checked and checks again those whose reports, of the week
	 * or the week before it compares with, have a newer version than the check. A result checked
	 * again is updated in place, so its ticket stays with it.
	 *
	 * @param staleOnly true to check again only, leaving rules never checked to the scheduled task
	 */
	private List<DqResult> evaluateWeek(Date anyDateInWeek, boolean staleOnly) {
		Date weekStart = DqPeriods.weekStart(anyDateInWeek);
		DqConfig config = DqConfigStore.getConfig();
		List<DqResult> existing = new ArrayList<DqResult>();
		List<DqRule> unchecked = new ArrayList<DqRule>();
		List<DqRule> stale = new ArrayList<DqRule>();
		List<DqResult> previous = new ArrayList<DqResult>();
		List<DqReportRun> runs = null;
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.WEEK)) {
			List<DqResult> results = dqDao.getResults(rule.getId(), weekStart, weekStart);
			if (results.isEmpty()) {
				if (!staleOnly) {
					unchecked.add(rule);
				}
				continue;
			}
			// A week checked with other settings, e.g. other indicators, is checked again
			if (settingsChanged(rule, results)) {
				stale.add(rule);
				previous.addAll(results);
				continue;
			}
			if (rule.readsReportValues()) {
				if (runs == null) {
					runs = dqDao.getLatestWeeklyRuns(DqPeriods.addDays(weekStart, -7), weekStart);
				}
				if (isStale(results, rule.getReports(config), runs)) {
					stale.add(rule);
					previous.addAll(results);
					continue;
				}
			}
			existing.addAll(results);
		}
		DqRuleContext context = new ServiceDqRuleContext(this, config, null, weekStart, DqPeriods.weekEnd(weekStart));
		if (!stale.isEmpty()) {
			existing.addAll(recheck(DqRuleEngine.evaluate(context, stale), previous, config));
		}
		if (!unchecked.isEmpty()) {
			List<DqResult> results = DqRuleEngine.evaluate(context, unchecked);
			saveResultsAndTickets(results, config);
			existing.addAll(results);
		}
		return existing;
	}

	@Override
	public List<DqResult> evaluateMonth(Date month, Date now) {
		return evaluateMonth(month, now, false);
	}

	@Override
	public List<DqResult> recheckTimeliness(Date first, Date last, Date now) {
		DqConfig config = DqConfigStore.getConfig();
		List<DqRule> rules = new ArrayList<DqRule>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.MONTH)) {
			DqConfig.RuleDefinition definition = config.getRuleDefinition(rule.getId());
			if (definition != null && DqConfig.RuleDefinition.TYPE_TIMELINESS.equals(definition.getType())) {
				rules.add(rule);
			}
		}
		List<DqResult> saved = new ArrayList<DqResult>();
		if (rules.isEmpty()) {
			return saved;
		}
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			DqRuleContext context = null;
			for (DqRule rule : rules) {
				// Only months already checked: the others are checked by the daily task once due
				List<DqResult> previous = dqDao.getResults(rule.getId(), month, month);
				if (previous.isEmpty()) {
					continue;
				}
				if (context == null) {
					context = new ServiceDqRuleContext(this, config, null, month, DqPeriods.lastDayOfMonth(month));
				}
				saved.addAll(recheck(DqRuleEngine.evaluate(context, Collections.singletonList(rule)),
						new ArrayList<DqResult>(previous), config));
			}
		}
		return saved;
	}

	/**
	 * Checks the month's MONTH rules that are due and not yet checked and, for the rules that read
	 * report values, checks again those whose reports have a newer version than the check. A
	 * result checked again is updated in place, so its ticket stays with it.
	 *
	 * @param staleOnly true to check again only, leaving rules never checked to the scheduled task
	 */
	private List<DqResult> evaluateMonth(Date month, Date now, boolean staleOnly) {
		Date monthStart = DqPeriods.monthStart(month);
		DqConfig config = DqConfigStore.getConfig();
		List<DqRule> due = new ArrayList<DqRule>();
		List<DqResult> previous = new ArrayList<DqResult>();
		List<DqReportRun> runs = null;
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.MONTH)) {
			Date deadline = rule.checkAfter(config, monthStart);
			if (deadline != null && now.before(deadline)) {
				continue;
			}
			List<DqResult> existing = dqDao.getResults(rule.getId(), monthStart, monthStart);
			if (existing.isEmpty()) {
				if (!staleOnly) {
					due.add(rule);
				}
			} else if (rule.readsReportValues()) {
				if (runs == null) {
					runs = dqDao.getLatestReportRuns(monthStart);
				}
				if (isStale(existing, rule.getReports(config), runs)) {
					due.add(rule);
					previous.addAll(existing);
				}
			}
		}
		if (due.isEmpty()) {
			return Collections.emptyList();
		}
		DqRuleContext context = new ServiceDqRuleContext(this, config, null, monthStart, DqPeriods.lastDayOfMonth(monthStart));
		return recheck(DqRuleEngine.evaluate(context, due), previous, config);
	}

	/**
	 * @return true when a result was checked with settings other than the rule's now, e.g. before
	 * its indicators changed; results saved without their settings are left as they are
	 */
	static boolean settingsChanged(DqRule rule, List<DqResult> results) {
		if (rule.getDefinition() == null) {
			return false;
		}
		JsonNode now = PARAMS_MAPPER.valueToTree(rule.getDefinition().getValues());
		for (DqResult result : results) {
			if (StringUtils.isBlank(result.getParams())) {
				continue;
			}
			try {
				if (!now.equals(PARAMS_MAPPER.readTree(result.getParams()))) {
					return true;
				}
			}
			catch (IOException e) {
				return true;
			}
		}
		return false;
	}

	private static final ObjectMapper PARAMS_MAPPER = new ObjectMapper();

	/**
	 * @return true when one of the reports has a version generated after the results were checked
	 */
	static boolean isStale(List<DqResult> results, Set<String> reports, List<DqReportRun> latestRuns) {
		Date checked = null;
		for (DqResult result : results) {
			if (result.getEvaluatedAt() != null && (checked == null || result.getEvaluatedAt().after(checked))) {
				checked = result.getEvaluatedAt();
			}
		}
		for (DqReportRun run : latestRuns) {
			if (reports.contains(run.getReportName()) && run.getGeneratedAt() != null
					&& (checked == null || run.getGeneratedAt().after(checked))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Puts each result in place of the previous result of its rule and indicator, keeping the
	 * previous result's id and ticket. A rule with a result per baseline (T22-03) is matched on the
	 * baseline too, falling back to any previous result of the rule and indicator. A rule with one
	 * result for the period (T22-08) takes the place of its one previous result even when its
	 * indicators changed.
	 *
	 * @param changed receives the results that are new or whose outcome changed: those that may
	 * need a ticket
	 * @return the results to save
	 */
	static List<DqResult> replace(List<DqResult> results, List<DqResult> previous, List<DqResult> changed) {
		List<DqResult> saved = new ArrayList<DqResult>();
		for (DqResult result : results) {
			DqResult match = null;
			for (DqResult candidate : previous) {
				if (StringUtils.equals(candidate.getRuleId(), result.getRuleId())
						&& StringUtils.equals(candidate.getIndicatorCode(), result.getIndicatorCode())) {
					if (StringUtils.equals(baselineOf(candidate), baselineOf(result))) {
						match = candidate;
						break;
					}
					if (match == null) {
						match = candidate;
					}
				}
			}
			if (match == null) {
				match = onlyOf(result.getRuleId(), results, previous);
			}
			if (match == null) {
				saved.add(result);
				changed.add(result);
				continue;
			}
			previous.remove(match);
			match.setIndicatorCode(result.getIndicatorCode());
			if (match.getOutcome() != result.getOutcome()) {
				changed.add(match);
			}
			match.setPeriodEnd(result.getPeriodEnd());
			match.setMeasureValue(result.getMeasureValue());
			match.setNumerator(result.getNumerator());
			match.setDenominator(result.getDenominator());
			match.setThreshold(result.getThreshold());
			match.setOutcome(result.getOutcome());
			match.setMessage(result.getMessage());
			match.setDetail(result.getDetail());
			match.setParams(result.getParams());
			match.setEvaluatedAt(result.getEvaluatedAt());
			saved.add(match);
		}
		return saved;
	}

	/**
	 * @return the rule's one previous result when the rule has one result now and one before, else null
	 */
	private static DqResult onlyOf(String ruleId, List<DqResult> results, List<DqResult> previous) {
		DqResult only = null;
		for (DqResult candidate : previous) {
			if (StringUtils.equals(candidate.getRuleId(), ruleId)) {
				if (only != null) {
					return null;
				}
				only = candidate;
			}
		}
		int now = 0;
		for (DqResult result : results) {
			if (StringUtils.equals(result.getRuleId(), ruleId)) {
				now++;
			}
		}
		return now == 1 ? only : null;
	}

	/**
	 * @return the baseline in a T22-03 result's detail, or null
	 */
	private static String baselineOf(DqResult result) {
		String detail = result.getDetail();
		if (detail == null) {
			return null;
		}
		if (detail.contains("\"" + ConsistencyOverTimeRule.BASELINE_ROLLING + "\"")) {
			return ConsistencyOverTimeRule.BASELINE_ROLLING;
		}
		if (detail.contains("\"" + ConsistencyOverTimeRule.BASELINE_LAST_YEAR + "\"")) {
			return ConsistencyOverTimeRule.BASELINE_LAST_YEAR;
		}
		return null;
	}

	@Override
	public int recordKhisValues(DqReportRun run, Map<String, Double> khisValues, Date readAt) {
		int compared = 0;
		for (DqValue value : dqDao.getValues(run)) {
			if (value.getDataElement() == null) {
				continue;
			}
			value.setKhisValue(khisValues.get(value.getDataElement() + "|" + value.getCategoryOptionCombo()));
			value.setKhisLastUpdated(readAt);
			compared++;
		}
		return compared;
	}

	@Override
	public List<DqResult> evaluateKhisParity(DqReportRun run) {
		DqConfig config = DqConfigStore.getConfig();
		DqRuleContext context = new ServiceDqRuleContext(this, config, run, run.getPeriodStart(), run.getPeriodEnd());
		List<DqResult> all = dqDao.getResults(run);
		List<DqResult> saved = new ArrayList<DqResult>();
		for (DqRule rule : DqRuleTypes.rules(config, DqRule.Schedule.KHIS_READBACK)) {
			List<DqResult> previous = new ArrayList<DqResult>();
			for (DqResult result : all) {
				if (rule.getId().equals(result.getRuleId())) {
					previous.add(result);
				}
			}
			List<DqResult> results = DqRuleEngine.evaluate(context, Collections.singletonList(rule));
			if (previous.isEmpty()) {
				saveResultsAndTickets(results, config);
				saved.addAll(results);
			} else {
				// Read back again, e.g. after the report was sent again: compared again in place, so the
				// ticket follows the result
				saved.addAll(recheck(results, previous, config));
			}
		}
		return saved;
	}

	private void saveResultsAndTickets(List<DqResult> results, DqConfig config) {
		for (DqResult result : results) {
			dqDao.saveResult(result);
		}
		followUp(results, config);
	}

	/**
	 * Raises the tickets of the results that need follow-up, and closes the open ticket of each
	 * finding a result now passes
	 */
	private void followUp(List<DqResult> results, DqConfig config) {
		raiseTickets(DqRuleEngine.ticketsFor(results, config));
		Date now = new Date();
		for (DqResult result : results) {
			if (result.getOutcome() != DqOutcome.PASS) {
				continue;
			}
			for (DqTicket open : dqDao.getUnresolvedTickets(result.getRuleId(), result.getPeriodStart())) {
				if (isFinding(open, result.getProgramCode(), result.getRuleId(), result.getIndicatorCode(),
						result.getPeriodStart())) {
					dqDao.saveTicket(closeAsPassed(open, now));
				}
			}
		}
	}

	/**
	 * Resolves a ticket whose check passed when run again: by no one, with a note saying so after
	 * any note already made
	 */
	/**
	 * @return why the ticket's finding is no longer checked, after its rule, report or indicators were
	 * taken out of the configuration; null while it is still checked
	 */
	static String noLongerChecked(DqTicket ticket, DqConfig config) {
		DqConfig.RuleDefinition definition = config.getRuleDefinition(ticket.getRuleId());
		if (definition == null) {
			return "rule " + ticket.getRuleId() + " was removed from the data quality settings";
		}
		DqReportRun run = ticket.getResult() != null ? ticket.getResult().getReportRun() : null;
		if (run != null && config.getExpectedReport(run.getReportName()) == null) {
			return run.getReportName() + " was removed from the data quality settings";
		}
		Set<String> codes = new HashSet<String>(definition.getIndicatorCodes());
		if (!codes.isEmpty() && ticket.getIndicatorCode() != null) {
			for (String code : ticket.getIndicatorCode().split("/")) {
				if (!codes.contains(code)) {
					return code + " was removed from " + ticket.getRuleId();
				}
			}
		}
		return null;
	}

	static DqTicket closeAsPassed(DqTicket ticket, Date now) {
		return close(ticket, now, "Closed automatically: the check passed when it was run again");
	}

	private static DqTicket close(DqTicket ticket, Date now, String note) {
		ticket.setStatus(DqTicketStatus.RESOLVED);
		ticket.setResolvedAt(now);
		ticket.setResolvedBy(null);
		ticket.setResolutionNote(StringUtils.isBlank(ticket.getResolutionNote()) ? note
				: StringUtils.abbreviate(ticket.getResolutionNote().trim() + "\n" + note, 1000));
		return ticket;
	}

	/**
	 * Saves the tickets raised, one per finding: a finding (program, rule, indicator and period) that
	 * already has a ticket not resolved updates it with the latest check, keeping its status, note and
	 * when it was opened. A cross check is checked with each report it reads, and again with each new
	 * version, so the same finding is raised many times.
	 */
	private void raiseTickets(List<DqTicket> raised) {
		for (DqTicket ticket : raised) {
			DqTicket open = sameFinding(dqDao.getUnresolvedTickets(ticket.getRuleId(), ticket.getPeriodStart()), ticket);
			if (open != null) {
				dqDao.saveTicket(mergeInto(open, ticket));
			} else if (resolvedWithSameFigures(dqDao.getTickets(ticket.getRuleId(), ticket.getPeriodStart()), ticket) == null) {
				// A finding someone resolved stays resolved while its figures are the same, e.g. when
				// the report is generated again without the data changing
				dqDao.saveTicket(ticket);
			}
		}
	}

	/**
	 * @param newestFirst the tickets of the rule for the period, the newest first
	 * @return the latest ticket of the finding when a person resolved it with the figures raised again
	 * now, else null. A ticket closed automatically (its check passed, or it is no longer checked)
	 * does not count: the finding failing again is news.
	 */
	static DqTicket resolvedWithSameFigures(List<DqTicket> newestFirst, DqTicket raised) {
		DqTicket latest = sameFinding(newestFirst, raised);
		if (latest == null || latest.getStatus() != DqTicketStatus.RESOLVED || latest.getResolvedBy() == null) {
			return null;
		}
		return same(latest.getEmrValue(), raised.getEmrValue()) && same(latest.getKhisValue(), raised.getKhisValue())
				&& same(latest.getVariancePct(), raised.getVariancePct()) ? latest : null;
	}

	private static boolean same(Double a, Double b) {
		return a == null ? b == null : b != null && Math.abs(a - b) < 1e-9;
	}

	/**
	 * @return the ticket of the candidates about the same finding as the one raised, or null
	 */
	static DqTicket sameFinding(List<DqTicket> candidates, DqTicket raised) {
		for (DqTicket candidate : candidates) {
			if (isFinding(candidate, raised.getProgramCode(), raised.getRuleId(), raised.getIndicatorCode(),
					raised.getPeriodStart())) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * @return true when the ticket is about the finding of this program, rule, indicator and period
	 */
	static boolean isFinding(DqTicket ticket, String programCode, String ruleId, String indicatorCode, Date periodStart) {
		return StringUtils.equals(ticket.getProgramCode(), programCode)
				&& StringUtils.equals(ticket.getRuleId(), ruleId)
				&& StringUtils.equals(ticket.getIndicatorCode(), indicatorCode)
				&& ticket.getPeriodStart().getTime() == periodStart.getTime();
	}

	/**
	 * Gives the open ticket the figures of the check that raised the finding again
	 */
	static DqTicket mergeInto(DqTicket open, DqTicket raised) {
		open.setResult(raised.getResult());
		open.setEmrValue(raised.getEmrValue());
		open.setKhisValue(raised.getKhisValue());
		open.setVariancePct(raised.getVariancePct());
		open.setSuggestedCause(raised.getSuggestedCause());
		return open;
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
	public boolean hasReportRuns() {
		return dqDao.hasReportRuns();
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
	public List<DqResult> getProgramResults(String programCode, String ruleId, Date fromPeriod, Date toPeriod) {
		return dqDao.getProgramResults(programCode, ruleId, fromPeriod, toPeriod);
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

	private boolean hasSubmissionLedger() {
		List<Object[]> ledger = dqDao.executeSqlQuery("select count(*), 0 from information_schema.tables "
				+ "where table_schema = database() and table_name = 'il_report_submission'", new HashMap<String, Object>());
		return !ledger.isEmpty() && ledger.get(0)[0] instanceof Number && ((Number) ledger.get(0)[0]).intValue() > 0;
	}

	@Override
	public boolean recordKhisSendFailure(DqReportRun run, String payloadFormat, Integer httpCode, String response) {
		if (!hasSubmissionLedger()) {
			return false;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("uuid", UUID.randomUUID().toString());
		parameters.put("runId", run.getId());
		parameters.put("reportName", run.getReportName());
		parameters.put("periodStart", run.getPeriodStart());
		parameters.put("version", run.getVersion());
		parameters.put("format", payloadFormat);
		parameters.put("httpCode", httpCode);
		parameters.put("error", response);
		parameters.put("approvedBy", run.getApprovedBy() != null ? run.getApprovedBy().getUserId() : null);
		parameters.put("approvedAt", run.getApprovedAt());
		parameters.put("approvalMode", run.getApprovalMode() != null ? run.getApprovalMode() : "DIRECT");
		// A version KHIS accepted before keeps its status, send time and counts; only the failure is added
		dqDao.executeSqlUpdate("insert into il_report_submission (uuid, report_run_id, report_name, period_start, version, "
				+ "target_system, payload_format, status, attempts, http_code, import_summary, last_error, approved_by, "
				+ "approved_at, approval_mode, date_created) values (:uuid, :runId, :reportName, :periodStart, :version, "
				+ "'KHIS', :format, 'REJECTED', 1, :httpCode, :error, :error, :approvedBy, :approvedAt, :approvalMode, now()) "
				+ "on duplicate key update attempts = attempts + 1, "
				+ "status = if(sent_at is null, values(status), status), "
				+ "http_code = if(sent_at is null, values(http_code), http_code), "
				+ "import_summary = if(sent_at is null, values(import_summary), import_summary), "
				+ "last_error = values(last_error), date_changed = now()",
				parameters);
		recheckSentMonth(run);
		return true;
	}

	/** The month's timeliness as it stands after a send from the reports page; never fails the send */
	private void recheckSentMonth(DqReportRun run) {
		try {
			recheckTimeliness(run.getPeriodStart(), run.getPeriodStart(), new Date());
		}
		catch (RuntimeException e) {
			log.warn("Could not check the timeliness of " + run.getReportName() + " again after sending it", e);
		}
	}

	@Override
	public boolean recordKhisSend(DqReportRun run, String payloadFormat, int httpCode, Integer[] counts,
	                              String importSummary) {
		if (!hasSubmissionLedger()) {
			return false;
		}
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("uuid", UUID.randomUUID().toString());
		parameters.put("runId", run.getId());
		parameters.put("reportName", run.getReportName());
		parameters.put("periodStart", run.getPeriodStart());
		parameters.put("version", run.getVersion());
		parameters.put("format", payloadFormat);
		parameters.put("httpCode", httpCode);
		String[] names = { "imported", "updated", "ignored", "deleted" };
		for (int i = 0; i < names.length; i++) {
			parameters.put(names[i], counts != null && counts.length > i ? counts[i] : null);
		}
		parameters.put("summary", importSummary);
		parameters.put("approvedBy", run.getApprovedBy() != null ? run.getApprovedBy().getUserId() : null);
		parameters.put("approvedAt", run.getApprovedAt());
		// The reports page sends without the IL; the ledger's statuses are kenyaemrIL's
		parameters.put("approvalMode", run.getApprovalMode() != null ? run.getApprovalMode() : "DIRECT");
		dqDao.executeSqlUpdate("insert into il_report_submission (uuid, report_run_id, report_name, period_start, version, "
				+ "target_system, payload_format, status, attempts, sent_at, http_code, imported, updated, ignored, deleted, "
				+ "import_summary, approved_by, approved_at, approval_mode, date_created) values (:uuid, :runId, :reportName, "
				+ ":periodStart, :version, 'KHIS', :format, 'SENT', 1, now(), :httpCode, :imported, :updated, :ignored, "
				+ ":deleted, :summary, :approvedBy, :approvedAt, :approvalMode, now()) on duplicate key update "
				+ "attempts = attempts + 1, status = 'SENT', sent_at = coalesce(sent_at, now()), http_code = values(http_code), "
				+ "imported = values(imported), updated = values(updated), ignored = values(ignored), "
				+ "deleted = values(deleted), import_summary = values(import_summary), last_error = null, "
				// Sent again: KHIS's values may have changed, so it is read back again for G-RPT-02
				+ "read_back_at = null, read_back_error = null, date_changed = now()",
				parameters);
		recheckSentMonth(run);
		return true;
	}

	@Override
	public List<Object[]> executeSqlQuery(String sql, Map<String, Object> parameters) {
		return dqDao.executeSqlQuery(sql, parameters);
	}
}
