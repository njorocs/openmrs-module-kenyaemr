/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.hiv;

import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.reporting.ColumnParameters;
import org.openmrs.module.kenyaemr.reporting.EmrReportingUtils;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.DurationToNextAppointmentDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.KPTypeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.RevisedDatim.DatimIndicatorLibrary;
import org.openmrs.module.kenyaemr.reporting.library.shared.common.CommonDimensionLibrary;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reporting.data.converter.definition.TreatmentStopReasonDataDefinition;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Report builder for Datim report
 */
@Component
@Builds({"kenyaemr.etl.common.report.datim"})
public class DatimReportBuilder extends AbstractReportBuilder {

    static final int FSW_CONCEPT = 160579;
    static final int MSM_CONCEPT = 160578;
    static final int PWID_CONCEPT = 105;
    static final int TG_CONCEPT = 165100;
    static final int PRISONERS_CLOSED_SETTINGS_CONCEPT = 162277;
    static final int DEATH = 160034;
    static final int TRF_OUT = 5240;
    static final int STOPPED_TX = 819;
    static final int HIV_DISEASE_RESULTING_IN_TB = 163324;
    static final int HIV_DISEASE_RESULTING_IN_CANCER = 116030;
    static final int  HIV_DISEASE_RESULTING_IN_INFECTIOUS_PARASITIC_DISEASE = 160159;
    static final int OTHER_HIV_DISEASE = 160158;
    static final int OTHER_NATURAL_CAUSES = 133478;
    static final int NON_NATURAL_CAUSES = 123812;
    static final int UNKNOWN_CAUSE = 142917;
    static final int COVID_19 = 165609;

    @Autowired
    private CommonDimensionLibrary commonDimensions;

    @Autowired
    private DatimIndicatorLibrary datimIndicators;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

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
        return Arrays.asList(ReportUtils.map(careAndTreatmentDataSet(), "startDate=${startDate},endDate=${endDate}")
        );
    }


    /**
     * Creates the dataset for section #3: Care and Treatment
     *
     * @return the dataset
     */
    protected DataSetDefinition careAndTreatmentDataSet() {
        CohortIndicatorDataSetDefinition cohortDsd = new CohortIndicatorDataSetDefinition();
        cohortDsd.setName("3");
        cohortDsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cohortDsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cohortDsd.addDimension("age", ReportUtils.map(commonDimensions.datimFineAgeGroups(), "onDate=${endDate}"));
        cohortDsd.addDimension("gender", ReportUtils.map(commonDimensions.gender()));

        //HTS dimensions
        cohortDsd.addDimension("contactAge", ReportUtils.map(commonDimensions.contactAgeGroups(), "onDate=${endDate}"));
        cohortDsd.addDimension("contactFineAge", ReportUtils.map(commonDimensions.contactsFineAgeGroups(), "onDate=${endDate}"));
        cohortDsd.addDimension("contactGender", ReportUtils.map(commonDimensions.contactGender()));

        ColumnParameters colTotal = new ColumnParameters(null, "Total", "");

        /*DatimQ4 Column parameters*/

        ColumnParameters all0_to_2m = new ColumnParameters(null, "0-2", "age=0-2");
        ColumnParameters all2_to_12m = new ColumnParameters(null, "2-12", "age=2-12");

        /*New age disaggregations*/
        ColumnParameters fInfant = new ColumnParameters(null, "<1, Female", "gender=F|age=<1");
        ColumnParameters mInfant = new ColumnParameters(null, "<1, Male", "gender=M|age=<1");
        ColumnParameters f1_to4 = new ColumnParameters(null, "1-4, Female", "gender=F|age=1-4");
        ColumnParameters m1_to4 = new ColumnParameters(null, "1-4, Male", "gender=M|age=1-4");
        ColumnParameters f5_to9 = new ColumnParameters(null, "5-9, Female", "gender=F|age=5-9");
        ColumnParameters m5_to9 = new ColumnParameters(null, "5-9, Male", "gender=M|age=5-9");
        ColumnParameters fUnder10 = new ColumnParameters(null, "<10, Female", "gender=F|age=<10");
        ColumnParameters mUnder10 = new ColumnParameters(null, "<10, Male", "gender=M|age=<10");
        ColumnParameters f10_to14 = new ColumnParameters(null, "10-14, Female", "gender=F|age=10-14");
        ColumnParameters m10_to14 = new ColumnParameters(null, "10-14, Male", "gender=M|age=10-14");
        ColumnParameters f15_to19 = new ColumnParameters(null, "15-19, Female", "gender=F|age=15-19");
        ColumnParameters m15_to19 = new ColumnParameters(null, "15-19, Male", "gender=M|age=15-19");
        ColumnParameters f20_to24 = new ColumnParameters(null, "20-24, Female", "gender=F|age=20-24");
        ColumnParameters m20_to24 = new ColumnParameters(null, "20-24, Male", "gender=M|age=20-24");
        ColumnParameters f25_to29 = new ColumnParameters(null, "25-29, Female", "gender=F|age=25-29");
        ColumnParameters m25_to29 = new ColumnParameters(null, "25-29, Male", "gender=M|age=25-29");
        ColumnParameters f30_to34 = new ColumnParameters(null, "30-34, Female", "gender=F|age=30-34");
        ColumnParameters m30_to34 = new ColumnParameters(null, "30-34, Male", "gender=M|age=30-34");
        ColumnParameters f35_to39 = new ColumnParameters(null, "35-39, Female", "gender=F|age=35-39");
        ColumnParameters m35_to39 = new ColumnParameters(null, "35-39, Male", "gender=M|age=35-39");
        ColumnParameters f40_to44 = new ColumnParameters(null, "40-44, Female", "gender=F|age=40-44");
        ColumnParameters m40_to44 = new ColumnParameters(null, "40-44, Male", "gender=M|age=40-44");
        ColumnParameters f45_to49 = new ColumnParameters(null, "45-49, Female", "gender=F|age=45-49");
        ColumnParameters m45_to49 = new ColumnParameters(null, "45-49, Male", "gender=M|age=45-49");
        ColumnParameters fAbove50 = new ColumnParameters(null, "50+, Female", "gender=F|age=50+");
        ColumnParameters mAbove50 = new ColumnParameters(null, "50+, Male", "gender=M|age=50+");

        ColumnParameters fUnder15 = new ColumnParameters(null, "<15, Female", "gender=F|age=<15");
        ColumnParameters mUnder15 = new ColumnParameters(null, "<15, Male", "gender=M|age=<15");
        ColumnParameters f15AndAbove = new ColumnParameters(null, "15+, Female", "gender=F|age=15+");
        ColumnParameters m15AndAbove = new ColumnParameters(null, "15+, Male", "gender=M|age=15+");
        //KP age groups
        ColumnParameters below_15 = new ColumnParameters(null, "<15", "age=<15");
        ColumnParameters kp15_to_19 = new ColumnParameters(null, "15-19", "age=15-19");
        ColumnParameters kp20_to_24 = new ColumnParameters(null, "20-24", "age=20-24");
        ColumnParameters kp25_and_above = new ColumnParameters(null, "25+", "age=25+");

        ColumnParameters below_15_f = new ColumnParameters(null, "<15, Female", "gender=F|age=<15");
        ColumnParameters below_15_m = new ColumnParameters(null, "<15, Male", "gender=M|age=<15");
        ColumnParameters kp15_to_19_f = new ColumnParameters(null, "15-19, Female", "gender=F|age=15-19");
        ColumnParameters kp15_to_19_m = new ColumnParameters(null, "15-19, Male", "gender=M|age=15-19");
        ColumnParameters kp20_to_24_f = new ColumnParameters(null, "20-24, Female", "gender=F|age=20-24");
        ColumnParameters kp20_to_24_m = new ColumnParameters(null, "20-24, Male", "gender=M|age=20-24");
        ColumnParameters kp25_and_above_f = new ColumnParameters(null, "25+, Female", "gender=F|age=25+");
        ColumnParameters kp25_and_above_m = new ColumnParameters(null, "25+, Male", "gender=M|age=25+");

        //Patient Contacts
        ColumnParameters contacts_under_15_f = new ColumnParameters(null, "<15, Female", "contactGender=F|contactAge=<15");
        ColumnParameters contacts_under_15_m = new ColumnParameters(null, "<15, Male", "contactGender=M|contactAge=<15");
        ColumnParameters contacts_15_and_above_f = new ColumnParameters(null, ">=15, Female", "contactGender=F|contactAge=15+");
        ColumnParameters contacts_15_and_above_m = new ColumnParameters(null, ">=15, Male", "contactGender=M|contactAge=15+");

        ColumnParameters fCInfant = new ColumnParameters(null, "<1, Female", "contactGender=F|contactFineAge=<1");
        ColumnParameters mCInfant = new ColumnParameters(null, "<1, Male", "contactGender=M|contactFineAge=<1");
        ColumnParameters fC1_to4 = new ColumnParameters(null, "1-4, Female", "contactGender=F|contactFineAge=1-4");
        ColumnParameters mC1_to4 = new ColumnParameters(null, "1-4, Male", "contactGender=M|contactFineAge=1-4");
        ColumnParameters fC5_to9 = new ColumnParameters(null, "5-9, Female", "contactGender=F|contactFineAge=5-9");
        ColumnParameters mC5_to9 = new ColumnParameters(null, "5-9, Male", "contactGender=M|contactFineAge=5-9");
        ColumnParameters fC10_to14 = new ColumnParameters(null, "10-14, Female", "contactGender=F|contactFineAge=10-14");
        ColumnParameters mC10_to14 = new ColumnParameters(null, "10-14, Male", "contactGender=M|contactFineAge=10-14");
        ColumnParameters fC15_to19 = new ColumnParameters(null, "15-19, Female", "contactGender=F|contactFineAge=15-19");
        ColumnParameters mC15_to19 = new ColumnParameters(null, "15-19, Male", "contactGender=M|contactFineAge=15-19");
        ColumnParameters fC20_to24 = new ColumnParameters(null, "20-24, Female", "contactGender=F|contactFineAge=20-24");
        ColumnParameters mC20_to24 = new ColumnParameters(null, "20-24, Male", "contactGender=M|contactFineAge=20-24");
        ColumnParameters fC25_to29 = new ColumnParameters(null, "25-29, Female", "contactGender=F|contactFineAge=25-29");
        ColumnParameters mC25_to29 = new ColumnParameters(null, "25-29, Male", "contactGender=M|contactFineAge=25-29");
        ColumnParameters fC30_to34 = new ColumnParameters(null, "30-34, Female", "contactGender=F|contactFineAge=30-34");
        ColumnParameters mC30_to34 = new ColumnParameters(null, "30-34, Male", "contactGender=M|contactFineAge=30-34");
        ColumnParameters fC35_to39 = new ColumnParameters(null, "35-39, Female", "contactGender=F|contactFineAge=35-39");
        ColumnParameters mC35_to39 = new ColumnParameters(null, "35-39, Male", "contactGender=M|contactFineAge=35-39");
        ColumnParameters fC40_to44 = new ColumnParameters(null, "40-44, Female", "contactGender=F|contactFineAge=40-44");
        ColumnParameters mC40_to44 = new ColumnParameters(null, "40-44, Male", "contactGender=M|contactFineAge=40-44");
        ColumnParameters fC45_to49 = new ColumnParameters(null, "45-49, Female", "contactGender=F|contactFineAge=45-49");
        ColumnParameters mC45_to49 = new ColumnParameters(null, "45-49, Male", "contactGender=M|contactFineAge=45-49");
        ColumnParameters fCAbove50 = new ColumnParameters(null, "50+, Female", "contactGender=F|contactFineAge=50+");
        ColumnParameters mCAbove50 = new ColumnParameters(null, "50+, Male", "contactGender=M|contactFineAge=50+");
        //End of patient contacts

        List<ColumnParameters> datimNewAgeDisaggregation =
                Arrays.asList(fInfant, mInfant, f1_to4, m1_to4, f5_to9, m5_to9, f10_to14, m10_to14, f15_to19, m15_to19, f20_to24, m20_to24,
                        f25_to29, m25_to29, f30_to34, m30_to34, f35_to39, m35_to39, f40_to44, m40_to44, f45_to49, m45_to49, fAbove50, mAbove50, colTotal);


        List<ColumnParameters> datimPMTCTANCAgeDisaggregation =
                Arrays.asList(fUnder10, f10_to14, f15_to19, f20_to24, f25_to29, f30_to34, f35_to39, f40_to44, f45_to49, fAbove50, colTotal);

        List<ColumnParameters> datimPMTCTCXCAAgeDisaggregation =
                Arrays.asList(f10_to14, f15_to19, f20_to24, f25_to29, f30_to34, f35_to39, f40_to44, f45_to49, fAbove50, colTotal);

        List<ColumnParameters> datimAgeDisaggregationMonths = Arrays.asList(all0_to_2m, all2_to_12m, colTotal);

        List<ColumnParameters> datimPrEPNewAgeDisaggregation =
                Arrays.asList(f15_to19, m15_to19, f20_to24, m20_to24,
                        f25_to29, m25_to29, f30_to34, m30_to34, f35_to39, m35_to39, f40_to44, m40_to44, f45_to49, m45_to49, fAbove50, mAbove50, colTotal);

        List<ColumnParameters> datimTXTBOnART =
                Arrays.asList(fUnder15, f15AndAbove, mUnder15, m15AndAbove,colTotal);

        List<ColumnParameters>  kpAgeDisaggregation = Arrays.asList(below_15, kp15_to_19, kp20_to_24, kp25_and_above,colTotal);
        List<ColumnParameters> kpAgeGenderDisaggregation = Arrays.asList(below_15_f, below_15_m, kp15_to_19_f, kp15_to_19_m,
                kp20_to_24_f, kp20_to_24_m, kp25_and_above_f, kp25_and_above_m,colTotal);

        List<ColumnParameters> datimGBVDisaggregation =
                Arrays.asList(fUnder10,mUnder10,f10_to14, m10_to14,f15_to19, m15_to19, f20_to24, m20_to24,
                        f25_to29, m25_to29, f30_to34, m30_to34, f35_to39, m35_to39, f40_to44, m40_to44, f45_to49, m45_to49, fAbove50, mAbove50, colTotal);

        //Patient contacts disagggregations
        List<ColumnParameters> contactAgeSexDisaggregation = Arrays.asList(contacts_under_15_f, contacts_under_15_m, contacts_15_and_above_f, contacts_15_and_above_m, colTotal);
        List<ColumnParameters> contactAgeSexFineDisaggregation =
                Arrays.asList(fCInfant, mCInfant, fC1_to4, mC1_to4, fC5_to9, mC5_to9, fC10_to14, mC10_to14, fC15_to19, mC15_to19, fC20_to24, mC20_to24,
                        fC25_to29, mC25_to29, fC30_to34, mC30_to34, fC35_to39, mC35_to39, fC40_to44, mC40_to44, fC45_to49, mC45_to49, fCAbove50, mCAbove50, colTotal);
        //End of patient contact Disaggregations

        String indParams = "startDate=${startDate},endDate=${endDate}";
        String endDateParams = "endDate=${endDate}";

        //Prevention Indicators
        //PrEP_CT
        EmrReportingUtils.addRow(cohortDsd, "PrEP_CT", "People who returned for PrEP follow-up or re-initiation", ReportUtils.map(datimIndicators.prepCT(), indParams), datimPrEPNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17"));

        //Returned for PrEP and tested HIV Positive
        cohortDsd.addColumn("PrEP_CT_HIV_POS", "Returned for PrEP and tested HIV positive", ReportUtils.map(datimIndicators.prepCTByHIVStatus("\"Positive\""), indParams), "");

        //Returned for PrEP and tested HIV Negative
        cohortDsd.addColumn("PrEP_CT_HIV_NEG", "Returned for PrEP and tested HIV Negative", ReportUtils.map(datimIndicators.prepCTByHIVStatus("\"Negative\""), indParams), "");

        //Returned for PrEP and have incloncusive/unknown HIV test result
        cohortDsd.addColumn("PrEP_CT_HIV_OTHER", "Returned for PrEP with unknown test result", ReportUtils.map(datimIndicators.prepCTByHIVStatus("\"Inconclusive\""), indParams), "");

        //PWID KPs who Returned for PrEP
        cohortDsd.addColumn("PrEP_CT_PWID", "PWID KPs who Returned for PrEP", ReportUtils.map(datimIndicators.prepCTKP(mapKPType("PWID", PWID_CONCEPT)), indParams), "");
        //MSM KPs who Returned for PrEP
        cohortDsd.addColumn("PrEP_CT_MSM", "MSM KPs who Returned for PrEP", ReportUtils.map(datimIndicators.prepCTKP(mapKPType("MSM", MSM_CONCEPT)), indParams), "");
        //Transgender KPs who Returned for PrEP
        cohortDsd.addColumn("PrEP_CT_TG", "TG KPs who Returned for PrEP", ReportUtils.map(datimIndicators.prepCTKP(mapKPType("TG", TG_CONCEPT)), indParams), "");
        //FSW KPs who Returned for PrEP
        cohortDsd.addColumn("PrEP_CT_FSW", "FSW KPs who Returned for PrEP", ReportUtils.map(datimIndicators.prepCTKP(mapKPType("FSW", FSW_CONCEPT)), indParams), "");
        //FSW KPs who Returned for PrEP
        cohortDsd.addColumn("PrEP_CT_PRISONS_CLOSED_SETTINGS", "Prisoners and closed settings KPs who Returned for PrEP", ReportUtils.map(datimIndicators.prepCTKP(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //Returned for PrEP and pregnant
        cohortDsd.addColumn("PrEP_CT_PREG", "Returned for PrEP and pregnant", ReportUtils.map(datimIndicators.prepCTPregnant(), indParams), "");

        //Returned for PrEP and breastfeeding
        cohortDsd.addColumn("PrEP_CT_BF", "Returned for PrEP while breastfeeding", ReportUtils.map(datimIndicators.prepCTBreastfeeding(), indParams), "");

        // Number of people newly enrolled on Prep
        EmrReportingUtils.addRow(cohortDsd, "PrEP_NEWLY_ENROLLED", "Number of people newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEP(), indParams), datimPrEPNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17"));

        // Number of KPs newly enrolled on Prep
        cohortDsd.addColumn( "PrEP_NEWLY_ENROLLED_PWID", "Number of PWIDs newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPKP(mapKPType("PWID", PWID_CONCEPT)), indParams),"");
        cohortDsd.addColumn( "PrEP_NEWLY_ENROLLED_MSM", "Number of MSMs newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPKP(mapKPType("MSM", MSM_CONCEPT)), indParams), "");
        cohortDsd.addColumn("PrEP_NEWLY_ENROLLED_TG", "Number of Transgenders newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPKP(mapKPType("TG", TG_CONCEPT)), indParams),"");
        cohortDsd.addColumn("PrEP_NEWLY_ENROLLED_FSW", "Number of FSWs newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPKP(mapKPType("FSW", FSW_CONCEPT)), indParams),"");
        cohortDsd.addColumn("PrEP_NEWLY_ENROLLED_PRISONS_CLOSED_SETTINGS", "Number of prisoners and people in closed settings newly enrolled on Prep", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPKP(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //Newly eonrolled to prep with a recent HIV positive results within 3 months into enrolment
        cohortDsd.addColumn("PrEP_NEWLY_ENROLLED_HIVPOS", "Newly eonrolled to prep with a recent HIV positive results within 3 months into enrolment", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPHIVPos(), indParams), "");

        //Newly eonrolled to prep with a recent HIV negative results within 3 months into enrolment
        cohortDsd.addColumn("PrEP_NEWLY_ENROLLED_HIVNEG", "Newly eonrolled to prep with a recent HIV negative results within 3 months into enrolment", ReportUtils.map(datimIndicators.newlyEnrolledInPrEPHIVNeg(), indParams), "");

        // Proportion of ART patients who started on a standard course of TB Preventive Treatment (TPT) in the previous reporting period who completed therapy
        EmrReportingUtils.addRow(cohortDsd, "TB_PREV_ENROLLED_COMPLETED", "Proportion of ART patients who started on a standard course of TB Preventive Treatment (TPT) in the previous reporting period who completed therapy", ReportUtils.map(datimIndicators.previouslyOnIPTCompleted(), indParams), datimPrEPNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Number of beneficiaries served by PEPFAR OVC programs for children and families affected by HIV
        cohortDsd.addColumn("OVC_SERV_COMP", "Number of beneficiaries served by PEPFAR OVC Comprehensive program", ReportUtils.map(datimIndicators.totalBeneficiaryOfOVCComprehensiveProgram(), indParams), "");
        cohortDsd.addColumn("OVC_SERV_DREAMS", "Number of beneficiaries served by PEPFAR OVC DREAMS program", ReportUtils.map(datimIndicators.totalBeneficiaryOfOVCDreamsProgram(), indParams), "");
        cohortDsd.addColumn("OVC_SERV_PREV", "Number of beneficiaries served by PEPFAR OVC preventive program", ReportUtils.map(datimIndicators.totalBeneficiaryOfOVCPreventiveProgram(), indParams), "");

        //Testing Indicators
        //HTS_INDEX_OFFERED Index services
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_OFFERED", "Indexes offered Index testing services", ReportUtils.map(datimIndicators.offeredIndexServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX_ACCEPTED Index services
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_ACCEPTED", "Indexes who accepted Index testing services", ReportUtils.map(datimIndicators.acceptedIndexServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX_CONTACTS_ELICITED_MALES_UNDER15
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_ELICITED_CONTACTS", "HTS Index Elicited Contacts", ReportUtils.map(datimIndicators.htsIndexContactsElicited(), indParams), contactAgeSexDisaggregation, Arrays.asList("01", "02", "03", "04", "05"));

        //HTS_INDEX New Positives
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_POSITIVE", "Contacts tested HIV Positive", ReportUtils.map(datimIndicators.contactTestedPositive(), indParams), contactAgeSexFineDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX New Negatives
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_NEGATIVE", "Contacts tested HIV Negative", ReportUtils.map(datimIndicators.contactTestedNegative(), indParams), contactAgeSexFineDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX Known Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_KNOWN_POSITIVE", "Contacts Known HIV Positive", ReportUtils.map(datimIndicators.contactKnownPositive(), indParams), contactAgeSexFineDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_TST
        //1. Index Testing
        //Index Tested Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Index_Negative", "Index Tested Negative", ReportUtils.map(datimIndicators.indexTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "25"));

        //Index Tested Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Index_Positive", "Index Tested Positive", ReportUtils.map(datimIndicators.indexTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //2. VCT testing
        //Tested Negative VCT
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_VCT_Negative", "Tested Negative VCT", ReportUtils.map(datimIndicators.testedNegativeVCT(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive VCT
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_VCT_Positive", "Tested Positive VCT", ReportUtils.map(datimIndicators.testedPositiveVCT(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "25"));

        //3. Malnutrition Clinic
        //Tested Negative Malnutrition Clinic
        cohortDsd.addColumn("HTS_TST_Malnutrition_Negative", "Tested Negative Malnutrition Clinic", ReportUtils.map(datimIndicators.testedNegativeMalnutritionClinic(), indParams), "");

        //Tested Positive Malnutrition Clinic
        cohortDsd.addColumn("HTS_TST_Malnutrition_Positive", "Tested Positive Malnutrition Clinic", ReportUtils.map(datimIndicators.testedPositiveMalnutritionClinic(), indParams), "");

        //4. Paediatric Clinics
        //Tested Negative Paediatric services
        cohortDsd.addColumn("HTS_TST_Paediatric_Negative", "Tested Negative Paediatric services", ReportUtils.map(datimIndicators.testedNegativePaediatricServices(), indParams), "");

        //Tested Positive Paediatric services
        cohortDsd.addColumn("HTS_TST_Paediatric_Positive", "Tested Positive Paediatric Services", ReportUtils.map(datimIndicators.testedPositivePaediatricServices(), indParams), "");

        //5. TB Clinics

        //Tested Negative TB Clinic
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_TB_Negative", "Tested Negative TB Clinic", ReportUtils.map(datimIndicators.testedNegativeTBClinic(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive TB Clinic
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_TB_Positive", "Tested Positive TB Clinic", ReportUtils.map(datimIndicators.testedPositiveTBClinic(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //6.PMTCT_ANC-1 Only
        //Tested Negative PMTCT services ANC-1 only
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_ANC1_Negative", "Tested Negative PMTCT at 1st ANC", ReportUtils.map(datimIndicators.testedNegativePMTCTANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Tested Positive PMTCT services ANC-1 only
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_ANC1_Positive", "Tested Positive PMTCT at 1st ANC", ReportUtils.map(datimIndicators.testedPositivePMTCTANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //7.PMTCT [Post ANC1, Preg/L&D/BF]
        //Tested Negative PMTCT services Post ANC-1 (including labour and delivery and BF)
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_POSTANC1_Negative", "Tested Negative PMTCT Post ANC-1 (Incl L&D,BF)", ReportUtils.map(datimIndicators.testedNegativePMTCTPostANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Tested Positive PMTCT services Post ANC-1 (including labour and delivery and BF)
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_POSTANC1_Positive", "Tested Positive PMTCT Post ANC-1 (Incl L&D,BF)", ReportUtils.map(datimIndicators.testedPositivePMTCTPostANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //8.STI
        //9. Inpatient
        //Tested Negative Inpatient Services
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Inpatient_Negative", "Tested Negative Inpatient Services", ReportUtils.map(datimIndicators.testedNegativeInpatientServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive Inpatient Services
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Inpatient_Positive", "Tested Positive Inpatient Services", ReportUtils.map(datimIndicators.testedPositiveInpatientServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //10. Emergency Ward
        //11. VMMC

        //12. Other
        //Tested Negative Other
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Other_Negative", "Tested Negative Other", ReportUtils.map(datimIndicators.testedNegativeOther(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive Other
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Other_Positive", "Tested Positive Other", ReportUtils.map(datimIndicators.testedPositiveOther(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //13. KP
        //PWID Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_PWID_POS", "PWID Tested Positive", ReportUtils.map(datimIndicators.pwidTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //14. Mobile Outreach
        //MO Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_MOBILE_POSITIVE", "Tested Positive Mobile Outreach", ReportUtils.map(datimIndicators.testedPositiveMobile(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //MO Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_MOBILE_NEGATIVE", "Tested Negative Mobile Outreach", ReportUtils.map(datimIndicators.testedNegativeMobile(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //PWID Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_PWID_NEG", "PWID Tested Negative", ReportUtils.map(datimIndicators.pwidTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        //MSM Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_MSM_POS", "MSM Tested Positive", ReportUtils.map(datimIndicators.msmTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //MSM Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_MSM_NEG", "MSM Tested Negative", ReportUtils.map(datimIndicators.msmTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //FSW Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_FSW_POS", "FSW Tested Positive", ReportUtils.map(datimIndicators.fswTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //FSW Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_KP_FSW_NEG", "FSW Tested Negative", ReportUtils.map(datimIndicators.fswTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //PMTCT_STAT
        //Known positive before ANC
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_STAT_KNOWN_POSITIVE", "Positive HIV status before ANC ", ReportUtils.map(datimIndicators.clientsWithPositiveHivStatusBeforeAnc1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //4 HIV Positive at ANC
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_STAT_ANC_Positive", "Tested HIV Positive at ANC", ReportUtils.map(datimIndicators.patientsTestPositiveAtANC(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //4 HIV Negative at ANC
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_STAT_ANC_Negative", "Tested HIV Negative at ANC", ReportUtils.map(datimIndicators.patientsTestNegativeAtANC(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Known Negative before ANC
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_STAT_RECENT_NEGATIVE", "With Negative HIV status before ANC ", ReportUtils.map(datimIndicators.clientsWithNegativeHivStatusBeforeAnc1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Newly enrolled to ANC
        //cohortDsd.addColumn( "PMTCT_STAT_Denominator", "Newly enrolled to ANC", ReportUtils.map(datimIndicators.clientsNewlyEnrolledToANC(), indParams), "");
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_STAT_Denominator", "Newly enrolled to ANC", ReportUtils.map(datimIndicators.clientsNewlyEnrolledToANC(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //PMTCT_EID
        //Infants tested Negative for Virology
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_EID_SampleTaken", "Infants sample taken for Virology test", ReportUtils.map(datimIndicators.infantsSampleTakenForVirology(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //Infants tested Negative for Virology
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_EID_Negative", "Infants tested Negative for Virology", ReportUtils.map(datimIndicators.infantsTestedNegativeForVirology(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //Infants tested Positive for Virology
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_EID_Positive", "Infants tested Positive for Virology", ReportUtils.map(datimIndicators.infantsTestedPositiveForVirology(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //Infant Virology with no results
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_EID_No_Results", "Infants tested for Virology with no results", ReportUtils.map(datimIndicators.infantsTestedForVirologyNoResult(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //PMTCT_HEI_POS
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_HEI_POS", "Infants identified HIV Positive within 12 months after birth", ReportUtils.map(datimIndicators.infantsTurnedHIVPositive(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //PMTCT_HEI_POS_ART
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_HEI_POS_ART", "Infants identified HIV Positive within 12 months after birth and Started ART", ReportUtils.map(datimIndicators.infantsTurnedHIVPositiveOnART(), indParams), datimAgeDisaggregationMonths, Arrays.asList("01", "02", "03"));

        //CXCA_SCRN_NEGATIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_SCRN_NEGATIVE", "HIV Positive women on ART screened Negative for cervical cancer 1st time", ReportUtils.map(datimIndicators.firstTimescreenedCXCANegative(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_SCRN_POSITIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_SCRN_POSITIVE", "HIV Positive women on ART screened Positive for cervical cancer 1st time", ReportUtils.map(datimIndicators.firstTimescreenedCXCAPositive(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_SCRN_PRESUMED
        EmrReportingUtils.addRow(cohortDsd, "CXCA_SCRN_PRESUMED", "Women on ART with Presumed cervical cancer after re-screening", ReportUtils.map(datimIndicators.rescreenedCXCAPresumed(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_RESCRN_NEGATIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_RESCRN_NEGATIVE", "Women on ART re-screened Negative for cervical cancer", ReportUtils.map(datimIndicators.rescreenedCXCANegative(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_RESCRN_POSITIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_RESCRN_POSITIVE", "Women on ART re-screened Positive for cervical cancer", ReportUtils.map(datimIndicators.rescreenedCXCAPositive(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_RESCRN_PRESUMED
        EmrReportingUtils.addRow(cohortDsd, "CXCA_RESCRN_PRESUMED", "HIV Positive women on ART with Presumed cervical cancer 1st time screening", ReportUtils.map(datimIndicators.firstTimescreenedCXCAPresumed(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_TX_NEGATIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_TX_NEGATIVE", "Women on ART and Cx treatment screened Negative for cervical cancer", ReportUtils.map(datimIndicators.postTreatmentscreenedCXCANegative(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_TX_POSITIVE
        EmrReportingUtils.addRow(cohortDsd, "CXCA_TX_POSITIVE", "Women on ART and Cx treatment screened Positive for cervical cancer", ReportUtils.map(datimIndicators.postTreatmentscreenedCXCAPositive(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //CXCA_TX_PRESUMED
        EmrReportingUtils.addRow(cohortDsd, "CXCA_TX_PRESUMED", "Women on ART and Cx treatment screened presumed for cervical cancer", ReportUtils.map(datimIndicators.postTreatmentscreenedCXCAPresumed(), indParams), datimPMTCTCXCAAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10"));

        //Number of OVC current on ART reported to implementing partner
        cohortDsd.addColumn("OVC_HIVSTAT_ONART", "Number of OVC Current on ART reported to implementing partner", ReportUtils.map(datimIndicators.ovcOnART(), indParams), "");

        //Number of OVC Not on ART reported to implementing partner
        cohortDsd.addColumn("OVC_HIVSTAT_NOT_ON_ART", "Number of OVC not on ART reported to implementing partner", ReportUtils.map(datimIndicators.ovcNotOnART(), indParams), "");
        //PMTCT_FO
        //HEI Cohort HIV infected
        cohortDsd.addColumn("PMTCT_FO_INFECTED_HEI", "HEI Cohort HIV+", ReportUtils.map(datimIndicators.hivInfectedHEI(), indParams), "");

        //HEI Cohort HIV uninfected
        cohortDsd.addColumn("PMTCT_FO_UNINFECTED_HEI", "HEI Cohort HIV-", ReportUtils.map(datimIndicators.hivUninfectedHEI(), indParams), "");

        //HEI Cohort HIV-final status unknown
        cohortDsd.addColumn("PMTCT_FO_HEI_UNKNOWN_HIV_STATUS", "HEI Cohort with unknown HIV Status", ReportUtils.map(datimIndicators.unknownHIVStatusHEI(), indParams), "");

        //HEI died with HIV-final status unknown
        cohortDsd.addColumn("PMTCT_FO_HEI_DIED_HIV_STATUS_UNKNOWN", "HEI died with unknown HIV Status", ReportUtils.map(datimIndicators.heiDiedWithunknownHIVStatus(), indParams), "");

        //Treatment Indicators
        //TX_New
        //Disaggregated by Age / Sex
        EmrReportingUtils.addRow(cohortDsd, "TX_New", "Newly Started ART", ReportUtils.map(datimIndicators.newlyStartedARTByAgeSex(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Newly Started ART While BreastFeeding
        cohortDsd.addColumn("TX_New_BF", "Newly Started ART While Breastfeeding", ReportUtils.map(datimIndicators.newlyStartedARTWhileBF(), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - PWID
        cohortDsd.addColumn("TX_NEW_PWID", "PWID with HIV new on ART", ReportUtils.map(datimIndicators.kpNewlyStartedART(mapKPType("PWID", PWID_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - MSM
        cohortDsd.addColumn("TX_NEW_MSM", "MSM with HIV new on ART", ReportUtils.map(datimIndicators.kpNewlyStartedART(mapKPType("MSM", MSM_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - TG
        cohortDsd.addColumn("TX_NEW_TG", "Transgenders with HIV new on ART", ReportUtils.map(datimIndicators.kpNewlyStartedART(mapKPType("TG", TG_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - FSW
        cohortDsd.addColumn("TX_NEW_FSW", "FSW with HIV new on ART", ReportUtils.map(datimIndicators.kpNewlyStartedART(mapKPType("FSW", FSW_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - TX_NEW_PRISONS_CLOSED_SETTINGS
        cohortDsd.addColumn("TX_NEW_PRISONS_CLOSED_SETTINGS", "Prisoners and Closed settings with HIV new on ART", ReportUtils.map(datimIndicators.kpNewlyStartedART(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //TX_CURR

        //Number of Adults and Children with HIV infection receiving ART By Age/Sex Disagreggation
        EmrReportingUtils.addRow(cohortDsd, "TX_CURR", "Adults and Children with HIV infection receiving ART", ReportUtils.map(datimIndicators.currentlyOnArt(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Number of Pregnant women with HIV infection receiving antiretroviral therapy (ART)
        cohortDsd.addColumn("TX_CURR_PREGNANT", "Pregnant women with HIV receiving ART", ReportUtils.map(datimIndicators.pregnantCurrentlyOnART(), indParams), "");

        //Number of Breastfeeding mothers with HIV infection receiving antiretroviral therapy (ART)
        cohortDsd.addColumn("TX_CURR_BF", "Breast Feeding mothers with HIV receiving ART", ReportUtils.map(datimIndicators.bfMothersCurrentlyOnART(), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - PWID
        cohortDsd.addColumn("TX_CURR_PWID", "PWID with HIV receiving ART", ReportUtils.map(datimIndicators.kpCurrentlyOnART(mapKPType("PWID", PWID_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - MSM
        cohortDsd.addColumn("TX_CURR_MSM", "MSM with HIV receiving ART", ReportUtils.map(datimIndicators.kpCurrentlyOnART(mapKPType("MSM", MSM_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - TG
        cohortDsd.addColumn("TX_CURR_TG", "Transgenders with HIV receiving ART", ReportUtils.map(datimIndicators.kpCurrentlyOnART(mapKPType("TG", TG_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - FSW
        cohortDsd.addColumn("TX_CURR_FSW", "FSW with HIV receiving ART", ReportUtils.map(datimIndicators.kpCurrentlyOnART(mapKPType("FSW", FSW_CONCEPT)), indParams), "");

        //Number of Adults with HIV infection receiving ART By KP Type Disagreggation - TX_CURR_PRISONS_CLOSED_SETTINGS
        cohortDsd.addColumn("TX_CURR_PRISONS_CLOSED_SETTINGS", "Prisoners and Closed settings with HIV receiving ART", ReportUtils.map(datimIndicators.kpCurrentlyOnART(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //One month before next appointment
        EmrReportingUtils.addRow(cohortDsd, "TX_CURR_MMD_BELOW_3_MONTHS", "Less than 3 months drugs", ReportUtils.map(datimIndicators.currentlyOnARTMMD(durationMapper("Under 3 Months", "< 3")), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //two month before next appointment
        EmrReportingUtils.addRow(cohortDsd, "TX_CURR_MMD_3TO5_MONTHS", "3-5 months drugs", ReportUtils.map(datimIndicators.currentlyOnARTMMD(durationMapper("3-5 Months", "between 3 and 5")), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //three month before next appointment
        EmrReportingUtils.addRow(cohortDsd, "TX_CURR_MMD_6+_MONTHS", "Over 6 months drugs", ReportUtils.map(datimIndicators.currentlyOnARTMMD(durationMapper("6+ Months", ">= 6")), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

//PMTCT_ART

        //Mothers new on ART during current pregnancy
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_ART_New", "Mothers new on ART during current pregnancy", ReportUtils.map(datimIndicators.mothersNewOnARTDuringCurrentPregnancy(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Mothers already on ART at start of current pregnancy
        EmrReportingUtils.addRow(cohortDsd, "PMTCT_ART_Already", "Number of Mothers Already on ART at the start of current Pregnancy", ReportUtils.map(datimIndicators.mothersAlreadyOnARTAtStartOfCurrentPregnancy(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        // TB_ART Proportion of HIV-positive new and relapsed TB cases New on ART during TB treatment
        EmrReportingUtils.addRow(cohortDsd, "TB_ART_NEW_ON_ART", "TB Patients New on ART ", ReportUtils.map(datimIndicators.newOnARTTBInfected(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        // TB_ART Proportion of HIV-positive new and relapsed TB cases already on ART during TB treatment
        EmrReportingUtils.addRow(cohortDsd, "TB_ART_ALREADY_ON_ART", "TB patients already on ART", ReportUtils.map(datimIndicators.alreadyOnARTTBInfected(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_TB
        //Numerator_new_on_art
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_NUM_NEW_ON_ART", "Starting TB treatment newly started ART", ReportUtils.map(datimIndicators.startingTBTreatmentNewOnART(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //Numerator_Prev_on_art
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_NUM_PREV_ON_ART", "Starting TB treatment previously on ART", ReportUtils.map(datimIndicators.startingTBTreatmentPrevOnART(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //TX_TB(Denominator)
        //TX_TB_NEW_ON_ART_SCREENED_POSITIVE
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_NEW_ON_ART_SCREENED_POSITIVE", "New on ART Screened Positive", ReportUtils.map(datimIndicators.newOnARTScreenedPositive(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //TX_TB_PREVIOUSLY_ON_ART_SCREENED_POSITIVE
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_PREV_ON_ART_SCREENED_POSITIVE", "Previously on ART Screened Positive", ReportUtils.map(datimIndicators.prevOnARTScreenedPositive(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //TX_TB_NEW_ON_ART_SCREENED_NEGATIVE
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_NEW_ON_ART_SCREENED_NEGATIVE", "New on ART Screened Negative", ReportUtils.map(datimIndicators.newOnARTScreenedNegative(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //TX_TB_PREVIOUSLY_ON_ART_SCREENED_NEGATIVE
        EmrReportingUtils.addRow(cohortDsd, "TX_TB_PREV_ON_ART_SCREENED_NEGATIVE", "Previously on ART Screened Negative", ReportUtils.map(datimIndicators.prevOnARTScreenedNegative(), indParams), datimTXTBOnART, Arrays.asList("01", "02", "03", "04", "05"));

        //TX_TB_SPECIMEN_SENT
        cohortDsd.addColumn("TX_TB_SPECIMEN_SENT", "Specimen sent for bacteriologic diagnosis of active TB", ReportUtils.map(datimIndicators.specimenSent(), indParams), "");

        //TX_TB_GeneXpert MTB/RIF assay (with or without other testing)
        cohortDsd.addColumn("TX_TB_GeneXpert", "GeneXpert MTB/RIF assay (with or without other testing)", ReportUtils.map(datimIndicators.geneXpertMTBRIF(), indParams), "");

        //TX_TB_SMEAR_MICROSCOPY_ONLY
        cohortDsd.addColumn( "TX_TB_SMEAR_MICROSCOPY_ONLY", "Smear microscopy only", ReportUtils.map(datimIndicators.smearMicroscopy(), indParams), "");

        //TX_TB_ADDITIONAL_TESTS (other than GeneXpert)
        cohortDsd.addColumn( "TX_TB_ADDITIONAL_TESTS", "Additional test other than GeneXpert", ReportUtils.map(datimIndicators.additionalTBTests(), indParams),"");

        //TX_TB_POSITIVE_RESULT_RETURNED
        cohortDsd.addColumn( "TX_TB_POSITIVE_RESULT_RETURNED", "Positive result returned for bacteriologic diagnosis of active TB", ReportUtils.map(datimIndicators.resultsReturned(), indParams), "");

     //TX_ML
        //TX_ML_DIED Number of ART patients with no clinical contact since their last expected contact due to Death (confirmed)
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_DIED", "ART patients with missed appointment due to death", ReportUtils.map(datimIndicators.txmlPatientByTXStopReason(mapTxStopReason("DIED",DEATH)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        /*   //TX_ML LTFU ON DRUGS <3 MONTHS Number of ART patients with no clinical contact since their last expected contact and have been on drugs for less than 3 months
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIIT(durationMapper("Under 3 Months", "< 3")), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_ML LTFU ON DRUGS 3-5 MONTHS Number of ART patients with no clinical contact since their last expected contact and have been on drugs for 3-5 months
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIIT(durationMapper("3-5 Months", "between 3 and 5")), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_ML LTFU ON DRUGS >6 MONTHS Number of ART patients with no clinical contact since their last expected contact and have been on drugs for 3-5 months
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIIT(durationMapper("6+ Months", ">= 6")), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_ML_PREV_UNDOCUMENTED_TRF Number of ART patients with no clinical contact since their last expected contact due to Previously undocumented transfer
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_TRF_OUT", "ART patients with missed appointment due to transfer out", ReportUtils.map(datimIndicators.txmlPatientByTXStopReason(mapTxStopReason("TRF OUT",TRF_OUT)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_ML_STOPPED_TREATMENT Number of ART patients with no clinical contact since their last expected contact because they stopped treatment
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_STOPPED_TREATMENT", "ART patients with missed appointment because they stopped treatment", ReportUtils.map(datimIndicators.txmlPatientByTXStopReason(mapTxStopReason("STOPPED_TX",STOPPED_TX)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_ML_KPs who died
        cohortDsd.addColumn( "TX_ML_PWID_DIED", "PWID KPs who died", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PWID", PWID_CONCEPT),mapTxStopReason("DIED",DEATH)), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_DIED", "MSM KPs TXML who died", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("MSM", MSM_CONCEPT),mapTxStopReason("DIED",DEATH)), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_DIED", "TG KPs TXML who died", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("TG", TG_CONCEPT),mapTxStopReason("DIED",DEATH)), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_DIED", "FSW KPs TXML who died", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("FSW", FSW_CONCEPT),mapTxStopReason("DIED",DEATH)), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_DIED", "Prisoners KPs TXML who died", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),mapTxStopReason("DIED",DEATH)), indParams),"");
        //TX_ML  KPs IIT < 3 MONTHS
        cohortDsd.addColumn( "TX_ML_PWID_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PWID", PWID_CONCEPT),durationMapper("Under 3 Months", "< 3")), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("MSM", MSM_CONCEPT),durationMapper("Under 3 Months", "< 3")), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("TG", TG_CONCEPT),durationMapper("Under 3 Months", "< 3")), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("FSW", FSW_CONCEPT),durationMapper("Under 3 Months", "< 3")), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_IIT_UNDER_3_MONTHS", "IIT After being on Treatment for <3 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),durationMapper("Under 3 Months", "< 3")), indParams),"");

        //TX_ML  KPs IIT 3-5 MONTHS
        cohortDsd.addColumn( "TX_ML_PWID_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PWID", PWID_CONCEPT),durationMapper("3-5 Months", "between 3 and 5")), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("MSM", MSM_CONCEPT),durationMapper("3-5 Months", "between 3 and 5")), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("TG", TG_CONCEPT),durationMapper("3-5 Months", "between 3 and 5")), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("FSW", FSW_CONCEPT),durationMapper("3-5 Months", "between 3 and 5")), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_IIT_3_TO_5_MONTHS", "IIT After being on Treatment for 3-5 months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),durationMapper("3-5 Months", "between 3 and 5")), indParams),"");

        //TX_ML  KPs IIT 6+ MONTHS
        cohortDsd.addColumn( "TX_ML_PWID_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PWID", PWID_CONCEPT),durationMapper("6+ Months", ">= 6")), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("MSM", MSM_CONCEPT),durationMapper("6+ Months", ">= 6")), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("TG", TG_CONCEPT),durationMapper("6+ Months", ">= 6")), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("FSW", FSW_CONCEPT),durationMapper("6+ Months", ">= 6")), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_IIT_6_MONTHS_AND_ABOVE", "IIT After being on Treatment for 6+ months", ReportUtils.map(datimIndicators.txMLIITKp(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),durationMapper("6+ Months", ">= 6")), indParams),"");

        //TX_ML  KPs Transferred out
        cohortDsd.addColumn( "TX_ML_PWID_TOUT", "Transferred Out", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PWID", PWID_CONCEPT),mapTxStopReason("DIED",TRF_OUT)), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_TOUT", "Transferred Out", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("MSM", MSM_CONCEPT),mapTxStopReason("DIED",TRF_OUT)), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_TOUT", "Transferred Out", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("TG", TG_CONCEPT),mapTxStopReason("DIED",TRF_OUT)), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_TOUT", "Transferred Out", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("FSW", FSW_CONCEPT),mapTxStopReason("DIED",TRF_OUT)), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_TOUT", "Transferred Out", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),mapTxStopReason("DIED",TRF_OUT)), indParams),"");

        //TX_ML KPs Transferred out
        cohortDsd.addColumn( "TX_ML_PWID_STOPPED_TX", "Refused (Stopped) Treatment", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PWID", PWID_CONCEPT),mapTxStopReason("DIED",STOPPED_TX)), indParams),"");
        cohortDsd.addColumn("TX_ML_MSM_STOPPED_TX", "Refused (Stopped) Treatment", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("MSM", MSM_CONCEPT),mapTxStopReason("DIED",STOPPED_TX)), indParams), "");
        cohortDsd.addColumn( "TX_ML_TG_STOPPED_TX", "Refused (Stopped) Treatment", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("TG", TG_CONCEPT),mapTxStopReason("DIED",STOPPED_TX)), indParams), "");
        cohortDsd.addColumn("TX_ML_FSW_STOPPED_TX", "Refused (Stopped) Treatment", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("FSW", FSW_CONCEPT),mapTxStopReason("DIED",STOPPED_TX)), indParams), "");
        cohortDsd.addColumn( "TX_ML_PRISONS_CLOSED_SETTINGS_STOPPED_TX", "Refused (Stopped) Treatment", ReportUtils.map(datimIndicators.txmlKPStopReason(mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT),mapTxStopReason("DIED",STOPPED_TX)), indParams),"");

        //TX_ML Cause of death
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_HIV_TB", "HIV disease resulting in TB", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("HIV DISEASE RESULTING IN TB",HIV_DISEASE_RESULTING_IN_TB)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_HIV_CANCER", "HIV disease resulting in Cancer", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("HIV DISEASE RESULTING IN CANCER",HIV_DISEASE_RESULTING_IN_CANCER)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_HIV_INFECTIOUS_PARASITIC", "HIV disease resulting in other infectious and parasitic disease", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("HIV DISEASE RESULTING IN INFECTIOUS PARASITIC DISEASE",HIV_DISEASE_RESULTING_IN_INFECTIOUS_PARASITIC_DISEASE)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_HIV_OTHER", "Other HIV disease, resulting in other diseases or conditions leading to death", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("OTHER HIV DISEASE",OTHER_HIV_DISEASE)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_OTHER_NATURAL", "Other natural causes", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("OTHER NATURAL CAUSES",OTHER_NATURAL_CAUSES)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_NON_NATURAL", "Non-natural causes", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("NON NATURAL CAUSES",NON_NATURAL_CAUSES)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_UNKNOWN", "Unknown Cause", ReportUtils.map(datimIndicators.txMLCauseOfDeath(mapTxStopReason("UNKNOWN CAUSE",UNKNOWN_CAUSE)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        EmrReportingUtils.addRow(cohortDsd, "TX_ML_COVID19", "Covid-19", ReportUtils.map(datimIndicators.txMLSpecificCauseOfDeath(mapTxStopReason("COVID-19",COVID_19)), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
*/
        //TX_RTT Number of ART patients with no clinical contact (or ARV drug pick-up) for greater than 30 days since their last expected contact who restarted ARVs within the reporting period
        EmrReportingUtils.addRow(cohortDsd, "TX_RTT", "Number restarted Treatment during the reporting period", ReportUtils.map(datimIndicators.txRTT(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //90-90-90 Viral Suppression
        //TX_PVLS Number of patients on ART with Routine VL results within the past 12 months
        EmrReportingUtils.addRow(cohortDsd, "TX_PVLS_DENOMINATOR", "On ART with current VL results", ReportUtils.map(datimIndicators.onARTWithVLLast12Months(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_PVLS Number of adults and pediatric patients on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months disaggregated by Age/Sex
        EmrReportingUtils.addRow(cohortDsd, "TX_PVLS_SUPP_ROUTINE", "On ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.onARTSuppVLAgeSex("(\"ROUTINE\")"), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_PVLS Number of adults and pediatric patients on ART with suppressed targeted viral load results (<1,000 copies/ml) within the past 12 months disaggregated by Age/Sex
        EmrReportingUtils.addRow(cohortDsd, "TX_PVLS_SUPP_TARGETED", "On ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.onARTSuppVLAgeSex("(\"STAT\")"), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //TX_PVLS Number pregnant or breastfeeding patients on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        EmrReportingUtils.addRow(cohortDsd, "TX_PVLS_SUPP_PG/BF_ROUTINE", "Pregnant or Breastfeeding on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.pregnantAndBFOnARTWithSuppressedVLLast12Months("(\"ROUTINE\")"), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //TX_PVLS Number pregnant or breastfeeding patients on ART with suppressed targeted viral load results (<1,000 copies/ml) within the past 12 months
        EmrReportingUtils.addRow(cohortDsd, "TX_PVLS_SUPP_PG/BF_TARGETED", "Pregnant or Breastfeeding on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.pregnantAndBFOnARTWithSuppressedVLLast12Months("(\"STAT\")"), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //TX_PVLS Number of adults and pediatric patients on ART with suppressed viral load results (<1,000 copies/ml) within the past 12 months. Disaggregated by Pregnant / undocumented
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_PWID_ROUTINE", "PWID on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"ROUTINE\")",mapKPType("PWID", PWID_CONCEPT)), indParams), "");

        //TX_PVLS Number of MSM KPs on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_MSM_ROUTINE", "MSM on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"ROUTINE\")",mapKPType("MSM", MSM_CONCEPT)), indParams), "");

        //TX_PVLS Number of Transgender KPs on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_TG_ROUTINE", "Transgender on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"ROUTINE\")",mapKPType("TG", TG_CONCEPT)), indParams), "");

        //TX_PVLS Number of FSW KPs on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_FSW_ROUTINE", "FSW on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"ROUTINE\")",mapKPType("FSW", FSW_CONCEPT)), indParams), "");

        //TX_PVLS Number of prisoners and people in closed settings KPs on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_PRISONS_CLOSED_SETTINGS_ROUTINE", "Prisoners and People in closed settings on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"ROUTINE\")",mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //TX_PVLS Number of PWID KPs on ART with suppressed targeted viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_PWID_TARGETED", "PWID on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"STAT\")",mapKPType("PWID", PWID_CONCEPT)), indParams), "");

        //TX_PVLS Number of MSM KPs on ART with suppressed targeted viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_MSM_TARGETED", "MSM on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"STAT\")",mapKPType("MSM", MSM_CONCEPT)), indParams), "");

        //TX_PVLS Number of Transgender KPs on ART with targeted suppressed viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_TG_TARGETED", "Transgender on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"STAT\")",mapKPType("TG", TG_CONCEPT)), indParams), "");

        //TX_PVLS Number of FSW KPs on ART with targeted suppressed VL results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_FSW_TARGETED", "FSW on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"STAT\")",mapKPType("FSW", FSW_CONCEPT)), indParams), "");

        //TX_PVLS Number of prisoners and people in closed settings KPs on ART with suppressed routine viral load results (<1,000 copies/ml) within the past 12 months
        cohortDsd.addColumn("TX_PVLS_SUPP_KP_PRISONS_CLOSED_SETTINGS_TARGETED", "Prisoners and People in closed settings on ART with current suppressed VL results (<1,000 copies/ml)", ReportUtils.map(datimIndicators.kpOnARTSuppVLLast12Months("(\"STAT\")",mapKPType("PRISONERS_CLOSED_SETTINGS", PRISONERS_CLOSED_SETTINGS_CONCEPT)), indParams), "");

        //HTS_INDEX_OFFERED Index services
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_OFFERED", "Indexes offered Index testing services", ReportUtils.map(datimIndicators.offeredIndexServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX_ACCEPTED Index services
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_ACCEPTED", "Indexes who accepted Index testing services", ReportUtils.map(datimIndicators.acceptedIndexServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX New Positives
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_POSITIVE", "Contacts tested HIV Positive", ReportUtils.map(datimIndicators.contactTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX New Negatives
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_NEGATIVE", "Contacts tested HIV Negative", ReportUtils.map(datimIndicators.contactTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_INDEX Known Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_INDEX_KNOWN_POSITIVE", "Contacts Known HIV Positive", ReportUtils.map(datimIndicators.contactKnownPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //HTS_TST
        //1. Index Testing
        //Index Tested Negative
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Index_Negative", "Index Tested Negative", ReportUtils.map(datimIndicators.indexTestedNegative(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "25"));

        //Index Tested Positive
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Index_Positive", "Index Tested Positive", ReportUtils.map(datimIndicators.indexTestedPositive(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //2. VCT testing
        //Tested Negative VCT
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_VCT_Negative", "Tested Negative VCT", ReportUtils.map(datimIndicators.testedNegativeVCT(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive VCT
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_VCT_Positive", "Tested Positive VCT", ReportUtils.map(datimIndicators.testedPositiveVCT(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "25"));

        //3. Malnutrition Clinic
        //Tested Negative Malnutrition Clinic
        cohortDsd.addColumn("HTS_TST_Malnutrition_Negative", "Tested Negative Malnutrition Clinic", ReportUtils.map(datimIndicators.testedNegativeMalnutritionClinic(), indParams), "");

        //Tested Positive Malnutrition Clinic
        cohortDsd.addColumn("HTS_TST_Malnutrition_Positive", "Tested Positive Malnutrition Clinic", ReportUtils.map(datimIndicators.testedPositiveMalnutritionClinic(), indParams), "");

        //4. Paediatric Clinics
        //Tested Negative Paediatric services
        cohortDsd.addColumn("HTS_TST_Paediatric_Negative", "Tested Negative Paediatric services", ReportUtils.map(datimIndicators.testedNegativePaediatricServices(), indParams), "");

        //Tested Positive Paediatric services
        cohortDsd.addColumn("HTS_TST_Paediatric_Positive", "Tested Positive Paediatric Services", ReportUtils.map(datimIndicators.testedPositivePaediatricServices(), indParams), "");

        //5. TB Clinics

        //Tested Negative TB Clinic
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_TB_Negative", "Tested Negative TB Clinic", ReportUtils.map(datimIndicators.testedNegativeTBClinic(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive TB Clinic
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_TB_Positive", "Tested Positive TB Clinic", ReportUtils.map(datimIndicators.testedPositiveTBClinic(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //6.PMTCT_ANC-1 Only
        //Tested Negative PMTCT services ANC-1 only
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_ANC1_Negative", "Tested Negative PMTCT at 1st ANC", ReportUtils.map(datimIndicators.testedNegativePMTCTANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Tested Positive PMTCT services ANC-1 only
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_ANC1_Positive", "Tested Positive PMTCT at 1st ANC", ReportUtils.map(datimIndicators.testedPositivePMTCTANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //7.PMTCT [Post ANC1, Preg/L&D/BF]
        //Tested Negative PMTCT services Post ANC-1 (including labour and delivery and BF)
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_POSTANC1_Negative", "Tested Negative PMTCT Post ANC-1 (Incl L&D,BF)", ReportUtils.map(datimIndicators.testedNegativePMTCTPostANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //Tested Positive PMTCT services Post ANC-1 (including labour and delivery and BF)
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_PMTCT_POSTANC1_Positive", "Tested Positive PMTCT Post ANC-1 (Incl L&D,BF)", ReportUtils.map(datimIndicators.testedPositivePMTCTPostANC1(), indParams), datimPMTCTANCAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"));

        //8.STI
        //9. Inpatient
        //Tested Negative Inpatient Services
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Inpatient_Negative", "Tested Negative Inpatient Services", ReportUtils.map(datimIndicators.testedNegativeInpatientServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive Inpatient Services
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Inpatient_Positive", "Tested Positive Inpatient Services", ReportUtils.map(datimIndicators.testedPositiveInpatientServices(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //10. Emergency Ward
        //11. VMMC

        //12. Other
        //Tested Negative Other
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Other_Negative", "Tested Negative Other", ReportUtils.map(datimIndicators.testedNegativeOther(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        //Tested Positive Other
        EmrReportingUtils.addRow(cohortDsd, "HTS_TST_Other_Positive", "Tested Positive Other", ReportUtils.map(datimIndicators.testedPositiveOther(), indParams), datimNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));

        // Proportion of ART patients who started on a standard course of TB Preventive Treatment (TPT) in the previous reporting period who completed therapy
        EmrReportingUtils.addRow(cohortDsd, "TB_PREV_ENROLLED_COMPLETED", "Proportion of ART patients who started on a standard course of TB Preventive Treatment (TPT) in the previous reporting period who completed therapy", ReportUtils.map(datimIndicators.previouslyOnIPTCompleted(), indParams), datimPrEPNewAgeDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25"));
        //cohortDsd.addColumn("HTS_TST_Malnutrition_Negative", "Tested Negative Malnutrition Clinic", ReportUtils.map(datimIndicators.testedNegativeMalnutritionClinic(), indParams), "");
        //3. KP_PREV
        cohortDsd.addColumn( "KP_PREV_PWID", "Received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrev("(\"PWID\")"), indParams), "");
        cohortDsd.addColumn( "KP_PREV_MSM", "Received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrev("(\"MSM\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_TG", "Received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrev("(\"Transgender\")"), indParams),"");
        cohortDsd.addColumn("KP_PREV_FSW", "Received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrev("(\"FSW\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_PRISONS_CLOSED_SETTINGS", "Received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrev("(\"People in prison and other closed settings\")"), indParams), "");

      //KP_PREV_KNOWN_POSITIVE
        cohortDsd.addColumn( "KP_PREV_PWID_KNOWN_POSITIVE", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevKnownPositive("(\"PWID\")"), indParams), "");
        cohortDsd.addColumn( "KP_PREV_MSM_KNOWN_POSITIVE", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevKnownPositive("(\"MSM\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_TG_KNOWN_POSITIVE", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevKnownPositive("(\"Transgender\")"), indParams),"");
        cohortDsd.addColumn("KP_PREV_FSW_KNOWN_POSITIVE", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevKnownPositive("(\"FSW\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_PRISONS_CLOSED_SETTINGS_KNOWN_POSITIVE", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevKnownPositive("(\"People in prison and other closed settings\")"), indParams), "");

        //KP_PREV_NEWLY_TESTED
        cohortDsd.addColumn( "KP_PREV_PWID_NEWLY_TESTED", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevNewlyTested("(\"PWID\")"), indParams), "");
        cohortDsd.addColumn( "KP_PREV_MSM_NEWLY_TESTED", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevNewlyTested("(\"MSM\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_TG_NEWLY_TESTED", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevNewlyTested("(\"Transgender\")"), indParams),"");
        cohortDsd.addColumn("KP_PREV_FSW_NEWLY_TESTED", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevNewlyTested("(\"FSW\")"), indParams), "");
        cohortDsd.addColumn("KP_PREV_PRISONS_CLOSED_SETTINGS_NEWLY_TESTED", "Known Positive KPS received care for the first time this year",
                ReportUtils.map(datimIndicators.kpPrevNewlyTested("(\"People in prison and other closed settings\")"), indParams), "");
        /*GEND_GBV
        Number of people receiving post-gender-based violence (GBV) clinical care based on the minimum package*/
        //1. Sexual violence (post-rape care)
        EmrReportingUtils.addRow(cohortDsd, "GEND_GBV_SEXUAL_VIOLENCE", "Received Sexual violence (post-rape) care", ReportUtils.map(datimIndicators.sexualGBV(), indParams), datimGBVDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"));

        //2. Physical and/or emotional violence (other Post-GBV care)
        EmrReportingUtils.addRow(cohortDsd, "GEND_GBV_PHY_EMOTIONAL_VIOLENCE", "Received physical and emotional violence care", ReportUtils.map(datimIndicators.physicalEmotionalGBV(), indParams), datimGBVDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"));

        //3. Number of People Receiving Post-exposure prophylaxis (PEP) Services. Disaggregate of the Sexual Violence Service Type
        EmrReportingUtils.addRow(cohortDsd, "GEND_GBV_PEP", "Received Post-exposure prophylaxis (PEP) Services", ReportUtils.map(datimIndicators.receivedPEP(), indParams), datimGBVDisaggregation, Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21"));
        return cohortDsd;
    }

    private KPTypeDataDefinition mapKPType(String name, Integer concept) {
        KPTypeDataDefinition kpType = new KPTypeDataDefinition(name, concept);
        return kpType;
    }

    private DurationToNextAppointmentDataDefinition durationMapper(String name, String duration) {
        DurationToNextAppointmentDataDefinition durationTonextVisit = new DurationToNextAppointmentDataDefinition(name, duration);
        return durationTonextVisit;
    }
    private TreatmentStopReasonDataDefinition mapTxStopReason(String name, Integer concept) {
        TreatmentStopReasonDataDefinition txStopReason = new TreatmentStopReasonDataDefinition(name, concept);
        return txStopReason;
    }
}


