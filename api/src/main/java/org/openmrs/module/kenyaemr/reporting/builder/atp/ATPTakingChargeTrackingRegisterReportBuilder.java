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
import org.openmrs.module.kenyaemr.reporting.cohort.definition.atp.ATPTakingChargeRegisterCohortDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AdultExpectationsDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpCadreDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpCommunicationCoveredDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpCommunicationDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpDateOfDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpDepartmentDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpSupportCoveredDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.AtpSupportDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.DateOfDisclosureKnownDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.OtherKnownDateOfDisclosureDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.SelfManagementDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.SelfMgtCoveredDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.TakingChargePointOfEntryDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.TreatmentLiteracyCoveredDataDefinition;
import org.openmrs.module.kenyaemr.reporting.data.converter.definition.atpTakingCharge.TreatmentLiteracyDataDefinition;
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
@Builds({"kenyaemr.hiv.report.atpTakingChargeTrackingRegister"})
public class ATPTakingChargeTrackingRegisterReportBuilder extends AbstractReportBuilder {
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
        dsd.setName("ATPTakingChargeTrackingInformation");
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
        dsd.addColumn("Point of entry at enrollment", new TakingChargePointOfEntryDataDefinition(), null);
        dsd.addColumn("Is the date of disclosure known", new DateOfDisclosureKnownDataDefinition(), null);
        dsd.addColumn("Date of full disclosure", new AtpDateOfDisclosureDataDefinition(), "", new DateConverter(DATE_FORMAT));
        dsd.addColumn("Please indicate MM/YY or YY whichever is known", new OtherKnownDateOfDisclosureDataDefinition(), null);
        dsd.addColumn("Treatment literacy", new TreatmentLiteracyDataDefinition(), null);
        dsd.addColumn("Sub section covered treatment literacy", new TreatmentLiteracyCoveredDataDefinition(), null);
        dsd.addColumn("Self management", new SelfManagementDataDefinition(), null);
        dsd.addColumn("Sub section covered Self management", new SelfMgtCoveredDataDefinition(), null);
        dsd.addColumn("Communication", new AtpCommunicationDataDefinition(), null);
        dsd.addColumn("Sub section covered Communication", new AtpCommunicationCoveredDataDefinition(), null);
        dsd.addColumn("Support", new AtpSupportDataDefinition(), null);
        dsd.addColumn("Sub section covered Support", new AtpSupportCoveredDataDefinition(), null);
        dsd.addColumn("Adult Expectations", new AdultExpectationsDataDefinition(), null);
        dsd.addColumn("Cadre", new AtpCadreDataDefinition(), null);
        dsd.addColumn("Department", new AtpDepartmentDataDefinition(), null);
    
        ATPTakingChargeRegisterCohortDefinition cd = new ATPTakingChargeRegisterCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));

        dsd.addRowFilter(cd, paramMapping);
        return dsd;

    }
}
