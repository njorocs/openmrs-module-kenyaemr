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

import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.kenyaemr.EmrActivator;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqEtl;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
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
 * down on the 1st catches up at its next run. It never runs in the startup thread, which it would
 * slow; but on a database with no report run stored yet (a new facility, or a restored database)
 * it runs once in the background {@link #FIRST_RUN_DELAY_MINUTES} minutes after the module starts
 * (see {@link #runFirstTimeInBackground()}), so the dashboard has data without waiting for 01:00.
 * To run it at another time, POST /ws/rest/v1/kenyaemr/dq/task/run, which runs it in the background.
 */
public class DataQualityTask extends AbstractTask {

	private static final Logger log = LoggerFactory.getLogger(DataQualityTask.class);

	/** How many months back the timeliness rule is checked again against the submission ledger each night */
	static final int TIMELINESS_RECHECK_MONTHS = 12;

	/** How many weeks back the weekly checks (T22-08) catch up */
	static final int WEEKS_TO_CATCH_UP = 8;

	/**
	 * How many months before last month are kept generated: a year of trend, and the year before it
	 * to compare each of those months with the same month last year
	 */
	static final int MONTHS_OF_HISTORY = 24;

	/** How long after the module starts a first run waits, so as not to slow the startup */
	static final int FIRST_RUN_DELAY_MINUTES = 5;

	/** When the last run finished, yyyy-MM-dd'T'HH:mm:ss; set by every run that was not skipped */
	public static final String GP_LAST_RUN = "kenyaemr.dq.lastRun";

	/** How many steps (a report, a week, a check) failed in the last run */
	public static final String GP_LAST_RUN_FAILURES = "kenyaemr.dq.lastRunFailures";

	/** When the last run with no failed step finished, yyyy-MM-dd'T'HH:mm:ss */
	public static final String GP_LAST_SUCCESSFUL_RUN = "kenyaemr.dq.lastSuccessfulRun";

	public static final String RUN_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	/** One run at a time */
	private static final AtomicBoolean RUNNING = new AtomicBoolean();

	/** Whether a run is in progress, scheduled or by hand */
	public static boolean isRunning() {
		return RUNNING.get();
	}

	/**
	 * Starts a run now in a background daemon thread, for a manual run that should not hold a
	 * request open for the minutes a run can take
	 *
	 * @return false when a run is already in progress
	 */
	public static boolean runInBackground() {
		if (RUNNING.get()) {
			return false;
		}
		log.info("Data quality: run by hand in the background");
		startDaemon(new Runnable() {

			@Override
			public void run() {
				new DataQualityTask().execute();
			}
		});
		return true;
	}

	/**
	 * At module start: when no report run is stored yet, runs the task once in the background after
	 * {@link #FIRST_RUN_DELAY_MINUTES} minutes, so a new or restored database has last month's
	 * reports and the history behind the trends within the hour instead of after the next 01:00.
	 * Once any run is stored this does nothing, so later restarts are not slowed.
	 */
	public static void runFirstTimeInBackground() {
		log.info("Data quality: first-run check scheduled for "
				+ new Date(System.currentTimeMillis() + FIRST_RUN_DELAY_MINUTES * 60L * 1000L) + " ("
				+ FIRST_RUN_DELAY_MINUTES + " minutes after startup)");
		startDaemon(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(FIRST_RUN_DELAY_MINUTES * 60L * 1000L);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.info("Data quality: first-run check cancelled, the server is stopping");
					return;
				}
				try {
					DqEtl.Status etl = DqEtl.status();
					if (!etl.isReady()) {
						log.warn("Data quality: first run not started because " + etl.describe()
								+ ". Run the ETL, then run the checks from the data quality dashboard;"
								+ " otherwise the 01:00 run starts them once the ETL is ready.");
						return;
					}
					if (Context.getService(DqService.class).hasReportRuns()) {
						log.info("Data quality: first-run check found report runs already stored, so no first run is needed");
						return;
					}
					long started = System.currentTimeMillis();
					log.info("Data quality: FIRST RUN STARTED at " + new Date(started)
							+ " - no report run stored yet, so generating last month and " + MONTHS_OF_HISTORY
							+ " months of history");
					new DataQualityTask().execute();
					log.info("Data quality: FIRST RUN FINISHED at " + new Date() + " after "
							+ minutesSince(started) + " minutes");
				}
				catch (RuntimeException e) {
					log.error("Data quality: the first run failed; the scheduled run will try again", e);
				}
			}
		});
	}

	private static void startDaemon(Runnable runnable) {
		DaemonToken token = EmrActivator.getDaemonToken();
		if (token == null) {
			throw new IllegalStateException("KenyaEMR daemon token is unavailable");
		}
		Thread thread = Daemon.runInDaemonThread(runnable, token);
		thread.setName("kenyaemr-data-quality");
	}

	/** The steps of this run that failed, each logged with its error */
	private int failures;

	@Override
	public void execute() {
		if (!RUNNING.compareAndSet(false, true)) {
			log.info("Data quality: a run is already in progress, so this one is skipped");
			return;
		}
		long started = System.currentTimeMillis();
		boolean ran = false;
		// The scheduler runs the same instance every night
		failures = 0;
		startExecuting();
		try {
			// The reports are evaluated against the ETL tables: without them every report would be
			// stored empty, and the first run would not be tried again
			DqEtl.Status etl = DqEtl.status();
			if (!etl.isReady()) {
				log.warn("Data quality: task not run because " + etl.describe());
				return;
			}
			ran = true;
			log.info("Data quality: task started (ETL last run " + etl.getLastRun() + ")");
			DqService service = Context.getService(DqService.class);
			Date today = new Date();
			generateLastMonth(service, DqPeriods.previousMonth(today));
			checkRecentWeeks(service, today);
			try {
				service.evaluateMonth(DqPeriods.previousMonth(today), today);
				// Reports sent, rejected or completed in KHIS since a month was checked, e.g. by the IL
				Date lastMonth = DqPeriods.previousMonth(today);
				service.recheckTimeliness(DqPeriods.addMonths(lastMonth, -(TIMELINESS_RECHECK_MONTHS - 1)), lastMonth, today);
			}
			catch (RuntimeException e) {
				failures++;
				log.error("Data quality: monthly timeliness check failed", e);
			}
			generateHistory(service, DqPeriods.previousMonth(today));
			// Last, so that the tickets take the figures of everything generated above
			try {
				int refreshed = service.refreshOpenTickets();
				log.info("Data quality: checked " + refreshed + " open tickets again");
			}
			catch (RuntimeException e) {
				failures++;
				log.error("Data quality: could not check the open tickets again", e);
			}
		}
		finally {
			stopExecuting();
			RUNNING.set(false);
			if (ran) {
				log.info("Data quality: task finished after " + minutesSince(started) + " minutes"
						+ (failures > 0 ? ", " + failures + " steps failed (see the errors above)" : ", every step succeeded"));
				recordRun(failures);
			}
		}
	}

	/** Keeps when this run finished, and when it is the latest with no failed step */
	private static void recordRun(int failures) {
		try {
			String now = new SimpleDateFormat(RUN_TIME_FORMAT).format(new Date());
			saveGlobalProperty(GP_LAST_RUN, now, "When the data quality checks last ran");
			saveGlobalProperty(GP_LAST_RUN_FAILURES, String.valueOf(failures),
					"How many steps of the last data quality run failed");
			if (failures == 0) {
				saveGlobalProperty(GP_LAST_SUCCESSFUL_RUN, now, "When the data quality checks last ran with no failed step");
			}
		}
		catch (RuntimeException e) {
			log.error("Data quality: could not record the run", e);
		}
	}

	private static void saveGlobalProperty(String name, String value, String description) {
		AdministrationService administration = Context.getAdministrationService();
		GlobalProperty property = administration.getGlobalPropertyObject(name);
		if (property == null) {
			property = new GlobalProperty(name, value, description);
		}
		property.setPropertyValue(value);
		administration.saveGlobalProperty(property);
	}

	private static String minutesSince(long started) {
		return String.format("%.1f", (System.currentTimeMillis() - started) / 60000.0);
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
				failures++;
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
				failures++;
				log.error("Data quality: could not generate " + report.getReport() + " for " + month, e);
			}
		}
		for (DqReportRun run : unchecked) {
			try {
				service.evaluateReportRun(service.getReportRunByUuid(run.getUuid()));
			}
			catch (RuntimeException e) {
				failures++;
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
					failures++;
					log.error("Data quality: could not generate " + report.getReport() + " for " + month, e);
				}
			}
		}
	}
}
