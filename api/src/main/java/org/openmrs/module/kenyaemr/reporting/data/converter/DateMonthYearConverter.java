/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.data.converter;

import org.openmrs.module.kenyaemr.reporting.ARTCohortReportUtils;
import org.openmrs.module.kenyaemr.reporting.RDQAReportUtils;
import org.openmrs.module.reporting.data.converter.DataConverter;

import java.util.Date;

/**
 * Converts a date to its month and year (MM/yyyy) for the ART cohort reports; "Missing" when there is
 * none
 */
public class DateMonthYearConverter implements DataConverter {

	@Override
	public Object convert(Object original) {
		if (!(original instanceof Date)) {
			return "Missing";
		}
		return RDQAReportUtils.formatdates((Date) original, ARTCohortReportUtils.DATE_FORMAT);
	}

	@Override
	public Class<?> getInputDataType() {
		return Date.class;
	}

	@Override
	public Class<?> getDataType() {
		return String.class;
	}
}
