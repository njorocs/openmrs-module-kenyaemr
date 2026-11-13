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
import org.openmrs.module.kenyaemr.model.dq.DqReportRun;
import org.openmrs.module.kenyaemr.model.dq.DqValue;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link DqIndicatorCalculator}
 */
public class DqIndicatorCalculatorTest {

	private static final String MOH_705A = "MOH 705A Outpatient summary";

	private static final String MOH_705B = "MOH 705B Outpatient summary";

	@Test
	public void calculate_shouldAddColumnsAcrossReports() {
		DqConfig.Indicator confirmed = indicator();

		DqIndicatorCalculator.IndicatorValue value = DqIndicatorCalculator.calculate(confirmed, Arrays.asList(
				value(MOH_705A, "MOH705A", "CMC-32", "12"),
				value(MOH_705B, "MOH705B", "COA-32", "30"),
				value(MOH_705B, "MOH705B", "TMC-32", "99")));

		Assert.assertEquals(Double.valueOf(42), value.getValue());
		Assert.assertTrue(value.isComplete());
	}

	@Test
	public void calculate_shouldReportMissingColumnsAndSumTheRest() {
		DqIndicatorCalculator.IndicatorValue value = DqIndicatorCalculator.calculate(indicator(),
				Collections.singletonList(value(MOH_705A, "MOH705A", "CMC-32", "12")));

		Assert.assertEquals(Double.valueOf(12), value.getValue());
		Assert.assertFalse(value.isComplete());
		Assert.assertEquals(Collections.singletonList(MOH_705B + " / MOH705B / COA-32"), value.getMissingColumns());
	}

	@Test
	public void calculate_shouldReturnNullWhenNoColumnIsFound() {
		DqIndicatorCalculator.IndicatorValue value = DqIndicatorCalculator.calculate(indicator(),
				Collections.<DqValue>emptyList());

		Assert.assertNull(value.getValue());
		Assert.assertEquals(2, value.getMissingColumns().size());
	}

	@Test
	public void calculate_shouldCountEmptyCellsAsZero() {
		DqIndicatorCalculator.IndicatorValue value = DqIndicatorCalculator.calculate(indicator(), Arrays.asList(
				value(MOH_705A, "MOH705A", "CMC-32", null),
				value(MOH_705B, "MOH705B", "COA-32", "3")));

		Assert.assertEquals(Double.valueOf(3), value.getValue());
		Assert.assertTrue(value.isComplete());
	}

	@Test
	public void calculate_shouldAddEveryRowOfADataset() {
		DqConfig.Indicator dispensed = stockIndicator(false);

		DqIndicatorCalculator.IndicatorValue value = DqIndicatorCalculator.calculate(dispensed, Arrays.asList(
				value("MOH 643", "AL", "quantity_used", "24"), value("MOH 643", "AL", "quantity_used", "6")));

		Assert.assertEquals(Double.valueOf(30), value.getValue());
	}

	@Test
	public void calculate_shouldCountAnEmptyStockDatasetAsZeroOnlyOnceTheReportExists() {
		DqIndicatorCalculator.IndicatorValue generated = DqIndicatorCalculator.calculate(stockIndicator(true),
				Collections.singletonList(value("MOH 643", "mRDT", "quantity_used", "5")));
		DqIndicatorCalculator.IndicatorValue notGenerated = DqIndicatorCalculator.calculate(stockIndicator(true),
				Collections.<DqValue>emptyList());

		Assert.assertEquals(Double.valueOf(0), generated.getValue());
		Assert.assertTrue(generated.isComplete());
		Assert.assertFalse(notGenerated.isComplete());
		Assert.assertFalse(DqIndicatorCalculator.calculate(stockIndicator(false),
				Collections.singletonList(value("MOH 643", "mRDT", "quantity_used", "5"))).isComplete());
	}

	private static DqConfig.Indicator stockIndicator(boolean zeroWhenEmpty) {
		DqConfig.Part part = new DqConfig.Part();
		part.setReport("MOH 643");
		part.setDataset("AL");
		part.setColumns(Collections.singletonList("quantity_used"));
		part.setZeroWhenEmpty(zeroWhenEmpty);
		DqConfig.Indicator indicator = new DqConfig.Indicator();
		indicator.setCode("MAL_AL_USED_643");
		indicator.setParts(Collections.singletonList(part));
		return indicator;
	}

	@Test
	public void parseNumber_shouldHandleBlankAndText() {
		Assert.assertEquals(Double.valueOf(5), DqIndicatorCalculator.parseNumber(" 5 "));
		Assert.assertNull(DqIndicatorCalculator.parseNumber(""));
		Assert.assertNull(DqIndicatorCalculator.parseNumber("Tablets"));
	}

	private static DqConfig.Indicator indicator() {
		DqConfig.Part a = new DqConfig.Part();
		a.setReport(MOH_705A);
		a.setDataset("MOH705A");
		a.setColumns(Collections.singletonList("CMC-32"));
		DqConfig.Part b = new DqConfig.Part();
		b.setReport(MOH_705B);
		b.setDataset("MOH705B");
		b.setColumns(Collections.singletonList("COA-32"));
		DqConfig.Indicator indicator = new DqConfig.Indicator();
		indicator.setCode("MAL_CONFIRMED_705");
		indicator.setParts(Arrays.asList(a, b));
		return indicator;
	}

	static DqValue value(String report, String dataset, String column, String text) {
		DqReportRun run = new DqReportRun();
		run.setReportName(report);
		DqValue value = new DqValue();
		value.setReportRun(run);
		value.setDatasetName(dataset);
		value.setColumnName(column);
		value.setValueText(text);
		value.setValueNumeric(DqIndicatorCalculator.parseNumber(text));
		return value;
	}
}
