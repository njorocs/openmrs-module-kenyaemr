/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.db;

import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data access for the data quality tables
 */
public interface DqDao {

	DqReportRun saveReportRun(DqReportRun run);

	DqReportRun getReportRunByUuid(String uuid);

	/**
	 * @return the highest version for the report and period, or null when none exists
	 */
	DqReportRun getLatestReportRun(String reportName, Date periodStart);

	/**
	 * @return the latest version of every report generated for the period
	 */
	List<DqReportRun> getLatestReportRuns(Date periodStart);

	/**
	 * @return the latest version per period for one report, oldest period first
	 */
	List<DqReportRun> getLatestReportRuns(String reportName, Date fromPeriod, Date toPeriod);

	void saveValues(Collection<DqValue> values);

	List<DqValue> getValues(DqReportRun run);

	/**
	 * @return values of the latest version per period of the named reports, for periods in range
	 */
	List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod);

	DqResult saveResult(DqResult result);

	DqResult getResultByUuid(String uuid);

	List<DqResult> getResults(DqReportRun run);

	List<DqResult> getResults(String ruleId, Date fromPeriod, Date toPeriod);

	DqOverride saveOverride(DqOverride override);

	List<DqOverride> getOverrides(DqReportRun run);

	DqTicket saveTicket(DqTicket ticket);

	DqTicket getTicketByUuid(String uuid);

	/**
	 * @param status the status to filter on, or null for all tickets
	 */
	List<DqTicket> getTickets(DqTicketStatus status);

	/**
	 * Runs a read-only SQL query, used by rules that work on patient-level ETL tables
	 */
	List<Object[]> executeSqlQuery(String sql, Map<String, Object> parameters);
}
