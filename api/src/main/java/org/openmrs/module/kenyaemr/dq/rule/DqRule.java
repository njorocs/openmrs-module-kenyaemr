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
import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * One validation rule of a program area, e.g. TR-26-110 T22-04 for malaria
 */
public interface DqRule {

	/**
	 * When a rule is checked
	 */
	enum Schedule {
		/** When a report is generated; the results belong to that report version */
		REPORT,
		/** Each week, for the week before (T22-08) */
		WEEK,
		/** Once the month's deadline has passed (G-RPT-01) */
		MONTH,
		/** When a sent report has been read back from KHIS (G-RPT-02) */
		KHIS_READBACK
	}

	/**
	 * @return the rule id, e.g. "T22-04"
	 */
	String getId();

	/**
	 * @return the code of the program the rule belongs to, e.g. "MALARIA"
	 */
	String getProgram();

	DqConfig.RuleDefinition getDefinition();

	Schedule getSchedule();

	/**
	 * @return the reports the rule reads; a rule that reads more than one cannot block a report
	 * unless it names it in "gates"
	 */
	Set<String> getReports(DqConfig config);

	/**
	 * @return true when the rule checks the report of the run in the context
	 */
	boolean appliesTo(DqRuleContext context);

	/**
	 * @return for a MONTH rule, when the month can be checked; by default once the month is over
	 */
	Date checkAfter(DqConfig config, Date month);

	/**
	 * Evaluates the rule. The results are not saved.
	 *
	 * @return one result per indicator, indicator pair or report checked
	 */
	List<DqResult> evaluate(DqRuleContext context);
}
