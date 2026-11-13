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

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Whether a monthly KHIS report was complete in KHIS by its deadline (the 5th of the next month by
 * default), worked out now from the submission ledger. As G-RPT-01, and as KHIS measures
 * timeliness, a report counts once it is marked complete in KHIS: one whose values KHIS accepted
 * but that is not marked complete is not submitted, with a note saying so.
 */
public final class DqKhisTimeliness {

	public enum Status {
		ON_TIME,
		LATE,
		NOT_SUBMITTED,
		/** The deadline has not passed and the report is not complete in KHIS yet */
		NOT_DUE
	}

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	private DqKhisTimeliness() {
	}

	/**
	 * One report for one month
	 */
	public static class Entry {

		private final String report;

		private final Date deadline;

		private final Status status;

		private final Date submittedAt;

		private final Date completedAt;

		private final Integer daysLate;

		private final String note;

		Entry(String report, Date deadline, Status status, Date submittedAt, Date completedAt, Integer daysLate, String note) {
			this.report = report;
			this.deadline = deadline;
			this.status = status;
			this.submittedAt = submittedAt;
			this.completedAt = completedAt;
			this.daysLate = daysLate;
			this.note = note;
		}

		public String getReport() {
			return report;
		}

		/**
		 * @return the deadline day
		 */
		public Date getDeadline() {
			return deadline;
		}

		public Status getStatus() {
			return status;
		}

		public Date getSubmittedAt() {
			return submittedAt;
		}

		public Date getCompletedAt() {
			return completedAt;
		}

		/**
		 * @return for a late report, the days after the deadline it was marked complete; for one not
		 * submitted, the days it is overdue so far; else null
		 */
		public Integer getDaysLate() {
			return daysLate;
		}

		/**
		 * @return e.g. that it is not marked complete in KHIS, or why it was not sent
		 */
		public String getNote() {
			return note;
		}
	}

	/**
	 * @param deadlineDay the deadline, e.g. the 5th of the next month
	 * @param state the report's sends for the month, or null when there are none
	 */
	public static Entry judge(String report, Date deadlineDay, KhisSubmissionState state, Date now) {
		// The deadline lasts to the end of its day
		Date deadlineEnd = DqPeriods.addDays(DqPeriods.dayOf(deadlineDay), 1);
		Date submittedAt = state != null ? state.getFirstSentAt() : null;
		Date completedAt = state != null ? state.getCompletedAt() : null;
		// As G-RPT-01 and KHIS itself: a report counts once it is marked complete in KHIS
		if (completedAt != null) {
			if (completedAt.before(deadlineEnd)) {
				return new Entry(report, deadlineDay, Status.ON_TIME, submittedAt, completedAt, null, null);
			}
			return new Entry(report, deadlineDay, Status.LATE, submittedAt, completedAt, daysAfter(deadlineEnd, completedAt),
					null);
		}
		String note = null;
		if (submittedAt != null) {
			note = "Reached KHIS on " + new SimpleDateFormat("d MMM yyyy").format(submittedAt)
					+ " but is not marked complete there";
		} else if (state != null && KhisSubmissionState.STATUS_NO_VALUES.equals(state.getLatestStatus())) {
			note = "Every value was zero, so it was not sent";
		} else if (state != null && state.getLatestError() != null) {
			note = StringUtils.abbreviate(KhisSubmissionState.khisWords(state.getLatestError()), 200);
		}
		if (now.before(deadlineEnd)) {
			return new Entry(report, deadlineDay, Status.NOT_DUE, submittedAt, null, null, note);
		}
		return new Entry(report, deadlineDay, Status.NOT_SUBMITTED, submittedAt, null, daysAfter(deadlineEnd, now), note);
	}

	/**
	 * @return whole days after the end of the deadline day, counting the day after it as 1
	 */
	static int daysAfter(Date deadlineEnd, Date when) {
		return (int) ((when.getTime() - deadlineEnd.getTime()) / DAY_MS) + 1;
	}
}
