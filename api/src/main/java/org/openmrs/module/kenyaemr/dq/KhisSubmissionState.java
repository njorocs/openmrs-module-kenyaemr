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

import java.util.Date;
import java.util.List;

/**
 * Where a report for a month stands in KHIS, from the KenyaEMR IL submission ledger, across all
 * the versions sent. KHIS counts a report as reported only once it is marked complete, so that is
 * what timeliness (G-RPT-01) looks at.
 */
public class KhisSubmissionState {

	/** Ledger status of a version whose values were all zero, so it was not sent */
	public static final String STATUS_NO_VALUES = "NO_VALUES";

	private final Date firstSentAt;

	private final Date completedAt;

	private final boolean completionReadFromKhis;

	private final String latestStatus;

	private final String latestError;

	/**
	 * @param firstSentAt when KHIS first saved values of any version, or null
	 * @param completedAt when the report was completed in KHIS, or null when it is not complete
	 * @param completionReadFromKhis true when completedAt is KHIS's own answer, read back; false when
	 * it is what the EMR recorded when it registered the completion
	 * @param latestStatus the ledger status of the latest version
	 * @param latestError why the latest version is not where it should be, or null
	 */
	public KhisSubmissionState(Date firstSentAt, Date completedAt, boolean completionReadFromKhis, String latestStatus,
	                           String latestError) {
		this.firstSentAt = firstSentAt;
		this.completedAt = completedAt;
		this.completionReadFromKhis = completionReadFromKhis;
		this.latestStatus = latestStatus;
		this.latestError = latestError;
	}

	public Date getFirstSentAt() {
		return firstSentAt;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public boolean isCompletionReadFromKhis() {
		return completionReadFromKhis;
	}

	public String getLatestStatus() {
		return latestStatus;
	}

	/**
	 * @return KHIS's own status and words from a reply or error, e.g. "KHIS: ERROR, Period is
	 * locked"; the text as it is when it is not KHIS's JSON; null for none
	 */
	public static String khisWords(String reply) {
		if (reply == null || reply.trim().isEmpty()) {
			return null;
		}
		String text = reply.trim();
		if (!text.startsWith("{")) {
			return text;
		}
		try {
			org.codehaus.jackson.JsonNode root = new org.codehaus.jackson.map.ObjectMapper().readTree(text);
			String status = root.path("response").path("status").getTextValue();
			if (status == null) {
				status = root.path("status").getTextValue();
			}
			String message = root.path("message").getTextValue();
			if (message == null) {
				message = root.path("response").path("description").getTextValue();
			}
			if (status == null && message == null) {
				return text;
			}
			return "KHIS: " + (status != null ? status : "") + (status != null && message != null ? ", " : "")
					+ (message != null ? message : "");
		}
		catch (Exception e) {
			return text;
		}
	}

	public String getLatestError() {
		return latestError;
	}

	/**
	 * @param rows the month's ledger rows for the report, latest version first, each: status,
	 * sent_at, completed_at, last_error, completion_read_at, khis_completed_at
	 * @return the state, or null when there are no rows
	 */
	public static KhisSubmissionState fromLedger(List<Object[]> rows) {
		if (rows.isEmpty()) {
			return null;
		}
		Date firstSentAt = null;
		Date recordedCompletion = null;
		Date lastRead = null;
		Date khisCompletion = null;
		for (Object[] row : rows) {
			firstSentAt = earliest(firstSentAt, date(row[1]));
			recordedCompletion = earliest(recordedCompletion, date(row[2]));
			// KHIS holds one completion per dataset and month, whichever version set it: its latest answer stands
			Date read = date(row[4]);
			if (read != null && (lastRead == null || read.after(lastRead))) {
				lastRead = read;
				khisCompletion = date(row[5]);
			}
		}
		Object[] latest = rows.get(0);
		return new KhisSubmissionState(firstSentAt, lastRead != null ? khisCompletion : recordedCompletion, lastRead != null,
				latest[0] != null ? latest[0].toString() : null, latest[3] != null ? latest[3].toString() : null);
	}

	private static Date date(Object value) {
		return value instanceof Date ? (Date) value : null;
	}

	private static Date earliest(Date a, Date b) {
		return a == null || (b != null && b.before(a)) ? b : a;
	}
}
