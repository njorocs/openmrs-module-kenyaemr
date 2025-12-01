/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "ekyc_otp_use_request")
public class OtpUseRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "verification_id")
    private Integer verificationId;

    @Column(name = "patient_uuid")
    private String patientUuid;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "otp_context")
    private String otpContext;

    @Column(name = "date_requested")
    private Date dateRequested = new Date();

    @Column(name = "approved")
    private Boolean approved = false;

    @Column(name = "approval_id")
    private String approvalId;

    @Column(name = "response_status")
    private String responseStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "expires_at")
    private Date expiresAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OtpUseRequest that = (OtpUseRequest) o;
        return Objects.equals(id, that.id) && Objects.equals(verificationId, that.verificationId) && Objects.equals(patientUuid, that.patientUuid) && Objects.equals(reason, that.reason) && Objects.equals(requestedBy, that.requestedBy) && Objects.equals(otpContext, that.otpContext) && Objects.equals(dateRequested, that.dateRequested) && Objects.equals(approved, that.approved) && Objects.equals(approvalId, that.approvalId) && Objects.equals(responseStatus, that.responseStatus) && Objects.equals(rejectionReason, that.rejectionReason) && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, verificationId, patientUuid, reason, requestedBy, otpContext, dateRequested, approved, approvalId, responseStatus, rejectionReason, expiresAt);
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getDateRequested() {
        return dateRequested;
    }
    public String getOtpContext() {
        return otpContext;
    }

    public void setOtpContext(String otpContext) {
        this.otpContext = otpContext;
    }

    public Integer getId() {
        return id;
    }

    public Integer getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Integer verificationId) {
        this.verificationId = verificationId;
    }

    public String getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setDateRequested(Date dateRequested) {
        this.dateRequested = dateRequested;
    }
}
