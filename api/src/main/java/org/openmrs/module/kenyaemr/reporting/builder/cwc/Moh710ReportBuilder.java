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
		addDailyVaccineRows(dsd, "BCG", "BCG", "BCG");
		addDailyVaccineRows(dsd, "OPV-0", "OPV at birth", "OPV_birth");
		addDailyVaccineRows(dsd, "OPV-1", "OPV 1", "OPV_1");
		addDailyVaccineRows(dsd, "OPV-2", "OPV 2", "OPV_2");
		addDailyVaccineRows(dsd, "OPV-3", "OPV 3", "OPV_3");
		addDailyVaccineRows(dsd, "IPV", "IPV", "IPV");
		addDailyVaccineRows(dsd, "DHH-1", "Dpt-Hep-Hib 1", "DPT_Hep_B_Hib_1");
		addDailyVaccineRows(dsd, "DHH-2", "Dpt-Hep-Hib 2", "DPT_Hep_B_Hib_2");
		addDailyVaccineRows(dsd, "DHH-3", "Dpt-Hep-Hib 3", "DPT_Hep_B_Hib_3");
		addDailyVaccineRows(dsd, "PCV-1", "Pneumococcal 1", "PCV_10_1");
		addDailyVaccineRows(dsd, "PCV-2", "Pneumococcal 2", "PCV_10_2");
		addDailyVaccineRows(dsd, "PCV-3", "Pneumococcal 3", "PCV_10_3");
		addDailyVaccineRows(dsd, "ROTA-1", "Rota 1", "ROTA_1");
		addDailyVaccineRows(dsd, "ROTA-2", "Rota 2", "ROTA_2");
		addDailyVaccineRows(dsd, "ROTA-3", "Rota 3", "ROTA_3");
		addDailyVaccineRows(dsd, "YF", "Yellow Fever", "Yellow_fever");
		addDailyVaccineRows(dsd, "MR-1", "Measles-Rubella 1", "Measles_rubella_1");
		addDailyVaccineRows(dsd, "TCV", "Typhoid Conjugate Vaccine", "typhoid_conjugate_vaccine");
		addDailyVaccineRows(dsd, "FIC", "Fully immunized children", "fully_immunized");

		// === Vitamin A: specific age bands, single row each, no age split ===
		addSingleDailyVaccineRow(dsd, "VA6M", "Vitamin A 6 Months", "VitaminA_6_months");
		addSingleDailyVaccineRow(dsd, "VA-1Y", "Vitamin A 1yr", "VitaminA_1_yr");
		addSingleDailyVaccineRow(dsd, "VA-1.5Y", "Vitamin A 1.5yr", "VitaminA_1_and_half_yr");
		addSingleDailyVaccineRow(dsd, "VA-2Y", "Vitamin A 2yr", "VitaminA_2_yr");
		addSingleDailyVaccineRow(dsd, "VA-2Y-5Y", "Vitamin A 2-5yr", "VitaminA_2_to_5_yr");
		addSingleDailyVaccineRow(dsd, "VA-3Y", "Vitamin A 3yr", "VitaminA_3_yr");

		//TODO	- Activate and etl these immunizations once they are available in the system
/*		addSingleDailyVaccineRow(dsd, "VA-6-11M", "Vitamin A 6-11 Months", "VitaminA_6_11_month");
		addSingleDailyVaccineRow(dsd, "VA-12-59M", "Vitamin 12-59 Months", "VitaminA_12_59_month");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_1", "1st Dose", "tetanus_taxoid_vaccine_pregnant_women_1");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_2", "2nd Dose", "tetanus_taxoid_vaccine_pregnant_women_2");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_3", "3rd Dose", "tetanus_taxoid_vaccine_pregnant_women_3");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_4", "4th Dose", "tetanus_taxoid_vaccine_pregnant_women_4");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine-Pregnant Women_5", "5th Dose", "tetanus_taxoid_vaccine_pregnant_women_5");
		addSingleDailyVaccineRow(dsd, "Tetanus Taxoid Vaccine for Trauma", "TTV for Trauma", "tetanus_taxoid_vaccine_for_trauma");*/

		// === MR-2: 2 rows with 31 daily columns: 18-24mo and >2yr ===
		addDailyVaccineColumnsForAge(dsd, "MR-2_18_24", "MR-2", "Measles_rubella_2", 1, 2, "18-24mo");
		addDailyVaccineColumnsForAge(dsd, "MR-2_GT2", "MR-2", "Measles_rubella_2", 2, null, ">2yr");

		// === HPV: single rows per dose ===
		addSingleDailyVaccineRow(dsd, "HPV-1", "HPV 1", "HPV_1");
		addSingleDailyVaccineRow(dsd, "HPV-2", "HPV 2", "HPV_2");

		return dsd;
	}

	/**
	 *
	 * Adds one row of 31 daily columns for a single age band of a vaccine
	 */
	private void addDailyVaccineColumnsForAge(CohortIndicatorDataSetDefinition dsd, String baseName, String label, String etlCol, Integer minAge, Integer maxAge, String ageSuffix) {
		for (int day = 1; day <= 31; day++) {
			String indMap = "startDate=${startDate},endDate=${endDate},dayOfMonth=" + day;
			String colLabel = label + (ageSuffix != null && !ageSuffix.isEmpty() ? " " + ageSuffix : "") + " (day " + day + ")";
			dsd.addColumn(baseName + "-" + day, colLabel,
					ReportUtils.map(moh710Indicators.vaccinatedOnDay(etlCol, minAge, maxAge), indMap),
					(String) null);
		}
	}

	/**
	 * Adds one row of 31 daily columns with no age filter
	 */
	private void addSingleDailyVaccineRow(CohortIndicatorDataSetDefinition dsd, String baseName, String label, String etlCol) {
		addDailyVaccineColumnsForAge(dsd, baseName, label, etlCol, null, null, null);
	}

	/**
	 * Adds two rows (under 1 year and 1 year+) of 31 daily columns each for a vaccine
	 */
	private void addDailyVaccineRows(CohortIndicatorDataSetDefinition dsd, String baseName, String label, String etlCol) {
		addDailyVaccineColumnsForAge(dsd, baseName, label, etlCol, 0, 1, "<1yr");
		addDailyVaccineColumnsForAge(dsd, baseName + "_O1", label, etlCol, 1, null, ">=1yr");
	}

}
