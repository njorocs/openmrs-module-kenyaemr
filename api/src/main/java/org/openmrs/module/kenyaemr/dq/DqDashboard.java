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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The series of a program's mRDQA-style dashboard for one facility. With one facility there are
 * no sites to compare, so each panel is drawn month by month: completeness and timeliness of the
 * key reports (from G-RPT-01), the cascade, cross checks and consistency over time (from the
 * stored indicator values), and the verification factors of one month (VF-01).
 */
public final class DqDashboard {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private DqDashboard() {
	}

	/**
	 * Values looked up by indicator code and month
	 */
	public interface Values {

		DqIndicatorCalculator.IndicatorValue get(String code, Date month);
	}

	/**
	 * Completeness (reports that reached KHIS) and timeliness (reports complete in KHIS by the
	 * deadline) of the key reports, each month, from the timeliness results
	 *
	 * @param results the timeliness rule's results; a report checked more than once counts its latest
	 */
	public static List<ReportingPoint> reporting(List<DqResult> results, Date first, Date last) {
		Map<Long, Map<String, DqResult>> byMonth = latestByMonthAndCode(results);
		List<ReportingPoint> points = new ArrayList<ReportingPoint>();
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			Map<String, DqResult> reports = byMonth.get(month.getTime());
			int expected = 0;
			int sent = 0;
			int onTime = 0;
			if (reports != null) {
				for (DqResult result : reports.values()) {
					if (result.getOutcome() == DqOutcome.NOT_APPLICABLE) {
						continue;
					}
					expected++;
					Map<String, Object> detail = detail(result);
					if (detail.get("sentAt") != null || detail.get("completedAt") != null) {
						sent++;
					}
					if (result.getOutcome() == DqOutcome.PASS) {
						onTime++;
					}
				}
			}
			points.add(new ReportingPoint(month, reports != null, expected, sent, onTime));
		}
		return points;
	}

	/**
	 * A cross check each month: numerator ÷ denominator of the stored indicator values
	 */
	public static List<RatioPoint> crossCheck(Values values, DqConfig.CrossCheck check, Date first, Date last) {
		List<RatioPoint> points = new ArrayList<RatioPoint>();
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			DqIndicatorCalculator.IndicatorValue numerator = values.get(check.getNumerator(), month);
			DqIndicatorCalculator.IndicatorValue denominator = values.get(check.getDenominator(), month);
			points.add(new RatioPoint(month, numerator.getValue(), denominator.getValue()));
		}
		return points;
	}

	/**
	 * Consistency over time each month, as mRDQA IV: the month ÷ the average of the three months
	 * before it, and the month ÷ the same month a year before. A ratio needs every month it uses.
	 *
	 * @param values must cover the 12 months before first as well
	 */
	public static List<ConsistencyPoint> consistency(Values values, String code, Date first, Date last) {
		List<ConsistencyPoint> points = new ArrayList<ConsistencyPoint>();
		for (Date month = DqPeriods.monthStart(first); !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			Double value = values.get(code, month).getValue();
			Double sum = 0.0;
			for (int i = 1; i <= 3 && sum != null; i++) {
				Double earlier = values.get(code, DqPeriods.addMonths(month, -i)).getValue();
				sum = earlier != null ? sum + earlier : null;
			}
			Double average = sum != null ? sum / 3 : null;
			Double lastYear = values.get(code, DqPeriods.addMonths(month, -12)).getValue();
			points.add(new ConsistencyPoint(month, value, average, lastYear));
		}
		return points;
	}

	/**
	 * @return the verification results of the latest month that has them, or none
	 */
	public static List<DqResult> latestMonth(List<DqResult> results) {
		Date latest = null;
		for (DqResult result : results) {
			if (latest == null || result.getPeriodStart().after(latest)) {
				latest = result.getPeriodStart();
			}
		}
		if (latest == null) {
			return Collections.emptyList();
		}
		Map<Long, Map<String, DqResult>> byMonth = latestByMonthAndCode(results);
		return new ArrayList<DqResult>(byMonth.get(DqPeriods.monthStart(latest).getTime()).values());
	}

	public static Double ratio(Double numerator, Double denominator) {
		return numerator == null || denominator == null || denominator == 0 ? null : numerator / denominator;
	}

	private static Map<Long, Map<String, DqResult>> latestByMonthAndCode(List<DqResult> results) {
		Map<Long, Map<String, DqResult>> byMonth = new LinkedHashMap<Long, Map<String, DqResult>>();
		for (DqResult result : results) {
			Long month = DqPeriods.monthStart(result.getPeriodStart()).getTime();
			if (!byMonth.containsKey(month)) {
				byMonth.put(month, new LinkedHashMap<String, DqResult>());
			}
			String code = String.valueOf(result.getIndicatorCode());
			DqResult other = byMonth.get(month).get(code);
			if (other == null || (result.getEvaluatedAt() != null && (other.getEvaluatedAt() == null
					|| result.getEvaluatedAt().after(other.getEvaluatedAt())))) {
				byMonth.get(month).put(code, result);
			}
		}
		return byMonth;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> detail(DqResult result) {
		if (result.getDetail() == null) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(result.getDetail(), Map.class);
		}
		catch (IOException e) {
			return Collections.emptyMap();
		}
	}

	/**
	 * One month of the key reports
	 */
	public static class ReportingPoint {

		private final Date month;

		private final boolean checked;

		private final int expected;

		private final int sent;

		private final int onTime;

		ReportingPoint(Date month, boolean checked, int expected, int sent, int onTime) {
			this.month = month;
			this.checked = checked;
			this.expected = expected;
			this.sent = sent;
			this.onTime = onTime;
		}

		public Date getMonth() {
			return month;
		}

		/**
		 * @return false before the month's timeliness check has run
		 */
		public boolean isChecked() {
			return checked;
		}

		public int getExpected() {
			return expected;
		}

		/**
		 * @return key reports whose values reached KHIS
		 */
		public int getSent() {
			return sent;
		}

		/**
		 * @return key reports complete in KHIS by their deadline
		 */
		public int getOnTime() {
			return onTime;
		}

		public Double getCompleteness() {
			return expected == 0 ? null : (double) sent / expected;
		}

		public Double getTimeliness() {
			return expected == 0 ? null : (double) onTime / expected;
		}
	}

	/**
	 * One month of a cross check
	 */
	public static class RatioPoint {

		private final Date month;

		private final Double numerator;

		private final Double denominator;

		RatioPoint(Date month, Double numerator, Double denominator) {
			this.month = month;
			this.numerator = numerator;
			this.denominator = denominator;
		}

		public Date getMonth() {
			return month;
		}

		public Double getNumerator() {
			return numerator;
		}

		public Double getDenominator() {
			return denominator;
		}

		public Double getRatio() {
			return ratio(numerator, denominator);
		}
	}

	/**
	 * One month of consistency over time
	 */
	public static class ConsistencyPoint {

		private final Date month;

		private final Double value;

		private final Double threeMonthAverage;

		private final Double sameMonthLastYear;

		ConsistencyPoint(Date month, Double value, Double threeMonthAverage, Double sameMonthLastYear) {
			this.month = month;
			this.value = value;
			this.threeMonthAverage = threeMonthAverage;
			this.sameMonthLastYear = sameMonthLastYear;
		}

		public Date getMonth() {
			return month;
		}

		public Double getValue() {
			return value;
		}

		public Double getThreeMonthAverage() {
			return threeMonthAverage;
		}

		public Double getSameMonthLastYear() {
			return sameMonthLastYear;
		}

		/**
		 * @return the month ÷ the average of the three months before it
		 */
		public Double getMonthToMonth() {
			return ratio(value, threeMonthAverage);
		}

		/**
		 * @return the month ÷ the same month a year before
		 */
		public Double getAnnual() {
			return ratio(value, sameMonthLastYear);
		}
	}
}
