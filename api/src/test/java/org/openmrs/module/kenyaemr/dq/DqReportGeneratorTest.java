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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
}
