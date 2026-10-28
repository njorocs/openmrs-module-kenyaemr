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

import org.openmrs.module.kenyaemr.api.DqService;
import org.openmrs.module.kenyaemr.dq.rule.DqRuleContext;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Rule context that reads stored report values through {@link DqService}
 */
public class ServiceDqRuleContext implements DqRuleContext {

	private final DqService service;

	private final DqConfig config;

	private final DqReportRun run;

	private final Date periodStart;

	private final Date periodEnd;

	private final Map<String, DqIndicatorCalculator.IndicatorValue> indicators;

	/** Values of report columns counted for a period other than a month, by report, columns and period */
	private final Map<String, List<DqValue>> liveValues = new HashMap<String, List<DqValue>>();

	private Boolean khisSubmissionTracked;

	private Boolean completionReadBackTracked;

	/**
	 * @param run the run being checked, or null for a weekly check
	 */
	public ServiceDqRuleContext(DqService service, DqConfig config, DqReportRun run, Date periodStart, Date periodEnd) {
		this(service, config, run, periodStart, periodEnd, new HashMap<String, DqIndicatorCalculator.IndicatorValue>());
	}

	private ServiceDqRuleContext(DqService service, DqConfig config, DqReportRun run, Date periodStart, Date periodEnd,
	                             Map<String, DqIndicatorCalculator.IndicatorValue> indicators) {
		this.service = service;
		this.config = config;
		this.run = run;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.indicators = indicators;
	}

	/**
	 * Reads an indicator's values for a range of months in one query, so that a trend does not
	 * query month by month
	 */
	public void preload(String code, Date fromMonth, Date toMonth) {
		DqConfig.Indicator indicator = config.getIndicator(code);
		if (indicator == null) {
			return;
		}
		Map<Long, List<DqValue>> byMonth = new HashMap<Long, List<DqValue>>();
		for (DqValue value : service.getLatestValues(indicator.getReports(), fromMonth, toMonth)) {
			Long month = DqPeriods.monthStart(value.getReportRun().getPeriodStart()).getTime();
			if (!byMonth.containsKey(month)) {
				byMonth.put(month, new ArrayList<DqValue>());
			}
			byMonth.get(month).add(value);
		}
		for (Date month = DqPeriods.monthStart(fromMonth); !month.after(toMonth); month = DqPeriods.addMonths(month, 1)) {
			String key = code + "@" + month.getTime();
			if (!indicators.containsKey(key)) {
				List<DqValue> values = byMonth.get(month.getTime());
				indicators.put(key, DqIndicatorCalculator.calculate(indicator,
						values != null ? values : Collections.<DqValue>emptyList()));
			}
		}
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
		String key = code + "@" + monthStart.getTime();
		DqIndicatorCalculator.IndicatorValue value = indicators.get(key);
		if (value == null) {
			DqConfig.Indicator indicator = config.getIndicator(code);
			if (indicator == null) {
				return new DqIndicatorCalculator.IndicatorValue(code, null, Collections.singletonList("indicator " + code
						+ " is not configured"));
			}
			List<DqValue> values = service.getLatestValues(indicator.getReports(), monthStart, monthStart);
			value = DqIndicatorCalculator.calculate(indicator, values);
			indicators.put(key, value);
		}
		return value;
	}

	@Override
	public DqIndicatorCalculator.IndicatorValue getIndicator(String code, Date start, Date end) {
		if (start.equals(DqPeriods.monthStart(start)) && DqPeriods.lastDayOfMonth(start).equals(end)) {
			return getIndicator(code, start);
		}
		String key = code + "@" + start.getTime() + "-" + end.getTime();
		DqIndicatorCalculator.IndicatorValue value = indicators.get(key);
		if (value == null) {
			DqConfig.Indicator indicator = config.getIndicator(code);
			if (indicator == null) {
				return new DqIndicatorCalculator.IndicatorValue(code, null, Collections.singletonList("indicator " + code
						+ " is not configured"));
			}
			// Only the indicator's own columns are counted, not the whole report
			Map<String, Map<String, Set<String>>> columnsByReport = new LinkedHashMap<String, Map<String, Set<String>>>();
			for (DqConfig.Part part : indicator.getParts()) {
				Map<String, Set<String>> columns = columnsByReport.get(part.getReport());
				if (columns == null) {
					columns = new TreeMap<String, Set<String>>();
					columnsByReport.put(part.getReport(), columns);
				}
				if (!columns.containsKey(part.getDataset())) {
					columns.put(part.getDataset(), new TreeSet<String>());
				}
				columns.get(part.getDataset()).addAll(part.getColumns());
			}
			List<DqValue> values = new ArrayList<DqValue>();
			for (Map.Entry<String, Map<String, Set<String>>> entry : columnsByReport.entrySet()) {
				String reportKey = entry.getKey() + entry.getValue() + "@" + start.getTime() + "-" + end.getTime();
				if (!liveValues.containsKey(reportKey)) {
					liveValues.put(reportKey, new DqReportGenerator().evaluateValues(entry.getKey(), entry.getValue(), start, end));
				}
				values.addAll(liveValues.get(reportKey));
			}
			value = DqIndicatorCalculator.calculate(indicator, values);
			indicators.put(key, value);
		}
		return value;
	}

	@Override
	public List<DqValue> getValues(DqReportRun run) {
		return service.getValues(run);
	}

	@Override
	public boolean isKhisSubmissionTracked() {
		if (khisSubmissionTracked == null) {
			List<Object[]> rows = service.executeSqlQuery("select count(*), 0 from information_schema.tables "
					+ "where table_schema = database() and table_name = 'il_report_submission'", new HashMap<String, Object>());
			khisSubmissionTracked = !rows.isEmpty() && rows.get(0)[0] instanceof Number && ((Number) rows.get(0)[0]).intValue() > 0;
		}
		return khisSubmissionTracked;
	}

	@Override
	public KhisSubmissionState getKhisSubmission(String reportName, Date monthStart) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("reportName", reportName);
		parameters.put("periodStart", monthStart);
		// Ledgers from before the completion read-back have no columns for it
		String completion = isCompletionReadBackTracked() ? "s.completion_read_at, s.khis_completed_at" : "null, null";
		List<Object[]> rows = service.executeSqlQuery("select s.status, s.sent_at, s.completed_at, s.last_error, "
				+ completion + " from il_report_submission s where s.report_name = :reportName "
				+ "and s.period_start = :periodStart and s.target_system = 'KHIS' order by s.version desc, s.submission_id desc",
				parameters);
		return KhisSubmissionState.fromLedger(rows);
	}

	private boolean isCompletionReadBackTracked() {
		if (completionReadBackTracked == null) {
			List<Object[]> rows = service.executeSqlQuery("select count(*), 0 from information_schema.columns "
					+ "where table_schema = database() and table_name = 'il_report_submission' "
					+ "and column_name = 'completion_read_at'", new HashMap<String, Object>());
			completionReadBackTracked = !rows.isEmpty() && rows.get(0)[0] instanceof Number
					&& ((Number) rows.get(0)[0]).intValue() > 0;
		}
		return completionReadBackTracked;
	}

	@Override
	public boolean isGenerated(String reportName, Date monthStart) {
		return service.getLatestReportRun(reportName, monthStart) != null;
	}

	@Override
	public String getEtlDatabase() {
		return CommonMetadata.EmrDatabaseEtl.ETL_DATABASE;
	}

	@Override
	public DqRuleContext forMonth(Date monthStart) {
		Date start = DqPeriods.monthStart(monthStart);
		return new ServiceDqRuleContext(service, config, null, start, DqPeriods.lastDayOfMonth(start), indicators);
	}

	@Override
	public List<Object[]> query(String sql, Map<String, Object> parameters) {
		return service.executeSqlQuery(sql, parameters);
	}
}
