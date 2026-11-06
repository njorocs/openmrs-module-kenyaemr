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

import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.reporting.report.ReportData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Decides, from the version of a report with the same figures, whether a report generated on the
 * reports page may be sent to KHIS. See {@link DqSendGate}.
 */
public final class DqSendGates {

	private DqSendGates() {
	}

	/**
	 * The figures of a report as generated: which version of the checks they belong to
	 */
	public static class Figures {

		private final String reportName;

		private final Date periodStart;

		private final Date periodEnd;

		private final String valuesHash;

		public Figures(String reportName, Date periodStart, Date periodEnd, String valuesHash) {
			this.reportName = reportName;
			this.periodStart = periodStart;
			this.periodEnd = periodEnd;
			this.valuesHash = valuesHash;
		}

		public String getReportName() {
			return reportName;
		}

		public Date getPeriodStart() {
			return periodStart;
		}

		public Date getPeriodEnd() {
			return periodEnd;
		}

		public String getValuesHash() {
			return valuesHash;
		}
	}

	/**
	 * @return true when the checks cover a report run for these dates: a whole month that has ended
	 */
	public static boolean isChecked(Date start, Date end, Date now) {
		return start != null && end != null && DqPeriods.isWholeMonth(start, end) && end.before(DqPeriods.monthStart(now));
	}

	/**
	 * @return true when the dates are a whole week, Monday to Sunday, that has ended
	 */
	public static boolean isCheckedWeek(Date start, Date end, Date now) {
		return DqPeriods.isWholeWeek(start, end) && DqPeriods.dayOf(end).before(DqPeriods.dayOf(now));
	}

	/**
	 * @return the gate of a report sent a week at a time, generated for other dates
	 */
	public static DqSendGate wrongPeriod(String reportName, Date start, Date end, Date now) {
		SimpleDateFormat day = new SimpleDateFormat("d MMM yyyy");
		String message;
		if ((DqPeriods.isWholeWeek(start, end) || DqPeriods.isWholeMonth(start, end)) && !end.before(DqPeriods.dayOf(now))) {
			message = "The period is not over yet: " + reportName + " can be checked and sent once " + day.format(end)
					+ " has passed.";
		} else {
			message = reportName + " is sent to KHIS a whole week (Monday to Sunday) or a whole month at a time, and "
					+ "these dates (" + day.format(start) + " to " + day.format(end) + ") are neither. Generate it for a "
					+ "whole week or month to check and send it.";
		}
		return new DqSendGate(DqSendGate.State.WRONG_PERIOD, null, message, null);
	}

	/**
	 * @return the report's figures, or null when the report or its dates cannot be read
	 */
	public static Figures figuresOf(String reportDefinitionUuid, ReportData reportData) {
		if (reportData == null || reportData.getContext() == null) {
			return null;
		}
		Object start = reportData.getContext().getParameterValue("startDate");
		Object end = reportData.getContext().getParameterValue("endDate");
		if (!(start instanceof Date) || !(end instanceof Date)) {
			return null;
		}
		DqReportGenerator.GeneratedRun generated = new DqReportGenerator().fromReportData(reportDefinitionUuid, reportData,
				(Date) start, (Date) end);
		if (generated == null) {
			return null;
		}
		return new Figures(generated.getRun().getReportName(), (Date) start, (Date) end, generated.getRun().getValuesHash());
	}

	/**
	 * @param latest the latest version of the report for the month, or null
	 * @param same true when the latest version has the figures to send
	 * @param checking true when the checks are running, or about to, for the report and month
	 * @param results the latest version's results, when it has the figures to send
	 */
	public static DqSendGate gate(DqReportRun latest, boolean same, boolean checking, List<DqResult> results) {
		if (!same || latest.getStatus() == DqRunStatus.GENERATED) {
			if (checking) {
				return new DqSendGate(DqSendGate.State.CHECKING, latest, "Data quality checks are running on these figures. "
						+ "Sending is possible once they finish with nothing blocking and nothing waiting for review.", null);
			}
			if (latest == null || same) {
				return new DqSendGate(DqSendGate.State.NOT_CHECKED, latest, "These figures have not been checked for data "
						+ "quality yet. Run the checks before sending.", null);
			}
			return new DqSendGate(DqSendGate.State.NOT_LATEST, latest, "These are not the latest figures: version "
					+ latest.getVersion() + " of the report" + generated(latest) + " has other figures. Generate the report "
					+ "again, or open the latest request, to send the current figures.", null);
		}
		List<String> blockers = messages(results, DqOutcome.BLOCK);
		List<String> warnings = messages(results, DqOutcome.WARN);
		List<String> tickets = messages(results, DqOutcome.TICKET);
		DqSendGate gate;
		switch (latest.getStatus()) {
			case APPROVED:
				gate = new DqSendGate(DqSendGate.State.READY, latest, "Checked and approved.", null);
				break;
			case BLOCKED:
				gate = new DqSendGate(DqSendGate.State.BLOCKED, latest, (blockers.size() == 1 ? "1 check blocks"
						: blockers.size() + " checks block") + " this report. Correct the records, refresh the ETL and "
						+ "generate the report again.", blockers);
				break;
			case AWAITING_APPROVAL:
				if (warnings.isEmpty()) {
					gate = new DqSendGate(DqSendGate.State.READY, latest, "Checked: nothing blocks this report and nothing "
							+ "needs review.", null);
				} else {
					gate = new DqSendGate(DqSendGate.State.NEEDS_REVIEW, latest, (warnings.size() == 1 ? "1 warning needs"
							: warnings.size() + " warnings need") + " a reason before sending. Give the reasons and approve "
							+ "the report on the Data quality page, then come back here to send.", warnings);
				}
				break;
			default:
				return new DqSendGate(DqSendGate.State.NOT_LATEST, latest, "This version (version " + latest.getVersion()
						+ ") is " + latest.getStatus() + ". Generate the report again to send it.", null);
		}
		// A warning answered when the report was approved needs nothing more
		return gate.withFindings(blockers, latest.getStatus() == DqRunStatus.APPROVED ? new ArrayList<String>() : warnings,
				tickets);
	}

	private static String generated(DqReportRun run) {
		return run.getGeneratedAt() != null ? ", generated " + new SimpleDateFormat("d MMM yyyy HH:mm").format(run.getGeneratedAt())
				: "";
	}

	private static List<String> messages(List<DqResult> results, DqOutcome outcome) {
		List<String> messages = new ArrayList<String>();
		for (DqResult result : results) {
			if (result.getOutcome() == outcome) {
				messages.add(result.getRuleId() + ": " + result.getMessage());
			}
		}
		return messages;
	}
}
