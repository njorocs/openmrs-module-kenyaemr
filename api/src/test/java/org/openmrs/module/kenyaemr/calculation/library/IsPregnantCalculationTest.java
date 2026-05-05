/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link IsPregnantCalculation}
 */
public class IsPregnantCalculationTest extends BaseModuleContextSensitiveTest {
	@Autowired
	private MchMetadata mchMetadata;

	/**
	 * Setup each test
	 */
	@Before
	public void setup() throws Exception {
		executeDataSet("dataset/test-concepts.xml");
		mchMetadata.install();
	}

	/**
	 * @see IsPregnantCalculation#getFlagMessage()
	 */
	@Test
	public void getFlagMessage() {
		Assert.assertThat(new IsPregnantCalculation().getFlagMessage(), notNullValue());
	}

	/**
	 * @see IsPregnantCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies calculate last recorded pregnancy status for all patients
	 */
	@Test
	public void evaluate_shouldCalculatePregnancyStatus() throws Exception {
		Concept pregnancyStatus = Dictionary.getConcept(Dictionary.PREGNANCY_STATUS);
		Concept yes = Dictionary.getConcept(Dictionary.YES);
		Concept no = Dictionary.getConcept(Dictionary.NO);

		// Give patient #6 a recent YES recording which should be ignored as they are male
		TestUtils.saveObs(TestUtils.getPatient(6), pregnancyStatus, yes, TestUtils.date(2012, 12, 1));

		// Give patient #7 an older YES recording
		TestUtils.saveObs(TestUtils.getPatient(7), pregnancyStatus, yes, TestUtils.date(2010, 11, 1));

		// Give patient #7 a recent NO recording
		TestUtils.saveObs(TestUtils.getPatient(7), pregnancyStatus, no, TestUtils.date(2012, 12, 1));

		// Give patient #8 a recent YES recording
		TestUtils.saveObs(TestUtils.getPatient(8), pregnancyStatus, yes, TestUtils.date(2012, 12, 1));
		
		List<Integer> ptIds = Arrays.asList(6, 7, 8);
		CalculationResultMap resultMap = new IsPregnantCalculation().evaluate(ptIds, null, Context.getService(PatientCalculationService.class).createCalculationContext());
		Assert.assertFalse((Boolean) resultMap.get(6).getValue()); // is male
		Assert.assertFalse((Boolean) resultMap.get(7).getValue());
		Assert.assertTrue((Boolean) resultMap.get(8).getValue());
	}

	/**
	 * @see IsPregnantCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies treat active ANC and legacy MCH ANC enrollments as pregnant
	 */
	@Test
	public void evaluate_shouldTreatActiveAncAndLegacyMchAncEnrollmentsAsPregnant() throws Exception {
		EncounterType mchEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ENROLLMENT);
		EncounterType ancEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_ENROLLMENT);
		Form mchEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ENROLLMENT);
		Form ancEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANC_ENROLLMENT_FORM);

		saveMchAncEnrollment(TestUtils.getPatient(2), mchEnrollmentType, mchEnrollmentForm, TestUtils.date(2012, 1, 1));
		TestUtils.saveEncounter(TestUtils.getPatient(8), ancEnrollmentType, ancEnrollmentForm, TestUtils.date(2012, 2, 1));

		CalculationResultMap resultMap = evaluatePatients(2, 8);
		Assert.assertTrue((Boolean) resultMap.get(2).getValue());
		Assert.assertTrue((Boolean) resultMap.get(8).getValue());
	}

	/**
	 * @see IsPregnantCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies clear pregnancy when later negative evidence exists
	 */
	@Test
	public void evaluate_shouldClearPregnancyWhenLaterNegativeEvidenceExists() throws Exception {
		EncounterType ancEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_ENROLLMENT);
		EncounterType mchEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ENROLLMENT);
		Form ancEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANC_ENROLLMENT_FORM);
		Form mchEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ENROLLMENT);
		Concept pregnancyStatus = Dictionary.getConcept(Dictionary.PREGNANCY_STATUS);
		Concept no = Dictionary.getConcept(Dictionary.NO);
		Concept confinementDate = Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT);

		TestUtils.saveEncounter(TestUtils.getPatient(2), ancEnrollmentType, ancEnrollmentForm, TestUtils.date(2012, 1, 1));
		TestUtils.saveObs(TestUtils.getPatient(2), confinementDate, TestUtils.date(2012, 2, 1), TestUtils.date(2012, 2, 1));

		saveMchAncEnrollment(TestUtils.getPatient(8), mchEnrollmentType, mchEnrollmentForm, TestUtils.date(2012, 1, 1));
		TestUtils.saveObs(TestUtils.getPatient(8), pregnancyStatus, no, TestUtils.date(2012, 3, 1));

		CalculationResultMap resultMap = evaluatePatients(2, 8);
		Assert.assertFalse((Boolean) resultMap.get(2).getValue());
		Assert.assertFalse((Boolean) resultMap.get(8).getValue());
	}

	/**
	 * @see IsPregnantCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies allow later positive evidence after earlier confinement and clear on discontinuation
	 */
	@Test
	public void evaluate_shouldUseLatestEvidenceAcrossEnrollmentConfinementAndDiscontinuation() throws Exception {
		EncounterType ancEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_ENROLLMENT);
		EncounterType ancDiscontinuationType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ANC_DISCONTINUATION);
		Form ancEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANC_ENROLLMENT_FORM);
		Form ancDiscontinuationForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_ANC_DISCONTINUATION_FORM);
		Concept confinementDate = Dictionary.getConcept(Dictionary.DATE_OF_CONFINEMENT);

		TestUtils.saveObs(TestUtils.getPatient(2), confinementDate, TestUtils.date(2011, 6, 1), TestUtils.date(2011, 6, 1));
		TestUtils.saveEncounter(TestUtils.getPatient(2), ancEnrollmentType, ancEnrollmentForm, TestUtils.date(2012, 1, 1));

		TestUtils.saveEncounter(TestUtils.getPatient(7), ancEnrollmentType, ancEnrollmentForm, TestUtils.date(2012, 1, 1));
		TestUtils.saveEncounter(TestUtils.getPatient(7), ancDiscontinuationType, ancDiscontinuationForm, TestUtils.date(2012, 2, 1));

		CalculationResultMap resultMap = evaluatePatients(2, 7);
		Assert.assertTrue((Boolean) resultMap.get(2).getValue());
		Assert.assertFalse((Boolean) resultMap.get(7).getValue());
	}

	private CalculationResultMap evaluatePatients(Integer... patientIds) {
		List<Integer> ptIds = Arrays.asList(patientIds);
		return new IsPregnantCalculation().evaluate(ptIds, null, Context.getService(PatientCalculationService.class).createCalculationContext());
	}

	private void saveMchAncEnrollment(Patient patient, EncounterType encounterType, Form form, Date encounterDate) {
		Obs[] enrollmentObs = {
				TestUtils.saveObs(patient, Dictionary.getConcept(Dictionary.MCH_SERVICE_TYPE), Dictionary.getConcept(Dictionary.ANC_SERVICE), encounterDate)
		};
		TestUtils.saveEncounter(patient, encounterType, form, encounterDate, enrollmentObs);
	}
}
