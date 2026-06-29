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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyacore.CoreUtils;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportManager;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.module.kenyaui.annotation.SharedAction;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.web.renderers.DefaultWebRenderer;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentActionRequest;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.openmrs.ui.framework.fragment.action.SuccessResult;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AJAX utility methods for reports
 */
public class ReportUtilsFragmentController {

	protected static final Log log = LogFactory.getLog(ReportUtilsFragmentController.class);

	private static final DateFormat iso8601Formatter = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Requests a report evaluation
	 * 
	 * @param reportUuid    the report definition UUID
	 * @param reportManager the report manager
	 * @return the report request id
	 */
	@SharedAction
	public Object requestReport(@RequestParam("reportUuid") String reportUuid,
			UiUtils ui,
			@SpringBean KenyaUiUtils kenyaui,
			@SpringBean FragmentActionRequest actionRequest,
			@SpringBean ReportManager reportManager,
			@SpringBean ReportService reportService,
			@SpringBean ReportDefinitionService definitionService) throws ParseException {

		// Step 1: Look up descriptor from in-memory registry
		ReportDescriptor report = reportManager.getReportDescriptorByUuid(reportUuid);
		if (report == null) {
			return new FailureResult("No report descriptor found for UUID: " + reportUuid);
		}

		// Step 2: Load definition from DB
		ReportDefinition definition = definitionService.getDefinitionByUuid(reportUuid);

		// Step 3: If not in DB, build on demand AND save it so queueReport() can
		// store the report_definition_uuid correctly. reporting 2.1.0's
		// MappedDefinitionType requires the definition to be persisted before
		// a ReportRequest referencing it can be saved.
		if (definition == null) {
			log.warn("Definition not found in DB for report: " + reportUuid
					+ ". Building and saving on demand.");
			definition = reportManager.buildReportDefinition(report);

			if (definition == null) {
				return new FailureResult("Could not build definition for: " + report.getName());
			}

			// Save so reporting 2.1.0 can resolve report_definition_uuid on queueReport()
			try {
				definition = definitionService.saveDefinition(definition);
				log.info("Saved on-demand definition for report: " + report.getName());
			} catch (Exception e) {
				log.error("Failed to save on-demand definition for report: "
						+ report.getName(), e);
				return new FailureResult("Could not save report definition: " + e.getMessage());
			}
		}

		// Step 4: Access check
		CoreUtils.checkAccess(report, kenyaui.getCurrentApp(actionRequest));

		Collection<String> missingParameters = new ArrayList<String>();
		Map<String, Object> parameterValues = new HashMap<String, Object>();

		for (Parameter parameter : definition.getParameters()) {
			String submitted = actionRequest.getParameter("param[" + parameter.getName() + "]");
			Object converted = StringUtils.isNotEmpty(submitted)
					? ui.convert(submitted, parameter.getType())
					: parameter.getDefaultValue();
			if (converted == null) {
				missingParameters.add(parameter.getName());
			}
			parameterValues.put(parameter.getName(), converted);
		}

		if (missingParameters.size() > 0) {
			return new FailureResult("Missing report parameters");
		}

		Mapped<ReportDefinition> mappedDefinition = new Mapped<ReportDefinition>(definition, parameterValues);

		ReportRenderer renderer = new DefaultWebRenderer();
		RenderingMode mode = renderer.getRenderingModes(definition).iterator().next();

		ReportRequest request = new ReportRequest();
		request.setReportDefinition(mappedDefinition);
		request.setRenderingMode(mode);

		request = reportService.queueReport(request);
		reportService.processNextQueuedReports();

		log.info("Requested report '" + definition.getName() + "' with params " + parameterValues);

		return SimpleObject.fromObject(request, ui, "id");
	}

	/**
	 * Cancels the given report request
	 * 
	 * @param request the report request
	 * @param ui      the UI utils
	 * @return the result
	 */
	@AppAction(EmrConstants.APP_ADMIN)
	public Object cancelRequest(@RequestParam("requestId") ReportRequest request,
			UiUtils ui,
			@SpringBean ReportService reportService) {

		boolean cancelable = ReportRequest.Status.REQUESTED.equals(request.getStatus())
				|| ReportRequest.Status.PROCESSING.equals(request.getStatus());

		if (cancelable) {
			reportService.purgeReportRequest(request);
			return new SuccessResult(ui.message("Report request cancelled"));
		}

		return new FailureResult(ui.message("Report request could not be cancelled"));
	}

	/**
	 * Gets the finished (failed or completed) requests for the given report
	 * 
	 * @param reportUuid    the report definition UUID
	 * @param ui            the UI utils
	 * @param reportService the report service
	 * @return the simplified requests
	 */
	public SimpleObject[] getFinishedRequests(@RequestParam(value = "reportUuid", required = false) String reportUuid,
			UiUtils ui,
			@SpringBean ReportService reportService) {

		List<ReportRequest> requests = fetchRequests(reportUuid, true, reportService);

		return ui.simplifyCollection(requests);
	}

	/**
	 * Gets the queued requests for the given report
	 * 
	 * @param reportUuid    the report definition UUID
	 * @param ui            the UI utils
	 * @param reportService the report service
	 * @return the simplified requests
	 */
	public SimpleObject[] getQueuedRequests(@RequestParam(value = "reportUuid", required = false) String reportUuid,
			UiUtils ui,
			@SpringBean ReportService reportService) {

		List<ReportRequest> requests = fetchRequests(reportUuid, false, reportService);

		// Filter out finished requests
		CollectionUtils.filter(requests, new Predicate() {
			@Override
			public boolean evaluate(Object obj) {
				ReportRequest request = (ReportRequest) obj;
				return !(ReportRequest.Status.COMPLETED.equals(request.getStatus())
						|| ReportRequest.Status.FAILED.equals(request.getStatus()));
			}
		});

		return ui.simplifyCollection(requests);
	}

	/**
	 * Gets the in-progress (processing) requests for the given report
	 * 
	 * @param reportUuid    the report definition UUID
	 * @param ui            the UI utils
	 * @param reportService the report service
	 * @return a map of request id to request for in-progress requests
	 */
	public HashMap<Integer, ReportRequest> getInProgressRequests(
			@RequestParam(value = "reportUuid", required = false) String reportUuid,
			UiUtils ui,
			@SpringBean ReportService reportService) {

		List<ReportRequest> requests = fetchRequests(reportUuid, false, reportService);

		// Filter to only in-progress requests
		CollectionUtils.filter(requests, new Predicate() {
			@Override
			public boolean evaluate(Object obj) {
				ReportRequest request = (ReportRequest) obj;
				return ReportRequest.Status.PROCESSING.equals(request.getStatus());
			}
		});

		HashMap<Integer, ReportRequest> inProgressMap = new HashMap<Integer, ReportRequest>();
		for (ReportRequest request : requests) {
			inProgressMap.put(request.getId(), request);
		}

		return inProgressMap;
	}

	/**
	 * Helper method to fetch report requests
	 * 
	 * @param reportUuid    the report definition UUID (optional)
	 * @param finishedOnly  only finished requests (completed or failed)
	 * @param reportService the report service
	 * @return the report requests
	 */
	protected List<ReportRequest> fetchRequests(String reportUuid, boolean finishedOnly, ReportService reportService) {
		ReportDefinition definition = null;

		// Hack to avoid loading (and thus de-serialising) the entire report
		if (StringUtils.isNotEmpty(reportUuid)) {
			definition = new ReportDefinition();
			definition.setUuid(reportUuid);
		}

		List<ReportRequest> requests = (finishedOnly)
				? reportService.getReportRequests(definition, null, null, ReportRequest.Status.COMPLETED,
						ReportRequest.Status.FAILED)
				: reportService.getReportRequests(definition, null, null);

		// Sort by requested date desc (more sane than the default sorting)
		Collections.sort(requests, new Comparator<ReportRequest>() {
			@Override
			public int compare(ReportRequest request1, ReportRequest request2) {
				return OpenmrsUtil.compareWithNullAsEarliest(request2.getRequestDate(), request1.getRequestDate());
			}
		});

		return requests;
	}
}