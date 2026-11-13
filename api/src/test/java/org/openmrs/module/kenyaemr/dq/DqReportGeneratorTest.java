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

import org.junit.Test;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.reporting.air.AdxMetadata;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class DqReportGeneratorTest {

	@Test
	public void keepsTheEndpointFromApiOnSoTheServerIsReadWhenSending() {
		AdxMetadata metadata = new AdxMetadata();
		metadata.setEndpointUrl("https://openhim.example/khis/api/dataValueSets?dataElementIdScheme=uid&orgUnitIdScheme=code");

		assertEquals("api/dataValueSets?dataElementIdScheme=uid&orgUnitIdScheme=code", DqReportGenerator.endpointPath(metadata));
		assertNull(DqReportGenerator.endpointPath(null));
	}

	@Test
	public void aReportWithoutKhisMappingHasNoPayload() {
		assertNull(DqReportGenerator.renderPayload(new DqReportRun(), null, null));
	}

	@Test
	public void hashesTheSameWayAsBefore() {
		assertEquals(DqReportGenerator.sha256(""), DqReportGenerator.hash(java.util.Collections.<org.openmrs.module.kenyaemr.model.dq.DqValue>emptyList()));
		assertEquals(64, DqReportGenerator.sha256("x").length());
	}

	@Test
	public void subsetKeepsOnlyTheWantedColumnsAndDatasets() {
		CohortIndicatorDataSetDefinition moh705 = new CohortIndicatorDataSetDefinition();
		moh705.setName("MOH705A");
		moh705.addParameter(new Parameter("startDate", "Start", Date.class));
		Mapped<CohortIndicator> tested = new Mapped<CohortIndicator>(new CohortIndicator("tested"), null);
		Mapped<CohortIndicator> confirmed = new Mapped<CohortIndicator>(new CohortIndicator("confirmed"), null);
		for (Mapped<CohortIndicator> indicator : Arrays.asList(tested, confirmed)) {
			String row = indicator == tested ? "TMC" : "CMC";
			moh705.addColumn(row + "-1", "Day 1", indicator, "day=1");
			moh705.addColumn(row + "-32", "Total", indicator, "");
		}
		SqlDataSetDefinition other = new SqlDataSetDefinition();
		other.setName("OTHER");
		ReportDefinition definition = new ReportDefinition();
		definition.setName("MOH 705A Outpatient summary");
		definition.addDataSetDefinition("MOH705A", new Mapped<DataSetDefinition>(moh705, Collections.<String, Object>singletonMap("startDate", "${startDate}")));
		definition.addDataSetDefinition("OTHER", new Mapped<DataSetDefinition>(other, null));

		Map<String, Collection<String>> columns = new HashMap<String, Collection<String>>();
		columns.put("MOH705A", Collections.singleton("TMC-32"));
		ReportDefinition subset = DqReportGenerator.subset(definition, columns);

		assertEquals("MOH 705A Outpatient summary", subset.getName());
		assertEquals(1, subset.getDataSetDefinitions().size());
		Mapped<? extends DataSetDefinition> mapped = subset.getDataSetDefinitions().get("MOH705A");
		assertEquals("${startDate}", mapped.getParameterMappings().get("startDate"));
		CohortIndicatorDataSetDefinition copy = (CohortIndicatorDataSetDefinition) mapped.getParameterizable();
		assertEquals("MOH705A", copy.getName());
		assertEquals(1, copy.getColumns().size());
		assertEquals("TMC-32", copy.getColumns().get(0).getName());
		assertSame(tested.getParameterizable(), copy.getColumns().get(0).getIndicator().getParameterizable());
		assertEquals(1, copy.getParameters().size());
		// The report's own definition is left as it was
		assertEquals(4, moh705.getColumns().size());
		assertFalse(subset.getDataSetDefinitions().containsKey("OTHER"));
	}
}
