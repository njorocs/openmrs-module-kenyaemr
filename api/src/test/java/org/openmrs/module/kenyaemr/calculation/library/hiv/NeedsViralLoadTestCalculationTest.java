/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.hiv;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Program;
import org.openmrs.api.OrderContext;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.HtsConstants;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test for {@link NeedsViralLoadTestCalculation}
 *
 */
public class NeedsViralLoadTestCalculationTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private CommonMetadata commonMetadata;

    @Autowired
    private HivMetadata hivMetadata;

    @Autowired
    private MchMetadata mchMetadata;

    /**
     * Setup each test
     */
    @Before
    public void setup() throws Exception {
        executeDataSet("dataset/test-concepts.xml");

        commonMetadata.install();
        hivMetadata.install();
        mchMetadata.install();
    }
    /**
     * @see NeedsViralLoadTestCalculation#getFlagMessage()
     */
    @Test
    public void getFlagMessage() {
        Assert.assertThat(new NeedsViralLoadTestCalculation().getFlagMessage(), notNullValue());
    }
    /**
     * @see NeedsViralLoadTestCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
     * @verifies determine whether patients need a Viral load test
     */
    @Test
    public void evaluate_shouldDetermineWhetherPatientsNeedsViralLoadTest() throws Exception {

        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        Program mchProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHMS);
        Concept stavudine = Dictionary.getConcept(Dictionary.STAVUDINE);

        // Enroll patients #6, #7 and #8  #9 in the HIV Program
        TestUtils.enrollInProgram(TestUtils.getPatient(4), hivProgram, TestUtils.date(2018, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(5), hivProgram, TestUtils.date(2018, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(6), hivProgram, TestUtils.date(2018, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(7), hivProgram, TestUtils.date(2018, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(8), hivProgram, TestUtils.date(2018, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(9), hivProgram, TestUtils.date(2018, 1, 1));
        // Enroll patients #8 and  #9 in the MCH Program
        TestUtils.enrollInProgram(TestUtils.getPatient(8), mchProgram, TestUtils.date(2022, 2, 1));  //newly on art
        TestUtils.enrollInProgram(TestUtils.getPatient(9), mchProgram, TestUtils.date(2022, 2, 1));  //Already on art

        //set the birthdate of #7 to be this year less than 24 years
        TestUtils.getPatient(7).setBirthdate(TestUtils.date(2013, 6, 1));
        //put patient #5, #6,#7,#8 and #9 on art
        TestUtils.saveDrugOrder(TestUtils.getPatient(6), stavudine, TestUtils.date(2019, 1, 1), null);
        TestUtils.saveDrugOrder(TestUtils.getPatient(7), stavudine, TestUtils.date(2019, 1, 1), null);
        TestUtils.saveDrugOrder(TestUtils.getPatient(8), stavudine, TestUtils.date(2021, 6, 1), null);
        TestUtils.saveDrugOrder(TestUtils.getPatient(9), stavudine, TestUtils.date(2019, 1, 1), null);
        //give patient #6,#7 and #8  viral load
        TestUtils.saveObs(TestUtils.getPatient(7),Dictionary.getConcept(Dictionary.HIV_VIRAL_LOAD), 850, TestUtils.date(2021, 5, 1)); //vl 9 months ago
        TestUtils.saveObs(TestUtils.getPatient(9),Dictionary.getConcept(Dictionary.HIV_VIRAL_LOAD), 1050, TestUtils.date(2022, 1, 1));

        List<Integer> ptIds = Arrays.asList(4, 5, 6, 7, 8, 9);
        PatientCalculationService patientCalculationService = Context.getService(PatientCalculationService.class);
        PatientCalculationContext context = patientCalculationService.createCalculationContext();
        context.setNow(TestUtils.date(2022, 2, 20));

        CalculationResultMap resultMap = new NeedsViralLoadTestCalculation().evaluate(ptIds, null, context);

        Assert.assertFalse((Boolean) resultMap.get(4).getValue()); // not in HIV Program
        Assert.assertFalse((Boolean) resultMap.get(5).getValue());   //Not on art
        Assert.assertThat((Boolean) resultMap.get(9).getValue(), is(true));   //Needs vl since pregnant, already on art and >1 months without vl
        Assert.assertThat((Boolean) resultMap.get(8).getValue(), is(true));   //Needs vl since pregnant, newly on art and >6 months without vl
        Assert.assertThat((Boolean) resultMap.get(7).getValue(), is(true));   //Needs vl since vl >6 months months ago and less than 24 yrs
        Assert.assertThat((Boolean) resultMap.get(6).getValue(), is(true));   //Needs vl no vl and has been on art >6 months
    }

    /**
     * @see NeedsViralLoadTestCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
     * @verifies flag pre art positive mothers in pregnancy delivery and postnatal workflows and clear them once vl is ordered
     */
    @Test
    public void evaluate_shouldFlagPreArtPositiveMothersAndDeflagWhenVlIsOrdered() throws Exception {
        Program mchProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHMS);
        EncounterType htsEncounterType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS);
        EncounterType mchConsultationEncounterType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION);
        Form htsInitialForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST);
        Form deliveryForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DELIVERY);

        TestUtils.enrollInProgram(TestUtils.getPatient(2), mchProgram, TestUtils.date(2022, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(5), mchProgram, TestUtils.date(2022, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(7), mchProgram, TestUtils.date(2022, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(8), mchProgram, TestUtils.date(2022, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(9), mchProgram, TestUtils.date(2022, 1, 1));

        savePregnancyStatus(TestUtils.getPatient(2), TestUtils.date(2022, 1, 5));
        savePositiveHtsEncounter(TestUtils.getPatient(2), htsEncounterType, htsInitialForm, TestUtils.date(2022, 1, 6), null);

        savePregnancyStatus(TestUtils.getPatient(7), TestUtils.date(2022, 1, 5));
        savePositiveHtsEncounter(TestUtils.getPatient(7), htsEncounterType, htsInitialForm, TestUtils.date(2022, 1, 6), null);
        saveVlOrder(TestUtils.getPatient(7), TestUtils.date(2022, 1, 7));

        savePositiveHtsEncounter(TestUtils.getPatient(5), htsEncounterType, htsInitialForm, TestUtils.date(2022, 1, 6), null);

        savePositiveDeliveryEncounter(TestUtils.getPatient(8), mchConsultationEncounterType, deliveryForm, TestUtils.date(2022, 1, 10));

        saveDeliveryEncounter(TestUtils.getPatient(9), mchConsultationEncounterType, deliveryForm, TestUtils.date(2022, 1, 10));
        savePositiveHtsEncounter(TestUtils.getPatient(9), htsEncounterType, htsInitialForm, TestUtils.date(2022, 1, 11),
                Dictionary.getConcept(Dictionary.NO));

        List<Integer> ptIds = Arrays.asList(2, 5, 7, 8, 9);
        CalculationResultMap resultMap = evaluate(ptIds, TestUtils.date(2022, 2, 20));

        Assert.assertTrue((Boolean) resultMap.get(2).getValue());
        Assert.assertFalse((Boolean) resultMap.get(5).getValue());
        Assert.assertFalse((Boolean) resultMap.get(7).getValue());
        Assert.assertTrue((Boolean) resultMap.get(8).getValue());
        Assert.assertTrue((Boolean) resultMap.get(9).getValue());
    }

    /**
     * @see NeedsViralLoadTestCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
     * @verifies flag postnatal positive mothers and deflag after a vl order is placed
     */
    @Test
    public void evaluate_shouldDeflagPreArtPostnatalClientsWhenVlOrderExists() throws Exception {
        Program mchProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHMS);
        EncounterType mchConsultationEncounterType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION);
        Form postnatalForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT);

        TestUtils.enrollInProgram(TestUtils.getPatient(2), mchProgram, TestUtils.date(2022, 1, 1));
        TestUtils.enrollInProgram(TestUtils.getPatient(7), mchProgram, TestUtils.date(2022, 1, 1));

        saveBreastFeedingStatus(TestUtils.getPatient(2), TestUtils.date(2022, 1, 20));
        savePositivePostnatalEncounter(TestUtils.getPatient(2), mchConsultationEncounterType, postnatalForm,
                TestUtils.date(2022, 1, 20), TestUtils.date(2022, 1, 10));

        saveBreastFeedingStatus(TestUtils.getPatient(7), TestUtils.date(2022, 1, 20));
        savePositivePostnatalEncounter(TestUtils.getPatient(7), mchConsultationEncounterType, postnatalForm,
                TestUtils.date(2022, 1, 20), TestUtils.date(2022, 1, 10));
        saveVlOrder(TestUtils.getPatient(7), TestUtils.date(2022, 1, 21));

        List<Integer> ptIds = Arrays.asList(2, 7);
        CalculationResultMap resultMap = evaluate(ptIds, TestUtils.date(2022, 2, 20));

        Assert.assertTrue((Boolean) resultMap.get(2).getValue());
        Assert.assertFalse((Boolean) resultMap.get(7).getValue());
    }

    /**
     * @see NeedsViralLoadTestCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
     * @verifies only evaluate the latest encounter, and require PMTCT_MAT entry point on delivery HTS encounters
     */
    @Test
    public void evaluate_shouldRespectLatestEncounterAndRequirePmtctMatForDeliveryHts() throws Exception {
        Program mchProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHMS);
        EncounterType htsEncounterType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.HTS);
        EncounterType mchConsultationType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION);
        Form htsInitialForm = MetadataUtils.existing(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST);
        Form postnatalForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT);

        // Patient 7 (female): MCH enrolled, breastfeeding, positive PNC form encounter followed by
        // a later PNC form encounter without a positive result — should NOT be flagged (latest wins)
        TestUtils.enrollInProgram(TestUtils.getPatient(7), mchProgram, TestUtils.date(2022, 1, 1));
        saveBreastFeedingStatus(TestUtils.getPatient(7), TestUtils.date(2022, 1, 5));
        savePositivePostnatalEncounter(TestUtils.getPatient(7), mchConsultationType, postnatalForm,
                TestUtils.date(2022, 1, 10), TestUtils.date(2022, 1, 5));
        TestUtils.saveEncounter(TestUtils.getPatient(7), mchConsultationType, postnatalForm, TestUtils.date(2022, 1, 20));

        // Patient 8 (female): positive HTS with PMTCT_MAT entry point at delivery — should be flagged
        // (delivery check does not require MCH enrollment)
        savePositiveHtsEncounter(TestUtils.getPatient(8), htsEncounterType, htsInitialForm, TestUtils.date(2022, 1, 10),
                Context.getConceptService().getConcept(NeedsViralLoadTestCalculation.HTS_PMTCT_MAT_ENTRY_POINT_CONCEPT_ID));

        List<Integer> ptIds = Arrays.asList(7, 8);
        CalculationResultMap resultMap = evaluate(ptIds, TestUtils.date(2022, 2, 20));

        Assert.assertFalse((Boolean) resultMap.get(7).getValue()); // latest PNC encounter has no positive result
        Assert.assertTrue((Boolean) resultMap.get(8).getValue());  // positive HTS at PMTCT_MAT entry → flagged
    }

    private CalculationResultMap evaluate(List<Integer> ptIds, Date now) {
        PatientCalculationService patientCalculationService = Context.getService(PatientCalculationService.class);
        PatientCalculationContext context = patientCalculationService.createCalculationContext();
        context.setNow(now);
        return new NeedsViralLoadTestCalculation().evaluate(ptIds, null, context);
    }

    private void savePregnancyStatus(Patient patient, Date date) {
        TestUtils.saveObs(patient, Dictionary.getConcept(Dictionary.PREGNANCY_STATUS), Dictionary.getConcept(Dictionary.YES), date);
    }

    private void saveBreastFeedingStatus(Patient patient, Date date) {
        TestUtils.saveObs(patient, Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING), Dictionary.getConcept(Dictionary.YES), date);
    }

    private void savePositiveHtsEncounter(Patient patient, EncounterType encounterType, Form form, Date encounterDate, Concept entryPoint) {
        Concept htsFinalTestQuestion = Context.getConceptService().getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept positiveResult = Context.getConceptService().getConcept(HtsConstants.HTS_POSITIVE_RESULT_CONCEPT_ID);
        Concept entryPointQuestion = Context.getConceptService().getConcept(NeedsViralLoadTestCalculation.HTSENTRYPOINT_QUESTION_CONCEPT_ID);
        Obs[] obss = entryPoint != null
                ? new Obs[] {
                        TestUtils.saveObs(patient, htsFinalTestQuestion, positiveResult, encounterDate),
                        TestUtils.saveObs(patient, entryPointQuestion, entryPoint, encounterDate)
                }
                : new Obs[] {
                        TestUtils.saveObs(patient, htsFinalTestQuestion, positiveResult, encounterDate)
                };
        TestUtils.saveEncounter(patient, encounterType, form, encounterDate, obss);
    }

    private void savePositiveDeliveryEncounter(Patient patient, EncounterType encounterType, Form form, Date encounterDate) {
        Concept htsFinalTestQuestion = Context.getConceptService().getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept positiveResult = Context.getConceptService().getConcept(HtsConstants.HTS_POSITIVE_RESULT_CONCEPT_ID);
        Concept dateOfDelivery = Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT);
        Obs[] obss = {
                TestUtils.saveObs(patient, htsFinalTestQuestion, positiveResult, encounterDate),
                TestUtils.saveObs(patient, dateOfDelivery, encounterDate, encounterDate)
        };
        TestUtils.saveEncounter(patient, encounterType, form, encounterDate, obss);
    }

    private void saveDeliveryEncounter(Patient patient, EncounterType encounterType, Form form, Date encounterDate) {
        Concept dateOfDelivery = Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT);
        Obs[] obss = {
                TestUtils.saveObs(patient, dateOfDelivery, encounterDate, encounterDate)
        };
        TestUtils.saveEncounter(patient, encounterType, form, encounterDate, obss);
    }

    private void savePositivePostnatalEncounter(Patient patient, EncounterType encounterType, Form form, Date encounterDate, Date deliveryDate) {
        Concept htsFinalTestQuestion = Context.getConceptService().getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept positiveResult = Context.getConceptService().getConcept(HtsConstants.HTS_POSITIVE_RESULT_CONCEPT_ID);
        Concept dateOfDelivery = Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT);
        Obs[] obss = {
                TestUtils.saveObs(patient, htsFinalTestQuestion, positiveResult, encounterDate),
                TestUtils.saveObs(patient, dateOfDelivery, deliveryDate, encounterDate)
        };
        TestUtils.saveEncounter(patient, encounterType, form, encounterDate, obss);
    }

    private void saveVlOrder(Patient patient, Date orderDate) {
        CareSetting careSetting = Context.getOrderService().getCareSetting(1);
        OrderType testOrderType = Context.getOrderService().getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID);
        Encounter encounter = Context.getEncounterService().getEncounter(3);
        Provider orderer = Context.getProviderService().getProvider(1);

        Order order = new Order();
        order.setPatient(patient);
        order.setEncounter(encounter);
        order.setConcept(Dictionary.getConcept(Dictionary.HIV_VIRAL_LOAD));
        order.setOrderer(orderer);
        order.setDateActivated(orderDate);

        OrderContext orderContext = new OrderContext();
        orderContext.setCareSetting(careSetting);
        orderContext.setOrderType(testOrderType);
        Context.getOrderService().saveOrder(order, orderContext);
    }
}
