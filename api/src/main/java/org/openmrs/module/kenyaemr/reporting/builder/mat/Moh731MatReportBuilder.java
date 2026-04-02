/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.mat;

import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.reporting.ColumnParameters;
import org.openmrs.module.kenyaemr.reporting.EmrReportingUtils;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.MOH731Greencard.ETLMoh731GreenCardIndicatorLibrary;
import org.openmrs.module.kenyaemr.reporting.library.mat.Moh731MatIndicatorLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.common.CommonDimensionLibrary;
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
 * Report builder for ETL MOH 731-6 MAT
 */
@Component
@Builds({"kenyaemr.etl.common.report.matReport"})
public class Moh731MatReportBuilder extends AbstractReportBuilder {
    @Autowired
    private CommonDimensionLibrary commonDimensions;

    @Autowired
    private Moh731MatIndicatorLibrary moh731MatIndicators;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    ColumnParameters boys_0_to_60_days = new ColumnParameters(null, "0-60 days", "gender=M|age=0-60");
    ColumnParameters boys_61_days_to_9_years = new ColumnParameters(null, "61 days-9 yrs", "gender=M|age=61-9");

    ColumnParameters maleInfants = new ColumnParameters(null, "<1, Male", "gender=M|age=<1");
    ColumnParameters femaleInfants = new ColumnParameters(null, "<1, Female", "gender=F|age=<1");

    ColumnParameters children_0_to_4 = new ColumnParameters(null, "0-4", "age=0-4");
    ColumnParameters children_5_to_9 = new ColumnParameters(null, "5-9", "age=5-9");
    ColumnParameters children_10_to_14 = new ColumnParameters(null, "10-14", "age=10-14");
    ColumnParameters adult_15_to_19 = new ColumnParameters(null, "15-19", "age=15-19");
    ColumnParameters adult_20_and_above = new ColumnParameters(null, "20+", "age=20+");

    // specific to pre-art
    ColumnParameters under15 = new ColumnParameters(null, "<15", "age=<15");
    ColumnParameters adult_15_and_above = new ColumnParameters(null, "15+", "age=15+");
    ColumnParameters male_15_and_above = new ColumnParameters(null, "15+", "gender=M|age=15+");
    // end of pre-art

    ColumnParameters m_1_to_4 = new ColumnParameters(null, "1-4, Male", "gender=M|age=1-4");
    ColumnParameters f_1_to_4 = new ColumnParameters(null, "1-4, Female", "gender=F|age=1-4");
    ColumnParameters m_2_to_9 = new ColumnParameters(null, "2-9, Male", "gender=M|age=2-9");
    ColumnParameters f_2_to_9 = new ColumnParameters(null, "2-9, Female", "gender=F|age=2-9");
    ColumnParameters m_5_to_9 = new ColumnParameters(null, "5-9, Male", "gender=M|age=5-9");
    ColumnParameters f_5_to_9 = new ColumnParameters(null, "5-9, Female", "gender=F|age=5-9");

    ColumnParameters m_10_to_14 = new ColumnParameters(null, "10-14, Male", "gender=M|age=10-14");
    ColumnParameters f_10_to_14 = new ColumnParameters(null, "10-14, Female", "gender=F|age=10-14");
    ColumnParameters m_15_to_19 = new ColumnParameters(null, "15-19, Male", "gender=M|age=15-19");
    ColumnParameters f_15_to_19 = new ColumnParameters(null, "15-19, Female", "gender=F|age=15-19");
    ColumnParameters f10_to_19 = new ColumnParameters(null, "10-19, Female", "gender=F|age=10-19");
    ColumnParameters m_20_to_24 = new ColumnParameters(null, "20-24, Male", "gender=M|age=20-24");
    ColumnParameters f_20_to_24 = new ColumnParameters(null, "20-24, Female", "gender=F|age=20-24");
    ColumnParameters m_25_and_above = new ColumnParameters(null, "25+, Male", "gender=M|age=25+");
    ColumnParameters f_25_and_above = new ColumnParameters(null, "25+, Female", "gender=F|age=25+");
    ColumnParameters males = new ColumnParameters(null, "Male", "gender=M");
    ColumnParameters females = new ColumnParameters(null, "Female", "gender=F");

    ColumnParameters colTotal = new ColumnParameters(null, "Total", "");

    List<ColumnParameters> standardAgeOnlyDisaggregation = Arrays.asList(
            children_0_to_4, children_5_to_9, children_10_to_14, adult_15_to_19,
            adult_20_and_above);

    List<ColumnParameters> allAgeDisaggregation = Arrays.asList(
            maleInfants, femaleInfants, m_1_to_4,  f_1_to_4, m_5_to_9, f_5_to_9, m_10_to_14, f_10_to_14,m_15_to_19, f_15_to_19,
            m_20_to_24, f_20_to_24, m_25_and_above, f_25_and_above);

    List<ColumnParameters> vmmcDisaggregation = Arrays.asList(
            boys_0_to_60_days, boys_61_days_to_9_years, m_10_to_14, male_15_and_above);

    List<ColumnParameters> adolescentYoungWomenAgeDisaggregation = Arrays.asList(f10_to_19,f_20_to_24);
    List<ColumnParameters> genderDisaggregation = Arrays.asList(males,females);

    List<ColumnParameters> under15And15PlusDisaggregation = Arrays.asList(under15,adult_15_and_above);

    @Override
    protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
        return Arrays.asList(
                new Parameter("startDate", "Start Date", Date.class),
                new Parameter("endDate", "End Date", Date.class),
                new Parameter("dateBasedReporting", "", String.class)
        );
    }

    @Override
    protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor reportDescriptor, ReportDefinition reportDefinition) {
        return Arrays.asList(
                ReportUtils.map(matDatasetDefinition(), "startDate=${startDate},endDate=${endDate}")
        );
    }
      /**
     * Creates the dataset for section #1
     * : hiv testing and counseling
     * @return the dataset
     *
     */
    protected DataSetDefinition matDatasetDefinition() {
        CohortIndicatorDataSetDefinition cohortDsd = new CohortIndicatorDataSetDefinition();
        cohortDsd.setName("mat_dataset");
        cohortDsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cohortDsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cohortDsd.addDimension("age", ReportUtils.map(commonDimensions.moh731GreenCardAgeGroups(), "onDate=${endDate}"));
        cohortDsd.addDimension("gender", ReportUtils.map(commonDimensions.gender()));
        String indParams = "startDate=${startDate},endDate=${endDate}";

       //1.1 Ever Inducted
        EmrReportingUtils.addRow(cohortDsd, "Ever Enrolled In Mat", "", ReportUtils.map(moh731MatIndicators.matAllNumberInducted(), indParams), genderDisaggregation, Arrays.asList("19", "20"));

//        cohortDsd.addColumn("HV01-01", "Tested (M)", ReportUtils.map(moh731GreenCardIndicators.htsTestsMales(), indParams),"");


        return cohortDsd;

    }


}
