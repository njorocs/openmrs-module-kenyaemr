/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.model.dq;

import org.openmrs.BaseOpenmrsObject;

import java.util.Date;

/**
 * The result of one rule for one period: for one indicator, one indicator pair, or the whole
 * report. Results tied to an approval carry the report run; weekly rules such as T22-08 do not.
 */
public class DqResult extends BaseOpenmrsObject {

	private Integer id;

	/** Rule id from TR-26-110, e.g. "T22-04" */
	private String ruleId;

	/** Null for rules that apply to the whole report */
	private String indicatorCode;

	private DqReportRun reportRun;

	private Date periodStart;

	private Date periodEnd;

	private Double measureValue;

	private Double numerator;

	private Double denominator;

	/** Human-readable threshold actually applied, e.g. "<= 1.0" */
	private String threshold;

	private DqOutcome outcome;

	/** Plain-language explanation shown on the approval screen */
	private String message;

	/** JSON with rule-specific detail, such as the query parameters for a patient list */
	private String detail;

	private Date evaluatedAt;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public String getRuleId() {
		return ruleId;
	}

	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	public String getIndicatorCode() {
		return indicatorCode;
	}

	public void setIndicatorCode(String indicatorCode) {
		this.indicatorCode = indicatorCode;
	}

	public DqReportRun getReportRun() {
		return reportRun;
	}

	public void setReportRun(DqReportRun reportRun) {
		this.reportRun = reportRun;
	}

	public Date getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(Date periodStart) {
		this.periodStart = periodStart;
	}

	public Date getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(Date periodEnd) {
		this.periodEnd = periodEnd;
	}

	public Double getMeasureValue() {
		return measureValue;
	}

	public void setMeasureValue(Double measureValue) {
		this.measureValue = measureValue;
	}

	public Double getNumerator() {
		return numerator;
	}

	public void setNumerator(Double numerator) {
		this.numerator = numerator;
	}

	public Double getDenominator() {
		return denominator;
	}

	public void setDenominator(Double denominator) {
		this.denominator = denominator;
	}

	public String getThreshold() {
		return threshold;
	}

	public void setThreshold(String threshold) {
		this.threshold = threshold;
	}

	public DqOutcome getOutcome() {
		return outcome;
	}

	public void setOutcome(DqOutcome outcome) {
		this.outcome = outcome;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public Date getEvaluatedAt() {
		return evaluatedAt;
	}

	public void setEvaluatedAt(Date evaluatedAt) {
		this.evaluatedAt = evaluatedAt;
	}
}
