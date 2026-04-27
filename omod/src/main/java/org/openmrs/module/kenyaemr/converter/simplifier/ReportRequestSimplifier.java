/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.converter.simplifier;

import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.simplifier.AbstractSimplifier;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts a report request to a simple object
 */
@Component
public class ReportRequestSimplifier extends AbstractSimplifier<ReportRequest> {

	@Autowired
	private UiUtils ui;

	@Autowired
	private KenyaUiUtils kenyaui;

	@Autowired
	private ReportDefinitionSimplifier definitionSimplifier;

	/**
	 * @see AbstractSimplifier#simplify(Object)
	 */
	@Override
	protected SimpleObject simplify(ReportRequest request) {
		
		Mapped<ReportDefinition> mappedDefinition = request.getReportDefinition();
		ReportDefinition definition = (mappedDefinition != null)
				? mappedDefinition.getParameterizable()
				: null;

		Long timeTaken = getTimeTaken(request);

		SimpleObject ret = new SimpleObject();
		ret.put("id", request.getId());

		// Use null-safe definition simplification
		if (definition != null) {
			ret.put("report", definitionSimplifier.convert(definition));
		} else {
			ret.put("report", SimpleObject.create(
					"id", null,
					"uuid", null,
					"name", "[Definition unavailable — please re-run report]"));
		}

		ret.put("requestDate", kenyaui.formatDateParam(request.getRequestDate()));
		ret.put("requestedBy", ui.simplifyObject(request.getRequestedBy()));
		ret.put("status", request.getStatus());
		ret.put("finished", request.getStatus().equals(ReportRequest.Status.COMPLETED)
				|| request.getStatus().equals(ReportRequest.Status.FAILED));
		ret.put("timeTaken", timeTaken != null ? kenyaui.formatDuration(timeTaken) : null);
		ret.put("parameters", getDate(request));
		ret.put("hasData", request.getStatus().equals(ReportRequest.Status.COMPLETED));
		ret.put("hasDataSet", request.getStatus().equals(ReportRequest.Status.COMPLETED));

		return ret;
	}

	/**
	 * Calculates a time taken display value for the given request
	 * 
	 * @param request the request
	 * @return the time taken in milliseconds
	 */
	protected Long getTimeTaken(ReportRequest request) {
		if (request.getEvaluateStartDatetime() == null
				|| request.getStatus().equals(ReportRequest.Status.FAILED)) {
			return null;
		}
		Date start = request.getEvaluateStartDatetime();
		Date end = request.getEvaluateCompleteDatetime() != null
				? request.getEvaluateCompleteDatetime()
				: new Date();
		return end.getTime() - start.getTime();
	}

	/**
	 * Gets parameter mappings for the given request.
	 * Guards against null mappedDefinition to avoid NPE when definition
	 * could not be deserialized from the stored ReportRequest.
	 *
	 * @param reportRequest the report request
	 * @return parameter mappings, or a map with a default startDate if unavailable
	 */
	Map<String, Object> getDate(ReportRequest reportRequest) {
		Map<String, Object> mappings = new HashMap<String, Object>();

		Mapped<ReportDefinition> mappedDefinition = reportRequest.getReportDefinition();

		// Guard: mappedDefinition or its parameterizable may be null if XStream
		// failed to deserialize the embedded definition (ForbiddenClassException)
		if (mappedDefinition == null || mappedDefinition.getParameterizable() == null) {
			mappings.put("startDate", new Date());
			return mappings;
		}

		if (mappedDefinition.getParameterMappings().isEmpty()) {
			mappings.put("startDate", new Date());
		} else {
			mappings.putAll(mappedDefinition.getParameterMappings());
		}

		return mappings;
	}
}