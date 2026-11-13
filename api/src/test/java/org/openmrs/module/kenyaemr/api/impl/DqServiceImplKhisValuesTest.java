/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.api.impl;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmrs.module.kenyaemr.api.db.DqDao;
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Recording the values read back from KHIS (G-RPT-02)
 */
public class DqServiceImplKhisValuesTest {

	@Test
	public void recordKhisValues_shouldMatchCellsByDataElementAndCategoryOptionCombo() {
		DqReportRun run = new DqReportRun();
		DqValue confirmed = value("deCMC", "coc1");
		DqValue tested = value("deTMC", "coc1");
		DqValue unmapped = value(null, null);
		DqDao dao = Mockito.mock(DqDao.class);
		Mockito.when(dao.getValues(run)).thenReturn(Arrays.asList(confirmed, tested, unmapped));
		DqServiceImpl service = new DqServiceImpl();
		service.setDqDao(dao);
		Map<String, Double> khis = new HashMap<String, Double>();
		khis.put("deCMC|coc1", 94.0);
		Date readAt = new Date();

		int compared = service.recordKhisValues(run, khis, readAt);

		Assert.assertEquals(2, compared);
		Assert.assertEquals(Double.valueOf(94.0), confirmed.getKhisValue());
		Assert.assertEquals(readAt, confirmed.getKhisLastUpdated());
		// KHIS has no value for it
		Assert.assertNull(tested.getKhisValue());
		Assert.assertEquals(readAt, tested.getKhisLastUpdated());
		Assert.assertNull(unmapped.getKhisLastUpdated());
	}

	private static DqValue value(String dataElement, String categoryOptionCombo) {
		DqValue value = new DqValue();
		value.setDataElement(dataElement);
		value.setCategoryOptionCombo(categoryOptionCombo);
		return value;
	}
}
