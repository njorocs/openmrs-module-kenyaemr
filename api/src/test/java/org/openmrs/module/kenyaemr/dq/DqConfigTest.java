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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DqConfigTest {

	private static DqConfig.Part part(String report, String dataset, String column) {
		DqConfig.Part part = new DqConfig.Part();
		part.setReport(report);
		part.setDataset(dataset);
		part.setColumns(Collections.singletonList(column));
		return part;
	}

	@Test
	public void withIndicatorFromReport_shouldCountOnlyTheIndicatorsPartsFromTheReport() {
		DqConfig.Indicator confirmed = new DqConfig.Indicator();
		confirmed.setCode("MAL_CONFIRMED_705");
		confirmed.setName("Confirmed Malaria Cases");
		confirmed.setParts(Arrays.asList(part("MOH 705A Outpatient summary", "MOH705A", "CMC-32"),
				part("MOH 705B Outpatient summary", "MOH705B", "COA-32")));
		DqConfig.Indicator other = new DqConfig.Indicator();
		other.setCode("MAL_TESTS_706");
		other.setParts(Collections.singletonList(part("MOH 706", "MOH706", "T")));
		DqConfig config = new DqConfig();
		config.setIndicators(Arrays.asList(confirmed, other));

		DqConfig narrowed = config.withIndicatorFromReport("MAL_CONFIRMED_705", "MOH 705B Outpatient summary");

		DqConfig.Indicator part = narrowed.getIndicator("MAL_CONFIRMED_705");
		Assert.assertEquals(1, part.getParts().size());
		Assert.assertEquals("COA-32", part.getParts().get(0).getColumns().get(0));
		Assert.assertEquals(Collections.singleton("MOH 705B Outpatient summary"), part.getReports());
		Assert.assertSame(other, narrowed.getIndicator("MAL_TESTS_706"));
		// The configuration itself is left as it was
		Assert.assertEquals(2, config.getIndicator("MAL_CONFIRMED_705").getParts().size());
		// A report the indicator is not counted from changes nothing
		Assert.assertSame(config, config.withIndicatorFromReport("MAL_CONFIRMED_705", "MOH 706"));
	}
}
