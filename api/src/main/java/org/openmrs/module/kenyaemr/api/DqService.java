/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportPayload;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.module.reporting.report.ReportData;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data quality (DQ) checks on aggregate reports before they are sent to KHIS, following the
 * TR-26-110 malaria validation rules
 */
@Transactional
public interface DqService extends OpenmrsService {

	/**
	 * Lets a user without one of {@link #RUN_ROLES} approve reports for submission; a super admin and
	 * those roles may approve without it (see DqAccess)
	 */
	String PRIVILEGE_APPROVE = "Approve Data Quality Reports";

	/**
	 * Lets a user without one of {@link #RUN_ROLES} set the thresholds of the data quality rules for the
	 * facility; a super admin and those roles may without it
	 */
	String PRIVILEGE_MANAGE_SETTINGS = "Manage Data Quality Settings";

	/**
	 * The roles that may do everything with the data quality checks without its privileges: run
	 * them now (e.g. after an ETL refresh), generate reports, work the tickets, approve reports and
	 * set the thresholds. The HRIO, and the system administrators who restore the database and run
	 * the ETL.
	 */
	String[] RUN_ROLES = { "o3: Health Record Officer", "o3: System Administrator", "System Administrator" };

	/**
	 * Evaluates a report for a month, saves it as a new version and runs its data quality rules.
	 * The previous version, if any, is marked SUPERSEDED.
	 *
	 * @param reportName the report descriptor name, e.g. "MOH-743 Report"
	 * @param periodStart any date in the month to report on
	 * @return the saved run, BLOCKED or AWAITING_APPROVAL
	 */
	DqReportRun generateReportRun(String reportName, Date periodStart);

	/**
	 * As {@link #generateReportRun(String, Date)}, optionally leaving the run GENERATED so that rules
	 * comparing several reports can run once all of them exist
	 */
	DqReportRun generateReportRun(String reportName, Date periodStart, boolean evaluateRules);

	/**
	 * Runs the monthly rules on a GENERATED run, saves the results and any tickets, and moves the
	 * run to BLOCKED or AWAITING_APPROVAL. A run is evaluated once; to check again, regenerate it.
	 *
	 * @return the saved results
	 */
	List<DqResult> evaluateReportRun(DqReportRun run);

	/**
	 * Stores a report the HRIO ran on the reports page, so the data quality pages read it without
	 * regenerating. Only an expected monthly report, run for a whole month that has ended, is kept;
	 * it is saved as a new version and checked when its values differ from the latest version.
	 * Runs in its own transaction so a failure here never fails the report run.
	 *
	 * @param reportDefinitionUuid the evaluated report definition
	 * @return the saved run, or null when the report was not kept
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	DqReportRun captureReportRun(String reportDefinitionUuid, ReportData reportData, Date start, Date end);

	/**
	 * Whether a report generated on the reports page may be sent to KHIS: only once the version
	 * with the same figures has been checked, nothing blocks it and its warnings are reviewed
	 * (approved). A version with nothing to review may be sent without approval.
	 *
	 * A report sent a week at a time (MOH 505) is held for any period but a whole week or month.
	 *
	 * @param reportName the report, e.g. "MOH 705A Outpatient summary"
	 * @param periodStart the report's start date
	 * @param periodEnd the report's end date
	 * @param valuesHash the hash of the figures to send, see {@link org.openmrs.module.kenyaemr.dq.DqSendGates}
	 * @param completedAt when the report finished; its checks start just after
	 */
	@Transactional(readOnly = true)
	org.openmrs.module.kenyaemr.dq.DqSendGate getSendGate(String reportName, Date periodStart, Date periodEnd,
	                                                      String valuesHash, Date completedAt);

	/**
	 * Approves a run on behalf of the authenticated user (the HRIO). Refused while a hard block is
	 * open, for a superseded or already approved run, or when a warning has no override.
	 *
	 * @param overrides one per warning not already overridden, each with its result and a reason
	 * code from the configured list; creator and date are filled in
	 * @return the approved run
	 * @throws org.openmrs.api.APIException explaining why approval is refused
	 * @throws org.openmrs.api.APIAuthenticationException unless the user may approve (DqAccess)
	 */
	@Authorized
	DqReportRun approveReportRun(DqReportRun run, List<DqOverride> overrides);

	/**
	 * Auto-send on the deadline: approves last month's reports that are still awaiting approval once
	 * their deadline day has come. Warnings nobody answered are recorded with reason UNREVIEWED.
	 * Blocked reports, and versions never checked, are left alone.
	 *
	 * @param now the current time
	 * @return the runs approved
	 */
	@Authorized(PRIVILEGE_APPROVE)
	List<DqReportRun> autoApproveDueReports(Date now);

	/**
	 * Runs the weekly rules (T22-08) for the Monday-to-Sunday week containing the date. A week that
	 * has already been checked is not checked again.
	 *
	 * @return the results for the week
	 */
	List<DqResult> evaluateWeek(Date anyDateInWeek);

	/**
	 * Checks again, with the latest values, the finding of every ticket not resolved, so that its
	 * figures are those the trends show: a ticket whose finding still fails takes the new figures and
	 * message, and one whose finding now passes is closed. Run nightly, it catches tickets a new
	 * version did not reach, e.g. one generated before the re-check on generation was installed.
	 *
	 * @return how many tickets were checked again
	 */
	int refreshOpenTickets();

	/**
	 * Runs the month-level rules (G-RPT-01 timeliness) for a month, once each rule's deadlines are
	 * over. A rule already checked for the month is not checked again.
	 *
	 * @param month any date in the month
	 * @param now the current time; rules whose deadline day has not ended are skipped
	 * @return the results saved now
	 */
	List<DqResult> evaluateMonth(Date month, Date now);

	/**
	 * Checks the timeliness rules (G-RPT-01) again against the submission ledger as it is now, for
	 * the months from first to last already checked, so that a report sent, sent again, rejected or
	 * marked complete after the check shows as it stands. Each result is updated in place, so its
	 * ticket follows it: closed when the report now passes, raised when it now fails.
	 *
	 * @return the results checked again
	 */
	List<DqResult> recheckTimeliness(Date first, Date last, Date now);

	/**
	 * Records the values KHIS holds for a sent report version, read back from KHIS. Each stored cell
	 * with a KHIS mapping gets the value KHIS has for its data element and category option combo,
	 * or none when KHIS has no value.
	 *
	 * @param khisValues KHIS value by "dataElement|categoryOptionCombo"
	 * @param readAt when KHIS was read
	 * @return how many cells were compared
	 */
	int recordKhisValues(DqReportRun run, Map<String, Double> khisValues, Date readAt);

	/**
	 * Runs the KHIS parity rules (G-RPT-02) on a report version whose KHIS values have been recorded,
	 * saving the results and any reconciliation tickets. A version already compared is compared
	 * again with the values recorded now, in place, so its tickets follow: closed when KHIS now
	 * matches, raised when it no longer does.
	 *
	 * @return the results
	 */
	List<DqResult> evaluateKhisParity(DqReportRun run);

	DqReportRun saveReportRun(DqReportRun run);

	@Transactional(readOnly = true)
	DqReportRun getReportRunByUuid(String uuid);

	@Transactional(readOnly = true)
	DqReportRun getLatestReportRun(String reportName, Date periodStart);

	/**
	 * @return the latest version for exactly these dates, e.g. a week of MOH 505
	 */
	@Transactional(readOnly = true)
	DqReportRun getLatestReportRun(String reportName, Date periodStart, Date periodEnd);

	/**
	 * @return the latest weekly versions of reports, for weeks starting in the range
	 */
	@Transactional(readOnly = true)
	List<DqReportRun> getLatestWeeklyRuns(Date fromPeriod, Date toPeriod);

	@Transactional(readOnly = true)
	List<DqReportRun> getLatestReportRuns(Date periodStart);

	/** Whether any report run is stored at all, e.g. none on a new or restored database */
	@Transactional(readOnly = true)
	boolean hasReportRuns();

	@Transactional(readOnly = true)
	List<DqReportRun> getLatestReportRuns(String reportName, Date fromPeriod, Date toPeriod);

	void saveValues(Collection<DqValue> values);

	@Transactional(readOnly = true)
	List<DqValue> getValues(DqReportRun run);

	/**
	 * @return the KHIS payload rendered when the version was generated, or null when the report has
	 * no KHIS mapping
	 */
	@Transactional(readOnly = true)
	DqReportPayload getPayload(DqReportRun run);

	@Transactional(readOnly = true)
	List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod);

	DqResult saveResult(DqResult result);

	@Transactional(readOnly = true)
	DqResult getResultByUuid(String uuid);

	@Transactional(readOnly = true)
	List<DqResult> getResults(DqReportRun run);

	@Transactional(readOnly = true)
	List<DqResult> getResults(String ruleId, Date fromPeriod, Date toPeriod);

	/**
	 * @param programCode the program, or null for all
	 * @param ruleId the rule, or null for all
	 * @return the results for periods starting in the range, leaving out those of superseded
	 * report versions
	 */
	@Transactional(readOnly = true)
	List<DqResult> getProgramResults(String programCode, String ruleId, Date fromPeriod, Date toPeriod);

	DqOverride saveOverride(DqOverride override);

	@Transactional(readOnly = true)
	List<DqOverride> getOverrides(DqReportRun run);

	DqTicket saveTicket(DqTicket ticket);

	@Transactional(readOnly = true)
	DqTicket getTicketByUuid(String uuid);

	@Transactional(readOnly = true)
	List<DqTicket> getTickets(DqTicketStatus status);

	@Transactional(readOnly = true)
	List<Object[]> executeSqlQuery(String sql, Map<String, Object> parameters);

	/**
	 * Records a version sent straight to KHIS from the reports page, in kenyaemrIL's submission
	 * ledger, so its timeliness is known as for a version the IL sends. A version sent again keeps
	 * its first send time. Nothing is recorded where the ledger does not exist.
	 *
	 * @param payloadFormat ADX or JSON
	 * @param counts KHIS's import counts: imported, updated, ignored, deleted; each may be null
	 * @param importSummary KHIS's response
	 * @return true when recorded
	 */
	boolean recordKhisSend(DqReportRun run, String payloadFormat, int httpCode, Integer[] counts, String importSummary);

	/**
	 * Records a send from the reports page that KHIS refused, or that did not reach it, in the same
	 * ledger as REJECTED (kenyaemrIL's status for a send that is not retried), with KHIS's reply as
	 * the import summary and the last error, from which KHIS's own status is read. It has no send time, so it does not
	 * count as sent for timeliness. A version KHIS already accepted keeps its status; the failure is
	 * kept as its last error.
	 *
	 * @param httpCode KHIS's status code, or null when there was none
	 * @param response KHIS's response, or why the send failed
	 * @return true when recorded
	 */
	boolean recordKhisSendFailure(DqReportRun run, String payloadFormat, Integer httpCode, String response);
}
