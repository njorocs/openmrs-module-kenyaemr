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

import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.api.db.DqDao;
import org.openmrs.module.kenyaemr.dq.DqReportGenerator;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link DqService}
 */
public class DqServiceImpl extends BaseOpenmrsService implements DqService {

	private DqDao dqDao;

	public void setDqDao(DqDao dqDao) {
		this.dqDao = dqDao;
	}

	@Override
	public DqReportRun generateReportRun(String reportName, Date periodStart) {
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
		return run;
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
