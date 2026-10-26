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
	}

	/**
	 * One rule of a program: its id, rule type and settings
	 */
	public static class RuleDefinition {

		public static final String TYPE_RATIO = "RATIO";

		public static final String TYPE_EXPECTED_RANGE = "EXPECTED_RANGE";

		public static final String TYPE_RECONCILIATION = "RECONCILIATION";

		public static final String TYPE_REPEAT_EPISODE = "REPEAT_EPISODE";

		public static final String TYPE_CHANGE = "CHANGE";

		public static final String TYPE_TIMELINESS = "TIMELINESS";

		public static final String TYPE_KHIS_PARITY = "KHIS_PARITY";

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
