/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.task;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Optional auto-send on the deadline (off by default, per facility). Runs late each day so the HRIO
 * keeps the whole deadline day: from then on, last month's reports still awaiting approval and with no
 * open block are approved automatically, and kenyaemrIL sends them like any approved report.
 */
public class DataQualityAutoApprovalTask extends AbstractTask {

	public static final String GP_ENABLED = "kenyaemr.dq.autoApproveOnDeadline";

	private static final Logger log = LoggerFactory.getLogger(DataQualityAutoApprovalTask.class);

	@Override
	public void execute() {
		if (isExecuting) {
			return;
		}
		startExecuting();
		try {
			if (!"true".equalsIgnoreCase(StringUtils.trim(Context.getAdministrationService().getGlobalProperty(GP_ENABLED)))) {
				return;
			}
			for (DqReportRun run : Context.getService(DqService.class).autoApproveDueReports(new Date())) {
				log.info("Data quality: approved " + run.getReportName() + " for " + run.getPeriodStart() + " (version "
						+ run.getVersion() + ") automatically on its deadline");
			}
		}
		catch (RuntimeException e) {
			log.error("Data quality: automatic approval failed", e);
		}
		finally {
			stopExecuting();
		}
	}
}
