/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.cwc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.reporting.library.MOH710.Moh710IndicatorLibrary;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MOH 710 Report
 */
@Component
@Builds({"kenyaemr.mchcs.report.moh710"})
public class Moh710ReportBuilder extends AbstractReportBuilder {

	protected static final Log log = LogFactory.getLog(Moh710ReportBuilder.class);

	@Autowired
	private Moh710IndicatorLibrary moh710Indicators;

	// vaccine concept ids as stored in etl_immunization.vaccine
	private static final int BCG = 886;
	private static final int OPV = 783;
	private static final int IPV = 1422;
	private static final int DPT_HEP_B_HIB = 1685;
	private static final int PCV_10 = 82215;
	private static final int ROTA = 83531;
	private static final int YELLOW_FEVER = 5864;
	private static final int MEASLES_RUBELLA = 162586;
	private static final int TCV = 86022;
	private static final int VITAMIN_A_SUPPLEMENTATION = 86339;
	private static final int HPV = 159708;
	private static final int TETANUS_TAXOID = 84879;

	/**
	 * @see org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder#getParameters(org.openmrs.module.kenyacore.report.ReportDescriptor)
	 */
	@Override
	protected List<Parameter> getParameters(ReportDescriptor descriptor) {
		return Arrays.asList(
				new Parameter("startDate", "Start Date", Date.class),
				new Parameter("endDate", "End Date", Date.class),
				new Parameter("dateBasedReporting", "", String.class)
		);
	}

	/**
	 * @see org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder#buildDataSets(org.openmrs.module.kenyacore.report.ReportDescriptor, org.openmrs.module.reporting.report.definition.ReportDefinition)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
		return Arrays.asList(
				ReportUtils.map(immunizationsDataSet(), "startDate=${startDate},endDate=${endDate}")
		);
	}


	/**
	 * Creates the dataset for section #1: Immunizations
	 *
	 * @return the dataset
	 */
	protected DataSetDefinition immunizationsDataSet() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("Immunizations");
		dsd.setDescription("MOH 710 Immunizations");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));

		// === Standard childhood vaccines: <1yr / >=1yr split, 31 daily columns each ===
		addDailyVaccineRows(dsd, "BCG", "BCG", vaccine(null, BCG));
		addDailyVaccineRows(dsd, "OPV-0", "OPV at birth", vaccine(0, OPV));
		addDailyVaccineRows(dsd, "OPV-1", "OPV 1", vaccine(1, OPV));
		addDailyVaccineRows(dsd, "OPV-2", "OPV 2", vaccine(2, OPV));
		addDailyVaccineRows(dsd, "OPV-3", "OPV 3", vaccine(3, OPV));
		addDailyVaccineRows(dsd, "IPV", "IPV", vaccine(null, IPV));
		addDailyVaccineRows(dsd, "DHH-1", "Dpt-Hep-Hib 1", vaccine(1, DPT_HEP_B_HIB));
		addDailyVaccineRows(dsd, "DHH-2", "Dpt-Hep-Hib 2", vaccine(2, DPT_HEP_B_HIB));
		addDailyVaccineRows(dsd, "DHH-3", "Dpt-Hep-Hib 3", vaccine(3, DPT_HEP_B_HIB));
		addDailyVaccineRows(dsd, "PCV-1", "Pneumococcal 1", vaccine(1, PCV_10));
		addDailyVaccineRows(dsd, "PCV-2", "Pneumococcal 2", vaccine(2, PCV_10));
		addDailyVaccineRows(dsd, "PCV-3", "Pneumococcal 3", vaccine(3, PCV_10));
		addDailyVaccineRows(dsd, "ROTA-1", "Rota 1", vaccine(1, ROTA));
		addDailyVaccineRows(dsd, "ROTA-2", "Rota 2", vaccine(2, ROTA));
		addDailyVaccineRows(dsd, "ROTA-3", "Rota 3", vaccine(3, ROTA));
		addDailyVaccineRows(dsd, "YF", "Yellow Fever", vaccine(null, YELLOW_FEVER));
		addDailyVaccineRows(dsd, "MR-1", "Measles-Rubella 1", vaccine(1, MEASLES_RUBELLA));
		addDailyVaccineRows(dsd, "TCV", "Typhoid Conjugate Vaccine", vaccine(null, TCV));
		addDailyVaccineRows(dsd, "FIC", "Fully immunized children", fullyImmunized());

		// === Vitamin A: specific age bands, single row each, no age split ===
		addSingleDailyVaccineRow(dsd, "VA6M", "Vitamin A 6 Months", vaccine(1, VITAMIN_A_SUPPLEMENTATION));
		addSingleDailyVaccineRow(dsd, "VA-1Y", "Vitamin A 1yr", vaccine(2, VITAMIN_A_SUPPLEMENTATION));
		addSingleDailyVaccineRow(dsd, "VA-1.5Y", "Vitamin A 1.5yr", vaccine(3, VITAMIN_A_SUPPLEMENTATION));
		addSingleDailyVaccineRow(dsd, "VA-2Y", "Vitamin A 2yr", vaccine(4, VITAMIN_A_SUPPLEMENTATION));
		addSingleDailyVaccineRow(dsd, "VA-2Y-5Y", "Vitamin A 2-5yr", vaccine(5, VITAMIN_A_SUPPLEMENTATION));
		// addSingleDailyVaccineRow(dsd, "VA-3Y", "Vitamin A 3yr", vaccine(6, VITAMIN_A_SUPPLEMENTATION));

		//TODO	- Activate and etl these immunizations once they are available in the system
/*		addSingleDailyVaccineRow(dsd, "VA-6-11M", "Vitamin A 6-11 Months", "VitaminA_6_11_month");
		addSingleDailyVaccineRow(dsd, "VA-12-59M", "Vitamin 12-59 Months", "VitaminA_12_59_month");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_1", "1st Dose", vaccine(1, TETANUS_TAXOID));
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_2", "2nd Dose", vaccine(2,TETANUS_TAXOID));
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_3", "3rd Dose", vaccine(3, TETANUS_TAXOID));
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_4", "4th Dose", vaccine(4, TETANUS_TAXOID));
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_5", "5th Dose", vaccine(5, TETANUS_TAXOID));
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine for Trauma", "TTV for Trauma", vaccine(6, TETANUS_TAXOID));*/

		// === MR-2: 2 rows with 31 daily columns: 18-24mo and >2yr ===
		addDailyVaccineColumnsForAge(dsd, "MR-2_18_24", "MR-2 18-24 months", vaccine(2, MEASLES_RUBELLA), 1, 2, "18-24mo");
		addDailyVaccineColumnsForAge(dsd, "MR-2_GT2", "MR-2 24 months+", vaccine(2, MEASLES_RUBELLA), 2, null, ">2yr");

		// === HPV: single rows per dose ===
		addSingleDailyVaccineRow(dsd, "HPV-1", "HPV 1", vaccine(1, HPV));
		addSingleDailyVaccineRow(dsd, "HPV-2", "HPV 2", vaccine(2, HPV));

		return dsd;
	}

	/**
	 *
	 * Adds one row of 31 daily columns for a single age band of a vaccine
	 */
	private void addDailyVaccineColumnsForAge(CohortIndicatorDataSetDefinition dsd, String baseName, String label, RowIndicators row, Integer minAge, Integer maxAge, String ageSuffix) {
		for (int day = 1; day <= 31; day++) {
			String indMap = "startDate=${startDate},endDate=${endDate},dayOfMonth=" + day;
			String colLabel = label + (ageSuffix != null && !ageSuffix.isEmpty() ? " " + ageSuffix : "") + " (day " + day + ")";
			dsd.addColumn(baseName + "-" + day, colLabel,
					ReportUtils.map(row.onDay(minAge, maxAge), indMap),
					(String) null);
		}
		// Adding monthly total column
		dsd.addColumn(baseName + "-total", label + " Total",
				ReportUtils.map(row.total(minAge, maxAge), "startDate=${startDate},endDate=${endDate}"),
				(String) null);
	}

	/**
	 * Adds one row of 31 daily columns with no age filter
	 */
	private void addSingleDailyVaccineRow(CohortIndicatorDataSetDefinition dsd, String baseName, String label, RowIndicators row) {
		addDailyVaccineColumnsForAge(dsd, baseName, label, row, null, null, null);
	}

	/**
	 * Adds two rows (under 1 year and 1 year+) of 31 daily columns each for a vaccine
	 */
	private void addDailyVaccineRows(CohortIndicatorDataSetDefinition dsd, String baseName, String label, RowIndicators row) {
		addDailyVaccineColumnsForAge(dsd, baseName, label, row, 0, 1, "<1yr");
		addDailyVaccineColumnsForAge(dsd, baseName + "_01", label, row, 1, null, ">=1yr");
	}

	/**
	 * Day-of-month and period-total indicators for one report row, for a given age band
	 */
	private interface RowIndicators {
		CohortIndicator onDay(Integer minAge, Integer maxAge);
		CohortIndicator total(Integer minAge, Integer maxAge);
	}

	/**
	 * Row counting a vaccine dose
	 * @param sequence the dose number (null for any dose)
	 * @param vaccines the vaccine concept ids
	 */
	private RowIndicators vaccine(final Integer sequence, final Integer... vaccines) {
		return new RowIndicators() {
			public CohortIndicator onDay(Integer minAge, Integer maxAge) {
				return moh710Indicators.vaccinatedOnDay(minAge, maxAge, sequence, vaccines);
			}
			public CohortIndicator total(Integer minAge, Integer maxAge) {
				return moh710Indicators.vaccinatedTotal(minAge, maxAge, sequence, vaccines);
			}
		};
	}

	/**
	 * Row counting children recorded as fully immunized
	 */
	private RowIndicators fullyImmunized() {
		return new RowIndicators() {
			public CohortIndicator onDay(Integer minAge, Integer maxAge) {
				return moh710Indicators.fullyImmunizedOnDay(minAge, maxAge);
			}
			public CohortIndicator total(Integer minAge, Integer maxAge) {
				return moh710Indicators.fullyImmunizedTotal(minAge, maxAge);
			}
		};
	}

}
