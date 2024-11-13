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

import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyaemr.calculation.library.hiv.CountyAddressCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.SubCountyAddressCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.ComplaintCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.IllCasesCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.LabsCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.dmi.VitalSignsCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.CalculationResultConverter;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.MFLCodeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.art.AgeAtReportingDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.art.ETLNextAppointmentDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.ComplainCaseUniqueIdDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.ComplainDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.ComplainDurationDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.ComplainOnsetDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.DiagnosisCaseUniqueIdDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.DiagnosisDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.DiagnosisDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.LabsCaseUniqueIDDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.LabsOrderIDDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.LabsTestDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.LabsTestNameDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.LabsTestResultsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.VisitTypeWithComplaintsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.AdmissionDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.CaseUniqueIdDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.DateCreatedDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.DateUpdatedDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.FinalPatientManagementOutcomeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.FinalPatientManagementOutcomeDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.InterviewDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.dmi.casereport.OutpatientDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.opd.OPDOxygenSaturationDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.opd.OPDRespiratoryRateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.opd.OPDTemperatureDataDefinition;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.DateConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDatetimeDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ConvertedPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonIdDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.EncounterDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.VisitDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({"kenyaemr.common.report.caseReports"})
public class CaseReportsListReportBuilder extends AbstractReportBuilder {

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    @Override
    protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
        return Arrays.asList(new Parameter("startDate", "Start Date", Date.class), new Parameter("endDate", "End Date",
                Date.class), new Parameter("dateBasedReporting", "", String.class));
    }

    @Override
    protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
        return Arrays.asList(
                ReportUtils.map(illnessCasesDataSetDefinitionColumns(), "startDate=${startDate},endDate=${endDate}")/*,
                ReportUtils.map(labsDataSetDefinitionColumns(),"startDate=${startDate},endDate=${endDate}"),
                ReportUtils.map(diagnosisDataSetDefinitionColumns(),"startDate=${startDate},endDate=${endDate}"),
                ReportUtils.map(complaintDataSetDefinitionColumns(),"startDate=${startDate},endDate=${endDate}"),
                ReportUtils.map(vitalSignsDataSetDefinitionColumns(),"startDate=${startDate},endDate=${endDate}")*/
        );
    }

    protected DataSetDefinition illnessCasesDataSetDefinitionColumns() {
        VisitDataSetDefinition dsd = new VisitDataSetDefinition();
        dsd.setName("illness");
        dsd.setDescription("Report Cases information");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        String paramMapping = "startDate=${startDate},endDate=${endDate}";

        PatientIdentifierType openmrsId = MetadataUtils.existing(PatientIdentifierType.class,
                CommonMetadata._PatientIdentifierType.OPENMRS_ID);
        PatientIdentifierType nupi = MetadataUtils.existing(PatientIdentifierType.class,
                CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
        DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
        DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
                openmrsId.getName(), openmrsId), identifierFormatter);
        DataDefinition nupiDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
                nupi.getName(), nupi), identifierFormatter);
        AgeAtReportingDataDefinition ageAtReportingDataDefinition = new AgeAtReportingDataDefinition();
        ageAtReportingDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

    /*    ETLNextAppointmentDateDataDefinition lastAppointmentDateDataDefinition = new ETLNextAppointmentDateDataDefinition();
        lastAppointmentDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        lastAppointmentDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        VisitTypeWithComplaintsDataDefinition visitTypeDataDefinition = new VisitTypeWithComplaintsDataDefinition();
        visitTypeDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        visitTypeDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
*/
        CaseUniqueIdDataDefinition caseUniqueIdDataDefinition = new CaseUniqueIdDataDefinition();
        caseUniqueIdDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        caseUniqueIdDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        InterviewDateDataDefinition interviewDateDataDefinition = new InterviewDateDataDefinition();
        interviewDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        interviewDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        AdmissionDateDataDefinition admissionDateDataDefinition = new AdmissionDateDataDefinition();
        admissionDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        admissionDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        OutpatientDateDataDefinition outpatientDateDataDefinition = new OutpatientDateDataDefinition();
        outpatientDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        outpatientDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        DateCreatedDataDefinition dateCreatedDataDefinition = new DateCreatedDataDefinition();
        dateCreatedDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dateCreatedDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        DateUpdatedDataDefinition dateUpdatedDataDefinition = new DateUpdatedDataDefinition();
        dateUpdatedDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dateUpdatedDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        FinalPatientManagementOutcomeDataDefinition finalOutcomeDataDefinition = new FinalPatientManagementOutcomeDataDefinition();
        finalOutcomeDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        finalOutcomeDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        FinalPatientManagementOutcomeDateDataDefinition finalOutcomeDateDataDefinition = new FinalPatientManagementOutcomeDateDataDefinition();
        finalOutcomeDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        finalOutcomeDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addColumn("Patient Unique Id", identifierDef, "");
        dsd.addColumn("NUPI", nupiDef, "");
        dsd.addColumn("Sex", new GenderDataDefinition(), "", null);
        dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
        dsd.addColumn("MFL Code", new MFLCodeDataDefinition(), "");
        dsd.addColumn("County", new CalculationDataDefinition("County", new CountyAddressCalculation()), "",
                new CalculationResultConverter());
        dsd.addColumn("Sub County", new CalculationDataDefinition("Subcounty", new SubCountyAddressCalculation()), "",
                new CalculationResultConverter());
        dsd.addColumn("Case Unique ID", caseUniqueIdDataDefinition, paramMapping);

        dsd.addColumn("Interview Date", interviewDateDataDefinition, paramMapping);
         dsd.addColumn("Admission Date", admissionDateDataDefinition, paramMapping);
        dsd.addColumn("Outpatient Date", outpatientDateDataDefinition, paramMapping);
        dsd.addColumn("Created Date", dateCreatedDataDefinition, paramMapping);
        dsd.addColumn("Updated Date", dateUpdatedDataDefinition, paramMapping);
        dsd.addColumn("Final Outcome", finalOutcomeDataDefinition, paramMapping);
        dsd.addColumn("Final Outcome Date", finalOutcomeDateDataDefinition, paramMapping);

        IllCasesCohortDefinition cd = new IllCasesCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addRowFilter(cd, paramMapping);
        return dsd;
    }

    protected DataSetDefinition labsDataSetDefinitionColumns() {
        EncounterDataSetDefinition dsd = new EncounterDataSetDefinition();
        dsd.setName("labs");
        dsd.setDescription("Labs Report Cases information");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        String paramMapping = "startDate=${startDate},endDate=${endDate}";

        PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class,
                HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        PatientIdentifierType nupi = MetadataUtils.existing(PatientIdentifierType.class,
                CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
        DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
        DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
                upn.getName(), upn), identifierFormatter);
        DataDefinition nupiDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
                nupi.getName(), nupi), identifierFormatter);
        AgeAtReportingDataDefinition ageAtReportingDataDefinition = new AgeAtReportingDataDefinition();
        ageAtReportingDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        ETLNextAppointmentDateDataDefinition lastAppointmentDateDataDefinition = new ETLNextAppointmentDateDataDefinition();
        lastAppointmentDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        lastAppointmentDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        VisitTypeWithComplaintsDataDefinition visitTypeDataDefinition = new VisitTypeWithComplaintsDataDefinition();
        visitTypeDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        visitTypeDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        LabsCaseUniqueIDDataDefinition labCaseUniqueIDDataDefinition = new LabsCaseUniqueIDDataDefinition();
        labCaseUniqueIDDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        labCaseUniqueIDDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        LabsOrderIDDataDefinition labsOrderIDDataDefinition = new LabsOrderIDDataDefinition();
        labsOrderIDDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        labsOrderIDDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        LabsTestResultsDataDefinition labsTestResultsDataDefinition = new LabsTestResultsDataDefinition();
        labsTestResultsDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        labsTestResultsDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        LabsTestDateDataDefinition labsTestDateDataDefinition = new LabsTestDateDataDefinition();
        labsTestDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        labsTestDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        LabsTestNameDataDefinition labsTestNameDataDefinition = new LabsTestNameDataDefinition();
        labsTestNameDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        labsTestNameDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));


        /*DataConverter formatter = new ObjectFormatter("{familyName}, {givenName}");
        DataDefinition nameDef = new ConvertedPersonDataDefinition("name",
                new PreferredNameDataDefinition(), formatter);
        PersonAttributeType phoneNumber = MetadataUtils.existing(PersonAttributeType.class,
                CommonMetadata._PersonAttributeType.TELEPHONE_CONTACT);*/
        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Case Unique ID", labCaseUniqueIDDataDefinition, paramMapping);
        dsd.addColumn("Order ID", labsOrderIDDataDefinition, paramMapping);
        dsd.addColumn("Test Results", labsTestResultsDataDefinition, paramMapping);
        dsd.addColumn("Lab Date", labsTestDateDataDefinition, paramMapping, new DateConverter(DATE_FORMAT));
        dsd.addColumn("Test Name", labsTestNameDataDefinition, paramMapping);


        LabsCohortDefinition cd = new LabsCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addRowFilter(cd, paramMapping);
        return dsd;
    }

    protected DataSetDefinition diagnosisDataSetDefinitionColumns() {
        EncounterDataSetDefinition dsd = new EncounterDataSetDefinition();
        dsd.setName("diagnosis");
        dsd.setDescription("Diagnosis Report Cases information");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        String paramMapping = "startDate=${startDate},endDate=${endDate}";


        DiagnosisCaseUniqueIdDataDefinition diagnosisCaseUniqueIdDataDefinition = new DiagnosisCaseUniqueIdDataDefinition();
        diagnosisCaseUniqueIdDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        diagnosisCaseUniqueIdDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        DiagnosisDataDefinition diagnosisDataDefinition = new DiagnosisDataDefinition();
        diagnosisDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        diagnosisDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        DiagnosisDateDataDefinition diagnosisDateDataDefinition = new DiagnosisDateDataDefinition();
        diagnosisDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        diagnosisDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Case Unique ID", diagnosisCaseUniqueIdDataDefinition, paramMapping);
        dsd.addColumn("Diagnosis", diagnosisDataDefinition, paramMapping);
        dsd.addColumn("Diagnosis Date", diagnosisDateDataDefinition, paramMapping, new DateConverter(DATE_FORMAT));

        LabsCohortDefinition cd = new LabsCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addRowFilter(cd, paramMapping);
        return dsd;
    }

    protected DataSetDefinition complaintDataSetDefinitionColumns() {
        EncounterDataSetDefinition dsd = new EncounterDataSetDefinition();
        dsd.setName("complaint");
        dsd.setDescription("Complaint Report Cases information");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        String paramMapping = "startDate=${startDate},endDate=${endDate}";

        ComplainDurationDataDefinition complainDurationDataDefinition = new ComplainDurationDataDefinition();
        complainDurationDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        complainDurationDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        ComplainDataDefinition complainDataDefinition = new ComplainDataDefinition();
        complainDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        complainDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        ComplainOnsetDateDataDefinition complainOnsetDateDataDefinition = new ComplainOnsetDateDataDefinition();
        complainOnsetDateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        complainOnsetDateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        ComplainCaseUniqueIdDataDefinition complainCaseUniqueIdDataDefinition = new ComplainCaseUniqueIdDataDefinition();
        complainCaseUniqueIdDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        complainCaseUniqueIdDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Case Unique ID", complainCaseUniqueIdDataDefinition, paramMapping);
        dsd.addColumn("Complaint", complainDataDefinition, paramMapping);
        dsd.addColumn("Onset Date", complainOnsetDateDataDefinition, paramMapping, new DateConverter(DATE_FORMAT));
        dsd.addColumn("Duration", complainDurationDataDefinition, paramMapping);

        ComplaintCohortDefinition cd = new ComplaintCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addRowFilter(cd, paramMapping);
        return dsd;
    }

    protected DataSetDefinition vitalSignsDataSetDefinitionColumns() {
        EncounterDataSetDefinition dsd = new EncounterDataSetDefinition();
        dsd.setName("vitals");
        dsd.setDescription("Complaint Report Cases information");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        String paramMapping = "startDate=${startDate},endDate=${endDate}";

        OPDTemperatureDataDefinition opdTemperatureDataDefinition = new OPDTemperatureDataDefinition();
        opdTemperatureDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
        opdTemperatureDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));

        OPDRespiratoryRateDataDefinition opdRespiratoryRateDataDefinition = new OPDRespiratoryRateDataDefinition();
        opdRespiratoryRateDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
        opdRespiratoryRateDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));

        OPDOxygenSaturationDataDefinition opdOxygenSaturationDataDefinition = new OPDOxygenSaturationDataDefinition();
        opdOxygenSaturationDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
        opdOxygenSaturationDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));

        ComplainCaseUniqueIdDataDefinition complainCaseUniqueIdDataDefinition = new ComplainCaseUniqueIdDataDefinition();
        complainCaseUniqueIdDataDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
        complainCaseUniqueIdDataDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Case Unique ID", complainCaseUniqueIdDataDefinition, paramMapping);
        dsd.addColumn("Temperature", opdTemperatureDataDefinition, paramMapping);
        dsd.addColumn("Respiratory Rate", opdRespiratoryRateDataDefinition, paramMapping);
        dsd.addColumn("Oxygen Saturation", opdOxygenSaturationDataDefinition, paramMapping);
        dsd.addColumn("Vital Signs Date", new EncounterDatetimeDataDefinition(), "", new DateConverter(DATE_FORMAT));

        VitalSignsCohortDefinition cd = new VitalSignsCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addRowFilter(cd, paramMapping);
        return dsd;
    }

}
