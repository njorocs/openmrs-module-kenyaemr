/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.fragment.controller.report;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.DqSendGate;
import org.openmrs.module.kenyaemr.dq.DqSendGates;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.web.WebConstants;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds a report back from KHIS until its data quality checks allow it, for the reports page.
 * A request's figures never change, so they are worked out once per request and kept, and the
 * request list can show every request's state each time it refreshes.
 */
public final class KhisSendGates {

	private static final Log log = LogFactory.getLog(KhisSendGates.class);

	private static final int CACHED_REQUESTS = 500;

	/** Stands for a request the checks do not cover */
	private static final DqSendGates.Figures NOT_COVERED = new DqSendGates.Figures(null, null, null, null);

	private static final Map<String, DqSendGates.Figures> FIGURES = Collections.synchronizedMap(
			new LinkedHashMap<String, DqSendGates.Figures>(16, 0.75f, true) {

				@Override
				protected boolean removeEldestEntry(Map.Entry<String, DqSendGates.Figures> eldest) {
					return size() > CACHED_REQUESTS;
				}
			});

	private KhisSendGates() {
	}

	/**
	 * @param reportDefinitionUuid the request's report
	 * @param reportData the request's data, if already loaded; else it is loaded when needed
	 */
	public static DqSendGate gateFor(ReportRequest request, String reportDefinitionUuid, ReportData reportData,
	                                 ReportService reportService) {
		DqSendGates.Figures figures = FIGURES.get(request.getUuid());
		if (figures == null) {
			try {
				ReportData data = reportData != null ? reportData : reportService.loadReportData(request);
				figures = DqSendGates.figuresOf(reportDefinitionUuid, data);
			}
			catch (Exception e) {
				log.warn("Could not read the figures of report request " + request.getUuid(), e);
				return DqSendGate.notCheckedHere();
			}
			FIGURES.put(request.getUuid(), figures != null ? figures : NOT_COVERED);
		}
		if (figures == null || figures == NOT_COVERED) {
			return DqSendGate.notCheckedHere();
		}
		return Context.getService(DqService.class).getSendGate(figures.getReportName(), figures.getPeriodStart(),
				figures.getPeriodEnd(), figures.getValuesHash(), request.getEvaluateCompleteDatetime());
	}

	public static SimpleObject simplify(DqSendGate gate) {
		DqReportRun run = gate.getRun();
		String runUuid = run != null ? run.getUuid() : null;
		String period = run != null && run.getPeriodStart() != null ? new SimpleDateFormat("yyyy-MM").format(run.getPeriodStart())
				: null;
		SimpleObject simple = SimpleObject.create("state", gate.getState().name(), "sendable", gate.isSendable(), "label",
				label(gate), "message", gate.getMessage(), "problems", gate.getProblems(), "runUuid", runUuid, "dqUrl",
				dqUrl(runUuid));
		simple.put("blockers", gate.getBlockers());
		simple.put("warnings", gate.getWarnings());
		simple.put("tickets", gate.getTickets());
		simple.put("resolvedTickets", gate.getResolvedTickets());
		simple.put("reviewUrl", runUuid != null ? reviewUrl(runUuid, period) : null);
		simple.put("ticketsUrl", period != null ? ticketsUrl(period, gate.getTickets()) : null);
		return simple;
	}

	/**
	 * @return the version's review on the Data quality page: its checks, and where its warnings are
	 * answered and it is approved
	 */
	static String reviewUrl(String runUuid, String period) {
		return DQ_PAGE + "?tab=approval&run=" + runUuid + (period != null ? "&period=" + period : "");
	}

	/**
	 * @return the month's tickets to do on the Data quality page; only one rule's when they are all of
	 * one rule
	 */
	static String ticketsUrl(String period, List<String> tickets) {
		Set<String> rules = new LinkedHashSet<String>();
		for (String ticket : tickets) {
			rules.add(ticket.contains(":") ? ticket.substring(0, ticket.indexOf(':')) : ticket);
		}
		return DQ_PAGE + "?tab=tickets&period=" + period + (rules.size() == 1 ? "&rule=" + rules.iterator().next() : "");
	}

	/**
	 * @return a short label for the request list
	 */
	static String label(DqSendGate gate) {
		switch (gate.getState()) {
			case CHECKING:
				return "Checking data quality...";
			case NOT_CHECKED:
				return "Not checked";
			case NOT_LATEST:
				return "Not the latest figures";
			case WRONG_PERIOD:
				return "Not a whole week or month";
			case BLOCKED:
				return "Blocked by a check";
			case NEEDS_REVIEW:
				return "Needs review before sending";
			case READY:
				return "Ready to send";
			default:
				return null;
		}
	}

	/**
	 * @return the Data quality page, open on the version when there is one
	 */
	static String dqUrl(String runUuid) {
		return runUuid != null ? reviewUrl(runUuid, null) : DQ_PAGE;
	}

	private static final String DQ_PAGE = "/" + WebConstants.WEBAPP_NAME + "/spa/facility-dashboard/data-quality";
}
