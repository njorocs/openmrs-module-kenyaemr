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
import org.openmrs.module.kenyaemr.dq.DqIndicatorCalculator;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * What a rule can see while it is evaluated
 */
public interface DqRuleContext {

	DqConfig getConfig();

	/**
	 * @return the run being checked, or null for weekly rules that are not tied to a report
	 */
	DqReportRun getRun();

	/**
	 * @return the first moment of the period being checked
	 */
	Date getPeriodStart();

	/**
	 * @return the last day of the period being checked
	 */
	Date getPeriodEnd();

	/**
	 * @param code the indicator code
	 * @param monthStart the first day of the month
	 * @return the indicator from the latest version of each of its reports for that month; the
	 * value is null when none of those reports has been generated for the month
	 */
	DqIndicatorCalculator.IndicatorValue getIndicator(String code, Date monthStart);

	/**
	 * @param code the indicator code
	 * @return the indicator for any period, e.g. a week; a period other than a whole month is counted
	 * by running the indicator's reports for it, without storing anything
	 */
	DqIndicatorCalculator.IndicatorValue getIndicator(String code, Date start, Date end);

	/**
	 * @return the stored values of a report version, with the KHIS values read back
	 */
	List<DqValue> getValues(DqReportRun run);

	/**
	 * @return true when sending to KHIS is recorded on this server (the KenyaEMR IL module's ledger)
	 */
	boolean isKhisSubmissionTracked();

	/**
	 * @return when any version of the report for the month first reached KHIS, or null if none has
	 */
	Date getFirstSentToKhis(String reportName, Date monthStart);

	/**
	 * @return true when the report has been generated for the month
	 */
	boolean isGenerated(String reportName, Date monthStart);

	/**
	 * @return the name of the KenyaEMR ETL schema
	 */
	String getEtlDatabase();

	List<Object[]> query(String sql, Map<String, Object> parameters);

	/**
	 * @return a context for the month, with no run, sharing this context's stored values; the trend
	 * charts use it to check month after month without reading the same values again
	 */
	DqRuleContext forMonth(Date monthStart);
}
