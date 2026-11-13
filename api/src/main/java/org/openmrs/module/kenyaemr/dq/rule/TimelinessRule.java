/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq.rule;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.dq.KhisSubmissionState;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule type TIMELINESS (malaria G-RPT-01, T22-01): each report in "reports" must be complete in
 * KHIS by its deadline, the 5th of the following month unless the expected report says otherwise.
 * KHIS counts a report as reported only once it is marked complete, so values saved without the
 * completion count as missing. A late, incomplete or missing report is a warning, shown red on the
 * dashboard.
 * <p>
 * The completion date is KHIS's own, read back from the 10th (khisAggregate.readBackDay), so the
 * rule is checked once that day is over; when KHIS could not be asked, the EMR's record of
 * registering the completion is used. A report whose values were all zero is not sent, and says
 * so.
 * <p>
 * One result per report, with the report name as the indicator code and the days late as the
 * measure. Settings: reports, deadlineDay (overrides the expected reports' own), checkAfterDay
 * (default 10, the read-back day), severity (default WARN).
 */
public class TimelinessRule extends AbstractDqRule {

	public static final String ID = "G-RPT-01";

	static final int DEFAULT_DEADLINE_DAY = 5;

	/** The day KHIS is read back (khisAggregate.readBackDay's default) */
	static final int DEFAULT_CHECK_AFTER_DAY = 10;

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	public TimelinessRule(DqConfig.RuleDefinition definition) {
		super(definition);
	}

	@Override
	public Schedule getSchedule() {
		return Schedule.MONTH;
	}

	@Override
	public Set<String> getReports(DqConfig config) {
		return new LinkedHashSet<String>(settings().getStrings("reports"));
	}

	@Override
	public boolean appliesTo(DqRuleContext context) {
		return context.getRun() == null && !settings().getStrings("reports").isEmpty();
	}

	/**
	 * @return the end of the latest deadline day of the rule's reports for the month; the rule is
	 * checked after it
	 */
	public Date lastDeadline(DqConfig config, Date month) {
		Date last = null;
		for (String report : settings().getStrings("reports")) {
			Date end = deadlineEnd(config, report, month);
			if (last == null || end.after(last)) {
				last = end;
			}
		}
		return last;
	}

	/**
	 * @return when the rule can be checked for the month: once the last deadline and the KHIS
	 * read-back day (checkAfterDay) are both over
	 */
	@Override
	public Date checkAfter(DqConfig config, Date month) {
		Date readBack = DqPeriods.addDays(DqPeriods.deadline(month, settings().getInteger("checkAfterDay",
				DEFAULT_CHECK_AFTER_DAY)), 1);
		Date last = lastDeadline(config, month);
		return last == null || readBack.after(last) ? readBack : last;
	}

	@Override
	public List<DqResult> evaluate(DqRuleContext context) {
		DqRuleSettings settings = settings();
		DqOutcome severity = settings.getOutcome("severity", DqOutcome.WARN);
		SimpleDateFormat day = new SimpleDateFormat("d MMM yyyy");
		String month = new SimpleDateFormat("MMMM yyyy").format(context.getPeriodStart());

		List<DqResult> results = new ArrayList<DqResult>();
		for (String report : settings.getStrings("reports")) {
			Date deadlineEnd = deadlineEnd(context.getConfig(), report, context.getPeriodStart());
			String deadline = day.format(new Date(deadlineEnd.getTime() - 1));
			Map<String, Object> detail = new LinkedHashMap<String, Object>();
			detail.put("report", report);
			detail.put("deadline", deadline);

			DqResult result;
			if (!context.isKhisSubmissionTracked()) {
				result = result(context, report, DqOutcome.NOT_APPLICABLE, "Sending to KHIS is not tracked on this "
						+ "server (the KenyaEMR IL module is not installed), so the timeliness of " + report + " cannot be checked.");
			} else {
				result = check(context, report, month, deadline, deadlineEnd, severity, day, detail);
			}
			result.setThreshold("complete in KHIS by " + deadline);
			result.setDetail(json(detail));
			results.add(result);
		}
		return results;
	}

	private DqResult check(DqRuleContext context, String report, String month, String deadline, Date deadlineEnd,
	                       DqOutcome severity, SimpleDateFormat day, Map<String, Object> detail) {
		KhisSubmissionState state = context.getKhisSubmission(report, context.getPeriodStart());
		String name = report + " for " + month;
		if (state == null) {
			return result(context, report, severity, name + " had not been sent to KHIS by the deadline of " + deadline
					+ ", so it is incomplete in KHIS.");
		}
		detail.put("status", state.getLatestStatus());
		if (state.getFirstSentAt() != null) {
			detail.put("sentAt", state.getFirstSentAt().getTime());
		}
		Date completedAt = state.getCompletedAt();
		if (completedAt != null) {
			detail.put("completedAt", completedAt.getTime());
			detail.put("completionSource", state.isCompletionReadFromKhis() ? "KHIS" : "EMR");
			DqResult result;
			if (completedAt.before(deadlineEnd)) {
				result = result(context, report, DqOutcome.PASS, name + " was complete in KHIS on " + day.format(completedAt)
						+ ", by the deadline of " + deadline + ".");
				result.setMeasureValue(0.0);
			} else {
				long late = (completedAt.getTime() - deadlineEnd.getTime()) / DAY_MS + 1;
				result = result(context, report, severity, name + " was complete in KHIS on " + day.format(completedAt) + ", "
						+ late + (late == 1 ? " day" : " days") + " after the deadline of " + deadline + ".");
				result.setMeasureValue((double) late);
			}
			return result;
		}
		String khis = KhisSubmissionState.khisWords(state.getLatestError());
		// Abbreviated: when KHIS's reply is not JSON it is the whole body, e.g. an HTML error page
		String reason = khis != null ? " " + StringUtils.removeEnd(StringUtils.abbreviate(khis, 300), ".") + "." : "";
		if (state.getFirstSentAt() != null) {
			return result(context, report, severity, name + " reached KHIS on " + day.format(state.getFirstSentAt())
					+ " but is not marked complete there, so it is incomplete in KHIS." + reason);
		}
		if (KhisSubmissionState.STATUS_NO_VALUES.equals(state.getLatestStatus())) {
			return result(context, report, severity, name + " was not sent to KHIS because every value in it is zero, "
					+ "so it is incomplete in KHIS. A functioning facility should not have an empty report: check that "
					+ "the month's data was captured.");
		}
		return result(context, report, severity, name + " had not reached KHIS by the deadline of " + deadline
				+ ", so it is incomplete in KHIS." + reason);
	}


	private Date deadlineEnd(DqConfig config, String report, Date month) {
		Integer day = settings().getInteger("deadlineDay", null);
		if (day == null) {
			DqConfig.ExpectedReport expected = config.getExpectedReport(report);
			day = expected != null && expected.getDeadlineDay() != null ? expected.getDeadlineDay() : DEFAULT_DEADLINE_DAY;
		}
		return DqPeriods.addDays(DqPeriods.deadline(month, day), 1);
	}
}
