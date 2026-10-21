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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule context that reads stored report values through {@link DqService}
 */
public class ServiceDqRuleContext implements DqRuleContext {

	private final DqService service;

	private final DqConfig config;

	private final DqReportRun run;

	private final Date periodStart;

	private final Date periodEnd;

	private final Map<String, DqIndicatorCalculator.IndicatorValue> indicators = new HashMap<String, DqIndicatorCalculator.IndicatorValue>();

	/**
	 * @param run the run being checked, or null for a weekly check
	 */
	public ServiceDqRuleContext(DqService service, DqConfig config, DqReportRun run, Date periodStart, Date periodEnd) {
		this.service = service;
		this.config = config;
		this.run = run;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
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
	public boolean isGenerated(String reportName, Date monthStart) {
		return service.getLatestReportRun(reportName, monthStart) != null;
	}

	@Override
	public String getEtlDatabase() {
		return CommonMetadata.EmrDatabaseEtl.ETL_DATABASE;
	}

	@Override
	public List<Object[]> query(String sql, Map<String, Object> parameters) {
		return service.executeSqlQuery(sql, parameters);
	}
}
