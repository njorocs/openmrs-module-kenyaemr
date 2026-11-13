/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Tests for {@link DqService} data access
 */
public class DqServiceTest extends BaseModuleContextSensitiveTest {

	private static final String MOH_743 = "MOH-743 Report";

	private static final String MOH_706 = "MOH 706";

	private DqService service;

	private final Date january = TestUtils.date(2026, 1, 1);

	private final Date february = TestUtils.date(2026, 2, 1);

	@Before
	public void setup() {
		service = Context.getService(DqService.class);
	}

	@Test
	public void getLatestReportRun_shouldReturnHighestVersion() {
		saveRun(MOH_743, january, 1, DqRunStatus.SUPERSEDED);
		DqReportRun second = saveRun(MOH_743, january, 2, DqRunStatus.AWAITING_APPROVAL);
		saveRun(MOH_706, january, 1, DqRunStatus.AWAITING_APPROVAL);

		Assert.assertEquals(second, service.getLatestReportRun(MOH_743, january));
		Assert.assertNull(service.getLatestReportRun(MOH_743, february));
		Assert.assertEquals(second, service.getReportRunByUuid(second.getUuid()));
	}

	@Test
	public void getLatestReportRuns_shouldReturnLatestVersionPerReportAndPeriod() {
		saveRun(MOH_743, january, 1, DqRunStatus.SUPERSEDED);
		DqReportRun januaryLatest = saveRun(MOH_743, january, 2, DqRunStatus.APPROVED);
		DqReportRun februaryLatest = saveRun(MOH_743, february, 1, DqRunStatus.AWAITING_APPROVAL);
		DqReportRun lab = saveRun(MOH_706, january, 1, DqRunStatus.BLOCKED);

		Assert.assertEquals(Arrays.asList(januaryLatest, lab), service.getLatestReportRuns(january));
		Assert.assertEquals(Arrays.asList(januaryLatest, februaryLatest),
				service.getLatestReportRuns(MOH_743, january, february));
	}

	@Test
	public void getLatestValues_shouldOnlyReturnValuesOfLatestVersions() {
		DqReportRun first = saveRun(MOH_743, january, 1, DqRunStatus.SUPERSEDED);
		DqReportRun second = saveRun(MOH_743, january, 2, DqRunStatus.AWAITING_APPROVAL);
		service.saveValues(Arrays.asList(value(first, "4"), value(second, "5")));

		List<DqValue> values = service.getLatestValues(Collections.singleton(MOH_743), january, february);
		Assert.assertEquals(1, values.size());
		Assert.assertEquals(Double.valueOf(5), values.get(0).getValueNumeric());
		Assert.assertEquals(1, service.getValues(first).size());
	}

	@Test
	public void saveResult_shouldStoreResultsOverridesAndTickets() {
		DqReportRun run = saveRun(MOH_743, january, 1, DqRunStatus.AWAITING_APPROVAL);

		DqResult result = new DqResult();
		result.setRuleId("T22-03");
		result.setIndicatorCode("MAL_CONFIRMED_743");
		result.setReportRun(run);
		result.setPeriodStart(january);
		result.setPeriodEnd(TestUtils.date(2026, 1, 31));
		result.setMeasureValue(0.35);
		result.setOutcome(DqOutcome.WARN);
		result.setEvaluatedAt(new Date());
		service.saveResult(result);

		DqOverride override = new DqOverride();
		override.setResult(result);
		override.setReasonCode("SEASONAL");
		override.setCreator(Context.getAuthenticatedUser());
		override.setDateCreated(new Date());
		service.saveOverride(override);

		DqTicket ticket = new DqTicket();
		ticket.setRuleId("T22-03");
		ticket.setResult(result);
		ticket.setPeriodStart(january);
		ticket.setStatus(DqTicketStatus.OPEN);
		ticket.setOpenedAt(new Date());
		service.saveTicket(ticket);

		Assert.assertEquals(Collections.singletonList(result), service.getResults(run));
		Assert.assertEquals(Collections.singletonList(result), service.getResults("T22-03", january, february));
		Assert.assertEquals(Collections.singletonList(override), service.getOverrides(run));
		Assert.assertEquals(Collections.singletonList(ticket), service.getTickets(DqTicketStatus.OPEN));
		Assert.assertTrue(service.getTickets(DqTicketStatus.RESOLVED).isEmpty());
		Assert.assertEquals(1, service.getTickets(null).size());
	}

	private DqReportRun saveRun(String reportName, Date periodStart, int version, DqRunStatus status) {
		DqReportRun run = new DqReportRun();
		run.setReportName(reportName);
		run.setPeriodStart(periodStart);
		run.setPeriodEnd(periodStart);
		run.setVersion(version);
		run.setStatus(status);
		run.setGeneratedAt(new Date());
		return service.saveReportRun(run);
	}

	private DqValue value(DqReportRun run, String value) {
		DqValue dqValue = new DqValue();
		dqValue.setReportRun(run);
		dqValue.setDatasetName("SUSPECTED_MALARIA_AGE");
		dqValue.setColumnName("MICRO_LESS_5_POS");
		dqValue.setValueText(value);
		dqValue.setValueNumeric(Double.valueOf(value));
		dqValue.setSource(DqValue.SOURCE_REPORT);
		return dqValue;
	}
}
