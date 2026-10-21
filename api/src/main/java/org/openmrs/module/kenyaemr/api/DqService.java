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

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
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

	@Transactional(readOnly = true)
	List<DqValue> getLatestValues(Collection<String> reportNames, Date fromPeriod, Date toPeriod);

	DqResult saveResult(DqResult result);

	@Transactional(readOnly = true)
	DqResult getResultByUuid(String uuid);

	@Transactional(readOnly = true)
	List<DqResult> getResults(DqReportRun run);

	@Transactional(readOnly = true)
	List<DqResult> getResults(String ruleId, Date fromPeriod, Date toPeriod);

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
