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
 * Data quality configuration, edited by M&E in {@code kenyaemr/khis/dq_config.json}: the reports a
 * facility is expected to submit, the indicators built from their columns, and the rule thresholds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DqConfig {

	private List<ExpectedReport> expectedReports = new ArrayList<ExpectedReport>();

	private List<Indicator> indicators = new ArrayList<Indicator>();

	/** Rule id -> settings that apply to every indicator unless the indicator overrides them */
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
		Map<String, Object> global = rules.get(ruleId);
		if (global != null) {
			merged.putAll(global);
		}
		if (indicator != null && indicator.getRules().get(ruleId) != null) {
			merged.putAll(indicator.getRules().get(ruleId));
		}
		return new DqRuleSettings(merged);
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
	}
}
