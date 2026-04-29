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
import org.openmrs.module.kenyaemr.reporting.cohort.definition.atp.ATPReadinessRegisterCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.*;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.*;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BringsChildClinicDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BringsChildClinicOtherNonRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BringsChildClinicOtherRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BroughtChildClinicDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BroughtChildClinicOtherRelativeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.BroughtChildOtherNonRelativeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CadreDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CareGiverAnsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CareGiverAnsOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CaregiverQuesAboutHIvDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CaregiverWorriesDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.CaregiverWorriesOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildBeToldDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildBeToldOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildBehaviourAtHomeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildDoingInSchoolDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildHaveFriendsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildLikeSchoolDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildPerformanceSchoolDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildQuestionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildReactionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildSayDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ChildSayOtherDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.DepartmentDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.DisclosurePreferenceDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.DiscussedHivStatusDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.EverTalkAboutHivDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.GiveChildMedicineDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.GiveChildMedicineOtherNonRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.GiveChildMedicineOtherRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.HouseholdMemberHivPosDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.KnowChildHivStatusDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.KnowChildHivStatusOtherNonRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.KnowChildHivStatusOtherRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.LivesInHouseholdDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.LivesInHouseholdOtherNonRelaDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.LivesInHouseholdOtherRelativeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.MemberTakingArvsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.OtherPointOfEntryDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.OtherQuestionDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.OtherReasonDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.DiscussionOutComeDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.PointOfEntryDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.ReasonTakeMedicationDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atp.TakeMedicationDataDefinition;
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
@Builds({"kenyaemr.hiv.report.atpDisclosureReadinessRegister"})
public class ATPDisclosureReadinessRegisterReportBuilder extends AbstractReportBuilder {
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
        dsd.setName("ATPDisclosureReadinessInformation");
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
        dsd.addColumn("Point of entry", new PointOfEntryDataDefinition(), null);
        dsd.addColumn("Other Point of entry", new OtherPointOfEntryDataDefinition(), null);
        dsd.addColumn("Brought child clinic", new BroughtChildClinicDataDefinition(), null);
        dsd.addColumn("Brought child clinic(other relative)", new BroughtChildClinicOtherRelativeDataDefinition(), null);
        dsd.addColumn("Brought child clinic(other non relative)", new BroughtChildOtherNonRelativeDataDefinition(), null);
        dsd.addColumn("Lives in household", new LivesInHouseholdDataDefinition(), null);
        dsd.addColumn("Lives in household(other relative)", new LivesInHouseholdOtherRelativeDataDefinition(), null);
        dsd.addColumn("Lives in household(other non relative)", new LivesInHouseholdOtherNonRelaDataDefinition(), null);
        dsd.addColumn("Gives child medicine", new GiveChildMedicineDataDefinition(), null);
        dsd.addColumn("Gives child medicine(other relative)", new GiveChildMedicineOtherRelaDataDefinition(), null);
        dsd.addColumn("Gives child medicine(other non relative)", new GiveChildMedicineOtherNonRelaDataDefinition(), null);
        dsd.addColumn("Brings child clinic", new BringsChildClinicDataDefinition(), null);
        dsd.addColumn("Brings child clinic(other relative)", new BringsChildClinicOtherRelaDataDefinition(), null);
        dsd.addColumn("Brings child clinic(other non relative)", new BringsChildClinicOtherNonRelaDataDefinition(), null);
        dsd.addColumn("Knows child HIV status", new KnowChildHivStatusDataDefinition(), null);
        dsd.addColumn("Knows child HIV status(other relative)", new KnowChildHivStatusOtherRelaDataDefinition(), null);
        dsd.addColumn("Knows child HIV status(other non relative)", new KnowChildHivStatusOtherNonRelaDataDefinition(),null);
        dsd.addColumn("Other household member HIV", new HouseholdMemberHivPosDataDefinition(), null);
        dsd.addColumn("Family member Taking ARVS", new MemberTakingArvsDataDefinition(), null);
        dsd.addColumn("Care discussed with other household members", new DiscussedHivStatusDataDefinition(),null);
        dsd.addColumn("outcome", new DiscussionOutComeDataDefinition(), null);
        dsd.addColumn("Child told why comes clinic take medication", new TakeMedicationDataDefinition(), null);
        // dsd.addColumn("Care giver thinks child believes reason taking med", new ReasonTakeMedicationDataDefinition(), null);
        // dsd.addColumn("Other reason", new OtherReasonDataDefinition(), null);
        dsd.addColumn("Question child been asking", new ChildQuestionDataDefinition(), null);
        dsd.addColumn("Other question", new OtherQuestionDataDefinition(), null);
        dsd.addColumn("Care giver answer", new CareGiverAnsDataDefinition(), null);
        dsd.addColumn("Other answer", new CareGiverAnsOtherDataDefinition(), null);
        dsd.addColumn("Child ever talk about HIV", new EverTalkAboutHivDataDefinition(), null);
        dsd.addColumn("What child say", new ChildSayDataDefinition(), null);
        dsd.addColumn("What child say(Other)", new ChildSayOtherDataDefinition(), null);
        dsd.addColumn("Child doing in school", new ChildDoingInSchoolDataDefinition(), null);
        dsd.addColumn("Child's school performance", new ChildPerformanceSchoolDataDefinition(), null);
        dsd.addColumn("Like school", new ChildLikeSchoolDefinition(), null);
        dsd.addColumn("Have friends", new ChildHaveFriendsDataDefinition(), null);
        dsd.addColumn("Child behaviour at home", new ChildBehaviourAtHomeDataDefinition(), null);
        dsd.addColumn("Worries caregiver have child learning HIV status", new CaregiverWorriesDataDefinition(), null);
        dsd.addColumn("Other Worries", new CaregiverWorriesOtherDataDefinition(), null);
        dsd.addColumn("Caregiver preference child be disclosed to", new DisclosurePreferenceDataDefinition(), null);
        dsd.addColumn("When does caregiver thinks should be told", new ChildBeToldDataDefinition(), null);
        dsd.addColumn("When does caregiver thinks should be told(Other)", new ChildBeToldOtherDataDefinition(), null);
        dsd.addColumn("Caregiver thinks child reaction would be informed HIV status", new ChildReactionDataDefinition(), null);
        dsd.addColumn("Questions care giver has about HIV", new CaregiverQuesAboutHIvDataDefinition(), null);
        dsd.addColumn("Cadre", new CadreDataDefinition(), null);
        dsd.addColumn("Department", new DepartmentDataDefinition(), null);

        ATPReadinessRegisterCohortDefinition cd = new ATPReadinessRegisterCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addRowFilter(cd, paramMapping);
        return dsd;

    }
}
