/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.common;

import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.reporting.ColumnParameters;
import org.openmrs.module.kenyaemr.reporting.EmrReportingUtils;
import org.openmrs.module.kenyaemr.reporting.library.moh763.Moh763IndicatorLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.common.CommonDimensionLibrary;
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
import java.util.function.Function;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;

/**
 * MOH 763 report - Mental Health
 */
@Component
@Builds({"kenyaemr.ehrReports.report.moh763"})
public class MOH763ReportBuilder extends AbstractReportBuilder {

	// etl_clinical_encounter.visit_type text values, matching the convention used in Moh717CohortLibrary
	static final String NEW_VISIT = "New visit", RE_VISIT = "Revisit";

	@Autowired
	private CommonDimensionLibrary commonDimensions;

	@Autowired
	private Moh763IndicatorLibrary moh763Indicators;

	private final String indParams = "startDate=${startDate},endDate=${endDate}";

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
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
		return Arrays.asList(
				ReportUtils.map(mentalHealthDataSet(), indParams)
		);
	}

	// Column parameters: male/female across the 6 MOH 763 age bands
	ColumnParameters male0_9 = new ColumnParameters(null, "Male, 0-9 years", "gender=M|age=0-9");
	ColumnParameters female0_9 = new ColumnParameters(null, "Female, 0-9 years", "gender=F|age=0-9");
	ColumnParameters male10_18 = new ColumnParameters(null, "Male, 10-18 years", "gender=M|age=10-18");
	ColumnParameters female10_18 = new ColumnParameters(null, "Female, 10-18 years", "gender=F|age=10-18");
	ColumnParameters male19_24 = new ColumnParameters(null, "Male, 19-24 years", "gender=M|age=19-24");
	ColumnParameters female19_24 = new ColumnParameters(null, "Female, 19-24 years", "gender=F|age=19-24");
	ColumnParameters male25_34 = new ColumnParameters(null, "Male, 25-34 years", "gender=M|age=25-34");
	ColumnParameters female25_34 = new ColumnParameters(null, "Female, 25-34 years", "gender=F|age=25-34");
	ColumnParameters male35_64 = new ColumnParameters(null, "Male, 35-64 years", "gender=M|age=35-64");
	ColumnParameters female35_64 = new ColumnParameters(null, "Female, 35-64 years", "gender=F|age=35-64");
	ColumnParameters male65AndAbove = new ColumnParameters(null, "Male, 65 years and above", "gender=M|age=65+");
	ColumnParameters female65AndAbove = new ColumnParameters(null, "Female, 65 years and above", "gender=F|age=65+");

	List<ColumnParameters> mentalHealthAgeDisaggregation = Arrays.asList(
			male0_9, female0_9,
			male10_18, female10_18,
			male19_24, female19_24,
			male25_34, female25_34,
			male35_64, female35_64,
			male65AndAbove, female65AndAbove
	);

	List<String> mentalHealthColumnSuffixes = Arrays.asList(
			"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
	);

	/**
	 * Adds the columns for one disorder-category row, matching the MOH 763 tool's layout: for each
	 * of the 6 age bands x 2 sexes, a New/Revisit pair (24 columns, generated via addRow using
	 * baseName+"New"/"Revisit" - the actual New-vs-Revisit-per-age/sex column ORDER in the rendered
	 * sheet is entirely controlled by where each #token# is placed in the .xls template, not by the
     * order these two addRow calls run in), plus an un-disaggregated row-level New/Revisit total pair.
	 */
	private void addDisorderRows(CohortIndicatorDataSetDefinition dsd, String baseName, String label,
			Function<String, CohortIndicator> indicatorForVisitType) {
		EmrReportingUtils.addRow(dsd, baseName + "New", label + " (New)",
				ReportUtils.map(indicatorForVisitType.apply(NEW_VISIT), indParams), mentalHealthAgeDisaggregation, mentalHealthColumnSuffixes);
		EmrReportingUtils.addRow(dsd, baseName + "Revisit", label + " (Revisit)",
				ReportUtils.map(indicatorForVisitType.apply(RE_VISIT), indParams), mentalHealthAgeDisaggregation, mentalHealthColumnSuffixes);

		// Row totals: same indicators, no age/sex disaggregation
		dsd.addColumn(baseName + "NewTotal", label + " (New Total)", ReportUtils.map(indicatorForVisitType.apply(NEW_VISIT), indParams), "");
		dsd.addColumn(baseName + "RevisitTotal", label + " (Revisit Total)", ReportUtils.map(indicatorForVisitType.apply(RE_VISIT), indParams), "");
	}

	/**
	 * Mental Health dataset: one New/Revisit row pair per disorder category, each disaggregated by age and sex
	 * @return the data set
	 */
	private DataSetDefinition mentalHealthDataSet() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("Mental_Health");
		dsd.setDescription("MOH 763 Mental Health");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		dsd.addDimension("age", map(commonDimensions.moh763AgeGroups(), "onDate=${endDate}"));
		dsd.addDimension("gender", map(commonDimensions.gender()));

		addDisorderRows(dsd, "AnxietyDisorders", "Anxiety disorders", moh763Indicators::anxietyDisorders);
		addDisorderRows(dsd, "DepressiveDisorders", "Depressive disorders", moh763Indicators::depressiveDisorders);
		addDisorderRows(dsd, "SchizophreniaSpectrumDisorders", "Schizophrenia spectrum disorders", moh763Indicators::schizophreniaSpectrumDisorders);
		addDisorderRows(dsd, "BipolarDisorders", "Bipolar disorders", moh763Indicators::bipolarDisorders);
		addDisorderRows(dsd, "OtherPsychoticDisorders", "Other psychotic disorders", moh763Indicators::otherPsychoticDisorders);
		addDisorderRows(dsd, "NeurodevelopmentalDisorders", "Neurodevelopmental disorders", moh763Indicators::neurodevelopmentalDisorders);
		addDisorderRows(dsd, "AlcoholUseDisorders", "Alcohol use disorders", moh763Indicators::alcoholUseDisorders);
		addDisorderRows(dsd, "OtherSubstanceUseDisorders", "Other disorders due to substance use", moh763Indicators::otherSubstanceUseDisorders);
		addDisorderRows(dsd, "OtherBehaviouralAddictions", "Other behavioral addictions", moh763Indicators::otherBehaviouralAddictions);
		addDisorderRows(dsd, "NeurocognitiveDisorders", "Neurocognitive disorders", moh763Indicators::neurocognitiveDisorders);
		addDisorderRows(dsd, "StressTraumaRelatedDisorders", "Stress-trauma related disorders", moh763Indicators::stressTraumaRelatedDisorders);
		addDisorderRows(dsd, "SomatoformDisorders", "Somatoform disorders", moh763Indicators::somatoformDisorders);
		addDisorderRows(dsd, "SuicidalBehaviour", "Suicidal behaviour", moh763Indicators::suicidalBehaviour);
		addDisorderRows(dsd, "SleepWakeDisorders", "Sleep-wake disorders", moh763Indicators::sleepWakeDisorders);
		addDisorderRows(dsd, "PerinatalMentalDisorders", "Perinatal mental disorders", moh763Indicators::perinatalMentalDisorders);
		addDisorderRows(dsd, "EatingDisorders", "Eating disorders", moh763Indicators::eatingDisorders);
		addDisorderRows(dsd, "PersonalityDisorders", "Personality disorders", moh763Indicators::personalityDisorders);
		addDisorderRows(dsd, "SexualDysfunctionalDisorders", "Sexual dysfunctional disorders", moh763Indicators::sexualDysfunctionalDisorders);
		addDisorderRows(dsd, "ObsessiveCompulsiveAndRelatedDisorders", "Obsessive-compulsive & related disorders", moh763Indicators::obsessiveCompulsiveAndRelatedDisorders);
		addDisorderRows(dsd, "DisruptiveImpulseControlAndConductDisorder", "Disruptive impulse control and conduct disorder", moh763Indicators::disruptiveImpulseControlAndConductDisorder);

		return dsd;
	}
}
