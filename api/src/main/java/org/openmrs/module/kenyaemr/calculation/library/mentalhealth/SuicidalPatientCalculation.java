/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.mentalhealth;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Flags patients who answered "Yes" to the triage question asking whether they are currently
 * having thoughts of ending their life (concept Dictionary.MENTAL_SELF_HARM_CURRENT)
 */
public class SuicidalPatientCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {

	public static final EncounterType triageEncType = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.TRIAGE);

	@Override
	public String getFlagMessage() {
		return "Suicidal";
	}

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
		Set<Integer> alive = Filters.alive(cohort, context);
		PatientService patientService = Context.getPatientService();
		CalculationResultMap ret = new CalculationResultMap();

		for (Integer ptId : cohort) {
			boolean result = false;
			if (alive.contains(ptId)) {
				Patient patient = patientService.getPatient(ptId);
				Encounter lastTriageEnc = EmrUtils.lastEncounter(patient, triageEncType);
				result = lastTriageEnc != null && EmrUtils.hasYesAnswer(lastTriageEnc, Dictionary.MENTAL_SELF_HARM_CURRENT);
			}
			ret.put(ptId, new BooleanResult(result, this));
		}
		return ret;
	}
}
