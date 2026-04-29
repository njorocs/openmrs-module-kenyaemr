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

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;

import static org.openmrs.module.kenyacore.report.ReportUtils.map;

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
    ColumnParameters m_25_to_29 = new ColumnParameters(null, "25-29, Male", "gender=M|age=25-29");
    ColumnParameters f_25_to_29 = new ColumnParameters(null, "25-29, Female", "gender=F|age=25-29");
    ColumnParameters m_30_and_above = new ColumnParameters(null, "30+, Male", "gender=M|age=30+");
    ColumnParameters f_30_and_above = new ColumnParameters(null, "30+, Female", "gender=F|age=30+");
    ColumnParameters males = new ColumnParameters(null, "Male", "gender=M");
    ColumnParameters females = new ColumnParameters(null, "Female", "gender=F");

    ColumnParameters colTotal = new ColumnParameters(null, "Total", "");

    List<ColumnParameters> standardAgeOnlyDisaggregation = Arrays.asList(
            children_0_to_4, children_5_to_9, children_10_to_14, adult_15_to_19,
            adult_20_and_above);

    List<ColumnParameters> allAgeDisaggregation = Arrays.asList(
            maleInfants, femaleInfants, m_1_to_4,  f_1_to_4, m_5_to_9, f_5_to_9, m_10_to_14, f_10_to_14,m_15_to_19, f_15_to_19,
            m_20_to_24, f_20_to_24, m_25_to_29, f_25_to_29, m_30_and_above, f_30_and_above);

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

       // 1.0 EVER INDUCTED
        EmrReportingUtils.addRow(cohortDsd, "Ever Enrolled In Mat", "", ReportUtils.map(moh731MatIndicators.matAllNumberInducted(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd, "Total Ever Weaned off MAT", "", ReportUtils.map(moh731MatIndicators.matAllNumberInducted(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 1.1 MAT INDUCTION WITHIN THE REPORTING PERIOD
        EmrReportingUtils.addRow(cohortDsd,  "Newly Enrolled In Mat in Reporting period", "", ReportUtils.map(moh731MatIndicators.matNumberInductedInReportingPeriod(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));

        // 1.2 Currently on MAT Methadone 1.3 Currently on MAT(Buprenorphine)
        EmrReportingUtils.addRow(cohortDsd,  "Number currently on Methadone", "", ReportUtils.map(moh731MatIndicators.matNumberOnMethadone(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        EmrReportingUtils.addRow(cohortDsd,  "Number currently on Buprenorphine", "", ReportUtils.map(moh731MatIndicators.matNumberOnBuprenorphine(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        EmrReportingUtils.addRow(cohortDsd,  "Number currently on Methadone in Transit", "", ReportUtils.map(moh731MatIndicators.matNumberOnMethadoneInTransit(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number currently on Buprenorphine in Transit", "", ReportUtils.map(moh731MatIndicators.matNumberOnBuprenorphineInTransit(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 1.4 WEANING OFF
        EmrReportingUtils.addRow(cohortDsd, "Weaned off Methadone", "", ReportUtils.map(moh731MatIndicators.matNumberWeanedOffMethadone(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Weaned off Buprenorphine", "", ReportUtils.map(moh731MatIndicators.matNumberWeanedOffBuprenorphine(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 1.5 MAT INTERUPTIONS
        EmrReportingUtils.addRow(cohortDsd, "Number Discontinued MAT", "", ReportUtils.map(moh731MatIndicators.matNumberDiscontinued(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients who have died", "", ReportUtils.map(moh731MatIndicators.matNumberDied(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd, "Number missing more than 5 consecutive doses", "", ReportUtils.map(moh731MatIndicators.matNumberMissingDosage(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd, "Number of clients LTFU", "", ReportUtils.map(moh731MatIndicators.matNumberLTFU(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 1.6 HIV TESTING
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients tested for HIV", "", ReportUtils.map(moh731MatIndicators.matNumberTestedHIV(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        // 1.7 MAT Clients New-HIV Positive
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients HIV Positive", "", ReportUtils.map(moh731MatIndicators.matNumberHIVpositive(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        // 1.8 Number of MAT Clients Started on ART both offsite & Onsite
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients Started on ART", "", ReportUtils.map(moh731MatIndicators.matNumberStartedART(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        // 1.9 Number of MAT clients with Known HIV Positive Status
        EmrReportingUtils.addRow(cohortDsd,  "Total Number of Active MAT clients HIV Positive", "", ReportUtils.map(moh731MatIndicators.matNumberTotalHIVpositive(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));
        // 2.0 Total Number of MAT Clients Currently on ART both offsite and onsite
        EmrReportingUtils.addRow(cohortDsd,  "Total MAT Clients Currently on ART", "", ReportUtils.map(moh731MatIndicators.matNumberTotalStartedART(), indParams), allAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13","14","15","16"));

        // 2.1 Viral load tracking MAT Clients
        EmrReportingUtils.addRow(cohortDsd,  "Viral load result in the last 12 months", "", ReportUtils.map(moh731MatIndicators.matNumberViralLoadResult(), indParams),  genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Suppressed lt 200 copies", "", ReportUtils.map(moh731MatIndicators.matNumberViralLoadResultLt200(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Suppressed lt 50 copies", "", ReportUtils.map(moh731MatIndicators.matNumberViralLoadResultLt50(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.2 Overdose MAT clients
        EmrReportingUtils.addRow(cohortDsd,  "Experienced Overdose in Reporting period", "", ReportUtils.map(moh731MatIndicators.matNumberExperienceOverdose(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Received naloxone", "", ReportUtils.map(moh731MatIndicators.matNumberReceivedNaloxone(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Deaths due to overdose", "", ReportUtils.map(moh731MatIndicators.matNumberOverdoseDeaths(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.3 interventions
        EmrReportingUtils.addRow(cohortDsd,  "Number Received Psychosocial Interventions", "", ReportUtils.map(moh731MatIndicators.matNumberReceivedInterventions(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Supported with Reintegration", "", ReportUtils.map(moh731MatIndicators.matNumberSupportedWithReintegration(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        //2.4 Violence prevention and Support
        EmrReportingUtils.addRow(cohortDsd,  "Number Experienced Sexual Violence", "", ReportUtils.map(moh731MatIndicators.matNumberExperienceViolence(), "typeOfViolence=126582," + indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Experienced Physical Violence", "", ReportUtils.map(moh731MatIndicators.matNumberExperienceViolence(), "typeOfViolence=158358," + indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Experienced Emotional/Pyschological Violence", "", ReportUtils.map(moh731MatIndicators.matNumberExperienceViolence(), "typeOfViolence=118688," + indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number received support", "", ReportUtils.map(moh731MatIndicators.matNumberReceivedViolenceSupport(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.5 Mental Health
        EmrReportingUtils.addRow(cohortDsd,  "Number Screened for Mental Health", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedMH(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Diagnosed MH", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedMH(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Treated within the Facility MH", "", ReportUtils.map(moh731MatIndicators.matNumberTreatedWithinFacilityMH(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.6 STI  MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Screened STI", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedSTI(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Diagnosed with STI", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedSTI(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Treated STI", "", ReportUtils.map(moh731MatIndicators.matNumberTreatedSTI(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.7 HCV (Hepatitis C) MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Screened HCV", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedHCV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Positive HCV", "", ReportUtils.map(moh731MatIndicators.matNumberPositiveHCV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Positive HCV(Confirmatory PCR Test)", "", ReportUtils.map(moh731MatIndicators.matNumberPositiveHCVConfirmatoryPCRtest(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Treated HCV", "", ReportUtils.map(moh731MatIndicators.matNumberTreatedHCV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Total Number Currently on HCV Treatment", "", ReportUtils.map(moh731MatIndicators.matTotalNumberTreatedHCV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.7 HBV (Hepatitis B) MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Screened HBV", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedHBV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Negative HBV", "", ReportUtils.map(moh731MatIndicators.matNumberNegativeHBV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Negative HBV Vaccinated", "", ReportUtils.map(moh731MatIndicators.matNumberNegativeHBVvaccinated(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Positive HBV", "", ReportUtils.map(moh731MatIndicators.matNumberPositiveHBV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Positive HBV(Confirmatory PCR Test)", "", ReportUtils.map(moh731MatIndicators.matNumberPositiveHBVConfirmatoryPCRtest(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Treated HBV", "", ReportUtils.map(moh731MatIndicators.matNumberTreatedHBV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Total Number Currently on HBV Treatment", "", ReportUtils.map(moh731MatIndicators.matTotalNumberTreatedHBV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 2.9 TB MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Screened TB", "", ReportUtils.map(moh731MatIndicators.matNumberScreenedTB(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Diagnosed TB", "", ReportUtils.map(moh731MatIndicators.matNumberDiagnosedTB(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Started TB RX", "", ReportUtils.map(moh731MatIndicators.matNumberStartedTBRX(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Started TPT", "", ReportUtils.map(moh731MatIndicators.matNumberStartedTPT(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number of TB clients HIV positive", "", ReportUtils.map(moh731MatIndicators.matTotalNumberTBclientsHIVpositive(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Total number of TB Clients on HAART", "", ReportUtils.map(moh731MatIndicators.matTotalNumberTBClientsOnHAART(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 3.0 PrEP MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Initiated PrEP", "", ReportUtils.map(moh731MatIndicators.matNumberInitiatedPrep(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Testing HIV_positive while on PrEP", "", ReportUtils.map(moh731MatIndicators.matNumberHIVPositiveOnPrEP(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number of PrEP users diagnosed with STIs", "", ReportUtils.map(moh731MatIndicators.matNumberPrEPClientDiagnosedSTIs(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 3.1 PEP MAT
        EmrReportingUtils.addRow(cohortDsd,  "Number Exposed to HIV", "", ReportUtils.map(moh731MatIndicators.matNumberExposedToHIV(), indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number Receive PEP <72hrs", "", ReportUtils.map(moh731MatIndicators.matNumberReceivePEPlt72hrs(), indParams), genderDisaggregation, Arrays.asList("01", "02"));

        // 4.0 Nutrition support
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients SAM Severe", "", ReportUtils.map(moh731MatIndicators.matNumberMATClientsSAM(), "nutritionalStatus=163302," + indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number of MAT Clients SAM Moderate", "", ReportUtils.map(moh731MatIndicators.matNumberMATClientsSAM(), "nutritionalStatus=163303," + indParams), genderDisaggregation, Arrays.asList("01", "02"));
        EmrReportingUtils.addRow(cohortDsd,  "Number initiated on nutrition support", "", ReportUtils.map(moh731MatIndicators.matNumberInitiatedNutritionSupport(), indParams), genderDisaggregation, Arrays.asList("01", "02"));



        return cohortDsd;

    }


}
