/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.moh763;

import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.stereotype.Component;

import liquibase.pro.packaged.by;

import java.util.Date;

/**
 * Library of cohort definitions used specifically in the MOH763 report (Mental Health).
 *
 * Disorder categories are matched by primary diagnosis (encounter_diagnosis.dx_rank = 1), joined to
 * etl_clinical_encounter (by patient_id + encounter_id) to apply the visit type ('New visit'/
 * 'Revisit') and reporting period filters, matching the join pattern already used in
 * Moh717CohortLibrary. Two helpers are available depending on what's supplied for a category:
 * {@link #byDiagnosisCodes} when the raw diagnosis_coded concept ids are known (the convention
 * already used in Moh711CohortLibrary#depression()/bipolarDisorder()), or {@link #byDiagnosisUuids}
 * when only concept uuids are known, resolved via a concept/concept_name join.
 *
 * Categories not yet supplied with their diagnosis codes fall back to a PLACEHOLDER: the
 * "where 1 = 0" clause guarantees an empty (never fabricated/misleading) result until the real
 * code list for that category is supplied.
 */
@Component
public class Moh763CohortLibrary {

	private CohortDefinition placeholder(String name, String description, String visitType) {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		// TODO: replace "where 1 = 0" with the real diagnosis_coded/diagnosis_category filter for this category
		String sqlQuery = "select ce.patient_id\n" +
				"from " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_clinical_encounter ce\n" +
				"where 1 = 0\n" +
				"  and ce.visit_type = '" + visitType + "'\n" +
				"  and date(ce.visit_date) between date(:startDate) and date(:endDate);";
		cd.setName(name);
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription(description + " (placeholder - awaiting diagnosis code mapping)");
		return cd;
	}

	/**
	 * Patients whose primary diagnosis (dx_rank = 1) is one of the given concept uuids, restricted
	 * to the given visit type ('New visit'/'Revisit') and reporting period.
	 */
	private CohortDefinition byDiagnosisUuids(String name, String description, String visitType, String[] conceptUuids) {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		StringBuilder uuidList = new StringBuilder();
		for (int i = 0; i < conceptUuids.length; i++) {
			if (i > 0) uuidList.append(",");
			uuidList.append("'").append(conceptUuids[i]).append("'");
		}
		String sqlQuery = "select ce.patient_id\n" +
				"from encounter_diagnosis d\n" +
				"inner join concept_name cn on cn.concept_id = d.diagnosis_coded\n" +
				"  and cn.concept_name_type = 'FULLY_SPECIFIED' and cn.locale = 'en'\n" +
				"inner join concept c on c.concept_id = cn.concept_id\n" +
				"inner join " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_clinical_encounter ce\n" +
				"  on ce.patient_id = d.patient_id and ce.encounter_id = d.encounter_id\n" +
				"where d.dx_rank = 1\n" +
				"  and d.voided = 0\n" +
				"  and c.uuid in (" + uuidList + ")\n" +
				"  and ce.visit_type = '" + visitType + "'\n" +
				"  and date(ce.visit_date) between date(:startDate) and date(:endDate);";
		cd.setName(name);
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription(description);
		return cd;
	}

	/**
	 * Patients whose primary diagnosis (dx_rank = 1) is one of the given diagnosis_coded concept
	 * ids directly, restricted to the given visit type ('New visit'/'Revisit') and reporting period.
	 * Use this instead of {@link #byDiagnosisUuids} when the concept ids are already known (matching
	 * the convention used in Moh711CohortLibrary#depression()/bipolarDisorder()), rather than uuids.
	 */
	private CohortDefinition byDiagnosisCodes(String name, String description, String visitType, int[] conceptIds) {
		SqlCohortDefinition cd = new SqlCohortDefinition();
		StringBuilder idList = new StringBuilder();
		for (int i = 0; i < conceptIds.length; i++) {
			if (i > 0) idList.append(",");
			idList.append(conceptIds[i]);
		}
		String sqlQuery = "select ce.patient_id\n" +
				"from encounter_diagnosis d\n" +
				"inner join " + CommonMetadata.EmrDatabaseEtl.ETL_DATABASE + ".etl_clinical_encounter ce\n" +
				"  on ce.patient_id = d.patient_id and ce.encounter_id = d.encounter_id\n" +
				"where d.dx_rank = 1\n" +
				"  and d.voided = 0\n" +
				"  and d.diagnosis_coded in (" + idList + ")\n" +
				"  and ce.visit_type = '" + visitType + "'\n" +
				"  and date(ce.visit_date) between date(:startDate) and date(:endDate);";
		cd.setName(name);
		cd.setQuery(sqlQuery);
		cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		cd.addParameter(new Parameter("endDate", "End Date", Date.class));
		cd.setDescription(description);
		return cd;
	}

		// Anxiety disorders (F40-F48-equivalent) - concept uuids supplied 2026-09-24
	private static final String[] ANXIETY_DISORDERS_UUIDS = {
			"139545AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"130966AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"149209AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"8504538c-ed43-405d-99b8-d18e0abd7584",
			"26aa14dd-08a5-4f18-a85b-2e8edc61ebac",
			"eb96a55b-f985-42be-ba27-686edc9d9ffd",
			"118678AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"4066e7ee-6d42-40a8-b345-d167d00e5df4",
			"e401126e-d5f9-4a79-bdc9-9043ca7beae2",
			"2a5c8791-6aec-4cbb-a513-9289a17043a2",
			"138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
	};

	// Depressive disorders - concept ids supplied 2026-09-24
	private static final String[] DEPRESSIVE_DISORDERS_CONCEPT_IDS = {
			"56511393-3578-4e4e-8393-b23b5c15d1d6",
			"127839AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"118779AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"134079AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"457bb553-85dc-4f18-a49e-8eb2a16dea23",
			"129205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
	};


	// Schizophrenia spectrum disorders - concept uuids supplied 2026-09-24
	private static final String[] SCHIZOPHRENIA_SPECTRUM_DISORDERS_UUIDS = {
			"113155AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"113146AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"127132AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"113139AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
			"eeef67e2-46c4-4034-9670-afebfb9e9f9c",
			"f9f64b04-3a42-4758-998f-1788fb8d4194"
	};

	// Bipolar disorders - concept ids supplied 2026-09-24
	private static final String[] BIPOLAR_DISORDERS_CONCEPT_IDS = {
		"b9b6c971-774b-4d62-9f10-f2fb5cf14153",
		"e268313a-fd3c-43ea-95bc-0482033129b2",
		"119706AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"f5617406-ef3c-4773-9403-bc4b63582548",
		"94bee6c4-ed77-4382-a8f4-704684e4c812",
		"cf2cf259-ad16-4244-9068-47df57215202"
	};

	// Other psychotic disorders - concept ids supplied 2026-09-24
	private static final String[] OTHER_PSYCHOTIC_DISORDERS_CONCEPT_IDS = {
		"149145AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"2c31570c-7d38-4ad0-8040-b16d25d79f0f",
		"b355cc13-2072-4a18-bb60-30484eb12500",
		"685536dd-5aa4-4da5-95e5-71c5e6cebfa7",
		"54c49691-f82b-4cd0-bd1a-b57429c22431",
		"154937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"119570AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
	};

	// neurodevelopmental disorders - concept ids supplied 2026-09-24
	private static final String[] NEURODEVELOPMENTAL_DISORDERS_CONCEPT_IDS = {
		"119570AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"ffb8aad6-f9a5-437c-a6d8-b906734ac069",
		"121317AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"299aaf03-e33b-4983-91d9-8f04a1418ecc",
		"4ea428da-b7a7-4768-b7bf-2042e553577c",
		"3b5effb7-34df-4aa4-aeb1-f75f85f08bbd",
		"1bcc0cc1-4fdd-4be7-83a8-5576193165df",
		"6caa64cc-9097-4622-ada2-0f70fc012110",
		"1c6075da-762c-48fa-93db-da3b84f34761",
		"0221d459-49f9-4cd3-ad1e-58982c93d3d6",
		"145283AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"040f4cc0-a590-4fd1-96f7-26bfd38d29e9",
		"bf79519e-3a46-4cac-a356-a24a8b5e94d6",
		"142498AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"ae264043-9e1e-47c1-afeb-7eaff0355cab",
		"577a5c59-935a-4309-b99f-aef107ad7950",
		"548afc14-8cf0-46e7-a193-722b08beb92e",
		"19428922-477a-4364-8f6c-2cde10bc951b",
		"125902AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"1d61de72-48ff-4de8-a358-5260a768e82d",
		"16c11d8c-40e3-4685-bbf4-e12fe4ec10cc"
	};

	// alcohol use disorders - concept ids supplied 2026-09-24
	private static final String[] ALCOHOL_USE_DISORDERS_CONCEPT_IDS = {
		"5bbc219b-e67e-49bb-b449-eb9d937da950",
		"f5d7b01a-754f-4aee-ac5a-3a08eb2af064",
		"149166AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"121722AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"14ec5d95-57d8-43fd-a7f1-2d58e9a4632d",
		"141734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"149145AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"149146AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"149147AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"0b433717-f2bc-4bb1-98d0-45bec0988f8f",
		"726c55c4-bef4-41c2-a906-311932d41dec",
		"077d1d27-b0f5-487e-891a-33d0d6e52ad2",
		"d41704aa-4d5e-48ea-84cf-2bd5a211ac3e"
	};

	// other disorders due to  
	private static final String[] OTHER_DISORDERS_CONCEPT_IDS = {
		"cca4fff0-7707-42cd-a309-a2c196cf5c4b",
		"55c593f9-acaa-4e11-9cef-a7ce9980ee26",
		"0b65f441-48f5-4a0a-b206-68fb8119faeb",
		"646e261f-7579-412e-96ee-8a69577b92c8",
		"5dda1ec4-5385-4513-a8f0-dc5ba2450e1b",
		"8a8fc595-9f57-45ea-ae3e-2cfe5069e6c5",
		"e36375ec-9237-49d7-bf8e-2ba1e17c3298",
		"001a2b64-066f-49c2-9bc4-e9e47ccf361a",
		"f24b51fa-c6bd-49bc-b7ec-eac5ba521c03",
		"e9a26871-c19c-41bc-8e46-ddb7a49139c8",
		"b5617065-3405-4548-8d38-a7148c630da7",
		"7669c08a-353c-4465-8d13-4b4d6c8fe2d1",
		"a762a368-3dde-4af1-a7b8-b64f52fab196",
		"3a862ca3-fc78-48d5-823a-85d54ec2790d",
		"4f1e4efe-85a8-4e84-aa77-21d77ddb3a12",
		"13aea26b-1258-4402-b4d9-335f470608f8",
		"47cec5d0-94ec-4708-84db-c135c1dad708",
		"b8a9af36-f3fe-40d0-a734-b6d7bf5fb9d2",
		"19c142d4-a0b4-4923-aabe-c4ee1b050096",
		"955a48ed-30f4-4635-b8a5-c54522ad1804",
		"fc9bbcbb-9efe-46f9-aae6-cb87963fd949"
	};

	// other disorders due to  
	private static final String[] OTHER_BEHAVIOR_ADDICTIONS_CONCEPT_IDS = {
		"c4a57ac8-f11f-430a-a434-bc8822fbc6d5",
		"21c54a00-3869-4c3b-929a-bdd2a423e981",
		"c7344878-07a0-4209-befb-5e24ea317fce",
		"71f6a43a-9ff2-4b81-92d3-5579c0dbef79",
		"33148022-6fb1-4085-b21c-80ab09d6b4a2",
		"8b4a5f86-08d3-44de-a6ef-d25a8f4d4248",
		"5b9dc5cc-7399-4dbf-9d41-be9c24ce1021"
	};	

	// neurocognitive disorders  
	private static final String[] NEUROCOGNITIVE_DISORDERS_CONCEPT_IDS = {
		"119574AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"0f01ea7e-b186-425c-a445-30e2b8a5357d",
		"148978AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"afd5e2fd-9606-471e-8f6f-9a1296f238f3",
		"dfd6dc3b-37fd-4331-8daf-3b7046aa2053",
		"117958AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"4cfecdac-3761-461d-9b2e-e4d02c3f6350",
		"2ea7a7cb-6e19-4bac-894f-5f578effe287",
		"1565ebdc-3e70-4c28-9d62-7b217be68b48",
		"4b5038bb-fea0-44c0-aeba-83121d2e1cb4",
		"fc9bbcbb-9efe-46f9-aae6-cb87963fd949",
		"ebbf4c61-29cc-4299-8f40-c9f910218d3b",
		"ad94666d-0014-431e-bd56-27fe32276862",
		"80c99bf1-7d35-449b-b9ca-172098ea5284"
	};	

	// stress trauma related disorders
	private static final String[] STRESS_TRAUMA_RELATED_DISORDERS_CONCEPT_IDS = {
		"113881AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"940d481d-7b20-453d-8f0c-e86344b49667",
		"4af7f0cf-533b-41fc-933b-a0c827cae97e",
		"fe29ed89-86e8-49f3-ac76-345495ab5236",
		"127891AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"3de4f470-5482-4fbf-b607-b48e2eae78eb",
		"32d29885-201b-4150-9644-11c416cfecbf",
		"6bc2468e-b04e-416a-a5ce-be9e41f8e64a"
	};
	
	// somatoform disorders
	private static final String[] SOMATOFORM_DISORDERS_CONCEPT_IDS = {
		"26436c0c-aaeb-4b3a-a4cc-45cc8dfdea74",
		"5e4b9137-7856-40b9-a593-0be1abb929d6",
		"8a16a79b-8e3a-4349-bde3-2f41015164dd",
		"cac7c25c-3f9b-49e1-b3d6-b35535fa8812",
		"138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"5b92b7c3-07a9-4c18-80c3-1c840602d6e0",
		"bb44fe1a-4b3d-4a81-87b9-c1e36e96c255",
		"e2b2d022-d41e-4bce-91c7-e9b83bea7d3c",
		"919ff5d9-09ac-4afd-afe5-3b8d2cdfb058",
		"134731AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"b7ac8707-ae34-4e39-89ca-d918d1a6de15",
		"6211a455-46e1-49d3-af6c-71864deba4b9",
		"22805850-f3ad-4b64-91d1-61d1824f8091"
	};

	// suicidal behaviour
	private static final String[] SUICIDAL_BEHAVIOUR_CONCEPT_IDS = {
		"b9d9b30c-2b23-4493-975c-32c2fd896e94",
		"148143AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"4520dde0-7345-4dc9-a861-b364b2c0a700"
	};

	// sleep-wake disorders
	private static final String[] SLEEP_WAKE_DISORDERS_CONCEPT_IDS = {
		"7f78e4e0-ff8e-4b16-af09-eccafb7aece2",
		"f95a8306-6ae7-45e3-8dde-a8d3e252e5c0",
		"c6b52d45-1273-4626-973f-00167bf474b6",
		"207c24ed-e246-45c6-8bb7-6991da56947d",
		"88b90426-b6a6-4952-8a22-4008f5491dd2",
		"117417AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"a198d25d-6f25-47c2-8a2d-b798f12ec19c",
		"115405AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"114346AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"612d1664-eb67-480e-853a-6c33600f262a",
		"aeda5387-8294-438e-87c8-e0bb7130bb04",
		"034ded79-2e73-4e9e-9c71-b0c6a0933bf6",
		"9ea1e860-5cff-49f0-95a0-95492e696165",
		"132511AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"6055d428-9753-44d9-9246-6c0e3db81686",
		"df1a494f-3ddd-4f0d-8a20-5f6cff3e117d",
		"dbb7c34a-6b6c-4f16-9528-175cccebe88c",
		"738beb96-0f2a-4a33-8e88-06d42e4f5ada",
		"c71750e9-37a6-4e58-b292-ef4d1d236c61"
	};

	// perinatal mental health disorders
	private static final String[] PERINATAL_MENTAL_DISORDERS_CONCEPT_IDS = {
		"98f48aad-13f5-460e-a756-3a9e425d5e1a",
		"26aa92a6-1daf-48cd-b0bd-21c507883246"
	};

	// Eating disorders
	private static final String[] EATING_DISORDERS_CONCEPT_IDS = {
		"148649AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"146847AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"3b62806a-8db0-4093-9c62-bffb9450c7dd",
		"d82f4663-27c4-44f1-8310-d0fdcbb1e31a",
		"130220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"b9685745-4fa9-4d17-a0bb-d11bc8b4cc53",
		"fa639caa-fee6-4203-85c8-474fe23a8a65",
		"cee67f6b-6900-43ff-8219-e239550565bf"
	};

	// Personality disorders
	private static final String[] PERSONALITY_DISORDERS_CONCEPT_IDS = {
		"f1f4caa1-2426-4aed-bedf-b5f437d8698c",
		"64cc16b0-6b33-4e38-ba82-17651a41fe21",
		"ec00fb28-df4b-4a82-9169-59a2ef42bd92",
		"Personality disorder, severity unspecified",
		"09a4b3f2-4a5d-4877-8585-5da5a4bc0700",
		"cf2a036a-de10-4c8d-b160-9da04b1e51c1",
		"2ef11235-f979-49d1-ae42-90ffc3c7b458",
		"b179b1f2-efef-422e-9b96-06d92b7d3a44",
		"9c5e711c-acac-4943-ac2a-8f19542cbf4f",
		"63832872-549c-4b55-ad7d-619876ee3a15",
		"130852AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"127126AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"113139AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"121547AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"121057AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"117503AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"115407AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"148065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"142569AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"132612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"114193AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"cd899ccb-ee23-4209-8a6c-ae64b8b5d8d8",
		"ca9ddce4-69ca-4309-879b-d636b57cd12e",
		"89f33703-8bcd-4049-ba63-e06ef618a6d0"
	};


	// Sexual dysfunctional disorders
	private static final String[] SEXUAL_DYSFUNCTIONAL_DISORDERS_CONCEPT_IDS = {
		"57ce55b2-76df-4b53-a530-e6730a7008df",
		"bb2f240b-19ec-4713-92ce-9ddc9743dc0d",
		"f3636e82-7f76-4f6b-bf52-3745a5ff5637",
		"339f665a-0ba0-45d2-a5c0-0e5292806356",
		"0b573f5d-9d06-4b2a-8893-8d1bb0ff2ca5",
		"755c02c4-c697-460a-96af-c17738760f0e",
		"5d61fa75-380f-4e9c-bae9-2f565d65bc70",
		"0b573f5d-9d06-4b2a-8893-8d1bb0ff2ca5",
		"091cc503-94a0-4ccc-ae5f-b8709bafd599",
		"df9cfd7c-2d19-4aa9-b3e9-045da02b29e8"
	};

	// Obsessive-compulsive & related disorders
	private static final String[] OBSESSIVE_COMPULSIVE_AND_RELATED_DISORDERS_CONCEPT_IDS = {
		"132611AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"112196AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"18ed68d3-f797-4b0a-8863-a52443c07e8a",
		"d1a5e49e-4f57-4dc7-b1f9-41368123723d",
		"147101AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"96c7d622-8fea-4655-a366-3be1be7adf11",
		"5aba3382-2a26-45be-976c-c5466669909e",
		"138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"3e1280d1-28fc-49b4-97d6-97e26535f584",
		"6db5c372-12e1-4817-b28e-2d4bb11a134e",
		"d8bb3f3c-fcde-496b-8dc6-05057411377e",
		"a0ea1038-65ea-4fe8-9166-812c6ace9fb6",
		"e670c97e-5a5f-42af-ab89-07dd7673cb28"
	};

	// Disruptive impulse control & conduct disorder
	private static final String[] DISRUPTIVE_IMPULSE_CONTROL_AND_CONDUCT_DISORDER_CONCEPT_IDS = {
		"131765AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"457bb553-85dc-4f18-a49e-8eb2a16dea23",
		"8cad54ad-8a4b-4902-bd76-7f56e097cb7f",
		"cd4d04b7-565f-4286-addb-72b66a492f1a",
		"bcab3a97-9f8f-4351-91c7-0e1a7cf4e24c",
		"136746AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"127984AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"136254AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
		"33148022-6fb1-4085-b21c-80ab09d6b4a2",
		"71f6a43a-9ff2-4b81-92d3-5579c0dbef79",
		"c95cbd2a-8ab3-42c7-ba83-6958bdb02fd6",
		"c34f7591-0ba8-4395-bbbc-721590380531"
	};

	public CohortDefinition anxietyDisorders(String visitType) {
		return byDiagnosisUuids("anxietyDisorders", "Anxiety disorders", visitType, ANXIETY_DISORDERS_UUIDS);
	}

	public CohortDefinition depressiveDisorders(String visitType) {
		return byDiagnosisUuids("depressiveDisorders", "Depressive disorders", visitType, DEPRESSIVE_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition schizophreniaSpectrumDisorders(String visitType) {
		return byDiagnosisUuids("schizophreniaSpectrumDisorders", "Schizophrenia spectrum disorders", visitType, SCHIZOPHRENIA_SPECTRUM_DISORDERS_UUIDS);
	}

	public CohortDefinition bipolarDisorders(String visitType) {
		return byDiagnosisUuids("bipolarDisorders", "Bipolar disorders", visitType, BIPOLAR_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition otherPsychoticDisorders(String visitType) {
		return byDiagnosisUuids("otherPsychoticDisorders", "Other psychotic disorders", visitType, OTHER_PSYCHOTIC_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition neurodevelopmentalDisorders(String visitType) {
		return byDiagnosisUuids("neurodevelopmentalDisorders", "Neurodevelopmental disorders", visitType, NEURODEVELOPMENTAL_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition alcoholUseDisorders(String visitType) {
		return byDiagnosisUuids("alcoholUseDisorders", "Alcohol use disorders", visitType, ALCOHOL_USE_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition otherSubstanceUseDisorders(String visitType) {
		return byDiagnosisUuids("otherSubstanceUseDisorders", "Other substance use disorders", visitType, OTHER_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition otherBehaviouralAddictions(String visitType) {
		return byDiagnosisUuids("otherBehaviouralAddictions", "Other behavioral addictions", visitType, OTHER_BEHAVIOR_ADDICTIONS_CONCEPT_IDS );
	}

	public CohortDefinition neurocognitiveDisorders(String visitType) {
		return byDiagnosisUuids("neurocognitiveDisorders", "Neurocognitive disorders", visitType, NEUROCOGNITIVE_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition stressTraumaRelatedDisorders(String visitType) {
		return byDiagnosisUuids("stressTraumaRelatedDisorders", "Stress trauma related disorders", visitType, STRESS_TRAUMA_RELATED_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition somatoformDisorders(String visitType) {
		return byDiagnosisUuids("somatoformDisorders", "Somatoform disorders", visitType, SOMATOFORM_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition suicidalBehaviour(String visitType) {
		return byDiagnosisUuids("suicidalBehaviour", "Suicidal behaviour", visitType, SUICIDAL_BEHAVIOUR_CONCEPT_IDS);
	}

	public CohortDefinition sleepWakeDisorders(String visitType) {
		return byDiagnosisUuids("sleepWakeDisorders", "Sleep-wake disorders", visitType, SLEEP_WAKE_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition perinatalMentalDisorders(String visitType) {
		return byDiagnosisUuids("perinatalMentalDisorders", "Perinatal mental disorders", visitType, PERINATAL_MENTAL_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition eatingDisorders(String visitType) {
		return byDiagnosisUuids("eatingDisorders", "Eating disorders", visitType, EATING_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition personalityDisorders(String visitType) {
		return byDiagnosisUuids("personalityDisorders", "Personality disorders", visitType, PERSONALITY_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition sexualDysfunctionalDisorders(String visitType) {
		return byDiagnosisUuids("sexualDysfunctionalDisorders", "Sexual dysfunctional disorders", visitType, SEXUAL_DYSFUNCTIONAL_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition obsessiveCompulsiveAndRelatedDisorders(String visitType) {
		return byDiagnosisUuids("obsessiveCompulsiveAndRelatedDisorders", "Obsessive-compulsive & related disorders", visitType, OBSESSIVE_COMPULSIVE_AND_RELATED_DISORDERS_CONCEPT_IDS);
	}

	public CohortDefinition disruptiveImpulseControlAndConductDisorder(String visitType) {
		return byDiagnosisUuids("disruptiveImpulseControlAndConductDisorder", "Disruptive impulse control & conduct disorder", visitType, DISRUPTIVE_IMPULSE_CONTROL_AND_CONDUCT_DISORDER_CONCEPT_IDS);
	}
}
