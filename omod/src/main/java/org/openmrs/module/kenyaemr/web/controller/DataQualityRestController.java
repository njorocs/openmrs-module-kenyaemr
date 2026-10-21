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
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqIndicatorCalculator;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.ServiceDqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.DuplicateEpisodeRule;
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqOverride;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqRunStatus;
import org.openmrs.module.kenyaemr.model.dq.DqTicket;
import org.openmrs.module.kenyaemr.model.dq.DqTicketStatus;
import org.openmrs.module.kenyaemr.model.dq.DqValue;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for the data quality approval screen, dashboard and tickets, under
 * /ws/rest/v1/kenyaemr/dq. Periods are months written yyyy-MM.
 */
@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr/dq")
public class DataQualityRestController extends BaseRestController {

	private static final String MONTH = "yyyy-MM";

	private static final String DAY = "yyyy-MM-dd";

	/**
	 * The expected reports, indicators and override reasons the screens need
	 */
	@GetMapping("/config")
	public ResponseEntity<Object> getConfig() {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		List<SimpleObject> indicators = new ArrayList<SimpleObject>();
		for (DqConfig.Indicator indicator : config.getIndicators()) {
			indicators.add(new SimpleObject().add("code", indicator.getCode()).add("name", indicator.getName())
					.add("tracked", indicator.isTracked()).add("reports", new ArrayList<String>(indicator.getReports())));
		}
		List<SimpleObject> reports = new ArrayList<SimpleObject>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			reports.add(new SimpleObject().add("report", report.getReport()).add("frequency", report.getFrequency())
					.add("deadlineDay", report.getDeadlineDay()).add("target", report.getTarget()));
		}
		return ok(new SimpleObject().add("expectedReports", reports).add("indicators", indicators)
				.add("overrideReasons", config.getOverrideReasons())
				.add("problems", DqConfigStore.validate(config)));
	}

	/**
	 * One card per expected report for the month: the latest version and its rule outcomes, or
	 * status MISSING when it has not been generated
	 */
	@GetMapping("/runs")
	public ResponseEntity<Object> getRuns(@RequestParam(value = "period", required = false) String period) {
		requireAuthentication();
		DqService service = service();
		Date month = period != null ? parse(period, MONTH) : DqPeriods.previousMonth(new Date());
		Map<String, DqReportRun> runs = new HashMap<String, DqReportRun>();
		for (DqReportRun run : service.getLatestReportRuns(month)) {
			runs.put(run.getReportName(), run);
		}
		List<SimpleObject> cards = new ArrayList<SimpleObject>();
		for (DqConfig.ExpectedReport report : DqConfigStore.getConfig().getExpectedReports()) {
			DqReportRun run = runs.remove(report.getReport());
			cards.add(run != null ? summary(run, service.getResults(run))
					: new SimpleObject().add("reportName", report.getReport()).add("status", "MISSING"));
			if (run == null) {
				cards.get(cards.size() - 1).add("period", format(month, MONTH));
			}
		}
		// Reports generated by hand that are no longer in the expected list
		for (DqReportRun run : runs.values()) {
			cards.add(summary(run, service.getResults(run)));
		}
		return ok(new SimpleObject().add("period", format(month, MONTH)).add("results", cards));
	}

	/**
	 * Generates (or regenerates) one report for a month and checks it. Body: {report, period}
	 */
	@PostMapping("/runs")
	public ResponseEntity<Object> generate(@RequestBody Map<String, Object> body) {
		requireAuthentication();
		String report = string(body, "report");
		Date month = parse(string(body, "period"), MONTH);
		DqReportRun run = service().generateReportRun(report, month);
		return new ResponseEntity<Object>(detail(run), HttpStatus.CREATED);
	}

	/**
	 * Generates past months so that T22-03 has history. Months already generated are skipped and
	 * the backfilled runs are stored without being checked. Body: {from, to, reports?}
	 */
	@PostMapping("/backfill")
	@SuppressWarnings("unchecked")
	public ResponseEntity<Object> backfill(@RequestBody Map<String, Object> body) {
		requireAuthentication();
		DqService service = service();
		Date from = parse(string(body, "from"), MONTH);
		Date to = parse(string(body, "to"), MONTH);
		if (from.after(to)) {
			throw new APIException("from must not be after to");
		}
		List<String> reports = new ArrayList<String>();
		if (body.get("reports") instanceof List) {
			reports.addAll((List<String>) body.get("reports"));
		} else {
			for (DqConfig.ExpectedReport report : DqConfigStore.getConfig().getExpectedReports()) {
				if (DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency())) {
					reports.add(report.getReport());
				}
			}
		}

		List<SimpleObject> generated = new ArrayList<SimpleObject>();
		List<SimpleObject> failed = new ArrayList<SimpleObject>();
		for (Date month = from; !month.after(to); month = DqPeriods.addMonths(month, 1)) {
			for (String report : reports) {
				if (service.getLatestReportRun(report, month) != null) {
					continue;
				}
				try {
					DqReportRun run = service.generateReportRun(report, month, false);
					generated.add(new SimpleObject().add("reportName", report).add("period", format(month, MONTH))
							.add("uuid", run.getUuid()));
				}
				catch (APIException e) {
					failed.add(new SimpleObject().add("reportName", report).add("period", format(month, MONTH))
							.add("error", e.getMessage()));
				}
			}
		}
		return ok(new SimpleObject().add("generated", generated).add("failed", failed));
	}

	/**
	 * A run with its rule results, overrides and tracked indicators
	 */
	@GetMapping("/runs/{uuid}")
	public ResponseEntity<Object> getRun(@PathVariable("uuid") String uuid) {
		requireAuthentication();
		return ok(detail(run(uuid)));
	}

	/**
	 * Every stored cell of a run
	 */
	@GetMapping("/runs/{uuid}/values")
	public ResponseEntity<Object> getValues(@PathVariable("uuid") String uuid,
	                                        @RequestParam(value = "mappedOnly", defaultValue = "false") boolean mappedOnly) {
		requireAuthentication();
		List<SimpleObject> values = new ArrayList<SimpleObject>();
		for (DqValue value : service().getValues(run(uuid))) {
			if (mappedOnly && value.getDataElement() == null) {
				continue;
			}
			values.add(new SimpleObject().add("dataset", value.getDatasetName()).add("column", value.getColumnName())
					.add("mappingKey", value.getMappingKey()).add("dataElement", value.getDataElement())
					.add("categoryOptionCombo", value.getCategoryOptionCombo()).add("value", value.getValueText())
					.add("khisValue", value.getKhisValue()).add("importOutcome", value.getImportOutcome()));
		}
		return ok(new SimpleObject().add("results", values));
	}

	/**
	 * Approves a run. Body: {overrides: [{result: resultUuid, reasonCode, note}]}, one per warning.
	 * Needs the "Approve Data Quality Reports" privilege.
	 */
	@PostMapping("/runs/{uuid}/approve")
	@SuppressWarnings("unchecked")
	public ResponseEntity<Object> approve(@PathVariable("uuid") String uuid, @RequestBody(required = false) Map<String, Object> body) {
		requireAuthentication();
		DqService service = service();
		DqReportRun run = run(uuid);
		List<DqOverride> overrides = new ArrayList<DqOverride>();
		Object items = body != null ? body.get("overrides") : null;
		if (items instanceof List) {
			for (Object item : (List<Object>) items) {
				Map<String, Object> map = (Map<String, Object>) item;
				DqResult result = service.getResultByUuid(string(map, "result"));
				if (result == null) {
					throw new APIException("No result " + map.get("result"));
				}
				DqOverride override = new DqOverride();
				override.setResult(result);
				override.setReasonCode(string(map, "reasonCode"));
				override.setNote(map.get("note") != null ? map.get("note").toString() : null);
				overrides.add(override);
			}
		}
		return ok(detail(service.approveReportRun(run, overrides)));
	}

	/**
	 * The patients behind a T22-07 result: each repeat positive and the earlier positive it follows
	 */
	@GetMapping("/results/{uuid}/patients")
	public ResponseEntity<Object> getPatients(@PathVariable("uuid") String uuid) {
		requireAuthentication();
		DqService service = service();
		DqResult result = service.getResultByUuid(uuid);
		if (result == null) {
			return notFound("No result " + uuid);
		}
		if (!DuplicateEpisodeRule.ID.equals(result.getRuleId())) {
			throw new APIException("No patient list is available for " + result.getRuleId());
		}
		ServiceDqRuleContext context = new ServiceDqRuleContext(service, DqConfigStore.getConfig(), result.getReportRun(),
				result.getPeriodStart(), result.getPeriodEnd());
		List<SimpleObject> patients = new ArrayList<SimpleObject>();
		for (Object[] row : new DuplicateEpisodeRule().listPairs(context)) {
			Patient patient = Context.getPatientService().getPatient(((Number) row[0]).intValue());
			if (patient == null) {
				continue;
			}
			patients.add(new SimpleObject().add("patientUuid", patient.getUuid())
					.add("name", patient.getPersonName() != null ? patient.getPersonName().getFullName() : null)
					.add("firstPositive", row[1]).add("repeatPositive", row[2]));
		}
		return ok(new SimpleObject().add("results", patients));
	}

	/**
	 * Monthly values of one indicator with its T22-03 outcome, for the trend panel
	 */
	@GetMapping("/indicators/{code}/trend")
	public ResponseEntity<Object> getTrend(@PathVariable("code") String code,
	                                       @RequestParam(value = "to", required = false) String to,
	                                       @RequestParam(value = "months", defaultValue = "12") int months) {
		requireAuthentication();
		DqService service = service();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.Indicator indicator = config.getIndicator(code);
		if (indicator == null) {
			return notFound("No indicator " + code);
		}
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = DqPeriods.addMonths(last, -(Math.max(1, Math.min(months, 36)) - 1));
		ServiceDqRuleContext context = new ServiceDqRuleContext(service, config, null, first, last);

		Map<Long, DqOutcome> flags = new HashMap<Long, DqOutcome>();
		for (DqResult result : service.getResults("T22-03", first, last)) {
			if (code.equals(result.getIndicatorCode()) && (result.getReportRun() == null
					|| result.getReportRun().getStatus() != DqRunStatus.SUPERSEDED)) {
				DqOutcome current = flags.get(result.getPeriodStart().getTime());
				if (current == null || result.getOutcome().compareTo(current) > 0) {
					flags.put(result.getPeriodStart().getTime(), result.getOutcome());
				}
			}
		}

		List<SimpleObject> points = new ArrayList<SimpleObject>();
		for (Date month = first; !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			DqIndicatorCalculator.IndicatorValue value = context.getIndicator(code, month);
			points.add(new SimpleObject().add("period", format(month, MONTH)).add("emrValue", value.getValue())
					.add("complete", value.isComplete()).add("t2203", flags.get(month.getTime())));
		}
		return ok(new SimpleObject().add("code", code).add("name", indicator.getName()).add("results", points));
	}

	@GetMapping("/tickets")
	public ResponseEntity<Object> getTickets(@RequestParam(value = "status", required = false) String status) {
		requireAuthentication();
		DqTicketStatus filter = status != null ? DqTicketStatus.valueOf(status.toUpperCase()) : null;
		List<SimpleObject> tickets = new ArrayList<SimpleObject>();
		for (DqTicket ticket : service().getTickets(filter)) {
			tickets.add(ticket(ticket));
		}
		return ok(new SimpleObject().add("results", tickets));
	}

	/**
	 * Updates a ticket. Body: {status, resolutionNote}
	 */
	@PostMapping("/tickets/{uuid}")
	public ResponseEntity<Object> updateTicket(@PathVariable("uuid") String uuid, @RequestBody Map<String, Object> body) {
		requireAuthentication();
		DqService service = service();
		DqTicket ticket = service.getTicketByUuid(uuid);
		if (ticket == null) {
			return notFound("No ticket " + uuid);
		}
		if (body.get("status") != null) {
			DqTicketStatus status = DqTicketStatus.valueOf(body.get("status").toString().toUpperCase());
			if (status == DqTicketStatus.RESOLVED && ticket.getStatus() != DqTicketStatus.RESOLVED) {
				ticket.setResolvedAt(new Date());
				ticket.setResolvedBy(Context.getAuthenticatedUser());
			}
			ticket.setStatus(status);
		}
		if (body.get("resolutionNote") != null) {
			ticket.setResolutionNote(body.get("resolutionNote").toString());
		}
		return ok(ticket(service.saveTicket(ticket)));
	}

	/**
	 * Runs the weekly check (T22-08) for the week containing a date. Body: {date: yyyy-MM-dd}
	 */
	@PostMapping("/weeks")
	public ResponseEntity<Object> evaluateWeek(@RequestBody Map<String, Object> body) {
		requireAuthentication();
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (DqResult result : service().evaluateWeek(parse(string(body, "date"), DAY))) {
			results.add(result(result));
		}
		return ok(new SimpleObject().add("results", results));
	}

	@ExceptionHandler(APIAuthenticationException.class)
	public ResponseEntity<Object> handleAuthentication(APIAuthenticationException e) {
		return new ResponseEntity<Object>(new SimpleObject().add("error", e.getMessage()),
				Context.isAuthenticated() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Object> handleNotFound(NotFoundException e) {
		return notFound(e.getMessage());
	}

	@ExceptionHandler({ APIException.class, IllegalArgumentException.class })
	public ResponseEntity<Object> handleBadRequest(RuntimeException e) {
		return new ResponseEntity<Object>(new SimpleObject().add("error", e.getMessage()), HttpStatus.BAD_REQUEST);
	}

	private SimpleObject summary(DqReportRun run, List<DqResult> results) {
		int blocks = 0;
		int warnings = 0;
		int tickets = 0;
		for (DqResult result : results) {
			if (result.getOutcome() == DqOutcome.BLOCK) {
				blocks++;
			} else if (result.getOutcome() == DqOutcome.WARN) {
				warnings++;
			} else if (result.getOutcome() == DqOutcome.TICKET) {
				tickets++;
			}
		}
		DqConfig.ExpectedReport expected = DqConfigStore.getConfig().getExpectedReport(run.getReportName());
		return new SimpleObject().add("uuid", run.getUuid()).add("reportName", run.getReportName())
				.add("period", format(run.getPeriodStart(), MONTH)).add("version", run.getVersion())
				.add("status", run.getStatus().name()).add("generatedAt", run.getGeneratedAt())
				.add("generatedBy", userName(run.getGeneratedBy())).add("approvedAt", run.getApprovedAt())
				.add("approvedBy", userName(run.getApprovedBy()))
				.add("deadlineDay", expected != null ? expected.getDeadlineDay() : null)
				.add("blocks", blocks).add("warnings", warnings).add("tickets", tickets);
	}

	private SimpleObject detail(DqReportRun run) {
		DqService service = service();
		List<DqResult> results = service.getResults(run);
		Map<String, DqOverride> overrides = new HashMap<String, DqOverride>();
		for (DqOverride override : service.getOverrides(run)) {
			overrides.put(override.getResult().getUuid(), override);
		}

		// Blocks first, then warnings, tickets, passes and checks that could not run
		List<SimpleObject> items = new ArrayList<SimpleObject>();
		DqOutcome[] order = { DqOutcome.BLOCK, DqOutcome.WARN, DqOutcome.TICKET, DqOutcome.PASS, DqOutcome.NOT_APPLICABLE };
		for (DqOutcome outcome : order) {
			for (DqResult result : results) {
				if (result.getOutcome() == outcome) {
					SimpleObject item = result(result);
					DqOverride override = overrides.get(result.getUuid());
					if (override != null) {
						item.add("override", new SimpleObject().add("reasonCode", override.getReasonCode())
								.add("note", override.getNote()).add("by", userName(override.getCreator()))
								.add("at", override.getDateCreated()));
					}
					items.add(item);
				}
			}
		}

		// Tracked indicators this report feeds: this month, last month and the 12-month average
		DqConfig config = DqConfigStore.getConfig();
		ServiceDqRuleContext context = new ServiceDqRuleContext(service, config, run, run.getPeriodStart(), run.getPeriodEnd());
		List<SimpleObject> indicators = new ArrayList<SimpleObject>();
		for (DqConfig.Indicator indicator : config.getIndicators()) {
			if (!indicator.isTracked() || !indicator.getReports().contains(run.getReportName())) {
				continue;
			}
			double sum = 0;
			int months = 0;
			for (int i = 1; i <= 12; i++) {
				DqIndicatorCalculator.IndicatorValue value = context.getIndicator(indicator.getCode(),
						DqPeriods.addMonths(run.getPeriodStart(), -i));
				if (value.isComplete()) {
					sum += value.getValue();
					months++;
				}
			}
			DqOutcome flag = null;
			for (DqResult result : results) {
				if (indicator.getCode().equals(result.getIndicatorCode())
						&& (flag == null || result.getOutcome().compareTo(flag) > 0)) {
					flag = result.getOutcome();
				}
			}
			indicators.add(new SimpleObject().add("code", indicator.getCode()).add("name", indicator.getName())
					.add("thisMonth", context.getIndicator(indicator.getCode(), run.getPeriodStart()).getValue())
					.add("lastMonth", context.getIndicator(indicator.getCode(), DqPeriods.addMonths(run.getPeriodStart(), -1)).getValue())
					.add("average12Months", months > 0 ? sum / months : null).add("monthsInAverage", months)
					.add("flag", flag));
		}
		return summary(run, results).add("results", items).add("indicators", indicators);
	}

	private static SimpleObject result(DqResult result) {
		return new SimpleObject().add("uuid", result.getUuid()).add("ruleId", result.getRuleId())
				.add("indicatorCode", result.getIndicatorCode()).add("outcome", result.getOutcome().name())
				.add("message", result.getMessage()).add("measureValue", result.getMeasureValue())
				.add("numerator", result.getNumerator()).add("denominator", result.getDenominator())
				.add("threshold", result.getThreshold()).add("detail", result.getDetail())
				.add("period", result.getPeriodStart()).add("evaluatedAt", result.getEvaluatedAt());
	}

	private static SimpleObject ticket(DqTicket ticket) {
		return new SimpleObject().add("uuid", ticket.getUuid()).add("ruleId", ticket.getRuleId())
				.add("indicatorCode", ticket.getIndicatorCode()).add("period", ticket.getPeriodStart())
				.add("message", ticket.getResult() != null ? ticket.getResult().getMessage() : null)
				.add("emrValue", ticket.getEmrValue()).add("khisValue", ticket.getKhisValue())
				.add("variancePct", ticket.getVariancePct()).add("suggestedCause", ticket.getSuggestedCause())
				.add("assigneeRole", ticket.getAssigneeRole()).add("status", ticket.getStatus().name())
				.add("resolutionNote", ticket.getResolutionNote()).add("openedAt", ticket.getOpenedAt())
				.add("resolvedAt", ticket.getResolvedAt()).add("resolvedBy", userName(ticket.getResolvedBy()));
	}

	private DqReportRun run(String uuid) {
		DqReportRun run = service().getReportRunByUuid(uuid);
		if (run == null) {
			throw new NotFoundException("No report run " + uuid);
		}
		return run;
	}

	private static DqService service() {
		return Context.getService(DqService.class);
	}

	private static void requireAuthentication() {
		if (!Context.isAuthenticated()) {
			throw new APIAuthenticationException("Not authenticated");
		}
	}

	private static String userName(User user) {
		return user == null ? null : user.getPersonName() != null ? user.getPersonName().getFullName() : user.getUsername();
	}

	private static String string(Map<String, Object> body, String key) {
		Object value = body != null ? body.get(key) : null;
		if (value == null || StringUtils.isBlank(value.toString())) {
			throw new IllegalArgumentException("Missing " + key);
		}
		return value.toString().trim();
	}

	private static Date parse(String text, String pattern) {
		try {
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setLenient(false);
			return format.parse(text);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("Expected a date like " + pattern + ", got " + text);
		}
	}

	private static String format(Date date, String pattern) {
		return new SimpleDateFormat(pattern).format(date);
	}

	private static ResponseEntity<Object> ok(Object body) {
		return new ResponseEntity<Object>(body, HttpStatus.OK);
	}

	private static ResponseEntity<Object> notFound(String message) {
		return new ResponseEntity<Object>(new SimpleObject().add("error", message), HttpStatus.NOT_FOUND);
	}

	private static class NotFoundException extends RuntimeException {

		NotFoundException(String message) {
			super(message);
		}
	}
}
