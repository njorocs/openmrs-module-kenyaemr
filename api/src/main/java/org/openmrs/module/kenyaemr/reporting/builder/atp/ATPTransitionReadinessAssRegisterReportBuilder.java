/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.atp;

import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.atp.ATPTransitionReadinessRegisterCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ArvsWorksDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.AtpContraceptionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.AttendPeerSupportDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.CadreAtpDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.CameOnScheduledDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ClinicAloneDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ClinicAloneOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.CurrentClinicVisitDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.DepartmentAtpDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.DiscloseHivStatusDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ExplainHivDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ExplainSupressedVlDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.FreeAskQuestionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.IsYlhDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.KnowsVlSuppressedDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.MedicalProblemDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.MissMedicationDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.NameArvDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.NavigateClinicDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.NotReadyToTransitDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.PersonToSeekHelpDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.ReproductiveHealthDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.SeekMedicalCareDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.StateVlDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.TransIsDateOfDisclosureKnownDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.TransitAdultServicesDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.TransitionDateOfDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.TransitionPointOfEntryDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.TreatmentBuddyDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.UnknownDateOfDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTransition.VerbalizeGoalsDataDefinition;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.DateConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.encounter.definition.EncounterDatetimeDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.AgeDataDefinition;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ConvertedPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonAttributeDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.EncounterDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({"kenyaemr.hiv.report.atpTransitionReadinessRegister"})
public class ATPTransitionReadinessAssRegisterReportBuilder extends AbstractReportBuilder {
    public static final String ENC_DATE_FORMAT = "yyyy/MM/dd";
    public static final String DATE_FORMAT = "dd/MM/yyyy";

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
                ReportUtils.map(datasetColumns(), "startDate=${startDate},endDate=${endDate}")
        );
    }

    protected DataSetDefinition datasetColumns() {
        EncounterDataSetDefinition dsd = new EncounterDataSetDefinition();
        dsd.setName("ATPTransitionReadinessInformation");
        dsd.setDescription("Visit information");
        dsd.addSortCriteria("Visit Date", SortCriteria.SortDirection.ASC);
        dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        dsd.addParameter(new Parameter("endDate", "End Date", Date.class));

        String paramMapping = "startDate=${startDate},endDate=${endDate}";

        DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName} {middleName}");
        DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
        PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
        PatientIdentifierType nupi = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.NATIONAL_UNIQUE_PATIENT_IDENTIFIER);
        DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
        DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(upn.getName(), upn), identifierFormatter);
        DataDefinition nupiDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(nupi.getName(), nupi), identifierFormatter);
        PersonAttributeType phoneNumber = MetadataUtils.existing(PersonAttributeType.class, CommonMetadata._PersonAttributeType.TELEPHONE_CONTACT);
        PatientIdentifierType nationalId = MetadataUtils.existing(PatientIdentifierType.class, CommonMetadata._PatientIdentifierType.NATIONAL_ID);
        DataDefinition nationalIdDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(nationalId.getName(), nationalId), identifierFormatter);

        dsd.addColumn("Name", nameDef, "");
        dsd.addColumn("id", new PatientIdDataDefinition(), "");
        dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
        dsd.addColumn("Age", new AgeDataDefinition(), "");
        dsd.addColumn("Sex", new GenderDataDefinition(), "");
        dsd.addColumn("Telephone No", new PersonAttributeDataDefinition(phoneNumber), "");
        dsd.addColumn("National Id", nationalIdDef, null);
        dsd.addColumn("Unique Patient Number", identifierDef, null);
        dsd.addColumn("National Unique Patient Identifier", nupiDef, null);

        dsd.addColumn("Visit Date", new EncounterDatetimeDataDefinition(),"", new DateConverter(ENC_DATE_FORMAT));
        // new columns
        dsd.addColumn("Point of entry at enrollment", new TransitionPointOfEntryDataDefinition(), null);
        dsd.addColumn("Is the date of disclosure known", new TransIsDateOfDisclosureKnownDataDefinition(), null);
        dsd.addColumn("Date of full disclosure", new TransitionDateOfDisclosureDataDefinition(), "", new DateConverter(DATE_FORMAT));
        dsd.addColumn("Please indicate MM/YY or YY whichever is known", new UnknownDateOfDisclosureDataDefinition(), null);
        dsd.addColumn("Can explain what HIV is", new ExplainHivDataDefinition(), null);
        dsd.addColumn("Can explain how ARVs work", new ArvsWorksDataDefinition(), null);
        dsd.addColumn("Knows the names of their ARVs", new NameArvDataDefinition(), null);
        dsd.addColumn("Explain what it means to have a suppressed viral load", new ExplainSupressedVlDataDefinition(), null);
        dsd.addColumn("Can state what their viral load is", new StateVlDataDefinition(), null);
        dsd.addColumn("Knows if they are virally suppressed", new KnowsVlSuppressedDataDefinition(), null);
        dsd.addColumn("Knew the day and date of current clinic visit", new CurrentClinicVisitDataDefinition(), null);
        dsd.addColumn("Came to clinic on the scheduled day for current visit", new CameOnScheduledDataDefinition(), null);
        dsd.addColumn("Can explain what to do if they miss a medication dose", new MissMedicationDataDefinition(), null);
        dsd.addColumn("Can explain when and where they should seek medical care", new SeekMedicalCareDataDefinition(), null);
        dsd.addColumn("Knows how to navigate clinic services on their own to get what they need", new NavigateClinicDataDefinition(), null);
        dsd.addColumn("Feels comfortable coming to clinic alone", new ClinicAloneDataDefinition(), null);
        dsd.addColumn("Does not feel comfortable coming to clinic alone(why)", new ClinicAloneOtherDataDefinition(), null);
        dsd.addColumn("Can explain medical problems and report symptoms", new MedicalProblemDataDefinition(), null);
        dsd.addColumn("Feels free to ask the health care worker questions about their health", new FreeAskQuestionDataDefinition(), null);
        dsd.addColumn("Is the YLH having sex", new IsYlhDataDefinition(), null);
        dsd.addColumn("Feels free to ask the health care workers for reproductive health services", new ReproductiveHealthDataDefinition(), null);
        dsd.addColumn("Can name different contraception options", new AtpContraceptionDataDefinition(), null);
        dsd.addColumn("identify at least one person in the clinic that they feel safe to seek help from", new PersonToSeekHelpDataDefinition(), null);
        dsd.addColumn("Attends a peer support group", new AttendPeerSupportDataDefinition(), null);
        dsd.addColumn("explain when and how they would disclose HIV status to someone else", new DiscloseHivStatusDataDefinition(), null);
        dsd.addColumn("Has identified a treatment buddy", new TreatmentBuddyDataDefinition(), null);
        dsd.addColumn("Able to verbalize long-term life goals", new VerbalizeGoalsDataDefinition(), null);
        dsd.addColumn("Feels that he/she would be ready to transition to adult services", new TransitAdultServicesDataDefinition(), null);
        dsd.addColumn("If not ready to transition, why", new NotReadyToTransitDataDefinition(), null);
        dsd.addColumn("Cadre", new CadreAtpDataDefinition(), null);
        dsd.addColumn("Department", new DepartmentAtpDataDefinition(), null);
    
        ATPTransitionReadinessRegisterCohortDefinition cd = new ATPTransitionReadinessRegisterCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addRowFilter(cd, paramMapping);
        return dsd;

    }
}
