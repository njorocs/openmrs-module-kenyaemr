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

import org.openmrs.module.kenyaemr.model.dq.DqResult;

import java.util.List;

/**
 * One TR-26-110 validation rule
 */
public interface DqRule {

	/**
	 * @return the rule id, e.g. "T22-04"
	 */
	String getId();

	/**
	 * @return true when the rule checks the report of the run in the context
	 */
	boolean appliesTo(DqRuleContext context);

	/**
	 * Evaluates the rule. The results are not saved.
	 *
	 * @return one result per indicator, indicator pair or report checked
	 */
	List<DqResult> evaluate(DqRuleContext context);
}
