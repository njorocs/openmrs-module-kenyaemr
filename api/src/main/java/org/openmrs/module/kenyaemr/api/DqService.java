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

	/** Held by the HRIO who approves reports for submission */
	String PRIVILEGE_APPROVE = "Approve Data Quality Reports";

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
	 * Approves a run on behalf of the authenticated user (the HRIO). Refused while a hard block is
	 * open, for a superseded or already approved run, or when a warning has no override.
	 *
	 * @param overrides one per warning not already overridden, each with its result and a reason
	 * code from the configured list; creator and date are filled in
	 * @return the approved run
	 * @throws org.openmrs.api.APIException explaining why approval is refused
	 */
	@Authorized(PRIVILEGE_APPROVE)
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
	 * Runs the month-level rules (G-RPT-01 timeliness) for a month, once each rule's deadlines are
	 * over. A rule already checked for the month is not checked again.
	 *
	 * @param month any date in the month
	 * @param now the current time; rules whose deadline day has not ended are skipped
	 * @return the results saved now
	 */
	List<DqResult> evaluateMonth(Date month, Date now);

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
	 * saving the results and any reconciliation tickets. A version already compared is not compared
	 * again.
	 *
	 * @return the results
	 */
	List<DqResult> evaluateKhisParity(DqReportRun run);

	DqReportRun saveReportRun(DqReportRun run);

	@Transactional(readOnly = true)
	DqReportRun getReportRunByUuid(String uuid);

	@Transactional(readOnly = true)
	DqReportRun getLatestReportRun(String reportName, Date periodStart);

	@Transactional(readOnly = true)
	List<DqReportRun> getLatestReportRuns(Date periodStart);

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
}
