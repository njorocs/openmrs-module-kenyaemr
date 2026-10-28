/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.task;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Daily data quality task. It catches up rather than relying on running on an exact day:
 * <ul>
 * <li>every expected monthly report not yet generated for last month is generated, then all newly
 * generated reports are checked together, so rules that compare reports (X-RPT-01) see both;</li>
 * <li>the weekly checks (T22-08 test positivity) run once for each of the last 8 weeks not yet
 * checked, so weeks missed while the server was down are caught up;</li>
 * <li>once last month's deadlines are over, its month-level checks (G-RPT-01 timeliness) run once.</li>
 * </ul>
 * So on the 1st the HRIO finds last month's reports waiting for approval, and a server that was
 * down on the 1st catches up when it comes back.
 */
public class DataQualityTask extends AbstractTask {

	private static final Logger log = LoggerFactory.getLogger(DataQualityTask.class);

	/** How many weeks back the weekly checks (T22-08) catch up */
	static final int WEEKS_TO_CATCH_UP = 8;

	@Override
	public void execute() {
		if (isExecuting) {
			return;
		}
		startExecuting();
		try {
			DqService service = Context.getService(DqService.class);
			Date today = new Date();
			generateLastMonth(service, DqPeriods.previousMonth(today));
			checkRecentWeeks(service, today);
			try {
				service.evaluateMonth(DqPeriods.previousMonth(today), today);
			}
			catch (RuntimeException e) {
				log.error("Data quality: monthly timeliness check failed", e);
			}
		}
		finally {
			stopExecuting();
		}
	}

	/**
	 * Runs the weekly checks for each of the last {@link #WEEKS_TO_CATCH_UP} weeks not yet checked,
	 * oldest first, so weeks missed while the server was down are checked too. A week that fails is
	 * left for the next run.
	 */
	private void checkRecentWeeks(DqService service, Date today) {
		Date lastWeek = DqPeriods.addDays(DqPeriods.weekStart(today), -7);
		for (int i = WEEKS_TO_CATCH_UP - 1; i >= 0; i--) {
			Date week = DqPeriods.addDays(lastWeek, -7 * i);
			try {
				service.evaluateWeek(week);
			}
			catch (RuntimeException e) {
				log.error("Data quality: weekly check failed for the week of " + week, e);
			}
		}
	}

	private void generateLastMonth(DqService service, Date month) {
		DqConfig config = DqConfigStore.getConfig();
		List<DqReportRun> unchecked = new ArrayList<DqReportRun>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			if (!report.isGenerate() || !DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency())) {
				continue;
			}
			DqReportRun latest = service.getLatestReportRun(report.getReport(), month);
			if (latest != null) {
				// Generated earlier but not checked, e.g. the task stopped part way
				if (latest.getStatus() == DqRunStatus.GENERATED) {
					unchecked.add(latest);
				}
				continue;
			}
			try {
				unchecked.add(service.generateReportRun(report.getReport(), month, false));
				log.info("Data quality: generated " + report.getReport() + " for " + month);
			}
			catch (RuntimeException e) {
				log.error("Data quality: could not generate " + report.getReport() + " for " + month, e);
			}
		}
		for (DqReportRun run : unchecked) {
			try {
				service.evaluateReportRun(service.getReportRunByUuid(run.getUuid()));
			}
			catch (RuntimeException e) {
				log.error("Data quality: could not check " + run.getReportName() + " for " + month, e);
			}
		}
	}
}
