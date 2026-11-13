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
import org.openmrs.module.kenyaemr.model.dq.DqOutcome;
import org.openmrs.module.kenyaemr.model.dq.DqResult;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DqKhisParityViewTest {

	private static DqValue value(String column, String dataElement, String combo, Double emr, Double khis) {
		DqValue value = new DqValue();
		value.setDatasetName("MOH705A");
		value.setColumnName(column);
		value.setDataElement(dataElement);
		value.setCategoryOptionCombo(combo);
		value.setValueNumeric(emr);
		value.setKhisValue(khis);
		return value;
	}

	@Test
	public void rows_shouldCompareEachDataElementOverItsCombosAsTheRuleDoes() {
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("MOH705A\u0000CMC-32", "Confirmed Malaria (Total)");
		DqResult result = new DqResult();
		result.setIndicatorCode("KxT47tbKHsd");
		result.setOutcome(DqOutcome.TICKET);
		List<DqKhisParityView.Row> rows = DqKhisParityView.rows(Arrays.asList(
				value("CMC-32", "KxT47tbKHsd", "a", 100.0, 80.0),
				value("CMC-33", "KxT47tbKHsd", "b", 20.0, null),
				value("DWND-32", "JcgMuSM3NnI", "a", 3.0, 3.0),
				value("NOTE", null, null, 5.0, null)), labels, true, 0.05, Collections.singletonList(result),
				"{\"KxT47tbKHsd\":\"MOH 705A Rev 2020_Confirmed Malaria\",\"a\":\"default\"}");

		Assert.assertEquals(2, rows.size());
		DqKhisParityView.Row confirmed = rows.get(0);
		Assert.assertEquals("Confirmed Malaria (Total), CMC-33", confirmed.getLabel());
		Assert.assertEquals(120, confirmed.getEmr(), 1e-9);
		// A cell KHIS has no value for counts as 0
		Assert.assertEquals(80, confirmed.getKhis(), 1e-9);
		Assert.assertEquals(-1.0 / 3, confirmed.getVariance(), 1e-9);
		Assert.assertFalse(confirmed.getWithinTolerance());
		Assert.assertSame(result, confirmed.getResult());
		Assert.assertEquals("MOH 705A Rev 2020_Confirmed Malaria", confirmed.getKhisName());
		Assert.assertEquals("default", confirmed.getCells().get(0).getCategoryOptionComboName());
		Assert.assertNull(confirmed.getCells().get(1).getCategoryOptionComboName());
		Assert.assertNull(rows.get(1).getKhisName());
		Assert.assertNull(confirmed.getCells().get(1).getKhis());
		Assert.assertTrue(rows.get(1).getWithinTolerance());
	}

	@Test
	public void rows_shouldHaveNoKhisSideBeforeTheReadBack() {
		DqKhisParityView.Row row = DqKhisParityView.rows(Collections.singletonList(value("CMC-32", "KxT47tbKHsd", "a", 7.0,
				null)), new HashMap<String, String>(), false, 0.05, Collections.<DqResult>emptyList(), null).get(0);
		Assert.assertNull(row.getKhis());
		Assert.assertNull(row.getVariance());
		Assert.assertNull(row.getWithinTolerance());
	}

	@Test
	public void khisOnly_shouldGiveTheValuesKhisHoldsThatWereNotSent() {
		String reply = "{\"dataValues\":[{\"dataElement\":\"KxT47tbKHsd\",\"categoryOptionCombo\":\"a\",\"value\":\"229\"},"
				+ "{\"dataElement\":\"Lt0FqtnHraW\",\"categoryOptionCombo\":\"a\",\"value\":\"1\",\"storedBy\":\"hrio\"}]}";
		List<DqKhisParityView.KhisOnly> extra = DqKhisParityView.khisOnly(reply,
				new HashSet<String>(Collections.singletonList("KxT47tbKHsd|a")), "{\"Lt0FqtnHraW\":\"Deworming\",\"a\":\"default\"}");
		Assert.assertEquals(1, extra.size());
		Assert.assertEquals("Deworming", extra.get(0).getDataElementName());
		Assert.assertEquals("default", extra.get(0).getCategoryOptionComboName());
		Assert.assertEquals("Lt0FqtnHraW", extra.get(0).getDataElement());
		Assert.assertEquals("1", extra.get(0).getValue());
		Assert.assertEquals("hrio", extra.get(0).getStoredBy());
		// Without names, the uids are shown
		Assert.assertNull(DqKhisParityView.khisOnly(reply, new HashSet<String>(), "not json").get(0).getDataElementName());
		Assert.assertTrue(DqKhisParityView.khisOnly("<html>", new HashSet<String>(), null).isEmpty());
	}
}
