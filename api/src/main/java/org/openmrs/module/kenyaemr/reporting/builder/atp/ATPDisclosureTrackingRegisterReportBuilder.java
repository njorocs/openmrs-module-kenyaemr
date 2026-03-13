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
import org.openmrs.module.kenyaemr.reporting.cohort.definition.atp.ATPDisclosureTrackingRegisterCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.cohort.definition.atp.ATPTransitionReadinessRegisterCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ActionPlanCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ActionPlanChildDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ActionTakenAtVisitDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.AdherenceCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.AdherenceProviderDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.BehaviorCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.BehaviorProviderDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.BroughtClinicTodayDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ChildKnowsHaveHivDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ConductedDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ConductedDisclosureOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ContentOfDiscussionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.CounsellingDoneTodayDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.DisclosureDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.FollowupVisitDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.InteractionCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.InteractionCaregiverProviderDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.InteractionProviderDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.IsDisclosureDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.KnowsChildStatusDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.KnowsHaveIllnessDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.KnowsHivDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.MoodCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.MoodProviderDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.NatureOfDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.NoDisclosureDisscussionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.NoDisclosureDisscussionOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.PlaceDisclosureOccurredDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.PrimaryCaregiverDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.PrimaryCaregiverOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ReactionToDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ResponseTakingMedicineDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.ResponseTakingMedicineOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.TrackingCadreDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.TrackingDepartmentDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.UnknownDisclosureDateDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.WhoBroughtChildClinicDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTracking.WhoBroughtChildClinicOtherDataDefinition;
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
@Builds({"kenyaemr.hiv.report.atpDisclosureTrackingRegister"})
public class ATPDisclosureTrackingRegisterReportBuilder extends AbstractReportBuilder {
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
        dsd.setName("ATPDisclosureTrackingInformation");
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
        dsd.addColumn("Primary caregiver", new PrimaryCaregiverDataDefinition(), null);
        dsd.addColumn("Other primary caregiver", new PrimaryCaregiverOtherDataDefinition(), null);
        dsd.addColumn("Who brought child to clinic", new WhoBroughtChildClinicDataDefinition(), null);
        dsd.addColumn("Who brought child to clinic(Other)", new WhoBroughtChildClinicOtherDataDefinition(), null);
        dsd.addColumn("Does the clinic report the child knows they have HIV", new ChildKnowsHaveHivDataDefinition(), null);
        dsd.addColumn("Does the child know they have HIV", new KnowsHivDataDefinition(), null);
        dsd.addColumn("If no, does the child know they have an illness", new KnowsHaveIllnessDataDefinition(), null);
        dsd.addColumn("Why are you taking your medicine", new ResponseTakingMedicineDataDefinition(), null);
        dsd.addColumn("Other responses", new ResponseTakingMedicineOtherDataDefinition(), null);
        dsd.addColumn("Who brought child to clinic today", new BroughtClinicTodayDefinition(), null);
        dsd.addColumn("Disclosure counseling done today", new CounsellingDoneTodayDataDefinition(), null);
        dsd.addColumn("Content of discussion today", new ContentOfDiscussionDataDefinition(), null);
        dsd.addColumn("If no disclosure discussion, indicate why", new NoDisclosureDisscussionDataDefinition(), null);
        dsd.addColumn("If no disclosure discussion, indicate why(Other)", new NoDisclosureDisscussionOtherDataDefinition(), null);
        dsd.addColumn("Place where full disclosure occurred", new PlaceDisclosureOccurredDataDefinition(), null);
        dsd.addColumn("Is the date of disclosure known", new IsDisclosureDateDataDefinition(), null);
        dsd.addColumn("Date of full disclosure", new DisclosureDateDataDefinition(), "", new DateConverter(DATE_FORMAT));
        dsd.addColumn("Please indicate MM/YY or YY whichever is known", new UnknownDisclosureDateDataDefinition(), null);
        dsd.addColumn("Who conducted full disclosure", new ConductedDisclosureDataDefinition(), null);
        dsd.addColumn("Who conducted full disclosure(Other)", new ConductedDisclosureOtherDataDefinition(), null);
        dsd.addColumn("Nature of disclosure", new NatureOfDisclosureDataDefinition(), null);
        dsd.addColumn("Child’s reaction to full disclosure", new ReactionToDisclosureDataDefinition(), null);
        dsd.addColumn("Cadre", new TrackingCadreDataDefinition(), null);
        dsd.addColumn("Department", new TrackingDepartmentDataDefinition(), null);
        dsd.addColumn("Date of follow-up visit", new FollowupVisitDataDefinition(), "", new DateConverter(ENC_DATE_FORMAT));
        dsd.addColumn("Behavior(Caregiver reflection)", new BehaviorCaregiverDataDefinition(), null);
        dsd.addColumn("Mood(Caregiver reflection)", new MoodCaregiverDataDefinition(), null);
        dsd.addColumn("Adherence(Caregiver reflection)", new AdherenceCaregiverDataDefinition(), null);
        dsd.addColumn("Interaction(Caregiver reflection)", new InteractionCaregiverDataDefinition(), null);
        dsd.addColumn("Behavior(Provider reflection)", new BehaviorProviderDataDefinition(), null);
        dsd.addColumn("Mood(Provider reflection)", new MoodProviderDataDefinition(), null);
        dsd.addColumn("Adherence(Provider reflection)", new AdherenceProviderDataDefinition(), null);
        dsd.addColumn("Interaction(Provider reflection)", new InteractionProviderDataDefinition(), null);
        dsd.addColumn("Interaction with Caregiver(Provider reflection)", new InteractionCaregiverProviderDataDefinition(), null);
        dsd.addColumn("Action taken at visit", new ActionTakenAtVisitDataDefinition(), null);
        dsd.addColumn("Action plan(Caregiver)", new ActionPlanCaregiverDataDefinition(), null);
        dsd.addColumn("Action plan(Child)", new ActionPlanChildDataDefinition(), null);
        dsd.addColumn("Who else has been told the child HIV status", new KnowsChildStatusDataDefinition(), null);
     
    
        ATPDisclosureTrackingRegisterCohortDefinition cd = new ATPDisclosureTrackingRegisterCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addRowFilter(cd, paramMapping);
        return dsd;

    }
}
