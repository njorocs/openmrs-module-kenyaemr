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

import java.util.Calendar;
import java.util.Date;

/**
 * Reporting period arithmetic. Monthly periods are identified by their first day; weekly periods
 * (for T22-08) run Monday to Sunday.
 */
public final class DqPeriods {

	private DqPeriods() {
	}

	/**
	 * @return midnight on the first day of the month containing the date
	 */
	public static Date monthStart(Date date) {
		Calendar calendar = startOfDay(date);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}

	/**
	 * @return the last moment of the month containing the date
	 */
	public static Date monthEnd(Date date) {
		Calendar calendar = startOfDay(monthStart(date));
		calendar.add(Calendar.MONTH, 1);
		calendar.add(Calendar.MILLISECOND, -1);
		return calendar.getTime();
	}

	/**
	 * @return midnight on the last day of the month, the endDate the report UI passes for a month
	 */
	public static Date lastDayOfMonth(Date date) {
		Calendar calendar = startOfDay(monthStart(date));
		calendar.add(Calendar.MONTH, 1);
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		return calendar.getTime();
	}

	/**
	 * @return the first day of the month before the one containing the date
	 */
	public static Date previousMonth(Date date) {
		return addMonths(monthStart(date), -1);
	}

	/**
	 * @return midnight on the deadline day of a month's report, in the month after it; a day past
	 * the end of that month falls on its last day
	 */
	public static Date deadline(Date month, int deadlineDay) {
		Calendar calendar = startOfDay(addMonths(monthStart(month), 1));
		calendar.set(Calendar.DAY_OF_MONTH, Math.min(Math.max(deadlineDay, 1), calendar.getActualMaximum(Calendar.DAY_OF_MONTH)));
		return calendar.getTime();
	}

	public static Date addMonths(Date monthStart, int months) {
		Calendar calendar = startOfDay(monthStart);
		calendar.add(Calendar.MONTH, months);
		return calendar.getTime();
	}

	/**
	 * @return midnight on the Monday of the week containing the date
	 */
	public static Date weekStart(Date date) {
		Calendar calendar = startOfDay(date);
		int daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
		calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
		return calendar.getTime();
	}

	/**
	 * @return the last moment of the week starting on the given Monday
	 */
	public static Date weekEnd(Date weekStart) {
		Calendar calendar = startOfDay(weekStart);
		calendar.add(Calendar.DAY_OF_MONTH, 7);
		calendar.add(Calendar.MILLISECOND, -1);
		return calendar.getTime();
	}

	public static Date addDays(Date date, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, days);
		return calendar.getTime();
	}

	private static Calendar startOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
}
