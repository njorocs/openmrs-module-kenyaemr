/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.covid;

import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractHybridReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.covid.PatientsCurrentlyEnrolledOnCovidCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.IdentifierConverter;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.covid.*;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.*;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({"kenyaemr.covid.report.everDischargedFromQuarantineLineList"})
public class PatientsEverDischargedFromQuarantineReportBuilder extends AbstractHybridReportBuilder {
    public static final String DATE_FORMAT = "dd/MM/yyyy";

    @Override
    protected Mapped<CohortDefinition> buildCohort(HybridReportDescriptor descriptor, PatientDataSetDefinition dsd) {
        return covidPatientsCohort();
    }

    protected Mapped<CohortDefinition> covidPatientsCohort() {
        CohortDefinition cd = new PatientsCurrentlyEnrolledOnCovidCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setName("EverDischargedQuarantinePatients");
        return ReportUtils.map(cd, "startDate=${startDate},endDate=${endDate}");
    }

    @Override
    protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {

        PatientDataSetDefinition quarantinedPatients = covidDataSetDefinition();
        quarantinedPatients.addRowFilter(covidPatientsCohort());
        DataSetDefinition quarantinedPatientsDSD = quarantinedPatients;

        return Arrays.asList(
                ReportUtils.map(quarantinedPatientsDSD, "startDate=${startDate},endDate=${endDate}")
        );
    }

    @Override
    protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
        return Arrays.asList(
                new Parameter("startDate", "Start Date", Date.class),
                new Parameter("endDate", "End Date", Date.class),
                new Parameter("dateBasedReporting", "", String.class)
        );
    }

    protected PatientDataSetDefinition covidDataSetDefinition() {

        PatientIdentifierType nationalIdType = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.NATIONAL_ID);
        PatientIdentifierType passportNumberType = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.PASSPORT_NUMBER);
        DataDefinition natIdDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(nationalIdType.getName(), nationalIdType), new IdentifierConverter());
        DataDefinition passportDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(passportNumberType.getName(), passportNumberType), new IdentifierConverter());
        PersonAttributeType phoneNumber = MetadataUtils.existing(PersonAttributeType.class, CommonMetadata._PersonAttributeType.TELEPHONE_CONTACT);
        String DATE_FORMAT = "dd/MM/yyyy";
        PatientDataSetDefinition dsd = new PatientDataSetDefinition("CovidLinelist");
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        String defParam = "startDate=${startDate}";

        PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.NATIONAL_ID);
        DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
        DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(upn.getName(), upn), identifierFormatter);

        DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName} {middleName}");
        DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
        dsd.addColumn("Unique Patient No", identifierDef, "");
        dsd.addColumn("id", new PersonIdDataDefinition(), "");
        dsd.addColumn("Passport Number", passportDef, "");

        dsd.addColumn("Name", nameDef, "");
        dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
        dsd.addColumn("Age", new AgeDataDefinition(), "");
        dsd.addColumn("Sex", new GenderDataDefinition(), "");
        dsd.addColumn("Phone Number", new PersonAttributeDataDefinition(phoneNumber), "");
        dsd.addColumn("Occupation",new OccupationDataDefinition(), "");
        dsd.addColumn("County",new CountyDataDefinition(), "");
        dsd.addColumn("SubCounty",new SubcountyDataDefinition(), "");
        //dsd.addColumn("Ward",new WardDataDefinition(), "");
        //dsd.addColumn("Landmark",new LandmarkDataDefinition(), "");
        dsd.addColumn("Recently Travelled",new RecentTravelDataDefinition(), "");
        dsd.addColumn("Place Travelled From",new PlaceTravelledFromDataDefinition(), "");
        dsd.addColumn("Recent Contact With Covid-19 Case",new RecentContactWithCovidCaseDataDefinition(), "");
        dsd.addColumn("Has Symptoms",new SymptomsDataDefinition(), "");
        dsd.addColumn("Classification",new ClassificationDataDefinition(), "");
        dsd.addColumn("Symptoms onset Date",new SymptomsOnsetDateDataDefinition(), "");
        dsd.addColumn("Cough",new CoughDataDefinition(), "");
        dsd.addColumn("Fever",new FeverDataDefinition(), "");
        dsd.addColumn("Difficulty in breathing",new DifficultyBreathingDataDefinition(), "");
        dsd.addColumn("Other Symptoms",new OtherSymptomsDataDefinition(), "");
        dsd.addColumn("Visited health facility",new VisitedHFDataDefinition(), "");
        dsd.addColumn("Date visited Hospital",new HospitalVisitDateDataDefinition(), "");
        dsd.addColumn("Health Facility Name",new HealthFacilityNameDataDefinition(), "");
        dsd.addColumn("Hospitalized",new HospitalizedDataDefinition(), "");
        dsd.addColumn("Date of admission",new DateOfAdmissionDataDefinition(), "");
        dsd.addColumn("Sample Collected",new SampleCollectedDataDefinition(), "");
        dsd.addColumn("Date of sample collected",new DateSampleCollectedDataDefinition(), "");
        dsd.addColumn("Lab result",new LabResultDataDefinition(), "");
        dsd.addColumn("Lab results confirmation date",new LabResultConfirmationDateDataDefinition(), "");
        dsd.addColumn("Outcome",new OutcomeDataDefinition(), "");
        dsd.addColumn("Outcome date",new OutcomeDateDataDefinition(), "");

        return dsd;
    }

}

