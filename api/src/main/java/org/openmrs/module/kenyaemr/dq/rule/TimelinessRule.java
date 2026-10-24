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

import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
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
 * Rule type TIMELINESS (malaria G-RPT-01, T22-01): each report in "reports" must reach KHIS by its
 * deadline, the 5th of the following month unless the expected report says otherwise. Checked once
 * the deadline day is over; a late or missing report is a warning, shown red on the dashboard.
 * <p>
 * One result per report, with the report name as the indicator code and the days late as the
 * measure. Settings: reports, deadlineDay (overrides the expected reports' own), severity (default
 * WARN).
 */
public class TimelinessRule extends AbstractDqRule {

	public static final String ID = "G-RPT-01";

	static final int DEFAULT_DEADLINE_DAY = 5;

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
				Date sentAt = context.getFirstSentToKhis(report, context.getPeriodStart());
				if (sentAt != null) {
					detail.put("sentAt", sentAt.getTime());
				}
				if (sentAt != null && sentAt.before(deadlineEnd)) {
					result = result(context, report, DqOutcome.PASS, report + " for " + month + " reached KHIS on "
							+ day.format(sentAt) + ", by the deadline of " + deadline + ".");
					result.setMeasureValue(0.0);
				} else if (sentAt != null) {
					long late = (sentAt.getTime() - deadlineEnd.getTime()) / DAY_MS + 1;
					result = result(context, report, severity, report + " for " + month + " reached KHIS on "
							+ day.format(sentAt) + ", " + late + (late == 1 ? " day" : " days") + " after the deadline of "
							+ deadline + ".");
					result.setMeasureValue((double) late);
				} else {
					result = result(context, report, severity, report + " for " + month + " had not reached KHIS by the "
							+ "deadline of " + deadline + ".");
				}
			}
			result.setThreshold("in KHIS by " + deadline);
			result.setDetail(json(detail));
			results.add(result);
		}
		return results;
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
