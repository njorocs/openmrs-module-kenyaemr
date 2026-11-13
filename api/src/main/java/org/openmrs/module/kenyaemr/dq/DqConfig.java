/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data quality configuration, set by each facility within the national M&E guidelines: the reports a
 * facility is expected to submit, the indicators built from their columns, and the program areas
 * (malaria, TB, ...) whose rules check them. It is assembled by {@link DqConfigStore} from
 * {@code kenyaemr/khis/dq_config.json} and one file per program in {@code kenyaemr/khis/dq_programs}.
 * <p>
 * Indicators form one library that every program can use. A rule belongs to one program and is an
 * instance of one of the rule types in {@link org.openmrs.module.kenyaemr.dq.rule.DqRuleTypes}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DqConfig {

	private List<ExpectedReport> expectedReports = new ArrayList<ExpectedReport>();

	private List<Indicator> indicators = new ArrayList<Indicator>();

	private List<Program> programs = new ArrayList<Program>();

	/**
	 * Rule id -> settings, from before program areas. Only read for a rule no program defines; kept
	 * so that a facility file from then still loads.
	 */
	private Map<String, Map<String, Object>> rules = new LinkedHashMap<String, Map<String, Object>>();

	/** The fixed list of reasons an HRIO can give when accepting a warning */
	private List<String> overrideReasons = new ArrayList<String>();

	public List<ExpectedReport> getExpectedReports() {
		return expectedReports;
	}

	public void setExpectedReports(List<ExpectedReport> expectedReports) {
		this.expectedReports = expectedReports != null ? expectedReports : new ArrayList<ExpectedReport>();
	}

	public List<Indicator> getIndicators() {
		return indicators;
	}

	public void setIndicators(List<Indicator> indicators) {
		this.indicators = indicators != null ? indicators : new ArrayList<Indicator>();
	}

	public Map<String, Map<String, Object>> getRules() {
		return rules;
	}

	public void setRules(Map<String, Map<String, Object>> rules) {
		this.rules = rules != null ? rules : new LinkedHashMap<String, Map<String, Object>>();
	}

	public List<Program> getPrograms() {
		return programs;
	}

	public void setPrograms(List<Program> programs) {
		this.programs = programs != null ? programs : new ArrayList<Program>();
	}

	public Program getProgram(String code) {
		for (Program program : programs) {
			if (program.getCode() != null && program.getCode().equalsIgnoreCase(code)) {
				return program;
			}
		}
		return null;
	}

	/**
	 * @return the rules of every enabled program, in program order
	 */
	@JsonIgnore
	public List<RuleDefinition> getRuleDefinitions() {
		List<RuleDefinition> definitions = new ArrayList<RuleDefinition>();
		for (Program program : programs) {
			if (program.isEnabled()) {
				for (Map<String, Object> rule : program.getRules()) {
					definitions.add(new RuleDefinition(program, rule));
				}
			}
		}
		return definitions;
	}

	/**
	 * @return the rule of an enabled program with this id, or null
	 */
	public RuleDefinition getRuleDefinition(String ruleId) {
		for (RuleDefinition definition : getRuleDefinitions()) {
			if (definition.getId() != null && definition.getId().equals(ruleId)) {
				return definition;
			}
		}
		return null;
	}

	/**
	 * @return true when an enabled expected-range rule (T22-03) checks the indicator, or the indicator
	 * is marked tracked; tracked indicators get a trend panel
	 */
	public boolean isTracked(Indicator indicator) {
		if (indicator.isTracked()) {
			return true;
		}
		for (RuleDefinition definition : getRuleDefinitions()) {
			if (RuleDefinition.TYPE_EXPECTED_RANGE.equals(definition.getType())
					&& definition.getIndicatorCodes().contains(indicator.getCode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the reports a program reads: those of its indicators and its rules
	 */
	public Set<String> getReports(Program program) {
		Set<String> reports = new LinkedHashSet<String>();
		for (String code : program.getIndicators()) {
			Indicator indicator = getIndicator(code);
			if (indicator != null) {
				reports.addAll(indicator.getReports());
			}
		}
		for (Map<String, Object> rule : program.getRules()) {
			RuleDefinition definition = new RuleDefinition(program, rule);
			for (String code : definition.getIndicatorCodes()) {
				Indicator indicator = getIndicator(code);
				if (indicator != null) {
					reports.addAll(indicator.getReports());
				}
			}
			reports.addAll(definition.getSettings().getReportNames());
		}
		return reports;
	}

	public List<String> getOverrideReasons() {
		return overrideReasons;
	}

	public void setOverrideReasons(List<String> overrideReasons) {
		this.overrideReasons = overrideReasons != null ? overrideReasons : new ArrayList<String>();
	}

	/**
	 * @return a copy in which the indicator counts only its parts from one report, e.g. confirmed
	 *         malaria from MOH 705A alone when the indicator adds up 705A and 705B; everything else is
	 *         shared with this configuration. This one is returned when the indicator has no part from
	 *         the report.
	 */
	public DqConfig withIndicatorFromReport(String code, String report) {
		Indicator indicator = getIndicator(code);
		if (indicator == null) {
			return this;
		}
		List<Part> parts = new ArrayList<Part>();
		for (Part part : indicator.getParts()) {
			if (part.getReport() != null && part.getReport().equals(report)) {
				parts.add(part);
			}
		}
		if (parts.isEmpty()) {
			return this;
		}
		Indicator narrowed = new Indicator();
		narrowed.setCode(indicator.getCode());
		narrowed.setName(indicator.getName());
		narrowed.setTracked(indicator.isTracked());
		narrowed.setRules(indicator.getRules());
		narrowed.setParts(parts);
		List<Indicator> copied = new ArrayList<Indicator>();
		for (Indicator other : indicators) {
			copied.add(other == indicator ? narrowed : other);
		}
		DqConfig copy = new DqConfig();
		copy.setExpectedReports(expectedReports);
		copy.setIndicators(copied);
		copy.setPrograms(programs);
		copy.setRules(rules);
		copy.setOverrideReasons(overrideReasons);
		return copy;
	}

	public Indicator getIndicator(String code) {
		for (Indicator indicator : indicators) {
			if (indicator.getCode().equals(code)) {
				return indicator;
			}
		}
		return null;
	}

	public ExpectedReport getExpectedReport(String reportName) {
		for (ExpectedReport report : expectedReports) {
			if (report.getReport().equals(reportName)) {
				return report;
			}
		}
		return null;
	}

	/**
	 * @return the settings for a rule, with an indicator's own settings layered over the global ones
	 * @param indicator the indicator, or null for the global settings only
	 */
	public DqRuleSettings getRuleSettings(String ruleId, Indicator indicator) {
		Map<String, Object> merged = new LinkedHashMap<String, Object>();
		RuleDefinition definition = getRuleDefinition(ruleId);
		Map<String, Object> global = definition != null ? definition.getValues() : rules.get(ruleId);
		if (global != null) {
			merged.putAll(global);
		}
		if (indicator != null && indicator.getRules().get(ruleId) != null) {
			merged.putAll(indicator.getRules().get(ruleId));
		}
		return new DqRuleSettings(merged);
	}

	/**
	 * A program area, e.g. malaria: the indicators it watches and the rules that check them
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Program {

		private String code;

		private String name;

		/** Tickets of this program's rules go to this role unless a rule names another */
		private String ownerRole;

		private boolean enabled = true;

		/** The national M&E guideline version the facility's settings follow */
		private String guidelineVersion;

		/**
		 * The reports this program may hold back with a hard block. On any other report a block of
		 * this program's rules is shown as a warning, so one program cannot stop a shared report.
		 */
		private List<String> owns = new ArrayList<String>();

		/** Indicator codes the program watches; its trend panels */
		private List<String> indicators = new ArrayList<String>();

		/** Rule instances: id, type and the type's settings */
		private List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();

		/** What the program's data quality dashboard draws; none when it has no dashboard */
		private Dashboard dashboard;

		/** Report cells the reviewer can compare, patient by patient, with their source records */
		private List<Reconciliation> reconciliations = new ArrayList<Reconciliation>();

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOwnerRole() {
			return ownerRole;
		}

		public void setOwnerRole(String ownerRole) {
			this.ownerRole = ownerRole;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getGuidelineVersion() {
			return guidelineVersion;
		}

		public void setGuidelineVersion(String guidelineVersion) {
			this.guidelineVersion = guidelineVersion;
		}

		public List<String> getOwns() {
			return owns;
		}

		public void setOwns(List<String> owns) {
			this.owns = owns != null ? owns : new ArrayList<String>();
		}

		public List<String> getIndicators() {
			return indicators;
		}

		public void setIndicators(List<String> indicators) {
			this.indicators = indicators != null ? indicators : new ArrayList<String>();
		}

		public List<Map<String, Object>> getRules() {
			return rules;
		}

		public void setRules(List<Map<String, Object>> rules) {
			this.rules = rules != null ? rules : new ArrayList<Map<String, Object>>();
		}

		public Dashboard getDashboard() {
			return dashboard;
		}

		public void setDashboard(Dashboard dashboard) {
			this.dashboard = dashboard;
		}

		public List<Reconciliation> getReconciliations() {
			return reconciliations;
		}

		public void setReconciliations(List<Reconciliation> reconciliations) {
			this.reconciliations = reconciliations != null ? reconciliations : new ArrayList<Reconciliation>();
		}
	}

	/**
	 * A report cell and the source records it should agree with, patient by patient: e.g. the
	 * confirmed malaria in MOH 705B against the positive malaria tests of patients aged 5 and over.
	 * The patients in only one of the two are the ones to check.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Reconciliation {

		public static final String SOURCE_TESTED = "MALARIA_TESTED";

		public static final String SOURCE_POSITIVE = "MALARIA_POSITIVE";

		/** Unique within the program, e.g. 705B-CONFIRMED */
		private String id;

		private String name;

		private String report;

		private String dataset;

		/** The month's total column, e.g. COA-32 */
		private String column;

		/** MALARIA_TESTED (a malaria test with a result) or MALARIA_POSITIVE (a positive one) */
		private String source;

		/** Age on the last day of the month, as the report computes it; null for no limit */
		private Integer minAgeYears;

		/** Exclusive */
		private Integer maxAgeYears;

		/** The malaria test concepts the report counts */
		private List<Integer> labTests = new ArrayList<Integer>();

		/** For MALARIA_POSITIVE: the diagnoses the report counts as confirmed, to explain mismatches */
		private List<Integer> diagnoses = new ArrayList<Integer>();

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getReport() {
			return report;
		}

		public void setReport(String report) {
			this.report = report;
		}

		public String getDataset() {
			return dataset;
		}

		public void setDataset(String dataset) {
			this.dataset = dataset;
		}

		public String getColumn() {
			return column;
		}

		public void setColumn(String column) {
			this.column = column;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public Integer getMinAgeYears() {
			return minAgeYears;
		}

		public void setMinAgeYears(Integer minAgeYears) {
			this.minAgeYears = minAgeYears;
		}

		public Integer getMaxAgeYears() {
			return maxAgeYears;
		}

		public void setMaxAgeYears(Integer maxAgeYears) {
			this.maxAgeYears = maxAgeYears;
		}

		public List<Integer> getLabTests() {
			return labTests;
		}

		public void setLabTests(List<Integer> labTests) {
			this.labTests = labTests != null ? labTests : new ArrayList<Integer>();
		}

		public List<Integer> getDiagnoses() {
			return diagnoses;
		}

		public void setDiagnoses(List<Integer> diagnoses) {
			this.diagnoses = diagnoses != null ? diagnoses : new ArrayList<Integer>();
		}
	}

	/**
	 * A program's mRDQA-style dashboard for one facility: the target for completeness and
	 * timeliness, the cascade and cross checks drawn month by month, and the indicator whose
	 * consistency over time is drawn. The verification factors come from the program's VERIFICATION
	 * rules.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Dashboard {

		/** For completeness and timeliness, e.g. 0.9 */
		private Double target;

		private List<CrossCheck> crossChecks = new ArrayList<CrossCheck>();

		private Consistency consistency;

		private Cascade cascade;

		public Double getTarget() {
			return target;
		}

		public void setTarget(Double target) {
			this.target = target;
		}

		public List<CrossCheck> getCrossChecks() {
			return crossChecks;
		}

		public void setCrossChecks(List<CrossCheck> crossChecks) {
			this.crossChecks = crossChecks != null ? crossChecks : new ArrayList<CrossCheck>();
		}

		public Consistency getConsistency() {
			return consistency;
		}

		public void setConsistency(Consistency consistency) {
			this.consistency = consistency;
		}

		public Cascade getCascade() {
			return cascade;
		}

		public void setCascade(Cascade cascade) {
			this.cascade = cascade;
		}
	}

	/**
	 * The steps of care month by month, each an indicator of its own report, drawn as one chart of
	 * absolute numbers: for malaria suspected (MOH 705A and 705B), tested and confirmed (MOH 706) and
	 * treated (MOH 743)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Cascade {

		/** The rule that judges the steps, shown with the chart */
		private String ruleId;

		private String name;

		private List<CascadeStep> steps = new ArrayList<CascadeStep>();

		public String getRuleId() {
			return ruleId;
		}

		public void setRuleId(String ruleId) {
			this.ruleId = ruleId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<CascadeStep> getSteps() {
			return steps;
		}

		public void setSteps(List<CascadeStep> steps) {
			this.steps = steps != null ? steps : new ArrayList<CascadeStep>();
		}
	}

	/** One step of a cascade: an indicator, and its short name in the chart */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CascadeStep {

		private String indicator;

		private String label;

		public String getIndicator() {
			return indicator;
		}

		public void setIndicator(String indicator) {
			this.indicator = indicator;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}

	/**
	 * A cross check between two data sources: numerator ÷ denominator for each month, within min
	 * and max
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CrossCheck {

		private String id;

		private String name;

		private String description;

		private String numerator;

		private String denominator;

		private Double min;

		private Double max;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getNumerator() {
			return numerator;
		}

		public void setNumerator(String numerator) {
			this.numerator = numerator;
		}

		public String getDenominator() {
			return denominator;
		}

		public void setDenominator(String denominator) {
			this.denominator = denominator;
		}

		public Double getMin() {
			return min;
		}

		public void setMin(Double min) {
			this.min = min;
		}

		public Double getMax() {
			return max;
		}

		public void setMax(Double max) {
			this.max = max;
		}
	}

	/**
	 * The indicator drawn for consistency over time, with the band its ratios should fall in
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Consistency {

		private String indicator;

		private Double min;

		private Double max;

		public String getIndicator() {
			return indicator;
		}

		public void setIndicator(String indicator) {
			this.indicator = indicator;
		}

		public Double getMin() {
			return min;
		}

		public void setMin(Double min) {
			this.min = min;
		}

		public Double getMax() {
			return max;
		}

		public void setMax(Double max) {
			this.max = max;
		}
	}

	/**
	 * One rule of a program: its id, rule type and settings
	 */
	public static class RuleDefinition {

		public static final String TYPE_RATIO = "RATIO";

		public static final String TYPE_EXPECTED_RANGE = "EXPECTED_RANGE";

		public static final String TYPE_REPEAT_EPISODE = "REPEAT_EPISODE";

		public static final String TYPE_CHANGE = "CHANGE";

		public static final String TYPE_TIMELINESS = "TIMELINESS";

		public static final String TYPE_KHIS_PARITY = "KHIS_PARITY";

		public static final String TYPE_VERIFICATION = "VERIFICATION";

		/** Settings that hold indicator codes, alone or in a list of pairs */
		private static final String[] INDICATOR_KEYS = { "numerator", "denominator", "consumed", "treated" };

		private final Program program;

		private final Map<String, Object> values;

		public RuleDefinition(Program program, Map<String, Object> values) {
			this.program = program;
			this.values = values != null ? values : new LinkedHashMap<String, Object>();
		}

		public Program getProgram() {
			return program;
		}

		public String getProgramCode() {
			return program != null ? program.getCode() : null;
		}

		public String getId() {
			return getSettings().getString("id", null);
		}

		public String getType() {
			String type = getSettings().getString("type", null);
			return type != null ? type.trim().toUpperCase() : null;
		}

		/** @return where the rule comes from, e.g. "TR-26-110 Table 22" */
		public String getSource() {
			return getSettings().getString("source", null);
		}

		/** @return the only report a cross-report rule may block, or null when it may block none */
		public String getGates() {
			return getSettings().getString("gates", null);
		}

		/** @return the role the rule's tickets go to: its own, else the program's, else HRIO */
		public String getAssigneeRole() {
			String role = getSettings().getString("assigneeRole", null);
			if (role == null && program != null) {
				role = program.getOwnerRole();
			}
			return role != null ? role : "HRIO";
		}

		public Map<String, Object> getValues() {
			return values;
		}

		public DqRuleSettings getSettings() {
			return new DqRuleSettings(values);
		}

		/**
		 * @return every indicator code the rule names: numerator, denominator, consumed, treated, in
		 * pairs or alone, and the "indicators" list
		 */
		public Set<String> getIndicatorCodes() {
			Set<String> codes = new LinkedHashSet<String>();
			DqRuleSettings settings = getSettings();
			List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>(settings.getObjectList("pairs"));
			groups.add(values);
			for (Map<String, Object> group : groups) {
				for (String key : INDICATOR_KEYS) {
					if (group.get(key) != null) {
						codes.add(group.get(key).toString());
					}
				}
			}
			codes.addAll(settings.getStrings("indicators"));
			return codes;
		}
	}

	/**
	 * A report the facility must submit each period
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ExpectedReport {

		public static final String FREQUENCY_MONTHLY = "MONTHLY";

		public static final String FREQUENCY_WEEKLY = "WEEKLY";

		/** Report descriptor name, as in the KHIS mapping file */
		private String report;

		private String frequency = FREQUENCY_MONTHLY;

		/** Day of the month by which the report is due */
		private Integer deadlineDay;

		/** KHIS or KNLMS */
		private String target = "KHIS";

		/** False to stop generating the report on the 1st without removing it from the list */
		private boolean generate = true;

		/**
		 * True for a report also sent to KHIS a week at a time (Monday to Sunday), e.g. MOH 505: each
		 * week generated on the reports page is stored as its own version, checked by the week's
		 * checks (T22-08) and held from KHIS like a month
		 */
		private boolean weekly;

		public String getReport() {
			return report;
		}

		public void setReport(String report) {
			this.report = report;
		}

		public String getFrequency() {
			return frequency;
		}

		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}

		public Integer getDeadlineDay() {
			return deadlineDay;
		}

		public void setDeadlineDay(Integer deadlineDay) {
			this.deadlineDay = deadlineDay;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public boolean isGenerate() {
			return generate;
		}

		public void setGenerate(boolean generate) {
			this.generate = generate;
		}

		public boolean isWeekly() {
			return weekly;
		}

		public void setWeekly(boolean weekly) {
			this.weekly = weekly;
		}
	}

	/**
	 * A named sum of report columns, e.g. confirmed malaria cases
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Indicator {

		private String code;

		private String name;

		/** Tracked indicators get the consistency-over-time check (T22-03) and a trend panel */
		private boolean tracked;

		private List<Part> parts = new ArrayList<Part>();

		/** Rule id -> settings for this indicator only */
		private Map<String, Map<String, Object>> rules = new LinkedHashMap<String, Map<String, Object>>();

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isTracked() {
			return tracked;
		}

		public void setTracked(boolean tracked) {
			this.tracked = tracked;
		}

		public List<Part> getParts() {
			return parts;
		}

		public void setParts(List<Part> parts) {
			this.parts = parts != null ? parts : new ArrayList<Part>();
		}

		public Map<String, Map<String, Object>> getRules() {
			return rules;
		}

		public void setRules(Map<String, Map<String, Object>> rules) {
			this.rules = rules != null ? rules : new LinkedHashMap<String, Map<String, Object>>();
		}

		/**
		 * @return the reports this indicator reads from
		 */
		@JsonIgnore
		public Set<String> getReports() {
			Set<String> reports = new LinkedHashSet<String>();
			for (Part part : parts) {
				reports.add(part.getReport());
			}
			return reports;
		}
	}

	/**
	 * Columns of one dataset in one report that are added into an indicator
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Part {

		private String report;

		private String dataset;

		private List<String> columns = new ArrayList<String>();

		/**
		 * True for a dataset that has no rows when there is nothing to count, such as a stock dataset
		 * with no movement: its columns then count as zero rather than missing, once the report itself
		 * has been generated
		 */
		private boolean zeroWhenEmpty;

		public String getReport() {
			return report;
		}

		public void setReport(String report) {
			this.report = report;
		}

		public String getDataset() {
			return dataset;
		}

		public void setDataset(String dataset) {
			this.dataset = dataset;
		}

		public List<String> getColumns() {
			return columns;
		}

		public void setColumns(List<String> columns) {
			this.columns = columns != null ? columns : new ArrayList<String>();
		}

		public boolean isZeroWhenEmpty() {
			return zeroWhenEmpty;
		}

		public void setZeroWhenEmpty(boolean zeroWhenEmpty) {
			this.zeroWhenEmpty = zeroWhenEmpty;
		}
	}
}
