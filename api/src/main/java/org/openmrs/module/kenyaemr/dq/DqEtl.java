/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Whether the KenyaEMR ETL database is ready for the data quality reports,
 * which are evaluated against it. On a new or restored database it may not exist or be empty until
 * the ETL is run from ETL Administration; until then a data quality run would store empty reports,
 * so it does not run. The ETL is never started from here: it is long and facility-wide, so the user
 * starts it.
 * <p>
 * The ETL database is the tenant's, as for the reports: {@code kenyaemr_etl} for the openmrs
 * database, {@code kenyaemr_etl_<tenant>} for {@code openmrs_<tenant>}.
 */
public final class DqEtl {

	/** A run started this long ago without stopping is taken as crashed, not running */
	static final int STALE_RUN_MINUTES = 180;

	/** Why the ETL is not ready, or READY */
	public enum State {
		/** The ETL database or its tables do not exist */
		MISSING,
		/** The tables exist but no ETL run has completed, or they are empty */
		NOT_POPULATED,
		/** An ETL run is in progress */
		RUNNING,
		READY
	}

	public static final class Status {

		private final State state;

		private final Date lastRun;

		Status(State state, Date lastRun) {
			this.state = state;
			this.lastRun = lastRun;
		}

		public State getState() {
			return state;
		}

		public boolean isReady() {
			return state == State.READY;
		}

		/** When the last ETL run completed without error, or null */
		public Date getLastRun() {
			return lastRun;
		}

		/** For the log: what to do */
		public String describe() {
			switch (state) {
				case MISSING:
					return "the ETL database " + schema() + " does not exist; recreate the ETL tables from ETL Administration";
				case NOT_POPULATED:
					return "the ETL tables in " + schema() + " have not been populated; run the ETL from ETL Administration";
				case RUNNING:
					return "an ETL run is in progress";
				default:
					return "the ETL is ready (last run " + lastRun + ")";
			}
		}
	}

	private DqEtl() {
	}

	/** The ETL database the reports are evaluated against */
	public static String schema() {
		return CommonMetadata.EmrDatabaseEtl.ETL_DATABASE;
	}

	public static Status status() {
		String schema = schema();
		if (count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + schema
				+ "' AND table_name IN ('etl_script_status', 'etl_patient_demographics')") < 2) {
			return new Status(State.MISSING, null);
		}
		Date lastRun = date("SELECT MAX(stop_time) FROM " + schema
				+ ".etl_script_status WHERE stop_time IS NOT NULL AND error IS NULL");
		if (count("SELECT COUNT(*) FROM " + schema + ".etl_script_status WHERE stop_time IS NULL AND error IS NULL"
				+ " AND start_time >= (NOW() - INTERVAL " + STALE_RUN_MINUTES + " MINUTE)") > 0) {
			return new Status(State.RUNNING, lastRun);
		}
		if (lastRun == null
				|| count("SELECT COUNT(*) FROM (SELECT 1 FROM " + schema + ".etl_patient_demographics LIMIT 1) d") == 0) {
			return new Status(State.NOT_POPULATED, lastRun);
		}
		return new Status(State.READY, lastRun);
	}

	private static long count(String sql) {
		Object value = first(sql);
		return value instanceof Number ? ((Number) value).longValue() : 0;
	}

	/** A DATETIME comes back as a Timestamp or, from newer MySQL drivers, a LocalDateTime */
	private static Date date(String sql) {
		Object value = first(sql);
		if (value instanceof LocalDateTime) {
			return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
		}
		return value instanceof Date ? (Date) value : null;
	}

	private static Object first(String sql) {
		List<List<Object>> rows = Context.getAdministrationService().executeSQL(sql, true);
		return rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).isEmpty() ? null : rows.get(0).get(0);
	}
}
