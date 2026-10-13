/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.metadata;

import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.openmrs.module.metadatadeploy.bundle.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.openmrs.module.kenyaemr.metadata.MetadataUtils.shouldInstallForms;
import static org.openmrs.module.kenyaemr.metadata.MetadataUtils.shouldInstallPrograms;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.*;

/**
 * Oncology metadata bundle
 */
@Component
@Requires({ CommonMetadata.class })
public class OncologyMetadata extends AbstractMetadataBundle {

	private static final Logger logger = LoggerFactory.getLogger(OncologyMetadata.class);

	public static final class _Program {
		public static final String ONCOLOGY = "9902616a-3922-4a77-ab19-813258d0dc1d";
	}
	public static final class _EncounterType {
		public static final String ONCOLOGY_ENROLMENT_ENCOUNTER = "24697a73-e06d-4ba8-bef6-b364c3b04760";
		public static final String ONCOLOGY_DISCONTINUATION_ENCOUNTER = "1d56284c-ee28-49ae-9275-f6f400142fc2";
		public static final String ONCOLOGY_TREATMENT_ENCOUNTER = "72babccc-f162-4c52-942d-d5f813daf6cf";
	
	}

	public static final class _Form {
		public static final String ONCOLOGY_ENROLMENT_FORM = "52d63759-3ee6-4a12-9eb3-80ad380049c7";
		public static final String ONCOLOGY_DISCONTINUATION_FORM = "caf94ef9-e34f-4b37-98f8-ac08c4584b72";
		public static final String ONCOLOGY_TREATMENT_FORM = "42046b40-048b-44b4-be7c-8b85e488f089";
	
	}
	/**
	 * @see AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		boolean installPrograms = shouldInstallPrograms();

		if (installPrograms) {
			install(program("Oncology", "Oncology or Cancer Program", Dictionary.ONCOLOGY_PROGRAM, _Program.ONCOLOGY));
		}
		install(encounterType("Oncology Enrolment Encounter", "Oncology Enrolment Encounter", _EncounterType.ONCOLOGY_ENROLMENT_ENCOUNTER));
		install(encounterType("Oncology Discontinuation Encounter", "Oncology Discontinuation Encounter", _EncounterType.ONCOLOGY_DISCONTINUATION_ENCOUNTER));
		install(encounterType("Oncology Treatment Encounter", "Oncology Treatment Encounter", _EncounterType.ONCOLOGY_TREATMENT_ENCOUNTER));
			

		boolean installForms = shouldInstallForms();
		
		if (installForms) {
			logger.info("=== OncologyMetadata: Installing forms because shouldInstallForms() returned true ===");
			install(form("Oncology Enrolment form", null, _EncounterType.ONCOLOGY_ENROLMENT_ENCOUNTER, "1", _Form.ONCOLOGY_ENROLMENT_FORM));
			install(form("Oncology Discontinuation form", null, _EncounterType.ONCOLOGY_DISCONTINUATION_ENCOUNTER, "1", _Form.ONCOLOGY_DISCONTINUATION_FORM));
			install(form("Oncology Treatment form", null, _EncounterType.ONCOLOGY_TREATMENT_ENCOUNTER, "1", _Form.ONCOLOGY_TREATMENT_FORM));
					
		} else {
			logger.info("=== OncologyMetadata: SKIPPING form installation because shouldInstallForms() returned false ===");
		}

	}
}
