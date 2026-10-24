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
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.dq.DqTrends;
import org.openmrs.module.kenyaemr.dq.ServiceDqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRule;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleTypes;
import org.openmrs.module.kenyaemr.dq.rule.DuplicateEpisodeRule;
import org.openmrs.module.kenyaemr.dq.rule.IndicatorRatioRule;
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
					.add("tracked", config.isTracked(indicator)).add("reports", new ArrayList<String>(indicator.getReports())));
		}
		List<SimpleObject> reports = new ArrayList<SimpleObject>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			reports.add(new SimpleObject().add("report", report.getReport()).add("frequency", report.getFrequency())
					.add("deadlineDay", report.getDeadlineDay()).add("target", report.getTarget()));
		}
		List<SimpleObject> programs = new ArrayList<SimpleObject>();
		for (DqConfig.Program program : config.getPrograms()) {
			List<SimpleObject> rules = new ArrayList<SimpleObject>();
			for (Map<String, Object> values : program.getRules()) {
				DqConfig.RuleDefinition rule = new DqConfig.RuleDefinition(program, values);
				DqRuleSettings settings = rule.getSettings();
				rules.add(new SimpleObject().add("id", rule.getId()).add("type", rule.getType()).add("source", rule.getSource())
						.add("enabled", settings.isEnabled()).add("severity", settings.getString("severity", null))
						.add("ticket", settings.getBoolean("ticket", false)).add("gates", rule.getGates())
						.add("assigneeRole", rule.getAssigneeRole())
						.add("indicators", new ArrayList<String>(rule.getIndicatorCodes())).add("settings", values));
			}
			programs.add(new SimpleObject().add("code", program.getCode()).add("name", program.getName())
					.add("ownerRole", program.getOwnerRole()).add("enabled", program.isEnabled())
					.add("guidelineVersion", program.getGuidelineVersion()).add("owns", program.getOwns())
					.add("reports", new ArrayList<String>(config.getReports(program)))
					.add("indicators", program.getIndicators()).add("rules", rules));
		}
		return ok(new SimpleObject().add("expectedReports", reports).add("indicators", indicators)
				.add("programs", programs).add("overrideReasons", config.getOverrideReasons())
				.add("problems", problems(config)));
	}

	/**
	 * One card per expected report for the month: the latest version and its rule outcomes, or
	 * status MISSING when it has not been generated
	 */
	@GetMapping("/runs")
	public ResponseEntity<Object> getRuns(@RequestParam(value = "period", required = false) String period,
	                                      @RequestParam(value = "program", required = false) String programCode) {
		requireAuthentication();
		DqService service = service();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.Program program = program(config, programCode);
		Date month = period != null ? parse(period, MONTH) : DqPeriods.previousMonth(new Date());
		Map<String, DqReportRun> runs = new HashMap<String, DqReportRun>();
		for (DqReportRun run : service.getLatestReportRuns(month)) {
			runs.put(run.getReportName(), run);
		}
		List<SimpleObject> cards = new ArrayList<SimpleObject>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			if (program != null && !config.getReports(program).contains(report.getReport())) {
				runs.remove(report.getReport());
				continue;
			}
			DqReportRun run = runs.remove(report.getReport());
			cards.add(run != null ? summary(run, forProgram(service.getResults(run), program))
					: new SimpleObject().add("reportName", report.getReport()).add("status", "MISSING"));
			if (run == null) {
				cards.get(cards.size() - 1).add("period", format(month, MONTH));
			}
		}
		// Reports generated by hand that are no longer in the expected list
		for (DqReportRun run : runs.values()) {
			if (program == null) {
				cards.add(summary(run, service.getResults(run)));
			}
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
		return new ResponseEntity<Object>(detail(run, null), HttpStatus.CREATED);
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
	public ResponseEntity<Object> getRun(@PathVariable("uuid") String uuid,
	                                     @RequestParam(value = "program", required = false) String programCode) {
		requireAuthentication();
		return ok(detail(run(uuid), program(DqConfigStore.getConfig(), programCode)));
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
		return ok(detail(service.approveReportRun(run, overrides), null));
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
		DqConfig config = DqConfigStore.getConfig();
		DqRule rule = DqRuleTypes.rule(config, result.getRuleId());
		if (!(rule instanceof DuplicateEpisodeRule)) {
			throw new APIException("No patient list is available for " + result.getRuleId());
		}
		ServiceDqRuleContext context = new ServiceDqRuleContext(service, config, result.getReportRun(),
				result.getPeriodStart(), result.getPeriodEnd());
		List<SimpleObject> patients = new ArrayList<SimpleObject>();
		for (Object[] row : ((DuplicateEpisodeRule) rule).listPairs(context)) {
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
	 * Monthly values of one indicator with its T22-03 expected ranges, for the trend chart. Each
	 * month is checked against the values stored now, so a backfilled month shows the range the
	 * rule would apply today.
	 *
	 * @param to the last month, default last month
	 * @param months how many months, 1 to 36
	 */
	@GetMapping("/indicators/{code}/trend")
	public ResponseEntity<Object> getTrend(@PathVariable("code") String code,
	                                       @RequestParam(value = "to", required = false) String to,
	                                       @RequestParam(value = "months", defaultValue = "12") int months) {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.Indicator indicator = config.getIndicator(code);
		if (indicator == null) {
			return notFound("No indicator " + code);
		}
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = firstMonth(last, months);
		ServiceDqRuleContext context = new ServiceDqRuleContext(service(), config, null, first, last);
		// The rolling baseline of the first month reaches back 12 months
		context.preload(code, DqPeriods.addMonths(first, -12), last);

		List<SimpleObject> points = new ArrayList<SimpleObject>();
		for (DqTrends.IndicatorPoint point : DqTrends.indicatorTrend(context, indicator, first, last)) {
			points.add(new SimpleObject().add("period", format(point.getMonth(), MONTH)).add("value", point.getValue())
					.add("complete", point.isComplete()).add("rolling", baseline(point.getRolling()))
					.add("lastYear", baseline(point.getLastYear())));
		}
		ConsistencyOverTimeRule expectedRange = DqTrends.expectedRangeRule(config, indicator);
		DqRuleSettings settings = config.getRuleSettings(expectedRange.getId(), indicator);
		SimpleObject rule = new SimpleObject().add("id", expectedRange.getId()).add("enabled", settings.isEnabled())
				.add("band", settings.getDouble("band", 0.20)).add("ratioMin", settings.getDouble("ratioMin", 0.8))
				.add("ratioMax", settings.getDouble("ratioMax", 1.2))
				.add("minHistoryMonths", settings.getInteger("minHistoryMonths", 3));
		return ok(new SimpleObject().add("code", code).add("name", indicator.getName())
				.add("tracked", config.isTracked(indicator)).add("rule", rule).add("results", points));
	}

	/**
	 * Monthly numerator, denominator and ratio of a ratio rule (T22-04, X-RPT-01), for the trend chart
	 */
	@GetMapping("/rules/{ruleId}/trend")
	public ResponseEntity<Object> getRuleTrend(@PathVariable("ruleId") String ruleId,
	                                           @RequestParam(value = "to", required = false) String to,
	                                           @RequestParam(value = "months", defaultValue = "12") int months) {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		IndicatorRatioRule rule = DqTrends.ratioRule(config, ruleId);
		if (rule == null) {
			return notFound("No ratio rule " + ruleId);
		}
		DqRuleSettings settings = config.getRuleSettings(ruleId, null);
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = firstMonth(last, months);
		ServiceDqRuleContext context = new ServiceDqRuleContext(service(), config, null, first, last);
		List<String[]> pairs = rule.pairs();
		for (String[] pair : pairs) {
			for (String code : pair) {
				if (config.getIndicator(code) != null) {
					context.preload(code, first, last);
				}
			}
		}

		List<SimpleObject> pairTrends = new ArrayList<SimpleObject>();
		for (int i = 0; i < pairs.size(); i++) {
			List<SimpleObject> points = new ArrayList<SimpleObject>();
			for (DqTrends.RatioPoint point : DqTrends.ratioTrend(context, rule, i, first, last)) {
				points.add(new SimpleObject().add("period", format(point.getMonth(), MONTH))
						.add("numerator", point.getNumerator()).add("denominator", point.getDenominator())
						.add("ratio", point.getRatio()).add("outcome", point.getOutcome().name())
						.add("message", point.getMessage()));
			}
			pairTrends.add(new SimpleObject().add("numerator", indicatorRef(config.getIndicator(pairs.get(i)[0])))
					.add("denominator", indicatorRef(config.getIndicator(pairs.get(i)[1]))).add("results", points));
		}
		// The first pair at the top level, as before there were pairs
		SimpleObject firstPair = pairTrends.get(0);
		return ok(new SimpleObject().add("ruleId", ruleId).add("program", rule.getProgram()).add("enabled", settings.isEnabled())
				.add("numerator", firstPair.get("numerator")).add("denominator", firstPair.get("denominator"))
				.add("min", settings.getDouble("min", null)).add("max", settings.getDouble("max", null))
				.add("results", firstPair.get("results")).add("pairs", pairTrends));
	}

	@GetMapping("/tickets")
	public ResponseEntity<Object> getTickets(@RequestParam(value = "status", required = false) String status,
	                                         @RequestParam(value = "program", required = false) String programCode) {
		requireAuthentication();
		DqTicketStatus filter = status != null ? DqTicketStatus.valueOf(status.toUpperCase()) : null;
		DqConfig.Program program = program(DqConfigStore.getConfig(), programCode);
		List<SimpleObject> tickets = new ArrayList<SimpleObject>();
		for (DqTicket ticket : service().getTickets(filter)) {
			if (program == null || program.getCode().equals(ticket.getProgramCode())) {
				tickets.add(ticket(ticket));
			}
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
	 * A program's results for a range of months, including those not tied to a report version:
	 * timeliness (G-RPT-01), weekly positivity (T22-08) and KHIS parity (G-RPT-02). Results of
	 * superseded report versions are left out.
	 *
	 * @param from the first month, default the last month
	 * @param to the last month, default the last month
	 */
	@GetMapping("/results")
	public ResponseEntity<Object> getResults(@RequestParam(value = "program", required = false) String programCode,
	                                         @RequestParam(value = "rule", required = false) String ruleId,
	                                         @RequestParam(value = "from", required = false) String from,
	                                         @RequestParam(value = "to", required = false) String to) {
		requireAuthentication();
		DqConfig.Program program = program(DqConfigStore.getConfig(), programCode);
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = from != null ? parse(from, MONTH) : last;
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (DqResult result : service().getProgramResults(program != null ? program.getCode() : null, ruleId, first,
				DqPeriods.lastDayOfMonth(last))) {
			results.add(result(result).add("reportName", result.getReportRun() != null ? result.getReportRun().getReportName() : null)
					.add("reportVersion", result.getReportRun() != null ? result.getReportRun().getVersion() : null));
		}
		return ok(new SimpleObject().add("results", results));
	}

	/**
	 * Runs the month-level checks (G-RPT-01 timeliness) for a month whose deadlines are over, if not
	 * run already. Body: {period: yyyy-MM}
	 */
	@PostMapping("/months")
	public ResponseEntity<Object> evaluateMonth(@RequestBody Map<String, Object> body) {
		requireAuthentication();
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (DqResult result : service().evaluateMonth(parse(string(body, "period"), MONTH), new Date())) {
			results.add(result(result));
		}
		return ok(new SimpleObject().add("results", results));
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

	/**
	 * @param program the program whose results and indicators to show, or null for all; approval
	 * still needs every warning of every program answered
	 */
	private SimpleObject detail(DqReportRun run, DqConfig.Program program) {
		DqService service = service();
		List<DqResult> results = forProgram(service.getResults(run), program);
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
			if (!config.isTracked(indicator) || !indicator.getReports().contains(run.getReportName())
					|| (program != null && !program.getIndicators().contains(indicator.getCode()))) {
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
				.add("program", result.getProgramCode()).add("params", result.getParams())
				.add("indicatorCode", result.getIndicatorCode()).add("outcome", result.getOutcome().name())
				.add("message", result.getMessage()).add("measureValue", result.getMeasureValue())
				.add("numerator", result.getNumerator()).add("denominator", result.getDenominator())
				.add("threshold", result.getThreshold()).add("detail", result.getDetail())
				.add("period", result.getPeriodStart()).add("evaluatedAt", result.getEvaluatedAt());
	}

	private static SimpleObject baseline(DqTrends.Baseline baseline) {
		if (baseline == null) {
			return null;
		}
		return new SimpleObject().add("outcome", baseline.getOutcome().name()).add("reference", baseline.getReference())
				.add("monthsOfHistory", baseline.getMonthsOfHistory()).add("expectedMin", baseline.getExpectedMin())
				.add("expectedMax", baseline.getExpectedMax()).add("message", baseline.getMessage());
	}

	private static SimpleObject indicatorRef(DqConfig.Indicator indicator) {
		return indicator == null ? null : new SimpleObject().add("code", indicator.getCode()).add("name", indicator.getName());
	}

	private static Date firstMonth(Date last, int months) {
		return DqPeriods.addMonths(last, -(Math.max(1, Math.min(months, 36)) - 1));
	}

	private static SimpleObject ticket(DqTicket ticket) {
		return new SimpleObject().add("uuid", ticket.getUuid()).add("ruleId", ticket.getRuleId())
				.add("program", ticket.getProgramCode())
				.add("indicatorCode", ticket.getIndicatorCode()).add("period", ticket.getPeriodStart())
				.add("message", ticket.getResult() != null ? ticket.getResult().getMessage() : null)
				.add("emrValue", ticket.getEmrValue()).add("khisValue", ticket.getKhisValue())
				.add("variancePct", ticket.getVariancePct()).add("suggestedCause", ticket.getSuggestedCause())
				.add("assigneeRole", ticket.getAssigneeRole()).add("status", ticket.getStatus().name())
				.add("resolutionNote", ticket.getResolutionNote()).add("openedAt", ticket.getOpenedAt())
				.add("resolvedAt", ticket.getResolvedAt()).add("resolvedBy", userName(ticket.getResolvedBy()));
	}

	/**
	 * @return the program with the code, or null when no code is given
	 * @throws APIException (a bad request) when no program has the code
	 */
	private static DqConfig.Program program(DqConfig config, String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		DqConfig.Program program = config.getProgram(code.trim());
		if (program == null) {
			throw new APIException("No program " + code + "; the programs are set in kenyaemr/khis/dq_programs");
		}
		return program;
	}

	private static List<String> problems(DqConfig config) {
		List<String> problems = new ArrayList<String>(DqConfigStore.validate(config));
		problems.addAll(DqConfigStore.mappingProblems(config));
		return problems;
	}

	private static List<DqResult> forProgram(List<DqResult> results, DqConfig.Program program) {
		if (program == null) {
			return results;
		}
		List<DqResult> filtered = new ArrayList<DqResult>();
		for (DqResult result : results) {
			if (program.getCode().equals(result.getProgramCode())) {
				filtered.add(result);
			}
		}
		return filtered;
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
