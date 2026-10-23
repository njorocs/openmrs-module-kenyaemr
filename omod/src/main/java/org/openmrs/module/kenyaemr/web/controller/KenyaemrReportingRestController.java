/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Program;
import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.kenyacore.program.ProgramDescriptor;
import org.openmrs.module.kenyacore.program.ProgramManager;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.IndicatorReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportManager;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.DateOfEnrollmentArtCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.LastViralLoadResultCalculation;
import org.openmrs.module.kenyaemr.page.controller.ReportExportPageController;
import org.openmrs.module.kenyaemr.reporting.air.AdxConfiguration;
import org.openmrs.module.kenyaemr.reporting.air.ConfigurableAdxGenerationStrategy;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.MapDataSet;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.dimension.CohortIndicatorAndDimensionResult;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.web.renderers.DefaultWebRenderer;
import org.openmrs.module.reportingcompatibility.service.ReportingCompatibilityService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.ui.framework.page.FileDownload;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.openmrs.util.OpenmrsClassLoader;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for KenyaEMR reports.
 */
@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr")
public class KenyaemrReportingRestController extends BaseRestController {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String DOWNLOAD_URL_FORMAT = "/ws/rest/v1/kenyaemr/reportRequests/%s/download/%s";

	/**
	 * Lists KenyaEMR report descriptors.
	 */
	@GetMapping("/reports")
	public ResponseEntity<SimpleObject> getReports(@RequestParam(value = "q", required = false) String query) {
		ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);

		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (ReportDescriptor descriptor : reportManager.getAllReportDescriptors()) {
			if (StringUtils.isBlank(query) || StringUtils.containsIgnoreCase(descriptor.getName(), query)
					|| StringUtils.containsIgnoreCase(descriptor.getDescription(), query)) {
				results.add(simplifyDescriptor(descriptor, false));
			}
		}

		SimpleObject response = new SimpleObject();
		response.put("results", results);
		return ResponseEntity.ok(response);
	}

	/**
	 * Lists KenyaEMR reports grouped like the legacy reports page.
	 */
	@GetMapping("/reports/grouped")
	public ResponseEntity<?> getGroupedReports(@RequestParam(value = "appId", required = false) String appId) {
		ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
		ProgramManager programManager = Context.getRegisteredComponents(ProgramManager.class).get(0);
		AppFrameworkService appFrameworkService = Context.getService(AppFrameworkService.class);

		String resolvedAppId = StringUtils.isNotBlank(appId) ? appId : EmrConstants.APP_REPORTS;
		AppDescriptor app = appFrameworkService.getApp(resolvedAppId);
		if (app == null) {
			return new ResponseEntity<Object>("App not found: " + resolvedAppId, HttpStatus.NOT_FOUND);
		}

		List<SimpleObject> groups = new ArrayList<SimpleObject>();
		addReportGroup(groups, "Common", reportManager.getCommonReports(app));
		addReportGroup(groups, "Cohort Analysis", reportManager.getCohortAnalysisReports(app));
		addReportGroup(groups, "EHR Reports", reportManager.getEhrReports(app));

		for (ProgramDescriptor programDescriptor : programManager.getAllProgramDescriptors()) {
			Program program = programDescriptor.getTarget();
			List<ReportDescriptor> reports = reportManager.getProgramReports(app, program);
			if (!reports.isEmpty()) {
				addReportGroup(groups, program.getName(), reports);
			}
		}

		SimpleObject response = new SimpleObject();
		response.put("appId", resolvedAppId);
		response.put("results", groups);
		return ResponseEntity.ok(response);
	}

	/**
	 * Gets a KenyaEMR report descriptor and its parameters.
	 */
	@GetMapping("/reports/{uuid}")
	public ResponseEntity<?> getReport(@PathVariable("uuid") String uuid) {
		ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
		ReportDescriptor descriptor = reportManager.getReportDescriptorByUuid(uuid);
		if (descriptor == null) {
			return new ResponseEntity<Object>("Report not found", HttpStatus.NOT_FOUND);
		}

		ReportDefinition definition = getOrBuildDefinition(descriptor, false);
		SimpleObject response = simplifyDescriptor(descriptor, true);
		response.put("definition", definition != null ? simplifyDefinition(definition) : null);
		return ResponseEntity.ok(response);
	}

	/**
	 * Queues a report request.
	 */
	@PostMapping("/reports/{uuid}/requests")
	public ResponseEntity<?> requestReport(@PathVariable("uuid") String uuid,
										   @RequestBody(required = false) Map<String, Object> payload) {
		ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
		ReportDefinitionService definitionService = Context.getService(ReportDefinitionService.class);
		ReportService reportService = Context.getService(ReportService.class);

		ReportDescriptor descriptor = reportManager.getReportDescriptorByUuid(uuid);
		if (descriptor == null) {
			return new ResponseEntity<SimpleObject>(errorResponse("Report not found"), HttpStatus.NOT_FOUND);
		}

		ReportDefinition definition = getOrBuildDefinition(descriptor, true);
		if (definition == null) {
			return new ResponseEntity<SimpleObject>(errorResponse("Could not build report definition"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (definition.getId() == null) {
			definition = definitionService.saveDefinition(definition);
		}

		Map<String, Object> parameters;
		try {
			parameters = buildParameterValues(definition, payload);
		}
		catch (IllegalArgumentException ex) {
			return new ResponseEntity<SimpleObject>(errorResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
		}

		Mapped<ReportDefinition> mappedDefinition = new Mapped<ReportDefinition>(definition, parameters);
		ReportRenderer renderer = new DefaultWebRenderer();
		RenderingMode mode = renderer.getRenderingModes(definition).iterator().next();

		ReportRequest request = new ReportRequest();
		request.setReportDefinition(mappedDefinition);
		request.setRenderingMode(mode);

		request = reportService.queueReport(request);
		reportService.processNextQueuedReports();
		if (request.getId() != null) {
			ReportRequest refreshed = reportService.getReportRequest(request.getId());
			if (refreshed != null) {
				request = refreshed;
			}
		}

		return new ResponseEntity<SimpleObject>(simplifyRequest(request), HttpStatus.CREATED);
	}

	/**
	 * Lists report requests.
	 */
	@GetMapping("/reportRequests")
	public ResponseEntity<SimpleObject> getReportRequests(@RequestParam(value = "reportUuid", required = false) String reportUuid,
														  @RequestParam(value = "status", required = false) String status,
														  @RequestParam(value = "limit", required = false) Integer limit) {
		ReportService reportService = Context.getService(ReportService.class);
		List<ReportRequest> requests;
		try {
			requests = fetchRequests(reportUuid, status, reportService);
		}
		catch (IllegalArgumentException ex) {
			SimpleObject response = new SimpleObject();
			response.put("error", "Invalid report request status: " + status);
			return new ResponseEntity<SimpleObject>(response, HttpStatus.BAD_REQUEST);
		}
		int max = limit != null && limit > 0 ? Math.min(limit, requests.size()) : requests.size();

		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (int i = 0; i < max; i++) {
			results.add(simplifyRequest(requests.get(i)));
		}

		SimpleObject response = new SimpleObject();
		response.put("results", results);
		return ResponseEntity.ok(response);
	}

	/**
	 * Gets one report request.
	 */
	@GetMapping("/reportRequests/{id}")
	public ResponseEntity<?> getReportRequest(@PathVariable("id") Integer id) {
		ReportRequest request = Context.getService(ReportService.class).getReportRequest(id);
		if (request == null) {
			return new ResponseEntity<Object>("Report request not found", HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.ok(simplifyRequest(request));
	}

	/**
	 * Loads completed report data as JSON.
	 */
	@GetMapping("/reportRequests/{id}/data")
	public ResponseEntity<?> getReportRequestData(@PathVariable("id") Integer id) {
		ReportService reportService = Context.getService(ReportService.class);
		ReportRequest request = reportService.getReportRequest(id);
		if (request == null) {
			return new ResponseEntity<Object>("Report request not found", HttpStatus.NOT_FOUND);
		}
		if (!ReportRequest.Status.COMPLETED.equals(request.getStatus())) {
			return new ResponseEntity<Object>("Report request is not completed", HttpStatus.CONFLICT);
		}

		ReportData data = reportService.loadReportData(request);
		SimpleObject response = new SimpleObject();
		response.put("request", simplifyRequest(request));
		response.put("definition", simplifyDefinition(data.getDefinition()));
		response.put("parameters", data.getContext().getParameterValues());
		response.put("dataSets", simplifyDataSets(data));
		return ResponseEntity.ok(response);
	}

	/**
	 * Resolves the saved cohort behind an indicator into a paged patient list.
	 */
	@Authorized(value = { "View Patients", "View Reports" }, requireAll = true)
	@GetMapping("/reportRequests/{id}/patients")
	public ResponseEntity<?> getReportPatients(@PathVariable("id") Integer id,
			@RequestParam("dataset") String dataset, @RequestParam("column") String column,
			@RequestParam(value = "startIndex", defaultValue = "0") int startIndex,
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		if (startIndex < 0 || limit < 1 || limit > 200) {
			return new ResponseEntity<Object>("startIndex must be non-negative and limit must be between 1 and 200",
					HttpStatus.BAD_REQUEST);
		}
		ReportService service = Context.getService(ReportService.class);
		ReportRequest request = service.getReportRequest(id);
		if (request == null) {
			return new ResponseEntity<Object>("Report request not found", HttpStatus.NOT_FOUND);
		}
		if (!ReportRequest.Status.COMPLETED.equals(request.getStatus())) {
			return new ResponseEntity<Object>("Report request is not completed", HttpStatus.CONFLICT);
		}
		ReportData data = service.loadReportData(request);
		DataSet dataSet = data.getDataSets().get(dataset);
		if (dataSet == null || dataSet.getMetaData().getColumn(column) == null) {
			return new ResponseEntity<Object>("Dataset or column not found", HttpStatus.NOT_FOUND);
		}
		if (!(dataSet instanceof MapDataSet)) {
			return new ResponseEntity<Object>("Dataset does not contain indicator cohorts", HttpStatus.BAD_REQUEST);
		}
		Object value = ((MapDataSet) dataSet).getData(dataSet.getMetaData().getColumn(column));
		Cohort cohort = getResultCohort(value);
		if (cohort == null) {
			return new ResponseEntity<Object>("Column has no saved patient cohort", HttpStatus.BAD_REQUEST);
		}
		List<Integer> ids = new ArrayList<Integer>(cohort.getMemberIds());
		Collections.sort(ids);
		int from = Math.min(startIndex, ids.size());
		int to = from + Math.min(limit, ids.size() - from);
		Set<Integer> page = new HashSet<Integer>(ids.subList(from, to));
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		if (!page.isEmpty()) {
			List<Patient> patients = Context.getService(ReportingCompatibilityService.class).getPatients(page);
			Collections.sort(patients, new Comparator<Patient>() {
				@Override
				public int compare(Patient first, Patient second) {
					return first.getPatientId().compareTo(second.getPatientId());
				}
			});
			Date endDate = (Date) data.getContext().getParameterValues().get("endDate");
			PatientCalculationContext context = Context.getService(PatientCalculationService.class).createCalculationContext();
			if (endDate != null) {
				context.setNow(endDate);
			}
			CalculationResultMap enrollment = new DateOfEnrollmentArtCalculation().evaluate(page, null, context);
			CalculationResultMap art = new InitialArtStartDateCalculation().evaluate(page, null, context);
			CalculationResultMap viralLoad = new LastViralLoadResultCalculation().evaluate(page, null, context);
			for (Patient patient : patients) {
				SimpleObject row = new SimpleObject();
				row.put("id", patient.getPatientId());
				row.put("uuid", patient.getUuid());
				row.put("name", patient.getPersonName() != null ? patient.getPersonName().getFullName() : null);
				row.put("gender", patient.getGender());
				row.put("age", patient.getAge());
				row.put("ageAtReporting", endDate != null ? patient.getAge(endDate) : null);
				List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
				for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
					SimpleObject item = new SimpleObject();
					item.put("identifier", identifier.getIdentifier());
					item.put("identifierType", identifier.getIdentifierType().getName());
					identifiers.add(item);
				}
				row.put("identifiers", identifiers);
				row.put("enrollmentDate", calculationValue(enrollment, patient.getPatientId()));
				row.put("artStartDate", calculationValue(art, patient.getPatientId()));
				row.put("lastViralLoad", calculationValue(viralLoad, patient.getPatientId()));
				results.add(row);
			}
		}
		SimpleObject response = new SimpleObject();
		response.put("results", results);
		response.put("totalCount", ids.size());
		response.put("startIndex", startIndex);
		response.put("limit", limit);
		response.put("hasMore", to < ids.size());
		return ResponseEntity.ok(response);
	}

	static Cohort getResultCohort(Object value) {
		if (value instanceof Cohort) {
			return (Cohort) value;
		}
		if (value instanceof CohortIndicatorAndDimensionResult) {
			return ((CohortIndicatorAndDimensionResult) value).getCohortIndicatorAndDimensionCohort();
		}
		return null;
	}

	private Object calculationValue(CalculationResultMap results, Integer patientId) {
		return results.get(patientId) != null ? results.get(patientId).getValue() : null;
	}

	/**
	 * Downloads completed report data in an export format.
	 */
	@GetMapping("/reportRequests/{id}/download/{type}")
	public ResponseEntity<?> downloadReportRequestData(@PathVariable("id") Integer id,
													   @PathVariable("type") String type) {
		String normalizedType = StringUtils.lowerCase(type);
		ReportService reportService = Context.getService(ReportService.class);
		ReportRequest request = reportService.getReportRequest(id);
		if (request == null) {
			return new ResponseEntity<Object>("Report request not found", HttpStatus.NOT_FOUND);
		}
		if (!ReportRequest.Status.COMPLETED.equals(request.getStatus())) {
			return new ResponseEntity<Object>("Report request is not completed", HttpStatus.CONFLICT);
		}

		ReportDescriptor report = getReportDescriptor(request);
		if (report == null) {
			return new ResponseEntity<Object>("Report descriptor not found", HttpStatus.NOT_FOUND);
		}
		if (!getAvailableDownloadUrls(request, report).containsKey(normalizedType)) {
			return new ResponseEntity<Object>("Report request is not available for download as: " + type, HttpStatus.BAD_REQUEST);
		}

		try {
			ReportData data = reportService.loadReportData(request);
			ResourceFactory resourceFactory = Context.getRegisteredComponents(ResourceFactory.class).get(0);
			FileDownload download = new ReportExportPageController().render(report, data, normalizedType, resourceFactory);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(download.getContentType()));
			headers.setContentLength(download.getFileContent().length);
			headers.set("Content-Disposition", "attachment; filename=\"" + download.getFilename() + "\"");
			return new ResponseEntity<byte[]>(download.getFileContent(), headers, HttpStatus.OK);
		}
		catch (RuntimeException ex) {
			return new ResponseEntity<Object>(ex.getMessage(), HttpStatus.BAD_REQUEST);
		}
		catch (Exception ex) {
			return new ResponseEntity<Object>("Could not export report request: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public String getNamespace() {
		return "v1/kenyaemr";
	}

	private ReportDefinition getOrBuildDefinition(ReportDescriptor descriptor, boolean save) {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(OpenmrsClassLoader.getInstance());
		try {
			ReportDefinitionService definitionService = Context.getService(ReportDefinitionService.class);
			ReportDefinition definition = definitionService.getDefinitionByUuid(descriptor.getTargetUuid());
			if (definition == null) {
				ReportManager reportManager = Context.getRegisteredComponents(ReportManager.class).get(0);
				definition = reportManager.buildReportDefinition(descriptor);
				if (definition != null && save) {
					definition = definitionService.saveDefinition(definition);
				}
			}
			return definition;
		}
		finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private Map<String, Object> buildParameterValues(ReportDefinition definition, Map<String, Object> payload) {
		Map<String, Object> values = new HashMap<String, Object>();
		Map<String, Object> submitted = payload != null ? payload : new HashMap<String, Object>();

		for (Parameter parameter : definition.getParameters()) {
			Object raw = submitted.get(parameter.getName());
			if (raw == null) {
				raw = parameter.getDefaultValue();
			}

			Object converted = convertParameter(raw, parameter);
			if (converted == null) {
				throw new IllegalArgumentException("Missing report parameter: " + parameter.getName());
			}
			values.put(parameter.getName(), converted);
		}

		return values;
	}

	private Object convertParameter(Object raw, Parameter parameter) {
		if (raw == null) {
			return null;
		}

		Class<?> type = parameter.getType();
		if (type == null || type.isInstance(raw)) {
			return raw;
		}

		String value = raw.toString();
		if (StringUtils.isBlank(value)) {
			return null;
		}

		try {
			if (Date.class.equals(type)) {
				synchronized (DATE_FORMAT) {
					return DATE_FORMAT.parse(value);
				}
			}
			if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
				return Integer.valueOf(value);
			}
			if (Long.class.equals(type) || Long.TYPE.equals(type)) {
				return Long.valueOf(value);
			}
			if (Double.class.equals(type) || Double.TYPE.equals(type)) {
				return Double.valueOf(value);
			}
			if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
				return Boolean.valueOf(value);
			}
			if (String.class.equals(type)) {
				return value;
			}
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException("Invalid date for parameter " + parameter.getName() + ". Use yyyy-MM-dd");
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid number for parameter " + parameter.getName());
		}

		throw new IllegalArgumentException("Unsupported parameter type for " + parameter.getName() + ": " + type.getName());
	}

	private List<ReportRequest> fetchRequests(String reportUuid, String status, ReportService reportService) {
		ReportDefinition definition = null;
		if (StringUtils.isNotBlank(reportUuid)) {
			definition = new ReportDefinition();
			definition.setUuid(reportUuid);
		}

		List<ReportRequest> requests;
		if (StringUtils.isNotBlank(status)) {
			requests = reportService.getReportRequests(definition, null, null, ReportRequest.Status.valueOf(status));
		}
		else {
			requests = reportService.getReportRequests(definition, null, null);
		}

		Collections.sort(requests, new Comparator<ReportRequest>() {
			@Override
			public int compare(ReportRequest request1, ReportRequest request2) {
				return OpenmrsUtil.compareWithNullAsEarliest(request2.getRequestDate(), request1.getRequestDate());
			}
		});
		return requests;
	}

	private SimpleObject simplifyDescriptor(ReportDescriptor descriptor, boolean includeLinks) {
		SimpleObject object = new SimpleObject();
		object.put("uuid", descriptor.getTargetUuid());
		object.put("name", descriptor.getName());
		object.put("description", descriptor.getDescription());
		object.put("type", descriptor.getClass().getSimpleName());
		object.put("indicator", descriptor instanceof IndicatorReportDescriptor);
		object.put("hybrid", descriptor instanceof HybridReportDescriptor);

		if (descriptor instanceof IndicatorReportDescriptor) {
			IndicatorReportDescriptor indicator = (IndicatorReportDescriptor) descriptor;
			object.put("template", indicator.getTemplate() != null ? indicator.getTemplate().toString() : null);
		}
		else if (descriptor instanceof HybridReportDescriptor) {
			HybridReportDescriptor hybrid = (HybridReportDescriptor) descriptor;
			object.put("template", hybrid.getTemplate() != null ? hybrid.getTemplate().toString() : null);
			object.put("repeatingSection", hybrid.getRepeatingSection());
		}

		if (includeLinks) {
			object.put("requestUrl", "/ws/rest/v1/kenyaemr/reports/" + descriptor.getTargetUuid() + "/requests");
			object.put("requestsUrl", "/ws/rest/v1/kenyaemr/reportRequests?reportUuid=" + descriptor.getTargetUuid());
		}
		return object;
	}

	private void addReportGroup(List<SimpleObject> groups, String name, List<ReportDescriptor> reports) {
		SimpleObject group = new SimpleObject();
		group.put("name", name);
		group.put("indicator", simplifyReportList(reports, true));
		group.put("patientFollowUpReports", simplifyReportList(reports, false));
		groups.add(group);
	}

	private List<SimpleObject> simplifyReportList(List<ReportDescriptor> reports, boolean indicator) {
		List<SimpleObject> simplified = new ArrayList<SimpleObject>();
		for (ReportDescriptor report : reports) {
			if ((report instanceof IndicatorReportDescriptor) == indicator) {
				simplified.add(simplifyDescriptor(report, false));
			}
		}
		return simplified;
	}

	private SimpleObject simplifyDefinition(ReportDefinition definition) {
		SimpleObject object = new SimpleObject();
		object.put("uuid", definition.getUuid());
		object.put("name", definition.getName());
		object.put("description", definition.getDescription());

		List<SimpleObject> parameters = new ArrayList<SimpleObject>();
		for (Parameter parameter : definition.getParameters()) {
			SimpleObject param = new SimpleObject();
			param.put("name", parameter.getName());
			param.put("label", parameter.getLabel());
			param.put("type", parameter.getType() != null ? parameter.getType().getName() : null);
			param.put("defaultValue", parameter.getDefaultValue());
			parameters.add(param);
		}
		object.put("parameters", parameters);

		List<String> dataSets = new ArrayList<String>();
		for (String key : definition.getDataSetDefinitions().keySet()) {
			dataSets.add(key);
		}
		object.put("dataSets", dataSets);
		return object;
	}

	private SimpleObject simplifyRequest(ReportRequest request) {
		SimpleObject object = new SimpleObject();
		object.put("id", request.getId());
		object.put("uuid", request.getUuid());
		object.put("status", request.getStatus());
		object.put("requestDate", request.getRequestDate());
		object.put("evaluateStartDatetime", formatDateTime(request.getEvaluateStartDatetime()));
		object.put("evaluateCompleteDatetime", formatDateTime(request.getEvaluateCompleteDatetime()));
		object.put("priority", request.getPriority());
		object.put("parameters", getParameterMappings(request));
		object.put("error", buildRequestError(request));

		User requestedBy = request.getRequestedBy();
		if (requestedBy != null) {
			SimpleObject user = new SimpleObject();
			user.put("uuid", requestedBy.getUuid());
			user.put("display", requestedBy.getDisplayString());
			object.put("requestedBy", user);
		}

		Mapped<ReportDefinition> mapped = request.getReportDefinition();
		if (mapped != null && mapped.getParameterizable() != null) {
			object.put("report", simplifyDefinitionBrief(mapped.getParameterizable()));
		}

		object.put("patientsUrl", request.getId() != null && ReportRequest.Status.COMPLETED.equals(request.getStatus())
				? "/ws/rest/v1/kenyaemr/reportRequests/" + request.getId() + "/patients" : null);
		object.put("downloadUrls", buildDownloadUrls(request));
		object.put("downloadFormats", buildDownloadFormats(request));
		return object;
	}

	private SimpleObject buildRequestError(ReportRequest request) {
		if (request == null || !ReportRequest.Status.FAILED.equals(request.getStatus())) {
			return new SimpleObject();
		}
		String message = null;
		try {
			message = Context.getService(ReportService.class).loadReportError(request);
		}
		catch (RuntimeException ignored) {
			// Fall through with a generic message when the error file cannot be loaded.
		}
		SimpleObject error = new SimpleObject();
		error.put("message", StringUtils.isNotBlank(message) ? message : "Report evaluation failed");
		return error;
	}

	private SimpleObject errorResponse(String message) {
		SimpleObject response = new SimpleObject();
		SimpleObject error = new SimpleObject();
		error.put("message", message);
		response.put("error", error);
		return response;
	}

	private String formatDateTime(Date date) {
		if (date == null) {
			return null;
		}
		synchronized (DATE_TIME_FORMAT) {
			return DATE_TIME_FORMAT.format(date);
		}
	}

	private SimpleObject simplifyDefinitionBrief(ReportDefinition definition) {
		SimpleObject object = new SimpleObject();
		object.put("uuid", definition.getUuid());
		object.put("name", definition.getName());
		object.put("description", definition.getDescription());
		return object;
	}

	private Map<String, Object> getParameterMappings(ReportRequest request) {
		Mapped<ReportDefinition> mapped = request.getReportDefinition();
		if (mapped == null || mapped.getParameterMappings() == null) {
			return new HashMap<String, Object>();
		}
		return mapped.getParameterMappings();
	}

	private SimpleObject buildDownloadUrls(ReportRequest request) {
		SimpleObject urls = new SimpleObject();
		if (request.getId() != null && ReportRequest.Status.COMPLETED.equals(request.getStatus())) {
			ReportDescriptor report = getReportDescriptor(request);
			for (Map.Entry<String, String> entry : getAvailableDownloadUrls(request, report).entrySet()) {
				urls.put(entry.getKey(), entry.getValue());
			}
		}
		return urls;
	}

	private List<String> buildDownloadFormats(ReportRequest request) {
		if (request.getId() == null || !ReportRequest.Status.COMPLETED.equals(request.getStatus())) {
			return new ArrayList<String>();
		}
		return new ArrayList<String>(getAvailableDownloadUrls(request, getReportDescriptor(request)).keySet());
	}

	private Map<String, String> getAvailableDownloadUrls(ReportRequest request, ReportDescriptor report) {
		Map<String, String> urls = new LinkedHashMap<String, String>();
		if (request == null || request.getId() == null || report == null) {
			return urls;
		}

		urls.put("csv", String.format(DOWNLOAD_URL_FORMAT, request.getId(), "csv"));
		urls.put("pdf", String.format(DOWNLOAD_URL_FORMAT, request.getId(), "pdf"));

		if (isExcelRenderable(report)) {
			urls.put("excel", String.format(DOWNLOAD_URL_FORMAT, request.getId(), "excel"));
		}

		String airFormat = getConfiguredAirFormat(report);
		if (StringUtils.isNotBlank(airFormat)) {
			urls.put(airFormat, String.format(DOWNLOAD_URL_FORMAT, request.getId(), airFormat));
		}
		return urls;
	}

	private ReportDescriptor getReportDescriptor(ReportRequest request) {
		if (request == null || request.getReportDefinition() == null || request.getReportDefinition().getParameterizable() == null) {
			return null;
		}
		ReportDefinition definition = request.getReportDefinition().getParameterizable();
		return Context.getRegisteredComponents(ReportManager.class).get(0).getReportDescriptor(definition);
	}

	private boolean isExcelRenderable(ReportDescriptor report) {
		if (report instanceof IndicatorReportDescriptor) {
			IndicatorReportDescriptor indicator = (IndicatorReportDescriptor) report;
			return indicator.getTemplate() != null && StringUtils.endsWithIgnoreCase(indicator.getTemplate().getPath(), ".xls");
		}
		if (report instanceof HybridReportDescriptor) {
			HybridReportDescriptor hybrid = (HybridReportDescriptor) report;
			return hybrid.getTemplate() != null && StringUtils.endsWithIgnoreCase(hybrid.getTemplate().getPath(), ".xls");
		}
		return false;
	}

	private String getConfiguredAirFormat(ReportDescriptor report) {
		ConfigurableAdxGenerationStrategy adxStrategy = new ConfigurableAdxGenerationStrategy();
		org.openmrs.module.reporting.config.ReportDescriptor reportDescriptor = new org.openmrs.module.reporting.config.ReportDescriptor();
		reportDescriptor.setName(report.getTarget().getName());

		if (!adxStrategy.canHandle(reportDescriptor)) {
			return null;
		}

		AdxConfiguration config = adxStrategy.getConfigurationForReport(report.getTarget().getName());
		return config != null && "json".equalsIgnoreCase(config.getOutputFormat()) ? "json" : "adx";
	}

	private SimpleObject simplifyDataSets(ReportData data) {
		SimpleObject dataSets = new SimpleObject();
		for (Map.Entry<String, DataSet> entry : data.getDataSets().entrySet()) {
			dataSets.put(entry.getKey(), simplifyDataSet(entry.getKey(), entry.getValue(), data.getDefinition()));
		}
		return dataSets;
	}

	private SimpleObject simplifyDataSet(String key, DataSet dataSet, ReportDefinition definition) {
		SimpleObject object = new SimpleObject();
		object.put("key", key);
		DataSetDefinition dataSetDefinition = getDataSetDefinition(key, definition);
		object.put("name", dataSetDefinition != null && StringUtils.isNotBlank(dataSetDefinition.getName())
				? dataSetDefinition.getName() : key);
		if (dataSetDefinition != null && StringUtils.isNotBlank(dataSetDefinition.getDescription())) {
			object.put("label", dataSetDefinition.getDescription());
		}

		List<SimpleObject> columns = new ArrayList<SimpleObject>();
		for (DataSetColumn column : dataSet.getMetaData().getColumns()) {
			SimpleObject col = new SimpleObject();
			col.put("name", column.getName());
			col.put("label", column.getLabel());
			columns.add(col);
		}
		object.put("columns", columns);

		if (dataSet instanceof MapDataSet) {
			object.put("rows", new ArrayList<SimpleObject>());
			object.put("values", simplifyColumnValues((MapDataSet) dataSet));
		}
		else {
			object.put("rows", simplifyRows(dataSet));
		}
		return object;
	}

	private DataSetDefinition getDataSetDefinition(String key, ReportDefinition definition) {
		if (definition.getDataSetDefinitions().containsKey(key)) {
			Mapped<? extends DataSetDefinition> mapped = definition.getDataSetDefinitions().get(key);
			if (mapped != null) {
				return mapped.getParameterizable();
			}
		}
		return null;
	}

	private List<SimpleObject> simplifyRows(DataSet dataSet) {
		List<SimpleObject> rows = new ArrayList<SimpleObject>();
		for (DataSetRow row : dataSet) {
			SimpleObject object = new SimpleObject();
			for (DataSetColumn column : dataSet.getMetaData().getColumns()) {
				object.put(column.getName(), simplifyValue(row.getColumnValue(column)));
			}
			rows.add(object);
		}
		return rows;
	}

	private SimpleObject simplifyColumnValues(MapDataSet dataSet) {
		SimpleObject values = new SimpleObject();
		for (DataSetColumn column : dataSet.getMetaData().getColumns()) {
			values.put(column.getName(), simplifyValue(dataSet.getData(column)));
		}
		return values;
	}

	private Object simplifyValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Cohort) {
			Cohort cohort = (Cohort) value;
			SimpleObject object = new SimpleObject();
			object.put("type", "cohort");
			object.put("hasPatientList", true);
			object.put("size", cohort.size());
			object.put("memberIds", cohort.getMemberIds());
			return object;
		}
		if (value instanceof CohortIndicatorAndDimensionResult) {
			CohortIndicatorAndDimensionResult result = (CohortIndicatorAndDimensionResult) value;
			SimpleObject object = new SimpleObject();
			object.put("type", "cohortIndicatorAndDimensionResult");
			object.put("hasPatientList", getResultCohort(result) != null);
			object.put("value", result.toString());
			object.put("size", result.getCohortIndicatorAndDimensionCohort() != null ? result.getCohortIndicatorAndDimensionCohort().size() : null);
			return object;
		}
		if (value instanceof Date) {
			return value;
		}
		if (value instanceof Number || value instanceof Boolean || value instanceof String) {
			return value;
		}
		return value.toString();
	}
}
