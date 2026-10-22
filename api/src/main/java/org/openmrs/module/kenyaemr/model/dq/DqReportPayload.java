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

import java.util.Date;

/**
 * The KHIS payload of one report version, rendered from the same report data as its values when the
 * version is generated. Sending the approved version sends exactly this, so data entered after
 * approval cannot change what goes to KHIS.
 */
public class DqReportPayload {

	public static final String FORMAT_ADX = "ADX";

	public static final String FORMAT_JSON = "JSON";

	private Integer id;

	private DqReportRun reportRun;

	private String format;

	/** The dataValueSets path and query, e.g. api/dataValueSets?dataElementIdScheme=uid&...; the server comes from ilServer.address */
	private String endpointPath;

	private String content;

	private String contentHash;

	private Date createdAt;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public DqReportRun getReportRun() {
		return reportRun;
	}

	public void setReportRun(DqReportRun reportRun) {
		this.reportRun = reportRun;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getEndpointPath() {
		return endpointPath;
	}

	public void setEndpointPath(String endpointPath) {
		this.endpointPath = endpointPath;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContentHash() {
		return contentHash;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
}
