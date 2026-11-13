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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The series of the single-facility dashboard
 */
public class DqDashboardTest {

	private final Date march = TestUtils.date(2026, 3, 1);

	private final Date april = TestUtils.date(2026, 4, 1);

	@Test
	public void reporting_shouldCountReportsThatReachedKhisAndThoseCompleteOnTime() {
		List<DqResult> results = Arrays.asList(
				result("G-RPT-01", "MOH 705A", march, DqOutcome.PASS, "{\"completedAt\":1}", 1),
				result("G-RPT-01", "MOH 706", march, DqOutcome.WARN, "{\"sentAt\":1}", 1),
				result("G-RPT-01", "MOH 711", march, DqOutcome.WARN, "{}", 1),
				result("G-RPT-01", "MOH 643", march, DqOutcome.NOT_APPLICABLE, "{}", 1),
				// Checked again later: the latest counts
				result("G-RPT-01", "MOH 711", march, DqOutcome.PASS, "{\"completedAt\":1}", 2));

		List<DqDashboard.ReportingPoint> points = DqDashboard.reporting(results, march, april);

		Assert.assertEquals(2, points.size());
		DqDashboard.ReportingPoint point = points.get(0);
		Assert.assertEquals(3, point.getExpected());
		Assert.assertEquals(3, point.getSent());
		Assert.assertEquals(2, point.getOnTime());
		Assert.assertEquals(2.0 / 3, point.getTimeliness(), 1e-12);
		Assert.assertFalse(points.get(1).isChecked());
		Assert.assertNull(points.get(1).getCompleteness());
	}

	@Test
	public void consistency_shouldUseTheThreeMonthsBeforeAndTheSameMonthLastYear() {
		final Map<Long, Double> values = new HashMap<Long, Double>();
		values.put(march.getTime(), 120.0);
		values.put(DqPeriods.addMonths(march, -1).getTime(), 90.0);
		values.put(DqPeriods.addMonths(march, -2).getTime(), 100.0);
		values.put(DqPeriods.addMonths(march, -3).getTime(), 110.0);
		values.put(DqPeriods.addMonths(march, -12).getTime(), 80.0);
		DqDashboard.Values lookup = new DqDashboard.Values() {

			@Override
			public DqIndicatorCalculator.IndicatorValue get(String code, Date month) {
				return new DqIndicatorCalculator.IndicatorValue(code, values.get(month.getTime()), Collections.<String>emptyList());
			}
		};

		List<DqDashboard.ConsistencyPoint> points = DqDashboard.consistency(lookup, "MAL_CONFIRMED_705", march, april);

		Assert.assertEquals(1.2, points.get(0).getMonthToMonth(), 1e-12);
		Assert.assertEquals(1.5, points.get(0).getAnnual(), 1e-12);
		// April has no value of its own, so no ratios, though January to March average 103.3
		Assert.assertNull(points.get(1).getMonthToMonth());
		Assert.assertEquals(310.0 / 3, points.get(1).getThreeMonthAverage(), 1e-12);
		Assert.assertNull(points.get(1).getAnnual());
	}

	@Test
	public void crossCheck_shouldDivideTheTwoSourcesEachMonth() {
		DqConfig.CrossCheck check = new DqConfig.CrossCheck();
		check.setNumerator("MAL_TESTS_706");
		check.setDenominator("MAL_SUSPECTED_705");
		DqDashboard.Values lookup = new DqDashboard.Values() {

			@Override
			public DqIndicatorCalculator.IndicatorValue get(String code, Date month) {
				Double value = month.equals(march) ? ("MAL_TESTS_706".equals(code) ? 45.0 : 50.0) : null;
				return new DqIndicatorCalculator.IndicatorValue(code, value, Collections.<String>emptyList());
			}
		};

		List<DqDashboard.RatioPoint> points = DqDashboard.crossCheck(lookup, check, march, april);

		Assert.assertEquals(0.9, points.get(0).getRatio(), 1e-12);
		Assert.assertNull(points.get(1).getRatio());
	}

	private static DqResult result(String ruleId, String code, Date month, DqOutcome outcome, String detail, long evaluated) {
		DqResult result = new DqResult();
		result.setRuleId(ruleId);
		result.setIndicatorCode(code);
		result.setPeriodStart(month);
		result.setOutcome(outcome);
		result.setDetail(detail);
		result.setEvaluatedAt(new Date(evaluated));
		return result;
	}
}
