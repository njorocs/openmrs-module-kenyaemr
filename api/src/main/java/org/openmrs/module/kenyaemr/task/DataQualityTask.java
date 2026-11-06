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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Daily data quality task. It catches up rather than relying on running on an exact day:
 * <ul>
 * <li>every expected monthly report not yet generated for last month is generated, then all newly
 * generated reports are checked together, so rules that compare reports (X-RPT-01) see both;</li>
 * <li>the weekly checks (T22-08 test positivity) run once for each of the last 8 weeks not yet
 * checked, so weeks missed while the server was down are caught up;</li>
 * <li>once last month's deadlines are over, its month-level checks (G-RPT-01 timeliness) run once;</li>
 * <li>any of the {@link #MONTHS_OF_HISTORY} months before last month not yet stored are generated
 * without being checked, so from the first run after deployment T22-03 and the trends have two
 * years of history: a year to show, each month with the same month the year before.</li>
 * </ul>
 * So on the 1st the HRIO finds last month's reports waiting for approval, and a server that was
 * down on the 1st catches up at its next run. It never runs at startup, which it would slow; to run
 * it at another time, POST /ws/rest/v1/kenyaemr/dq/task/run.
 */
public class DataQualityTask extends AbstractTask {

	private static final Logger log = LoggerFactory.getLogger(DataQualityTask.class);

	/** How many weeks back the weekly checks (T22-08) catch up */
	static final int WEEKS_TO_CATCH_UP = 8;

	/**
	 * How many months before last month are kept generated: a year of trend, and the year before it
	 * to compare each of those months with the same month last year
	 */
	static final int MONTHS_OF_HISTORY = 24;

	/** One run at a time */
	private static final AtomicBoolean RUNNING = new AtomicBoolean();

	/**
	 * Runs the task now, in the caller's thread, for a manual run; the scheduled run is at 01:00
	 *
	 * @return false when a run is already in progress
	 */
	public static boolean runNow() {
		if (RUNNING.get()) {
			return false;
		}
		log.info("Data quality: run by hand");
		new DataQualityTask().execute();
		return true;
	}

	@Override
	public void execute() {
		if (!RUNNING.compareAndSet(false, true)) {
			log.info("Data quality: a run is already in progress, so this one is skipped");
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
			generateHistory(service, DqPeriods.previousMonth(today));
			// Last, so that the tickets take the figures of everything generated above
			try {
				int refreshed = service.refreshOpenTickets();
				log.info("Data quality: checked " + refreshed + " open tickets again");
			}
			catch (RuntimeException e) {
				log.error("Data quality: could not check the open tickets again", e);
			}
		}
		finally {
			stopExecuting();
			RUNNING.set(false);
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

	/**
	 * Generates, without checking, each expected monthly report missing for the
	 * {@link #MONTHS_OF_HISTORY} months before last month, newest first. Months already stored are
	 * left as they are; a report that fails is tried again the next night.
	 */
	private void generateHistory(DqService service, Date lastMonth) {
		for (int i = 1; i <= MONTHS_OF_HISTORY; i++) {
			Date month = DqPeriods.addMonths(lastMonth, -i);
			for (DqConfig.ExpectedReport report : DqConfigStore.getConfig().getExpectedReports()) {
				if (!report.isGenerate() || !DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency())
						|| service.getLatestReportRun(report.getReport(), month) != null) {
					continue;
				}
				try {
					service.generateReportRun(report.getReport(), month, false);
					log.info("Data quality: generated " + report.getReport() + " for " + month + " as history");
				}
				catch (RuntimeException e) {
					log.error("Data quality: could not generate " + report.getReport() + " for " + month, e);
				}
			}
		}
	}
}
