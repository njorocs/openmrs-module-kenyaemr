/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.malaria;

import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.reporting.library.malaria.MalariaDrugs;
import org.openmrs.module.kenyaemr.reporting.library.malaria.Moh743IndicatorLibrary;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({ "kenyaemr.ehrReports.report.moh743" })
public class Moh743ReportBuilder extends AbstractReportBuilder {
	@Autowired
	private Moh743IndicatorLibrary moh743IndicatorLibrary;
	private static final Integer BLOOD_SMEAR = 2017917;
	private static final Integer RDT = 1642;
	private static final String COMPARATIVE_OPERATION_GREATER= ">=";
	private static final String COMPARATIVE_GREATER= ">";
	private static final String COMPARATIVE_OPERATION_LESS = "<";
	private static final String NOT_DETECTED = "NOT DETECTED";
	private static final String POSITIVE = "Positive";
	private static final String NEGATIVE = "NEGATIVE";
	private static final Integer AGE_FIVE = 5;
	private static final Double WEIGHT_0 = 0.0;
	private static final Double WEIGHT_FIVE = 5.0;
	private static final Double WEIGHT_10 = 10.0;
	private static final Double WEIGHT_15 = 15.0;
	private static final Double WEIGHT_20 = 20.0;
	private static final Double WEIGHT_25 = 25.0;
	private static final Double WEIGHT_30 = 30.0;
	private static final Double WEIGHT_35 = 35.0;
	private static final Double WEIGHT_50 = 50.0;
	private static final Double WEIGHT_60 = 60.0;
	private static final Double WEIGHT_80 = 80.0;
	private static final Double WEIGHT_1000 = 1000.0;



	static final int FACTOR_1 = 1;
	static final int TABLET = 1513,TEST = 1513,BLISTER = 1513, SACHETS = 1608, VITAL = 162382, PIECES = 2000635;

	String indParams = "startDate=${startDate},endDate=${endDate}";
	@Override
	protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
		return Arrays.asList(new Parameter("startDate", "Start Date", Date.class),
				new Parameter("endDate", "End Date", Date.class)
		);
	}

	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor reportDescriptor,
															ReportDefinition reportDefinition) {
		return Arrays.asList(

				ReportUtils.map(getDataSetDefinition("Rapid Diagnostic Tests", byConcept(MalariaDrugs.RAPID_DIAGNOSTIC_TEST_CONCEPT), FACTOR_1, TEST), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artemether-Lumefantrine 20/120 Tabs 6s", byDrug(MalariaDrugs.ARTEMETHER_LUMEFANTRINE_20_120MG_6S), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artemether-Lumefantrine 20/120 Tabs 12s", byDrug(MalariaDrugs.ARTEMETHER_LUMEFANTRINE_20_120MG_12S), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artemether-Lumefantrine 20/120 Tabs 18s", byDrug(MalariaDrugs.ARTEMETHER_LUMEFANTRINE_20_120MG_18S), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artemether-Lumefantrine 20/120 Tabs 24s", byDrug(MalariaDrugs.ARTEMETHER_LUMEFANTRINE_20_120MG_24S), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate-Pyronaridine 20/60", byDrug(MalariaDrugs.ARTESUNATE_PYRONARIDINE_20_60MG_GRANULES), FACTOR_1, SACHETS), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate-Pyronaridine 60/180", byDrug(MalariaDrugs.ARTESUNATE_PYRONARIDINE_60_180MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Dihydroartemesinin/piperaquine 160mg Tabs", byDrug(MalariaDrugs.DIHYDROARTEMISININ_PIPERAQUINE_20_160MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Dihydroartemesinin/piperaquine 320mg Tabs", byDrug(MalariaDrugs.DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_DISPERSIBLE_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Dihydroartemesinin/piperaquine 320mg Tabs", byDrug(MalariaDrugs.DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate-Amodiaquine 67/25", byDrug(MalariaDrugs.ARTESUNATE_AMODIAQUINE_67_25MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate-Amodiaquine 135/50", byDrug(MalariaDrugs.ARTESUNATE_AMODIAQUINE_135_50MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate-Amodiaquine 270/100", byDrug(MalariaDrugs.ARTESUNATE_AMODIAQUINE_270_100MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Artesunate Injection", byDrug(MalariaDrugs.ARTESUNATE_INJECTION), FACTOR_1, VITAL), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Sulphadoxine Pyrimethamine Tabs", byDrug(MalariaDrugs.SULPHADOXINE_PYRIMETHAMINE_500_25MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("LLINs", byConcept(MalariaDrugs.LONG_LASTING_INSECTICIDAL_NET_CONCEPT), FACTOR_1, PIECES), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(getDataSetDefinition("Primaguine 75mg", byDrug(MalariaDrugs.PRIMAQUINE_7_5MG_TAB), FACTOR_1, TABLET), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(totalPatientsByAgeSuspectedMalaria(), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(patientsTreatedWithAntiMalarials(), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(patientsGivenArtesunateInjection(), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(totalPatientsSuspectedMalariaByAge(), "startDate=${startDate},endDate=${endDate}"),
				ReportUtils.map(patientsTreatedForMalaria(), "startDate=${startDate},endDate=${endDate}")

		);
	}
	// This will be skipped by khis payload generator
	private DataSetDefinition totalPatientsByAgeSuspectedMalaria() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("SUSPECTED_MALARIA");
		dsd.setDescription("SUSPECTED_MALARIA");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("TOTAL_LESS_FIVE", "Total Patient less than 5", ReportUtils.map(moh743IndicatorLibrary.totalPatientsByAgeSuspectedMalaria(COMPARATIVE_OPERATION_LESS,AGE_FIVE), indParams), "");
		dsd.addColumn("TOTAL_PLUS_FIVE", "Total Patient greater less 5", ReportUtils.map(moh743IndicatorLibrary.totalPatientsByAgeSuspectedMalaria(COMPARATIVE_OPERATION_GREATER,AGE_FIVE), indParams), "");
		return dsd;
	}

	/**
	 * Patients given any malaria treatment, counted once however many drugs they had, for the
	 * data quality check T22-04. Not on the paper form and not mapped to KHIS, so it is not sent.
	 */
	private DataSetDefinition patientsTreatedForMalaria() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("DQ_MALARIA_TREATMENT");
		dsd.setDescription("Patients treated for malaria (data quality)");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("PATIENTS_TREATED", "Patients given an ACT, artesunate injection or primaquine", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedForMalaria(), indParams), "");
		return dsd;
	}

	/**
	 * @return a condition on the stock item's drug, for {@link #getMoh743DrugSummary}
	 */
	private static String byDrug(List<Integer> drugIds) {
		return "drug_id IN " + MalariaDrugs.sql(drugIds);
	}

	/**
	 * @return a condition on the stock item's concept, for commodities that are not drugs
	 */
	private static String byConcept(int conceptId) {
		return "concept_id = " + conceptId;
	}

	private DataSetDefinition getDataSetDefinition(String label, String stockItems, int factor, int unit) {
		SqlDataSetDefinition sqlDataSetDefinition = new SqlDataSetDefinition();
		sqlDataSetDefinition.setName(label);
		sqlDataSetDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		sqlDataSetDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		sqlDataSetDefinition.setSqlQuery(getMoh743DrugSummary(stockItems, factor, unit));
		return sqlDataSetDefinition;
	}

	private DataSetDefinition patientsTreatedWithAntiMalarials() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("Patients Given an ACT by Weight Band Category");
		dsd.setDescription("Patients treated with AL, DHAP, or ASPY anti malarial");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("under_5_years_5_to_under_10_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_FIVE, WEIGHT_10, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_5_to_under_10_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_FIVE, WEIGHT_10, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_10_to_under_15_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_10, WEIGHT_15, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_10_to_under_15_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_10, WEIGHT_15, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_15_to_under_20_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_15, WEIGHT_20, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_15_to_under_20_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_15, WEIGHT_20, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_20_to_under_25_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_20, WEIGHT_25, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_20_to_under_25_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_20, WEIGHT_25, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_25_to_under_30_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_25, WEIGHT_30, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_25_to_under_30_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_25, WEIGHT_30, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_30_to_under_35_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_30, WEIGHT_35, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_30_to_under_35_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_30, WEIGHT_35, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_35_to_under_60_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_35, WEIGHT_60, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_35_to_under_60_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_35, WEIGHT_60, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_60_to_under_80_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_60, WEIGHT_80, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_60_to_under_80_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_60, WEIGHT_80, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("under_5_years_80_and_above_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_80, WEIGHT_1000, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("aged_5_years_and_above_80_and_above_kg", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsTreatedWithAntiMalarialAgeWeightRange(WEIGHT_80, WEIGHT_1000, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		return dsd;
	}

	private DataSetDefinition patientsGivenArtesunateInjection() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("INJ_ARTESUNATE");
		dsd.setDescription("Patients treated with Artesunate Injection");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("20_Under_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_0, WEIGHT_20, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("20_Over_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_0, WEIGHT_20, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("20_50_Under_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_20, WEIGHT_50, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("20_50_Over_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_20, WEIGHT_50, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");
		dsd.addColumn("50_Under_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_50, WEIGHT_1000, COMPARATIVE_OPERATION_LESS,  AGE_FIVE), indParams), "");
		dsd.addColumn("50_Over_5", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsGivenArtesunateInjectionAgeWeightRange(WEIGHT_50, WEIGHT_1000, COMPARATIVE_GREATER,  AGE_FIVE), indParams), "");

		return dsd;
	}

	private DataSetDefinition totalPatientsSuspectedMalariaByAge() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("SUSPECTED_MALARIA_AGE");
		dsd.setDescription("Patients suspected of Malaria by Age");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addColumn("MICRO_LESS_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,BLOOD_SMEAR,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("MICRO_MORE_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,BLOOD_SMEAR,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("MICRO_LESS_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,BLOOD_SMEAR,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("MICRO_MORE_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,BLOOD_SMEAR,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("RTD_LESS_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,RDT,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("RTD_MORE_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,RDT,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("RTD_LESS_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,RDT,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("RTD_MORE_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedMalariaByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,RDT,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("RTD_MICRO_LESS_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("RTD_MICRO_MORE_5_POS", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + POSITIVE + "'"), "");
		dsd.addColumn("RTD_MICRO_LESS_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("RTD_MICRO_MORE_5_NEG", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,NEGATIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NEGATIVE + "'"), "");
		dsd.addColumn("RTD_MICRO_LESS_5_INVL", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,NOT_DETECTED), "startDate=${startDate},endDate=${endDate},resultName='" + NOT_DETECTED + "'"), "");
		dsd.addColumn("RTD_MICRO_MORE_5_INVL", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsSuspectedRDTMICROByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE,NOT_DETECTED), "startDate=${startDate},endDate=${endDate},resultName='" + NOT_DETECTED + "'"), "");
//		dsd.addColumn("NOT_TESTED_LESS_FIVE", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsNotTestedRDTMICROByAge( COMPARATIVE_OPERATION_LESS,  AGE_FIVE,POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NOT_DETECTED + "'"), "");
//		dsd.addColumn("NOT_TESTED_MORE_FIVE", "", ReportUtils.map(moh743IndicatorLibrary.totalPatientsNotTestedRDTMICROByAge( COMPARATIVE_OPERATION_GREATER,  AGE_FIVE, POSITIVE), "startDate=${startDate},endDate=${endDate},resultName='" + NOT_DETECTED + "'"), "");

		return dsd;
	}

	/**
	 * @param stockItems which stock items the row covers, a condition on stockmgmt_stock_item such as
	 *            "drug_id IN (1, 2)"
	 */
	private String getMoh743DrugSummary(String stockItems, int factor, int unit) {
		if (stockItems == null || stockItems.isEmpty()) {
			throw new IllegalArgumentException("The stock item condition cannot be empty");
		}
		String query =
				"SELECT\n" +
						"    FLOOR(COALESCE(prev_month.closing_balance, 0)) AS opening_balance,\n" +
						"    FLOOR(COALESCE(COALESCE(curr_receipts.quantity, 0) + COALESCE(opening_balance.quantity, 0), 0)) AS curr_receipts,\n" +
						"    FLOOR(COALESCE(curr_dispensed.quantity, 0)) AS curr_dispensed,\n" +
						"    FLOOR(COALESCE(curr_losses.quantity, 0)) AS curr_loss,\n" +
						"    FLOOR(COALESCE(pos_adjustments.quantity, 0)) AS pos_adj,\n" +
						"    FLOOR(COALESCE(neg_adjustments.quantity, 0)) AS neg_adj,\n" +
						"    FLOOR(\n" +
						"        COALESCE(prev_month.closing_balance, 0) +\n" +
						"        COALESCE(COALESCE(curr_receipts.quantity, 0) + COALESCE(opening_balance.quantity, 0), 0) -\n" +
						"        COALESCE(curr_dispensed.quantity, 0) -\n" +
						"        COALESCE(curr_losses.quantity, 0) +\n" +
						"        COALESCE(pos_adjustments.quantity, 0) -\n" +
						"        COALESCE(neg_adjustments.quantity, 0)\n" +
						"    ) AS stck_take,\n" +
						"    FLOOR(COALESCE(expired_commodities.quantity, 0)) AS quantity_of_expired_drugs,\n" +
						"    FLOOR(COALESCE(early_expiry.quantity, 0)) AS medicines_with_6_months_to_expiry,\n" +
						"    COALESCE(\n" +
						"        IF(early_expiry.earliest_expiry_date IS NULL,\n" +
						"            '0',\n" +
						"            DATE_FORMAT(early_expiry.earliest_expiry_date, '%%Y-%%m-%%d')\n" +
						"        ),\n" +
						"        '0'\n" +
						"    ) AS earliest_expiry_date,\n" +
						"    FLOOR(COALESCE(curr_requisitions.quantity, 0)) AS curr_requested,\n" +
						"    COALESCE(days_out_of_stock.days, 0) AS days_out_of_stock\n" +
						"FROM (SELECT %d AS dispensing_unit_id) AS params\n" +
						"LEFT JOIN stockmgmt_stock_item ssi\n" +
						"    ON ssi." + stockItems + "\n" +
						"    AND ssi.dispensing_unit_id = params.dispensing_unit_id\n" +
						"LEFT JOIN stockmgmt_stock_item_packaging_uom sspu\n" +
						"    ON sspu.stock_item_id = ssi.stock_item_id\n" +
						"    AND sspu.factor = %d\n" +

						"    -- Previous month\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id,\n" +
						"        SUM(CASE\n" +
						"            WHEN ssto.operation_type_id IN (9) THEN stit.quantity * uom.factor\n" +
						"            WHEN ssto.operation_type_id IN (4) THEN stit.quantity * uom.factor\n" +
						"            WHEN ssto.operation_type_id IN (1) AND stit.quantity > 0 THEN stit.quantity * uom.factor\n" +
						"            WHEN ssto.operation_type_id IN (6, 3, 2) THEN -stit.quantity * uom.factor\n" +
						"            WHEN ssto.operation_type_id IN (1) AND stit.quantity < 0 THEN stit.quantity * uom.factor\n" +
						"            WHEN ssto.stock_operation_id IS NULL AND stit.quantity < 0 THEN stit.quantity * uom.factor\n" +
						"            ELSE 0\n" +
						"        END) AS closing_balance\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    LEFT JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND stit.date_created < :startDate\n" +
						"      AND (ssto.status = 'COMPLETED' OR ssto.stock_operation_id IS NULL)\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") prev_month ON prev_month.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Current Received\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (4)\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") curr_receipts ON curr_receipts.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Opening Balance\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (9)\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") opening_balance ON opening_balance.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Current Dispensed\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(-stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND stit.stock_operation_id IS NULL AND stit.quantity < 0\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") curr_dispensed ON curr_dispensed.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Current Losses\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(-stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (2)\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") curr_losses ON curr_losses.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Positive Adjustment\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (1)\n" +
						"      AND stit.quantity > 0\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") pos_adjustments ON pos_adjustments.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Negative Adjustment\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(-stit.quantity * uom.factor) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    INNER JOIN stockmgmt_stock_item_packaging_uom uom ON stit.stock_item_packaging_uom_id = uom.stock_item_packaging_uom_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (1)\n" +
						"      AND stit.quantity < 0\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") neg_adjustments ON neg_adjustments.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Early Expiry\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id,\n" +
						"        MIN(ssbt.expiration) AS earliest_expiry_date,\n" +
						"        COUNT(ssbt.stock_batch_id) AS quantity\n" +
						"    FROM stockmgmt_stock_batch ssbt\n" +
						"    INNER JOIN stockmgmt_stock_item si ON ssbt.stock_item_id = si.stock_item_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssbt.voided = 0\n" +
						"      AND DATE(ssbt.expiration) BETWEEN :startDate AND DATE_ADD(:endDate, INTERVAL 6 MONTH)\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") early_expiry ON early_expiry.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Expired Commodities\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id,\n" +
						"        COUNT(ssbt.stock_batch_id) AS quantity\n" +
						"    FROM stockmgmt_stock_batch ssbt\n" +
						"    INNER JOIN stockmgmt_stock_item si ON ssbt.stock_item_id = si.stock_item_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssbt.voided = 0\n" +
						"      AND DATE(ssbt.expiration) < :startDate\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") expired_commodities ON expired_commodities.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Current Requisition\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id, SUM(stit.quantity) AS quantity\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_operation ssto ON stit.stock_operation_id = ssto.stock_operation_id\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND ssto.operation_type_id IN (7)\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND ssto.status = 'COMPLETED'\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") curr_requisitions ON curr_requisitions.dispensing_unit_id = params.dispensing_unit_id\n" +

						"    -- Days out of stock\n" +
						"LEFT JOIN (\n" +
						"    SELECT si.dispensing_unit_id,\n" +
						"        COUNT(DISTINCT DATE(stit.date_created)) AS days\n" +
						"    FROM stockmgmt_stock_item_transaction stit\n" +
						"    INNER JOIN stockmgmt_stock_item si ON stit.stock_item_id = si.stock_item_id\n" +
						"    WHERE si." + stockItems + " AND si.dispensing_unit_id = %d\n" +
						"      AND stit.date_created BETWEEN DATE(:startDate) AND DATE(:endDate)\n" +
						"      AND stit.quantity <= 0\n" +
						"    GROUP BY si.dispensing_unit_id\n" +
						") days_out_of_stock ON days_out_of_stock.dispensing_unit_id = params.dispensing_unit_id\n" +
						"GROUP BY concept_id;";

		return String.format(query, factor, unit, factor, unit, unit, unit, unit, unit, unit, unit, unit, unit, unit, unit);
	}
}

