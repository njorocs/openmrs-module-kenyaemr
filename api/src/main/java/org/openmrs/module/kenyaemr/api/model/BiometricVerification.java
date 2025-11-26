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
@Table(name = "ekyc_biometric_verification")
public class BiometricVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name="request_id", unique=true)
    private String requestId;
    @Column(name="relying_party_request_id")
    private String relyingPartyRequestId;
    @Column(name="patient_uuid")
    private String patientUuid;
    @Column(name="status")
    private String status;
    @Column(name="result")
    private String result;
    @Column(name="attempts_used")
    private Integer attemptsUsed = 0;
    @Column(name="total_attempts")
    private Integer totalAttempts;
    @Column(name="completed")
    private Boolean completed = false;
    @Column(name="final_result")
    private String finalResult;
    @Column(name="last_callback_raw", columnDefinition = "LONGTEXT")
    private String lastCallbackRaw;
    @Column(name="date_created")
    private Date dateCreated = new Date();
    @Column(name="date_updated")
    private Date dateUpdated;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BiometricVerification that = (BiometricVerification) o;
        return Objects.equals(id, that.id) && Objects.equals(requestId, that.requestId) && Objects.equals(relyingPartyRequestId, that.relyingPartyRequestId) && Objects.equals(patientUuid, that.patientUuid) && Objects.equals(status, that.status) && Objects.equals(result, that.result) && Objects.equals(attemptsUsed, that.attemptsUsed) && Objects.equals(totalAttempts, that.totalAttempts) && Objects.equals(completed, that.completed) && Objects.equals(finalResult, that.finalResult) && Objects.equals(lastCallbackRaw, that.lastCallbackRaw) && Objects.equals(dateCreated, that.dateCreated) && Objects.equals(dateUpdated, that.dateUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestId, relyingPartyRequestId, patientUuid, status, result, attemptsUsed, totalAttempts, completed, finalResult, lastCallbackRaw, dateCreated, dateUpdated);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRelyingPartyRequestId() {
        return relyingPartyRequestId;
    }

    public void setRelyingPartyRequestId(String relyingPartyRequestId) {
        this.relyingPartyRequestId = relyingPartyRequestId;
    }

    public String getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getAttemptsUsed() {
        return attemptsUsed;
    }

    public void setAttemptsUsed(Integer attemptsUsed) {
        this.attemptsUsed = attemptsUsed;
    }

    public Integer getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(Integer totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public String getLastCallbackRaw() {
        return lastCallbackRaw;
    }

    public void setLastCallbackRaw(String lastCallbackRaw) {
        this.lastCallbackRaw = lastCallbackRaw;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
}
