/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.dq.rule;

import org.openmrs.module.kenyaemr.dq.DqConfig;
import org.openmrs.module.kenyaemr.dq.DqConfigStore;
import org.openmrs.module.kenyaemr.dq.DqIndicatorCalculator;
import org.openmrs.module.kenyaemr.dq.DqPeriods;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule context over the bundled default configuration, with indicator values and query results
 * set by the test
 */
class FakeRuleContext implements DqRuleContext {

	private final DqConfig config;

	private final DqReportRun run;

	private final Date periodStart;

	private final Date periodEnd;

	private final Map<String, DqIndicatorCalculator.IndicatorValue> indicators;

	private final Set<String> generated;

	private final LinkedList<List<Object[]>> queryResults;

	private final Map<String, Date> sentToKhis;

	private final List<DqValue> values = new ArrayList<DqValue>();

	boolean khisTracked = true;

	final List<Map<String, Object>> queryParameters = new ArrayList<Map<String, Object>>();

	/**
	 * A monthly context for a run of the named report, with every expected report generated
	 */
	static FakeRuleContext monthly(String reportName, Date month) {
		DqReportRun run = new DqReportRun();
		run.setReportName(reportName);
		run.setPeriodStart(month);
		FakeRuleContext context = new FakeRuleContext(run, month, DqPeriods.lastDayOfMonth(month));
		for (DqConfig.ExpectedReport report : context.config.getExpectedReports()) {
			context.generated(report.getReport(), month);
		}
		return context;
	}

	static FakeRuleContext weekly(Date weekStart) {
		return new FakeRuleContext(null, weekStart, DqPeriods.weekEnd(weekStart));
	}

	private FakeRuleContext(DqReportRun run, Date periodStart, Date periodEnd) {
		this.config = DqConfigStore.getDefaultConfig();
		this.run = run;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.indicators = new HashMap<String, DqIndicatorCalculator.IndicatorValue>();
		this.generated = new HashSet<String>();
		this.queryResults = new LinkedList<List<Object[]>>();
		this.sentToKhis = new HashMap<String, Date>();
	}

	private FakeRuleContext(FakeRuleContext parent, Date monthStart) {
		this.config = parent.config;
		this.run = null;
		this.periodStart = monthStart;
		this.periodEnd = DqPeriods.lastDayOfMonth(monthStart);
		this.indicators = parent.indicators;
		this.generated = parent.generated;
		this.queryResults = parent.queryResults;
		this.sentToKhis = parent.sentToKhis;
	}

	/**
	 * A month-level context (G-RPT-01), with no run
	 */
	static FakeRuleContext month(Date month) {
		return new FakeRuleContext(null, month, DqPeriods.lastDayOfMonth(month));
	}

	FakeRuleContext sent(String report, Date month, Date sentAt) {
		sentToKhis.put(report + "@" + month.getTime(), sentAt);
		return this;
	}

	/**
	 * Adds a stored cell of the run, with the value KHIS holds for it
	 */
	FakeRuleContext value(String dataset, String column, String dataElement, Double emr, Double khis) {
		return value(dataset, column, dataElement, dataElement != null ? "COC" : null, emr, khis);
	}

	/**
	 * Adds a report value with its category option combo, as recorded after a KHIS read-back
	 */
	FakeRuleContext value(String dataset, String column, String dataElement, String categoryOptionCombo, Double emr,
	        Double khis) {
		DqValue value = new DqValue();
		value.setReportRun(run);
		value.setDatasetName(dataset);
		value.setColumnName(column);
		value.setDataElement(dataElement);
		value.setCategoryOptionCombo(categoryOptionCombo);
		value.setValueNumeric(emr);
		value.setKhisValue(khis);
		values.add(value);
		return this;
	}

	FakeRuleContext indicator(String code, Date month, Double value) {
		indicators.put(code + "@" + month.getTime(), new DqIndicatorCalculator.IndicatorValue(code, value,
				Collections.<String>emptyList()));
		return this;
	}

	FakeRuleContext incompleteIndicator(String code, Date month, Double value) {
		indicators.put(code + "@" + month.getTime(), new DqIndicatorCalculator.IndicatorValue(code, value,
				Collections.singletonList("MOH 705B Outpatient summary / MOH705B / COA-32")));
		return this;
	}

	FakeRuleContext generated(String report, Date month) {
		generated.add(report + "@" + month.getTime());
		return this;
	}

	FakeRuleContext notGenerated(String report, Date month) {
		generated.remove(report + "@" + month.getTime());
		return this;
	}

	/**
	 * Queues the rows returned by the next query
	 */
	FakeRuleContext queryReturns(Object[]... rows) {
		List<Object[]> list = new ArrayList<Object[]>();
		Collections.addAll(list, rows);
		queryResults.add(list);
		return this;
	}

	@Override
	public DqConfig getConfig() {
		return config;
	}

	@Override
	public DqReportRun getRun() {
		return run;
	}

	@Override
	public Date getPeriodStart() {
		return periodStart;
	}

	@Override
	public Date getPeriodEnd() {
		return periodEnd;
	}

	@Override
	public DqIndicatorCalculator.IndicatorValue getIndicator(String code, Date monthStart) {
		DqIndicatorCalculator.IndicatorValue value = indicators.get(code + "@" + monthStart.getTime());
		return value != null ? value : new DqIndicatorCalculator.IndicatorValue(code, null,
				Collections.singletonList("not generated"));
	}

	/**
	 * Week values are set with {@link #indicator(String, Date, Double)} on the week's first day
	 */
	@Override
	public DqIndicatorCalculator.IndicatorValue getIndicator(String code, Date start, Date end) {
		return getIndicator(code, start);
	}

	@Override
	public List<DqValue> getValues(DqReportRun run) {
		return values;
	}

	@Override
	public boolean isKhisSubmissionTracked() {
		return khisTracked;
	}

	@Override
	public Date getFirstSentToKhis(String reportName, Date monthStart) {
		return sentToKhis.get(reportName + "@" + monthStart.getTime());
	}

	@Override
	public boolean isGenerated(String reportName, Date monthStart) {
		return generated.contains(reportName + "@" + monthStart.getTime());
	}

	@Override
	public String getEtlDatabase() {
		return "kenyaemr_etl";
	}

	@Override
	public DqRuleContext forMonth(Date monthStart) {
		return new FakeRuleContext(this, monthStart);
	}

	@Override
	public List<Object[]> query(String sql, Map<String, Object> parameters) {
		queryParameters.add(parameters);
		return queryResults.isEmpty() ? Collections.<Object[]>emptyList() : queryResults.removeFirst();
	}
}
