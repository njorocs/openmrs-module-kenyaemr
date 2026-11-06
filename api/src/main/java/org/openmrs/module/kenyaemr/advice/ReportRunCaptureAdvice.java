/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.report.Report;
import org.openmrs.module.reporting.report.ReportRequest;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * Stores each monthly report the HRIO runs on the reports page (old or new UI) for the data quality
 * pages, so they read the reports as run without regenerating them. The reporting module deletes
 * its own results after a few days.
 */
public class ReportRunCaptureAdvice implements AfterReturningAdvice {

	private static final Log log = LogFactory.getLog(ReportRunCaptureAdvice.class);

	@Override
	public void afterReturning(Object returned, Method method, Object[] args, Object target) {
		if (!method.getName().equals("runReport") || !(returned instanceof Report)) {
			return;
		}
		Report report = (Report) returned;
		ReportRequest request = report.getRequest();
		if (report.getReportData() == null || request == null || request.getStatus() != ReportRequest.Status.COMPLETED
				|| request.getReportDefinition() == null || request.getReportDefinition().getParameterizable() == null) {
			return;
		}
		EvaluationContext context = report.getReportData().getContext();
		Object start = context != null ? context.getParameterValue("startDate") : null;
		Object end = context != null ? context.getParameterValue("endDate") : null;
		if (!(start instanceof Date) || !(end instanceof Date)) {
			return;
		}
		try {
			DqReportRun run = Context.getService(DqService.class).captureReportRun(
					request.getReportDefinition().getParameterizable().getUuid(), report.getReportData(), (Date) start,
					(Date) end);
			if (run != null) {
				log.info("Data quality: stored " + run.getReportName() + " for " + run.getPeriodStart()
						+ " as run on the reports page (version " + run.getVersion() + ")");
			}
		}
		catch (Exception e) {
			log.error("Data quality: could not store the report run " + request.getUuid(), e);
		}
	}
}
