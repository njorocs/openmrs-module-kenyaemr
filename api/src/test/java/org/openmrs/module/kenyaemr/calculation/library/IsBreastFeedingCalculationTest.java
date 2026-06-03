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
import java.util.List;

/**
 * Tests for {@link IsBreastFeedingCalculation}
 */
public class IsBreastFeedingCalculationTest extends BaseModuleContextSensitiveTest {
	@Autowired
	private MchMetadata mchMetadata;

	@Before
	public void setup() throws Exception {
		executeDataSet("dataset/test-concepts.xml");
		mchMetadata.install();
	}

	/**
	 * @see IsBreastFeedingCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies calculate last recorded breastfeeding status for all patients on Hiv Greencard
	 */
	@Test
	public void evaluate_shouldCalculateBreastFeedingStatusFromLatestObsEvidence() throws Exception {
		Concept infantFeedingMethod = Dictionary.getConcept(Dictionary.INFANT_FEEDING_METHOD);
		Concept currentlyBreastFeeding = Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING);
		Concept exclusiveBreastFeeding = Dictionary.getConcept(Dictionary.BREASTFED_EXCLUSIVELY);
		Concept infantNotBreastfeeding = Dictionary.getConcept(Dictionary.INFANT_NOT_BREASTFEEDING);
		Concept yes = Dictionary.getConcept(Dictionary.YES);
		Concept no = Dictionary.getConcept(Dictionary.NO);

		TestUtils.saveObs(TestUtils.getPatient(2), infantFeedingMethod, exclusiveBreastFeeding, TestUtils.date(2012, 1, 1));
		TestUtils.saveObs(TestUtils.getPatient(2), currentlyBreastFeeding, no, TestUtils.date(2012, 2, 1));

		TestUtils.saveObs(TestUtils.getPatient(7), currentlyBreastFeeding, yes, TestUtils.date(2012, 1, 1));
		TestUtils.saveObs(TestUtils.getPatient(7), infantFeedingMethod, infantNotBreastfeeding, TestUtils.date(2012, 2, 1));

		TestUtils.saveObs(TestUtils.getPatient(8), infantFeedingMethod, exclusiveBreastFeeding, TestUtils.date(2012, 2, 1));

		CalculationResultMap resultMap = evaluatePatients(2, 7, 8);
		Assert.assertFalse((Boolean) resultMap.get(2).getValue());
		Assert.assertFalse((Boolean) resultMap.get(7).getValue());
		Assert.assertTrue((Boolean) resultMap.get(8).getValue());
	}

	/**
	 * @see IsBreastFeedingCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies calculate last recorded breastfeeding status for all patients on PNC
	 */
	@Test
	public void evaluate_shouldTreatActivePncEnrollmentAsBreastFeeding() throws Exception {
		EncounterType pncEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_PNC_ENROLLMENT);
		Form pncEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_PNC_ENROLLMENT_FORM);

		TestUtils.saveEncounter(TestUtils.getPatient(2), pncEnrollmentType, pncEnrollmentForm, TestUtils.date(2012, 1, 1));

		CalculationResultMap resultMap = evaluatePatients(2);
		Assert.assertTrue((Boolean) resultMap.get(2).getValue());
	}

	/**
	 * @see IsBreastFeedingCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 * @verifies clear breastfeeding when later discontinuation exists
	 */
	@Test
	public void evaluate_shouldClearBreastFeedingWhenLaterDiscontinuationExists() throws Exception {
		EncounterType pncEnrollmentType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_PNC_ENROLLMENT);
		EncounterType pncDiscontinuationType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_PNC_DISCONTINUATION);
		EncounterType mchDiscontinuationType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_DISCONTINUATION);
		Form pncEnrollmentForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_PNC_ENROLLMENT_FORM);
		Form pncDiscontinuationForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_PNC_DISCONTINUATION_FORM);
		Form mchDiscontinuationForm = MetadataUtils.existing(Form.class, MchMetadata._Form.MCHMS_DISCONTINUATION);
		Concept currentBreastFeeding = Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING);
		Concept yes = Dictionary.getConcept(Dictionary.YES);

		TestUtils.saveEncounter(TestUtils.getPatient(2), pncEnrollmentType, pncEnrollmentForm, TestUtils.date(2012, 1, 1));
		TestUtils.saveEncounter(TestUtils.getPatient(2), pncDiscontinuationType, pncDiscontinuationForm, TestUtils.date(2012, 2, 1));

		TestUtils.saveObs(TestUtils.getPatient(8), currentBreastFeeding, yes, TestUtils.date(2012, 1, 1));
		TestUtils.saveEncounter(TestUtils.getPatient(8), mchDiscontinuationType, mchDiscontinuationForm, TestUtils.date(2012, 2, 1));

		CalculationResultMap resultMap = evaluatePatients(2, 8);
		Assert.assertFalse((Boolean) resultMap.get(2).getValue());
		Assert.assertFalse((Boolean) resultMap.get(8).getValue());
	}

	private CalculationResultMap evaluatePatients(Integer... patientIds) {
		List<Integer> ptIds = Arrays.asList(patientIds);
		return new IsBreastFeedingCalculation().evaluate(ptIds, null,
				Context.getService(PatientCalculationService.class).createCalculationContext());
	}
}
