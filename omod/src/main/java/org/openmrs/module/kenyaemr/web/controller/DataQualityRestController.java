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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqDashboard;
import org.openmrs.module.kenyaemr.dq.DqDispensingReconciliation;
import org.openmrs.module.kenyaemr.dq.DqIndicatorCalculator;
import org.openmrs.module.kenyaemr.dq.DqKhisTimeliness;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.dq.DqReconciliation;
import org.openmrs.module.kenyaemr.dq.DqReportGenerator;
import org.openmrs.module.kenyaemr.dq.DqRuleSettings;
import org.openmrs.module.kenyaemr.dq.DqRuleThresholds;
import org.openmrs.module.kenyaemr.dq.DqTrends;
import org.openmrs.module.kenyaemr.dq.ServiceDqRuleContext;
import org.openmrs.module.kenyaemr.dq.rule.ConsistencyOverTimeRule;
import org.openmrs.module.kenyaemr.task.DataQualityTask;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for the data quality approval screen, dashboard and tickets, under
 * /ws/rest/v1/kenyaemr/dq. Periods are months written yyyy-MM.
 */
@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/kenyaemr/dq")
public class DataQualityRestController extends BaseRestController {

	private static final Log log = LogFactory.getLog(DataQualityRestController.class);

	private static final String MONTH = "yyyy-MM";

	/** Needed to see the patients behind a reconciliation, as on the reports page */
	private static final String PRIVILEGE_VIEW_PATIENTS = "View Patients";

	private static final String PRIVILEGE_MANAGE_SCHEDULER = "Manage Scheduler";

	private static final String DAY = "yyyy-MM-dd";

	/**
	 * The expected reports, indicators and override reasons the screens need
	 */
	@GetMapping("/config")
	public ResponseEntity<Object> getConfig() {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		List<SimpleObject> indicators = new ArrayList<SimpleObject>();
		DqReportGenerator generator = new DqReportGenerator();
		for (DqConfig.Indicator indicator : config.getIndicators()) {
			// Each part's columns with their labels in the report builder, to describe the indicator
			List<SimpleObject> parts = new ArrayList<SimpleObject>();
			for (DqConfig.Part part : indicator.getParts()) {
				Map<String, String> labels = generator.columnLabels(part.getReport(), part.getDataset());
				List<SimpleObject> columns = new ArrayList<SimpleObject>();
				for (String column : part.getColumns()) {
					columns.add(new SimpleObject().add("column", column).add("label", labels.get(column)));
				}
				parts.add(new SimpleObject().add("report", part.getReport()).add("columns", columns));
			}
			indicators.add(new SimpleObject().add("code", indicator.getCode()).add("name", indicator.getName())
					.add("tracked", config.isTracked(indicator)).add("reports", new ArrayList<String>(indicator.getReports()))
					.add("parts", parts));
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
	 * The thresholds of each program's rules that the settings screen sets: the value in the
	 * facility's file (null when the file leaves it to the rule), the program's default, and what
	 * values it may take; and whether the user may change them
	 */
	@GetMapping("/thresholds")
	public ResponseEntity<Object> getThresholds() {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig defaults = DqConfigStore.getDefaultConfig();
		List<SimpleObject> programs = new ArrayList<SimpleObject>();
		for (DqConfig.Program program : config.getPrograms()) {
			DqConfig.Program defaultProgram = defaults.getProgram(program.getCode());
			List<SimpleObject> rules = new ArrayList<SimpleObject>();
			for (Map<String, Object> values : program.getRules()) {
				DqConfig.RuleDefinition rule = new DqConfig.RuleDefinition(program, values);
				Map<String, Object> defaultValues = defaultRule(defaultProgram, rule.getId());
				rules.add(new SimpleObject().add("id", rule.getId()).add("type", rule.getType())
						.add("enabled", rule.getSettings().isEnabled())
						.add("fields", thresholdFields(rule.getType(), values, defaultValues)));
			}
			programs.add(new SimpleObject().add("code", program.getCode()).add("name", program.getName()).add("rules", rules));
		}
		return ok(new SimpleObject().add("canEdit", Context.hasPrivilege(DqService.PRIVILEGE_MANAGE_SETTINGS))
				.add("programs", programs));
	}

	/**
	 * Sets thresholds of a rule. Body: {changes: {key: value}}, a null value removing an optional
	 * threshold. They apply to the checks run from then on; the open tickets are checked again in
	 * the night. Needs the "Manage Data Quality Settings" privilege.
	 */
	@PostMapping("/rules/{ruleId}/thresholds")
	@SuppressWarnings("unchecked")
	public ResponseEntity<Object> setThresholds(@PathVariable("ruleId") String ruleId, @RequestBody Map<String, Object> body)
			throws IOException {
		requireAuthentication();
		if (!Context.hasPrivilege(DqService.PRIVILEGE_MANAGE_SETTINGS)) {
			return new ResponseEntity<Object>(new SimpleObject().add("error", "Setting the thresholds needs the \""
					+ DqService.PRIVILEGE_MANAGE_SETTINGS + "\" privilege."), HttpStatus.FORBIDDEN);
		}
		Object changes = body != null ? body.get("changes") : null;
		if (!(changes instanceof Map) || ((Map<String, Object>) changes).isEmpty()) {
			throw new IllegalArgumentException("No changes");
		}
		List<String> problems;
		try {
			problems = DqConfigStore.updateRuleThresholds(ruleId, (Map<String, Object>) changes);
		}
		catch (IllegalArgumentException e) {
			return notFound(e.getMessage());
		}
		if (!problems.isEmpty()) {
			return new ResponseEntity<Object>(new SimpleObject().add("error", StringUtils.join(problems, "; "))
					.add("problems", problems), HttpStatus.BAD_REQUEST);
		}
		log.info("Data quality thresholds of " + ruleId + " changed by " + Context.getAuthenticatedUser().getUsername()
				+ ": " + changes);
		return getThresholds();
	}

	private static Map<String, Object> defaultRule(DqConfig.Program program, String ruleId) {
		if (program != null && ruleId != null) {
			for (Map<String, Object> values : program.getRules()) {
				if (ruleId.equals(values.get("id"))) {
					return values;
				}
			}
		}
		return Collections.emptyMap();
	}

	private static List<SimpleObject> thresholdFields(String type, Map<String, Object> values, Map<String, Object> defaults) {
		List<SimpleObject> fields = new ArrayList<SimpleObject>();
		for (DqRuleThresholds.Field field : DqRuleThresholds.fields(type)) {
			Object programDefault = defaults.containsKey(field.getKey()) ? defaults.get(field.getKey())
					: defaults.isEmpty() ? null : field.getFallback();
			SimpleObject object = new SimpleObject().add("key", field.getKey()).add("kind", field.getKind().name())
					.add("value", values.get(field.getKey())).add("default", programDefault)
					.add("fallback", field.getFallback()).add("optional", field.isOptional());
			if (field.getKind() == DqRuleThresholds.Kind.CHOICE) {
				object.add("options", field.getOptions());
			} else {
				object.add("min", field.getMin()).add("max", field.getMax());
			}
			fields.add(object);
		}
		return fields;
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
		// The weeks of reports sent a week at a time (MOH 505), each a version of its own
		List<SimpleObject> weeks = new ArrayList<SimpleObject>();
		for (DqReportRun run : service.getLatestWeeklyRuns(month, DqPeriods.lastDayOfMonth(month))) {
			if (program == null || config.getReports(program).contains(run.getReportName())) {
				weeks.add(summary(run, forProgram(service.getResults(run), program)));
			}
		}
		return ok(new SimpleObject().add("period", format(month, MONTH)).add("results", cards).add("weeks", weeks));
	}

	/**
	 * Generates (or regenerates) one report for a month and checks it. Body: {report, period,
	 * check?}; with check false the run is stored unchecked, as backfilled history is, e.g. to
	 * regenerate past months after a report definition changes without raising their tickets
	 */
	@PostMapping("/runs")
	public ResponseEntity<Object> generate(@RequestBody Map<String, Object> body) {
		requireAuthentication();
		String report = string(body, "report");
		Date month = parse(string(body, "period"), MONTH);
		boolean check = !Boolean.FALSE.equals(body.get("check")) && !"false".equals(String.valueOf(body.get("check")));
		DqReportRun run = service().generateReportRun(report, month, check);
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
	 * The cells of a report version that can be compared, patient by patient, with their source
	 * records, with the value stored in the version
	 */
	@GetMapping("/runs/{uuid}/reconciliations")
	public ResponseEntity<Object> getReconciliations(@PathVariable("uuid") String uuid) {
		requireAuthentication();
		DqReportRun run = run(uuid);
		Map<String, String> stored = new HashMap<String, String>();
		for (DqValue value : service().getValues(run)) {
			stored.put(value.getDatasetName() + "\u0000" + value.getColumnName(), value.getValueText());
		}
		List<SimpleObject> cells = new ArrayList<SimpleObject>();
		for (DqConfig.Reconciliation reconciliation : reconciliations(run)) {
			cells.add(cell(reconciliation).add("storedValue",
					DqIndicatorCalculator.parseNumber(stored.get(reconciliation.getDataset() + "\u0000" + reconciliation.getColumn()))));
		}
		return ok(new SimpleObject().add("results", cells));
	}

	/**
	 * Compares one cell of a report version with its source records, both counted now: the
	 * patients possibly missed and those possibly counted wrongly, each with the reasons found
	 */
	@GetMapping("/runs/{uuid}/reconciliations/{id}")
	public ResponseEntity<Object> reconcile(@PathVariable("uuid") String uuid, @PathVariable("id") String id) {
		requireAuthentication();
		Context.requirePrivilege(PRIVILEGE_VIEW_PATIENTS);
		DqReportRun run = run(uuid);
		DqConfig.Reconciliation reconciliation = null;
		for (DqConfig.Reconciliation candidate : reconciliations(run)) {
			if (candidate.getId().equals(id)) {
				reconciliation = candidate;
			}
		}
		if (reconciliation == null) {
			return notFound("No reconciliation " + id + " for " + run.getReportName());
		}
		DqReconciliation.Result result = new DqReconciliation(service()).reconcile(reconciliation, run.getPeriodStart());
		return ok(cell(reconciliation).add("period", format(run.getPeriodStart(), MONTH))
				.add("reportedCount", result.getReportedCount()).add("sourceCount", result.getSourceCount())
				.add("inBoth", result.getInBoth()).add("missed", findings(result.getMissed(), run.getPeriodEnd()))
				.add("extra", findings(result.getExtra(), run.getPeriodEnd())));
	}

	/**
	 * The reconciliations of the enabled programs for the run's report
	 */
	private static List<DqConfig.Reconciliation> reconciliations(DqReportRun run) {
		List<DqConfig.Reconciliation> found = new ArrayList<DqConfig.Reconciliation>();
		for (DqConfig.Program program : DqConfigStore.getConfig().getPrograms()) {
			if (!program.isEnabled()) {
				continue;
			}
			for (DqConfig.Reconciliation reconciliation : program.getReconciliations()) {
				if (run.getReportName().equals(reconciliation.getReport())) {
					found.add(reconciliation);
				}
			}
		}
		return found;
	}

	private static SimpleObject cell(DqConfig.Reconciliation reconciliation) {
		return new SimpleObject().add("id", reconciliation.getId()).add("name", reconciliation.getName())
				.add("dataset", reconciliation.getDataset()).add("column", reconciliation.getColumn())
				.add("source", reconciliation.getSource());
	}

	private static List<SimpleObject> findings(List<DqReconciliation.Finding> findings, Date monthEnd) {
		List<SimpleObject> rows = new ArrayList<SimpleObject>();
		for (DqReconciliation.Finding finding : findings) {
			List<Map<String, Object>> tests = new ArrayList<Map<String, Object>>();
			for (DqReconciliation.TestRecord test : finding.getTests()) {
				tests.add(test.toMap());
			}
			List<Map<String, Object>> diagnoses = new ArrayList<Map<String, Object>>();
			for (DqReconciliation.DiagnosisRecord diagnosis : finding.getDiagnoses()) {
				diagnoses.add(diagnosis.toMap());
			}
			rows.add(patientFields(new SimpleObject().add("patientId", finding.getPatientId()).add("reasons", finding.getReasons())
					.add("tests", tests).add("diagnoses", diagnoses), finding.getPatientId(), monthEnd));
		}
		return rows;
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
	 * The patients behind a T22-05 result: prescribed an ACT (MOH 743) with nothing dispensed,
	 * dispensed (MOH 643) but not counted as prescribed, and dispensed more than one course, each with why. Both
	 * sides are counted now.
	 */
	@GetMapping("/results/{uuid}/dispensing")
	public ResponseEntity<Object> getDispensing(@PathVariable("uuid") String uuid) {
		requireAuthentication();
		Context.requirePrivilege(PRIVILEGE_VIEW_PATIENTS);
		DqService service = service();
		DqResult result = service.getResultByUuid(uuid);
		if (result == null) {
			return notFound("No result " + uuid);
		}
		if (!DqDispensingReconciliation.ACT_DISPENSING.equals(patientList(result.getRuleId()))
				|| result.getIndicatorCode() == null || !result.getIndicatorCode().contains("/")) {
			throw new APIException("No dispensing list is available for " + result.getRuleId());
		}
		// The result is about the pair consumed/treated
		DqConfig config = DqConfigStore.getConfig();
		String[] pair = result.getIndicatorCode().split("/", 2);
		DqConfig.Indicator consumed = config.getIndicator(pair[0]);
		DqConfig.Indicator treated = config.getIndicator(pair[1]);
		if (treated == null) {
			return notFound("No indicator " + pair[1]);
		}
		DqDispensingReconciliation.Result found = new DqDispensingReconciliation(service).reconcile(treated,
				result.getPeriodStart());
		Date monthEnd = DqPeriods.lastDayOfMonth(result.getPeriodStart());
		return ok(new SimpleObject().add("period", format(result.getPeriodStart(), MONTH))
				.add("consumed", indicatorRef(consumed)).add("treated", indicatorRef(treated))
				.add("dispensedCourses", found.getDispensedCourses()).add("givenPatients", found.getGivenPatients())
				.add("notDispensed", dispensingFindings(found.getNotDispensed(), monthEnd))
				.add("notCounted", dispensingFindings(found.getNotCounted(), monthEnd))
				.add("moreThanOneCourse", dispensingFindings(found.getMoreThanOneCourse(), monthEnd)));
	}

	private static List<SimpleObject> dispensingFindings(List<DqDispensingReconciliation.Finding> findings, Date monthEnd) {
		List<SimpleObject> rows = new ArrayList<SimpleObject>();
		for (DqDispensingReconciliation.Finding finding : findings) {
			rows.add(patientFields(new SimpleObject().add("patientId", finding.getPatientId())
					.add("reasons", finding.getReasons()).add("records", finding.getRecords()), finding.getPatientId(), monthEnd));
		}
		return rows;
	}

	/**
	 * Adds who the patient is, for a link to their chart
	 */
	private static SimpleObject patientFields(SimpleObject row, Integer patientId, Date monthEnd) {
		Patient patient = Context.getPatientService().getPatient(patientId);
		if (patient != null) {
			row.add("patientUuid", patient.getUuid())
					.add("name", patient.getPersonName() != null ? patient.getPersonName().getFullName() : null)
					.add("identifier", patient.getPatientIdentifier() != null
							? patient.getPatientIdentifier().getIdentifier() : null)
					.add("gender", patient.getGender()).add("age", patient.getAge(monthEnd));
		}
		return row;
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

	/**
	 * A weekly rule's (T22-08) results week by week, for the trend chart. Only weeks the daily task
	 * has checked have values; the others come back with no outcome.
	 *
	 * @param to a month (yyyy-MM, its last week) or a day (yyyy-MM-dd, its week); default, and at
	 * most, last week
	 * @param weeks how many weeks, 1 to 52
	 */
	@GetMapping("/rules/{ruleId}/weekly-trend")
	public ResponseEntity<Object> getWeeklyTrend(@PathVariable("ruleId") String ruleId,
	                                             @RequestParam(value = "to", required = false) String to,
	                                             @RequestParam(value = "weeks", defaultValue = "12") int weeks) {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		DqRule rule = DqRuleTypes.rule(config, ruleId);
		if (rule == null || rule.getSchedule() != DqRule.Schedule.WEEK) {
			return notFound("No weekly rule " + ruleId);
		}
		Date lastChecked = DqPeriods.addDays(DqPeriods.weekStart(new Date()), -7);
		Date last = lastChecked;
		if (to != null) {
			Date requested = to.length() == MONTH.length() ? DqPeriods.lastDayOfMonth(parse(to, MONTH)) : parse(to, DAY);
			last = DqPeriods.weekStart(requested);
			if (last.after(lastChecked)) {
				last = lastChecked;
			}
		}
		Date first = DqPeriods.addDays(last, -7 * (Math.max(1, Math.min(weeks, 52)) - 1));
		List<DqResult> results = service().getResults(ruleId, first, last);

		List<SimpleObject> points = new ArrayList<SimpleObject>();
		for (DqTrends.WeekPoint point : DqTrends.weeklyTrend(results, rule.getProgram(), first, last)) {
			points.add(new SimpleObject().add("week", format(point.getWeek(), DAY))
					.add("weekEnd", format(DqPeriods.weekEnd(point.getWeek()), DAY))
					.add("numerator", point.getNumerator()).add("denominator", point.getDenominator())
					.add("rate", point.getRate()).add("previousNumerator", point.getPreviousNumerator())
					.add("previousDenominator", point.getPreviousDenominator()).add("change", point.getChange())
					.add("outcome", point.getOutcome() != null ? point.getOutcome().name() : null)
					.add("message", point.getMessage()));
		}
		DqRuleSettings settings = config.getRuleSettings(ruleId, null);
		return ok(new SimpleObject().add("ruleId", ruleId).add("program", rule.getProgram())
				.add("enabled", settings.isEnabled())
				.add("numerator", indicatorRef(config.getIndicator(settings.getString("numerator", null))))
				.add("denominator", indicatorRef(config.getIndicator(settings.getString("denominator", null))))
				.add("changeThreshold", settings.getDouble("changeThreshold", 0.5))
				.add("minDenominator", settings.getInteger("minDenominator", settings.getInteger("minTests", 20)))
				.add("results", points));
	}

	/**
	 * A program's mRDQA-style dashboard for this facility, month by month up to "to": completeness
	 * and timeliness of the key reports, the cascade, cross checks and consistency over time, and the verification factors of the latest month
	 * checked. A month not yet checked has no values.
	 *
	 * @param program the program; default the first enabled program with a dashboard
	 * @param to the last month, default last month
	 * @param months how many months, 1 to 36
	 */
	/**
	 * Whether each monthly KHIS report reached KHIS by its deadline (the 5th), month by month up to
	 * "to", worked out now from the submission ledger, with the days late of those that did not
	 *
	 * @param program only this program's reports; all expected KHIS reports when undefined
	 */
	@GetMapping("/khis-timeliness")
	public ResponseEntity<Object> getKhisTimeliness(@RequestParam(value = "program", required = false) String programCode,
	                                                @RequestParam(value = "to", required = false) String to,
	                                                @RequestParam(value = "months", defaultValue = "12") int months) {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.Program program = program(config, programCode);
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = firstMonth(last, months);
		ServiceDqRuleContext context = new ServiceDqRuleContext(service(), config, null, first, last);
		boolean tracked = context.isKhisSubmissionTracked();
		Date now = new Date();

		// The reports the timeliness rule (G-RPT-01) judges, so that a report checked only for another
		// rule, such as MOH 643 for T22-05, is not counted here
		Set<String> judged = new HashSet<String>();
		for (DqConfig.RuleDefinition definition : config.getRuleDefinitions()) {
			if (DqConfig.RuleDefinition.TYPE_TIMELINESS.equals(definition.getType()) && definition.getSettings().isEnabled()
					&& (program == null || program.getCode().equals(definition.getProgramCode()))) {
				judged.addAll(definition.getSettings().getReportNames());
			}
		}
		List<DqConfig.ExpectedReport> reports = new ArrayList<DqConfig.ExpectedReport>();
		for (DqConfig.ExpectedReport report : config.getExpectedReports()) {
			if ("KHIS".equalsIgnoreCase(report.getTarget())
					&& DqConfig.ExpectedReport.FREQUENCY_MONTHLY.equals(report.getFrequency())
					&& (program == null || config.getReports(program).contains(report.getReport()))
					&& (judged.isEmpty() || judged.contains(report.getReport()))) {
				reports.add(report);
			}
		}
		List<SimpleObject> points = new ArrayList<SimpleObject>();
		for (Date month = first; !month.after(last); month = DqPeriods.addMonths(month, 1)) {
			List<SimpleObject> entries = new ArrayList<SimpleObject>();
			Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
			for (DqKhisTimeliness.Status status : DqKhisTimeliness.Status.values()) {
				counts.put(status.name(), 0);
			}
			for (DqConfig.ExpectedReport report : reports) {
				Date deadline = DqPeriods.deadline(month, report.getDeadlineDay() != null ? report.getDeadlineDay() : 5);
				DqKhisTimeliness.Entry entry = DqKhisTimeliness.judge(report.getReport(), deadline,
						tracked ? context.getKhisSubmission(report.getReport(), month) : null, now);
				counts.put(entry.getStatus().name(), counts.get(entry.getStatus().name()) + 1);
				entries.add(new SimpleObject().add("report", report.getReport()).add("deadline", format(deadline, DAY))
						.add("status", entry.getStatus().name()).add("submittedAt", entry.getSubmittedAt())
						.add("completedAt", entry.getCompletedAt()).add("daysLate", entry.getDaysLate())
						.add("note", entry.getNote()));
			}
			points.add(new SimpleObject().add("period", format(month, MONTH)).add("reports", entries).add("counts", counts));
		}
		return ok(new SimpleObject().add("tracked", tracked).add("from", format(first, MONTH)).add("to", format(last, MONTH))
				.add("results", points));
	}

	@GetMapping("/dashboard")
	public ResponseEntity<Object> getDashboard(@RequestParam(value = "program", required = false) String programCode,
	                                           @RequestParam(value = "to", required = false) String to,
	                                           @RequestParam(value = "months", defaultValue = "12") int months) {
		requireAuthentication();
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.Program program = program(config, programCode);
		if (program == null) {
			for (DqConfig.Program candidate : config.getPrograms()) {
				if (candidate.isEnabled() && candidate.getDashboard() != null) {
					program = candidate;
					break;
				}
			}
		}
		if (program == null || program.getDashboard() == null) {
			return notFound("No program has a dashboard; add a \"dashboard\" section to its file in kenyaemr/khis/dq_programs");
		}
		DqConfig.Dashboard dashboard = program.getDashboard();
		Date last = to != null ? parse(to, MONTH) : DqPeriods.previousMonth(new Date());
		Date first = firstMonth(last, months);
		DqService service = service();

		List<SimpleObject> reporting = new ArrayList<SimpleObject>();
		for (DqDashboard.ReportingPoint point : DqDashboard.reporting(results(service, config, program,
				DqConfig.RuleDefinition.TYPE_TIMELINESS, first, last), first, last)) {
			reporting.add(new SimpleObject().add("period", format(point.getMonth(), MONTH)).add("checked", point.isChecked())
					.add("expected", point.getExpected()).add("sent", point.getSent()).add("onTime", point.getOnTime())
					.add("completeness", point.getCompleteness()).add("timeliness", point.getTimeliness()));
		}

		final ServiceDqRuleContext context = new ServiceDqRuleContext(service, config, null, first, last);
		List<SimpleObject> crossChecks = new ArrayList<SimpleObject>();
		DqDashboard.Values values = new DqDashboard.Values() {

			@Override
			public DqIndicatorCalculator.IndicatorValue get(String code, Date month) {
				return context.getIndicator(code, month);
			}
		};
		for (DqConfig.CrossCheck check : dashboard.getCrossChecks()) {
			context.preload(check.getNumerator(), first, last);
			context.preload(check.getDenominator(), first, last);
			List<SimpleObject> points = new ArrayList<SimpleObject>();
			for (DqDashboard.RatioPoint point : DqDashboard.crossCheck(values, check, first, last)) {
				points.add(new SimpleObject().add("period", format(point.getMonth(), MONTH)).add("numerator", point.getNumerator())
						.add("denominator", point.getDenominator()).add("ratio", point.getRatio()));
			}
			crossChecks.add(new SimpleObject().add("id", check.getId()).add("name", check.getName())
					.add("description", check.getDescription())
					.add("numerator", indicatorRef(config.getIndicator(check.getNumerator())))
					.add("denominator", indicatorRef(config.getIndicator(check.getDenominator())))
					.add("min", check.getMin()).add("max", check.getMax()).add("results", points));
		}

		SimpleObject cascade = null;
		if (dashboard.getCascade() != null) {
			List<SimpleObject> steps = new ArrayList<SimpleObject>();
			for (DqConfig.CascadeStep step : dashboard.getCascade().getSteps()) {
				context.preload(step.getIndicator(), first, last);
				DqConfig.Indicator indicator = config.getIndicator(step.getIndicator());
				steps.add(new SimpleObject().add("code", step.getIndicator()).add("label", step.getLabel())
						.add("name", indicator != null ? indicator.getName() : null)
						.add("reports", indicator != null ? new ArrayList<String>(indicator.getReports()) : Collections.emptyList()));
			}
			List<SimpleObject> points = new ArrayList<SimpleObject>();
			for (Date month = first; !month.after(last); month = DqPeriods.addMonths(month, 1)) {
				Map<String, Double> monthValues = new LinkedHashMap<String, Double>();
				for (DqConfig.CascadeStep step : dashboard.getCascade().getSteps()) {
					monthValues.put(step.getIndicator(), values.get(step.getIndicator(), month).getValue());
				}
				points.add(new SimpleObject().add("period", format(month, MONTH)).add("values", monthValues));
			}
			cascade = new SimpleObject().add("ruleId", dashboard.getCascade().getRuleId())
					.add("name", dashboard.getCascade().getName()).add("steps", steps).add("results", points);
		}

		SimpleObject consistency = null;
		if (dashboard.getConsistency() != null) {
			String code = dashboard.getConsistency().getIndicator();
			// The annual ratio of the first month reaches back a year
			context.preload(code, DqPeriods.addMonths(first, -12), last);
			List<SimpleObject> points = new ArrayList<SimpleObject>();
			for (DqDashboard.ConsistencyPoint point : DqDashboard.consistency(values, code, first, last)) {
				points.add(new SimpleObject().add("period", format(point.getMonth(), MONTH)).add("value", point.getValue())
						.add("threeMonthAverage", point.getThreeMonthAverage())
						.add("sameMonthLastYear", point.getSameMonthLastYear())
						.add("monthToMonth", point.getMonthToMonth()).add("annual", point.getAnnual()));
			}
			consistency = new SimpleObject().add("indicator", indicatorRef(config.getIndicator(code)))
					.add("min", dashboard.getConsistency().getMin()).add("max", dashboard.getConsistency().getMax())
					.add("results", points);
		}

		List<SimpleObject> verification = new ArrayList<SimpleObject>();
		String verifiedPeriod = null;
		for (DqResult result : DqDashboard.latestMonth(results(service, config, program,
				DqConfig.RuleDefinition.TYPE_VERIFICATION, first, last))) {
			Map<String, Object> detail = DqDashboard.detail(result);
			verifiedPeriod = format(result.getPeriodStart(), MONTH);
			verification.add(new SimpleObject().add("indicator", indicatorRef(config.getIndicator(result.getIndicatorCode())))
					.add("code", result.getIndicatorCode()).add("reported", detail.get("reported"))
					.add("recount", detail.get("recount")).add("khis", detail.get("khis"))
					.add("vfReport", detail.get("vfReport")).add("vfKhis", detail.get("vfKhis"))
					.add("outcome", result.getOutcome().name()).add("message", result.getMessage()));
		}

		return ok(new SimpleObject().add("program", program.getCode()).add("from", format(first, MONTH))
				.add("to", format(last, MONTH)).add("target", dashboard.getTarget() != null ? dashboard.getTarget() : 0.9)
				.add("reporting", reporting)
				.add("crossChecks", crossChecks).add("cascade", cascade).add("consistency", consistency)
				.add("verification", new SimpleObject().add("period", verifiedPeriod).add("results", verification)));
	}

	/**
	 * @return the saved results, for the months, of the program's rules of the type
	 */
	private static List<DqResult> results(DqService service, DqConfig config, DqConfig.Program program, String type,
	                                      Date first, Date last) {
		List<DqResult> results = new ArrayList<DqResult>();
		for (DqConfig.RuleDefinition definition : config.getRuleDefinitions()) {
			if (type.equals(definition.getType()) && program.getCode().equals(definition.getProgramCode())
					&& definition.getSettings().isEnabled()) {
				for (DqResult result : service.getResults(definition.getId(), first, DqPeriods.lastDayOfMonth(last))) {
					if (result.getReportRun() == null) {
						results.add(result);
					}
				}
			}
		}
		return results;
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
	 * Runs the daily data quality task now, as at 01:00: last month's reports, the weeks' checks,
	 * the month-level checks and any missing history. It runs in the request and can take some
	 * minutes. Needs the "Manage Scheduler" privilege.
	 */
	@PostMapping("/task/run")
	public ResponseEntity<Object> runTask() {
		requireAuthentication();
		if (!Context.hasPrivilege(PRIVILEGE_MANAGE_SCHEDULER)) {
			return new ResponseEntity<Object>(new SimpleObject().add("error", "Running the data quality task needs the \""
					+ PRIVILEGE_MANAGE_SCHEDULER + "\" privilege."), HttpStatus.FORBIDDEN);
		}
		long started = System.currentTimeMillis();
		if (!DataQualityTask.runNow()) {
			return new ResponseEntity<Object>(new SimpleObject().add("error", "The data quality task is already running."),
					HttpStatus.CONFLICT);
		}
		return ok(new SimpleObject().add("ran", true).add("seconds", (System.currentTimeMillis() - started) / 1000));
	}

	/**
	 * Runs the month-level checks (G-RPT-01 timeliness) for a month whose deadlines are over, if not
	 * run already, and runs again those (VF-01) whose reports have a newer version since. Body:
	 * {period: yyyy-MM}
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
				.add("weekly", DqPeriods.isWholeWeek(run.getPeriodStart(), run.getPeriodEnd()))
				.add("periodStart", format(run.getPeriodStart(), DAY)).add("periodEnd", format(run.getPeriodEnd(), DAY))
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

		// Tracked indicators this report feeds: this month, last month and the 12-month average; a
		// weekly version has no months to compare
		DqConfig config = DqConfigStore.getConfig();
		boolean weekly = DqPeriods.isWholeWeek(run.getPeriodStart(), run.getPeriodEnd());
		ServiceDqRuleContext context = new ServiceDqRuleContext(service, config, run, run.getPeriodStart(), run.getPeriodEnd());
		List<SimpleObject> indicators = new ArrayList<SimpleObject>();
		for (DqConfig.Indicator indicator : weekly ? Collections.<DqConfig.Indicator>emptyList() : config.getIndicators()) {
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
				.add("period", result.getPeriodStart()).add("evaluatedAt", result.getEvaluatedAt())
				.add("patients", patientList(result.getRuleId()));
	}

	/** The patient list of a T22-07 result: each repeat positive and the earlier one */
	static final String PATIENTS_REPEAT_POSITIVES = "REPEAT_POSITIVES";

	/**
	 * @return which list of patients the rule's results have, from /results/{uuid}/patients or
	 * /results/{uuid}/dispensing; null when they have none
	 */
	private static String patientList(String ruleId) {
		DqConfig config = DqConfigStore.getConfig();
		DqConfig.RuleDefinition definition = config.getRuleDefinition(ruleId);
		if (definition == null) {
			return null;
		}
		if (DqConfig.RuleDefinition.TYPE_REPEAT_EPISODE.equals(definition.getType())) {
			return PATIENTS_REPEAT_POSITIVES;
		}
		if (DqConfig.RuleDefinition.TYPE_RECONCILIATION.equals(definition.getType())
				&& DqDispensingReconciliation.ACT_DISPENSING.equals(config.getRuleSettings(ruleId, null).getString("patients", null))) {
			return DqDispensingReconciliation.ACT_DISPENSING;
		}
		return null;
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
				.add("checkedAt", ticket.getResult() != null ? ticket.getResult().getEvaluatedAt() : null)
				.add("emrValue", ticket.getEmrValue()).add("khisValue", ticket.getKhisValue())
				.add("variancePct", ticket.getVariancePct()).add("suggestedCause", ticket.getSuggestedCause())
				.add("assigneeRole", ticket.getAssigneeRole()).add("status", ticket.getStatus().name())
				.add("resolutionNote", ticket.getResolutionNote()).add("openedAt", ticket.getOpenedAt())
				.add("resolvedAt", ticket.getResolvedAt()).add("resolvedBy", userName(ticket.getResolvedBy()))
				.add("resultUuid", ticket.getResult() != null ? ticket.getResult().getUuid() : null)
				.add("patients", ticket.getResult() != null ? patientList(ticket.getRuleId()) : null);
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
