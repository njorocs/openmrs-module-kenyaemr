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
import org.openmrs.User;

import java.util.Date;

/**
 * A reconciliation or outlier issue assigned to someone to resolve
 */
public class DqTicket extends BaseOpenmrsObject {

	public static final String ROLE_HRIO = "HRIO";

	public static final String ROLE_SCHRIO = "SCHRIO";

	public static final String ROLE_IN_CHARGE = "IN_CHARGE";

	private Integer id;

	private DqResult result;

	private String ruleId;

	private String indicatorCode;

	private Date periodStart;

	private Double emrValue;

	private Double khisValue;

	private Double variancePct;

	private String suggestedCause;

	private String assigneeRole;

	private DqTicketStatus status;

	private String resolutionNote;

	private Date openedAt;

	private Date resolvedAt;

	private User resolvedBy;

	/** The program area whose rule opened the ticket */
	private String programCode;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public DqResult getResult() {
		return result;
	}

	public void setResult(DqResult result) {
		this.result = result;
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

	public Date getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(Date periodStart) {
		this.periodStart = periodStart;
	}

	public Double getEmrValue() {
		return emrValue;
	}

	public void setEmrValue(Double emrValue) {
		this.emrValue = emrValue;
	}

	public Double getKhisValue() {
		return khisValue;
	}

	public void setKhisValue(Double khisValue) {
		this.khisValue = khisValue;
	}

	public Double getVariancePct() {
		return variancePct;
	}

	public void setVariancePct(Double variancePct) {
		this.variancePct = variancePct;
	}

	public String getSuggestedCause() {
		return suggestedCause;
	}

	public void setSuggestedCause(String suggestedCause) {
		this.suggestedCause = suggestedCause;
	}

	public String getAssigneeRole() {
		return assigneeRole;
	}

	public void setAssigneeRole(String assigneeRole) {
		this.assigneeRole = assigneeRole;
	}

	public DqTicketStatus getStatus() {
		return status;
	}

	public void setStatus(DqTicketStatus status) {
		this.status = status;
	}

	public String getResolutionNote() {
		return resolutionNote;
	}

	public void setResolutionNote(String resolutionNote) {
		this.resolutionNote = resolutionNote;
	}

	public Date getOpenedAt() {
		return openedAt;
	}

	public void setOpenedAt(Date openedAt) {
		this.openedAt = openedAt;
	}

	public Date getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(Date resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public User getResolvedBy() {
		return resolvedBy;
	}

	public void setResolvedBy(User resolvedBy) {
		this.resolvedBy = resolvedBy;
	}

	public String getProgramCode() {
		return programCode;
	}

	public void setProgramCode(String programCode) {
		this.programCode = programCode;
	}
}
