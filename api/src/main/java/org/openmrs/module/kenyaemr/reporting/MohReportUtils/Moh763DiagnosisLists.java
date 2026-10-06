/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.MohReportUtils;

import java.util.Arrays;
import java.util.List;

public class Moh763DiagnosisLists {

	// Anxiety disorders
	public static final class AnxietyDisorders {
		public static final String ANX_1 = "139545AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ANX_2 = "130966AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ANX_3 = "149209AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ANX_4 = "8504538c-ed43-405d-99b8-d18e0abd7584";
		public static final String ANX_5 = "26aa14dd-08a5-4f18-a85b-2e8edc61ebac";
		public static final String ANX_6 = "eb96a55b-f985-42be-ba27-686edc9d9ffd";
		public static final String ANX_7 = "118678AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ANX_8 = "4066e7ee-6d42-40a8-b345-d167d00e5df4";
		public static final String ANX_9 = "e401126e-d5f9-4a79-bdc9-9043ca7beae2";
		public static final String ANX_10 = "2a5c8791-6aec-4cbb-a513-9289a17043a2";
		public static final String ANX_11 = "138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	}

	public static List<String> getAnxietyDisordersUuidsList() {
		return Arrays.asList(AnxietyDisorders.ANX_1, AnxietyDisorders.ANX_2, AnxietyDisorders.ANX_3, AnxietyDisorders.ANX_4, AnxietyDisorders.ANX_5, AnxietyDisorders.ANX_6, AnxietyDisorders.ANX_7, AnxietyDisorders.ANX_8, AnxietyDisorders.ANX_9, AnxietyDisorders.ANX_10, AnxietyDisorders.ANX_11);
	}

	// Depressive disorders
	public static final class DepressiveDisorders {
		public static final String DEP_1 = "56511393-3578-4e4e-8393-b23b5c15d1d6";
		public static final String DEP_2 = "127839AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DEP_3 = "118779AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DEP_4 = "134079AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DEP_5 = "457bb553-85dc-4f18-a49e-8eb2a16dea23";
		public static final String DEP_6 = "129205AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	}

	public static List<String> getDepressiveDisordersUuidsList() {
		return Arrays.asList(DepressiveDisorders.DEP_1, DepressiveDisorders.DEP_2, DepressiveDisorders.DEP_3, DepressiveDisorders.DEP_4, DepressiveDisorders.DEP_5, DepressiveDisorders.DEP_6);
	}

	// Schizophrenia spectrum disorders
	public static final class SchizophreniaSpectrumDisorders {
		public static final String SCZ_1 = "113155AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SCZ_2 = "113146AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SCZ_3 = "127132AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SCZ_4 = "113139AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SCZ_5 = "eeef67e2-46c4-4034-9670-afebfb9e9f9c";
		public static final String SCZ_6 = "f9f64b04-3a42-4758-998f-1788fb8d4194";
	}

	public static List<String> getSchizophreniaSpectrumDisordersUuidsList() {
		return Arrays.asList(SchizophreniaSpectrumDisorders.SCZ_1, SchizophreniaSpectrumDisorders.SCZ_2, SchizophreniaSpectrumDisorders.SCZ_3, SchizophreniaSpectrumDisorders.SCZ_4, SchizophreniaSpectrumDisorders.SCZ_5, SchizophreniaSpectrumDisorders.SCZ_6);
	}

	// Bipolar disorders
	public static final class BipolarDisorders {
		public static final String BIP_1 = "b9b6c971-774b-4d62-9f10-f2fb5cf14153";
		public static final String BIP_2 = "e268313a-fd3c-43ea-95bc-0482033129b2";
		public static final String BIP_3 = "119706AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String BIP_4 = "f5617406-ef3c-4773-9403-bc4b63582548";
		public static final String BIP_5 = "94bee6c4-ed77-4382-a8f4-704684e4c812";
		public static final String BIP_6 = "cf2cf259-ad16-4244-9068-47df57215202";
	}

	public static List<String> getBipolarDisordersUuidsList() {
		return Arrays.asList(BipolarDisorders.BIP_1, BipolarDisorders.BIP_2, BipolarDisorders.BIP_3, BipolarDisorders.BIP_4, BipolarDisorders.BIP_5, BipolarDisorders.BIP_6);
	}

	// Other psychotic disorders
	public static final class OtherPsychoticDisorders {
		public static final String OPSY_1 = "149145AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OPSY_2 = "2c31570c-7d38-4ad0-8040-b16d25d79f0f";
		public static final String OPSY_3 = "b355cc13-2072-4a18-bb60-30484eb12500";
		public static final String OPSY_4 = "685536dd-5aa4-4da5-95e5-71c5e6cebfa7";
		public static final String OPSY_5 = "54c49691-f82b-4cd0-bd1a-b57429c22431";
		public static final String OPSY_6 = "154937AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OPSY_7 = "119570AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	}

	public static List<String> getOtherPsychoticDisordersUuidsList() {
		return Arrays.asList(OtherPsychoticDisorders.OPSY_1, OtherPsychoticDisorders.OPSY_2, OtherPsychoticDisorders.OPSY_3, OtherPsychoticDisorders.OPSY_4, OtherPsychoticDisorders.OPSY_5, OtherPsychoticDisorders.OPSY_6, OtherPsychoticDisorders.OPSY_7);
	}

	// Neurodevelopmental disorders
	public static final class NeurodevelopmentalDisorders {
		public static final String NDD_1 = "119570AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NDD_2 = "ffb8aad6-f9a5-437c-a6d8-b906734ac069";
		public static final String NDD_3 = "121317AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NDD_4 = "299aaf03-e33b-4983-91d9-8f04a1418ecc";
		public static final String NDD_5 = "4ea428da-b7a7-4768-b7bf-2042e553577c";
		public static final String NDD_6 = "3b5effb7-34df-4aa4-aeb1-f75f85f08bbd";
		public static final String NDD_7 = "1bcc0cc1-4fdd-4be7-83a8-5576193165df";
		public static final String NDD_8 = "6caa64cc-9097-4622-ada2-0f70fc012110";
		public static final String NDD_9 = "1c6075da-762c-48fa-93db-da3b84f34761";
		public static final String NDD_10 = "0221d459-49f9-4cd3-ad1e-58982c93d3d6";
		public static final String NDD_11 = "145283AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NDD_12 = "040f4cc0-a590-4fd1-96f7-26bfd38d29e9";
		public static final String NDD_13 = "bf79519e-3a46-4cac-a356-a24a8b5e94d6";
		public static final String NDD_14 = "142498AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NDD_15 = "ae264043-9e1e-47c1-afeb-7eaff0355cab";
		public static final String NDD_16 = "577a5c59-935a-4309-b99f-aef107ad7950";
		public static final String NDD_17 = "548afc14-8cf0-46e7-a193-722b08beb92e";
		public static final String NDD_18 = "19428922-477a-4364-8f6c-2cde10bc951b";
		public static final String NDD_19 = "125902AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NDD_20 = "1d61de72-48ff-4de8-a358-5260a768e82d";
		public static final String NDD_21 = "16c11d8c-40e3-4685-bbf4-e12fe4ec10cc";
	}

	public static List<String> getNeurodevelopmentalDisordersUuidsList() {
		return Arrays.asList(NeurodevelopmentalDisorders.NDD_1, NeurodevelopmentalDisorders.NDD_2, NeurodevelopmentalDisorders.NDD_3, NeurodevelopmentalDisorders.NDD_4, NeurodevelopmentalDisorders.NDD_5, NeurodevelopmentalDisorders.NDD_6, NeurodevelopmentalDisorders.NDD_7, NeurodevelopmentalDisorders.NDD_8, NeurodevelopmentalDisorders.NDD_9, NeurodevelopmentalDisorders.NDD_10, NeurodevelopmentalDisorders.NDD_11, NeurodevelopmentalDisorders.NDD_12, NeurodevelopmentalDisorders.NDD_13, NeurodevelopmentalDisorders.NDD_14, NeurodevelopmentalDisorders.NDD_15, NeurodevelopmentalDisorders.NDD_16, NeurodevelopmentalDisorders.NDD_17, NeurodevelopmentalDisorders.NDD_18, NeurodevelopmentalDisorders.NDD_19, NeurodevelopmentalDisorders.NDD_20, NeurodevelopmentalDisorders.NDD_21);
	}

	// Alcohol use disorders
	public static final class AlcoholUseDisorders {
		public static final String ALC_1 = "5bbc219b-e67e-49bb-b449-eb9d937da950";
		public static final String ALC_2 = "f5d7b01a-754f-4aee-ac5a-3a08eb2af064";
		public static final String ALC_3 = "149166AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_4 = "121722AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_5 = "14ec5d95-57d8-43fd-a7f1-2d58e9a4632d";
		public static final String ALC_6 = "141734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_7 = "149145AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_8 = "149146AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_9 = "149147AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String ALC_10 = "0b433717-f2bc-4bb1-98d0-45bec0988f8f";
		public static final String ALC_11 = "726c55c4-bef4-41c2-a906-311932d41dec";
		public static final String ALC_12 = "077d1d27-b0f5-487e-891a-33d0d6e52ad2";
		public static final String ALC_13 = "d41704aa-4d5e-48ea-84cf-2bd5a211ac3e";
	}

	public static List<String> getAlcoholUseDisordersUuidsList() {
		return Arrays.asList(AlcoholUseDisorders.ALC_1, AlcoholUseDisorders.ALC_2, AlcoholUseDisorders.ALC_3, AlcoholUseDisorders.ALC_4, AlcoholUseDisorders.ALC_5, AlcoholUseDisorders.ALC_6, AlcoholUseDisorders.ALC_7, AlcoholUseDisorders.ALC_8, AlcoholUseDisorders.ALC_9, AlcoholUseDisorders.ALC_10, AlcoholUseDisorders.ALC_11, AlcoholUseDisorders.ALC_12, AlcoholUseDisorders.ALC_13);
	}

	// Other disorders due to substance use
	public static final class OtherSubstanceUseDisorders {
		public static final String OSUB_1 = "cca4fff0-7707-42cd-a309-a2c196cf5c4b";
		public static final String OSUB_2 = "55c593f9-acaa-4e11-9cef-a7ce9980ee26";
		public static final String OSUB_3 = "0b65f441-48f5-4a0a-b206-68fb8119faeb";
		public static final String OSUB_4 = "646e261f-7579-412e-96ee-8a69577b92c8";
		public static final String OSUB_5 = "5dda1ec4-5385-4513-a8f0-dc5ba2450e1b";
		public static final String OSUB_6 = "8a8fc595-9f57-45ea-ae3e-2cfe5069e6c5";
		public static final String OSUB_7 = "e36375ec-9237-49d7-bf8e-2ba1e17c3298";
		public static final String OSUB_8 = "001a2b64-066f-49c2-9bc4-e9e47ccf361a";
		public static final String OSUB_9 = "f24b51fa-c6bd-49bc-b7ec-eac5ba521c03";
		public static final String OSUB_10 = "e9a26871-c19c-41bc-8e46-ddb7a49139c8";
		public static final String OSUB_11 = "b5617065-3405-4548-8d38-a7148c630da7";
		public static final String OSUB_12 = "7669c08a-353c-4465-8d13-4b4d6c8fe2d1";
		public static final String OSUB_13 = "a762a368-3dde-4af1-a7b8-b64f52fab196";
		public static final String OSUB_14 = "3a862ca3-fc78-48d5-823a-85d54ec2790d";
		public static final String OSUB_15 = "4f1e4efe-85a8-4e84-aa77-21d77ddb3a12";
		public static final String OSUB_16 = "13aea26b-1258-4402-b4d9-335f470608f8";
		public static final String OSUB_17 = "47cec5d0-94ec-4708-84db-c135c1dad708";
		public static final String OSUB_18 = "b8a9af36-f3fe-40d0-a734-b6d7bf5fb9d2";
		public static final String OSUB_19 = "19c142d4-a0b4-4923-aabe-c4ee1b050096";
		public static final String OSUB_20 = "955a48ed-30f4-4635-b8a5-c54522ad1804";
		public static final String OSUB_21 = "fc9bbcbb-9efe-46f9-aae6-cb87963fd949";
	}

	public static List<String> getOtherSubstanceUseDisordersUuidsList() {
		return Arrays.asList(OtherSubstanceUseDisorders.OSUB_1, OtherSubstanceUseDisorders.OSUB_2, OtherSubstanceUseDisorders.OSUB_3, OtherSubstanceUseDisorders.OSUB_4, OtherSubstanceUseDisorders.OSUB_5, OtherSubstanceUseDisorders.OSUB_6, OtherSubstanceUseDisorders.OSUB_7, OtherSubstanceUseDisorders.OSUB_8, OtherSubstanceUseDisorders.OSUB_9, OtherSubstanceUseDisorders.OSUB_10, OtherSubstanceUseDisorders.OSUB_11, OtherSubstanceUseDisorders.OSUB_12, OtherSubstanceUseDisorders.OSUB_13, OtherSubstanceUseDisorders.OSUB_14, OtherSubstanceUseDisorders.OSUB_15, OtherSubstanceUseDisorders.OSUB_16, OtherSubstanceUseDisorders.OSUB_17, OtherSubstanceUseDisorders.OSUB_18, OtherSubstanceUseDisorders.OSUB_19, OtherSubstanceUseDisorders.OSUB_20, OtherSubstanceUseDisorders.OSUB_21);
	}

	// Other behavioral addictions
	public static final class OtherBehaviouralAddictions {
		public static final String OBA_1 = "c4a57ac8-f11f-430a-a434-bc8822fbc6d5";
		public static final String OBA_2 = "21c54a00-3869-4c3b-929a-bdd2a423e981";
		public static final String OBA_3 = "c7344878-07a0-4209-befb-5e24ea317fce";
		public static final String OBA_4 = "71f6a43a-9ff2-4b81-92d3-5579c0dbef79";
		public static final String OBA_5 = "33148022-6fb1-4085-b21c-80ab09d6b4a2";
		public static final String OBA_6 = "8b4a5f86-08d3-44de-a6ef-d25a8f4d4248";
		public static final String OBA_7 = "5b9dc5cc-7399-4dbf-9d41-be9c24ce1021";
	}

	public static List<String> getOtherBehaviouralAddictionsUuidsList() {
		return Arrays.asList(OtherBehaviouralAddictions.OBA_1, OtherBehaviouralAddictions.OBA_2, OtherBehaviouralAddictions.OBA_3, OtherBehaviouralAddictions.OBA_4, OtherBehaviouralAddictions.OBA_5, OtherBehaviouralAddictions.OBA_6, OtherBehaviouralAddictions.OBA_7);
	}

	// Neurocognitive disorders
	public static final class NeurocognitiveDisorders {
		public static final String NCD_1 = "119574AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NCD_2 = "0f01ea7e-b186-425c-a445-30e2b8a5357d";
		public static final String NCD_3 = "148978AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NCD_4 = "afd5e2fd-9606-471e-8f6f-9a1296f238f3";
		public static final String NCD_5 = "dfd6dc3b-37fd-4331-8daf-3b7046aa2053";
		public static final String NCD_6 = "117958AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String NCD_7 = "4cfecdac-3761-461d-9b2e-e4d02c3f6350";
		public static final String NCD_8 = "2ea7a7cb-6e19-4bac-894f-5f578effe287";
		public static final String NCD_9 = "1565ebdc-3e70-4c28-9d62-7b217be68b48";
		public static final String NCD_10 = "4b5038bb-fea0-44c0-aeba-83121d2e1cb4";
		public static final String NCD_11 = "fc9bbcbb-9efe-46f9-aae6-cb87963fd949";
		public static final String NCD_12 = "ebbf4c61-29cc-4299-8f40-c9f910218d3b";
		public static final String NCD_13 = "ad94666d-0014-431e-bd56-27fe32276862";
		public static final String NCD_14 = "80c99bf1-7d35-449b-b9ca-172098ea5284";
	}

	public static List<String> getNeurocognitiveDisordersUuidsList() {
		return Arrays.asList(NeurocognitiveDisorders.NCD_1, NeurocognitiveDisorders.NCD_2, NeurocognitiveDisorders.NCD_3, NeurocognitiveDisorders.NCD_4, NeurocognitiveDisorders.NCD_5, NeurocognitiveDisorders.NCD_6, NeurocognitiveDisorders.NCD_7, NeurocognitiveDisorders.NCD_8, NeurocognitiveDisorders.NCD_9, NeurocognitiveDisorders.NCD_10, NeurocognitiveDisorders.NCD_11, NeurocognitiveDisorders.NCD_12, NeurocognitiveDisorders.NCD_13, NeurocognitiveDisorders.NCD_14);
	}

	// Stress-trauma related disorders
	public static final class StressTraumaRelatedDisorders {
		public static final String STR_1 = "113881AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String STR_2 = "940d481d-7b20-453d-8f0c-e86344b49667";
		public static final String STR_3 = "4af7f0cf-533b-41fc-933b-a0c827cae97e";
		public static final String STR_4 = "fe29ed89-86e8-49f3-ac76-345495ab5236";
		public static final String STR_5 = "127891AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String STR_6 = "3de4f470-5482-4fbf-b607-b48e2eae78eb";
		public static final String STR_7 = "32d29885-201b-4150-9644-11c416cfecbf";
		public static final String STR_8 = "6bc2468e-b04e-416a-a5ce-be9e41f8e64a";
	}

	public static List<String> getStressTraumaRelatedDisordersUuidsList() {
		return Arrays.asList(StressTraumaRelatedDisorders.STR_1, StressTraumaRelatedDisorders.STR_2, StressTraumaRelatedDisorders.STR_3, StressTraumaRelatedDisorders.STR_4, StressTraumaRelatedDisorders.STR_5, StressTraumaRelatedDisorders.STR_6, StressTraumaRelatedDisorders.STR_7, StressTraumaRelatedDisorders.STR_8);
	}

	// Somatoform disorders
	public static final class SomatoformDisorders {
		public static final String SOM_1 = "26436c0c-aaeb-4b3a-a4cc-45cc8dfdea74";
		public static final String SOM_2 = "5e4b9137-7856-40b9-a593-0be1abb929d6";
		public static final String SOM_3 = "8a16a79b-8e3a-4349-bde3-2f41015164dd";
		public static final String SOM_4 = "cac7c25c-3f9b-49e1-b3d6-b35535fa8812";
		public static final String SOM_5 = "138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SOM_6 = "138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SOM_7 = "5b92b7c3-07a9-4c18-80c3-1c840602d6e0";
		public static final String SOM_8 = "bb44fe1a-4b3d-4a81-87b9-c1e36e96c255";
		public static final String SOM_9 = "e2b2d022-d41e-4bce-91c7-e9b83bea7d3c";
		public static final String SOM_10 = "919ff5d9-09ac-4afd-afe5-3b8d2cdfb058";
		public static final String SOM_11 = "134731AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SOM_12 = "b7ac8707-ae34-4e39-89ca-d918d1a6de15";
		public static final String SOM_13 = "6211a455-46e1-49d3-af6c-71864deba4b9";
		public static final String SOM_14 = "22805850-f3ad-4b64-91d1-61d1824f8091";
	}

	public static List<String> getSomatoformDisordersUuidsList() {
		return Arrays.asList(SomatoformDisorders.SOM_1, SomatoformDisorders.SOM_2, SomatoformDisorders.SOM_3, SomatoformDisorders.SOM_4, SomatoformDisorders.SOM_5, SomatoformDisorders.SOM_6, SomatoformDisorders.SOM_7, SomatoformDisorders.SOM_8, SomatoformDisorders.SOM_9, SomatoformDisorders.SOM_10, SomatoformDisorders.SOM_11, SomatoformDisorders.SOM_12, SomatoformDisorders.SOM_13, SomatoformDisorders.SOM_14);
	}

	// Suicidal behaviour
	public static final class SuicidalBehaviour {
		public static final String SUI_1 = "b9d9b30c-2b23-4493-975c-32c2fd896e94";
		public static final String SUI_2 = "148143AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SUI_3 = "4520dde0-7345-4dc9-a861-b364b2c0a700";
	}

	public static List<String> getSuicidalBehaviourUuidsList() {
		return Arrays.asList(SuicidalBehaviour.SUI_1, SuicidalBehaviour.SUI_2, SuicidalBehaviour.SUI_3);
	}

	// Sleep-wake disorders
	public static final class SleepWakeDisorders {
		public static final String SLP_1 = "7f78e4e0-ff8e-4b16-af09-eccafb7aece2";
		public static final String SLP_2 = "f95a8306-6ae7-45e3-8dde-a8d3e252e5c0";
		public static final String SLP_3 = "c6b52d45-1273-4626-973f-00167bf474b6";
		public static final String SLP_4 = "207c24ed-e246-45c6-8bb7-6991da56947d";
		public static final String SLP_5 = "88b90426-b6a6-4952-8a22-4008f5491dd2";
		public static final String SLP_6 = "117417AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SLP_7 = "a198d25d-6f25-47c2-8a2d-b798f12ec19c";
		public static final String SLP_8 = "115405AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SLP_9 = "114346AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SLP_10 = "612d1664-eb67-480e-853a-6c33600f262a";
		public static final String SLP_11 = "aeda5387-8294-438e-87c8-e0bb7130bb04";
		public static final String SLP_12 = "034ded79-2e73-4e9e-9c71-b0c6a0933bf6";
		public static final String SLP_13 = "9ea1e860-5cff-49f0-95a0-95492e696165";
		public static final String SLP_14 = "132511AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String SLP_15 = "6055d428-9753-44d9-9246-6c0e3db81686";
		public static final String SLP_16 = "df1a494f-3ddd-4f0d-8a20-5f6cff3e117d";
		public static final String SLP_17 = "dbb7c34a-6b6c-4f16-9528-175cccebe88c";
		public static final String SLP_18 = "738beb96-0f2a-4a33-8e88-06d42e4f5ada";
		public static final String SLP_19 = "c71750e9-37a6-4e58-b292-ef4d1d236c61";
	}

	public static List<String> getSleepWakeDisordersUuidsList() {
		return Arrays.asList(SleepWakeDisorders.SLP_1, SleepWakeDisorders.SLP_2, SleepWakeDisorders.SLP_3, SleepWakeDisorders.SLP_4, SleepWakeDisorders.SLP_5, SleepWakeDisorders.SLP_6, SleepWakeDisorders.SLP_7, SleepWakeDisorders.SLP_8, SleepWakeDisorders.SLP_9, SleepWakeDisorders.SLP_10, SleepWakeDisorders.SLP_11, SleepWakeDisorders.SLP_12, SleepWakeDisorders.SLP_13, SleepWakeDisorders.SLP_14, SleepWakeDisorders.SLP_15, SleepWakeDisorders.SLP_16, SleepWakeDisorders.SLP_17, SleepWakeDisorders.SLP_18, SleepWakeDisorders.SLP_19);
	}

	// Perinatal mental disorders
	public static final class PerinatalMentalDisorders {
		public static final String PMD_1 = "98f48aad-13f5-460e-a756-3a9e425d5e1a";
		public static final String PMD_2 = "26aa92a6-1daf-48cd-b0bd-21c507883246";
	}

	public static List<String> getPerinatalMentalDisordersUuidsList() {
		return Arrays.asList(PerinatalMentalDisorders.PMD_1, PerinatalMentalDisorders.PMD_2);
	}

	// Eating disorders
	public static final class EatingDisorders {
		public static final String EAT_1 = "148649AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String EAT_2 = "146847AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String EAT_3 = "3b62806a-8db0-4093-9c62-bffb9450c7dd";
		public static final String EAT_4 = "d82f4663-27c4-44f1-8310-d0fdcbb1e31a";
		public static final String EAT_5 = "130220AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String EAT_6 = "b9685745-4fa9-4d17-a0bb-d11bc8b4cc53";
		public static final String EAT_7 = "fa639caa-fee6-4203-85c8-474fe23a8a65";
		public static final String EAT_8 = "cee67f6b-6900-43ff-8219-e239550565bf";
	}

	public static List<String> getEatingDisordersUuidsList() {
		return Arrays.asList(EatingDisorders.EAT_1, EatingDisorders.EAT_2, EatingDisorders.EAT_3, EatingDisorders.EAT_4, EatingDisorders.EAT_5, EatingDisorders.EAT_6, EatingDisorders.EAT_7, EatingDisorders.EAT_8);
	}

	// Personality disorders
	public static final class PersonalityDisorders {
		public static final String PER_1 = "f1f4caa1-2426-4aed-bedf-b5f437d8698c";
		public static final String PER_2 = "64cc16b0-6b33-4e38-ba82-17651a41fe21";
		public static final String PER_3 = "ec00fb28-df4b-4a82-9169-59a2ef42bd92";
		public static final String PER_4 = "09a4b3f2-4a5d-4877-8585-5da5a4bc0700";
		public static final String PER_5 = "cf2a036a-de10-4c8d-b160-9da04b1e51c1";
		public static final String PER_6 = "2ef11235-f979-49d1-ae42-90ffc3c7b458";
		public static final String PER_7 = "b179b1f2-efef-422e-9b96-06d92b7d3a44";
		public static final String PER_8 = "9c5e711c-acac-4943-ac2a-8f19542cbf4f";
		public static final String PER_9 = "63832872-549c-4b55-ad7d-619876ee3a15";
		public static final String PER_10 = "130852AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_11 = "127126AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_12 = "127126AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_13 = "113139AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_14 = "121547AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_15 = "121057AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_16 = "117503AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_17 = "115407AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_18 = "148065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_19 = "142569AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_20 = "132612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_21 = "114193AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String PER_22 = "cd899ccb-ee23-4209-8a6c-ae64b8b5d8d8";
		public static final String PER_23 = "ca9ddce4-69ca-4309-879b-d636b57cd12e";
		public static final String PER_24 = "89f33703-8bcd-4049-ba63-e06ef618a6d0";
	}

	public static List<String> getPersonalityDisordersUuidsList() {
		return Arrays.asList(PersonalityDisorders.PER_1, PersonalityDisorders.PER_2, PersonalityDisorders.PER_3, PersonalityDisorders.PER_4, PersonalityDisorders.PER_5, PersonalityDisorders.PER_6, PersonalityDisorders.PER_7, PersonalityDisorders.PER_8, PersonalityDisorders.PER_9, PersonalityDisorders.PER_10, PersonalityDisorders.PER_11, PersonalityDisorders.PER_12, PersonalityDisorders.PER_13, PersonalityDisorders.PER_14, PersonalityDisorders.PER_15, PersonalityDisorders.PER_16, PersonalityDisorders.PER_17, PersonalityDisorders.PER_18, PersonalityDisorders.PER_19, PersonalityDisorders.PER_20, PersonalityDisorders.PER_21, PersonalityDisorders.PER_22, PersonalityDisorders.PER_23, PersonalityDisorders.PER_24);
	}

	// Sexual dysfunctional disorders
	public static final class SexualDysfunctionalDisorders {
		public static final String SEX_1 = "57ce55b2-76df-4b53-a530-e6730a7008df";
		public static final String SEX_2 = "bb2f240b-19ec-4713-92ce-9ddc9743dc0d";
		public static final String SEX_3 = "f3636e82-7f76-4f6b-bf52-3745a5ff5637";
		public static final String SEX_4 = "339f665a-0ba0-45d2-a5c0-0e5292806356";
		public static final String SEX_5 = "0b573f5d-9d06-4b2a-8893-8d1bb0ff2ca5";
		public static final String SEX_6 = "755c02c4-c697-460a-96af-c17738760f0e";
		public static final String SEX_7 = "5d61fa75-380f-4e9c-bae9-2f565d65bc70";
		public static final String SEX_8 = "0b573f5d-9d06-4b2a-8893-8d1bb0ff2ca5";
		public static final String SEX_9 = "091cc503-94a0-4ccc-ae5f-b8709bafd599";
		public static final String SEX_10 = "df9cfd7c-2d19-4aa9-b3e9-045da02b29e8";
	}

	public static List<String> getSexualDysfunctionalDisordersUuidsList() {
		return Arrays.asList(SexualDysfunctionalDisorders.SEX_1, SexualDysfunctionalDisorders.SEX_2, SexualDysfunctionalDisorders.SEX_3, SexualDysfunctionalDisorders.SEX_4, SexualDysfunctionalDisorders.SEX_5, SexualDysfunctionalDisorders.SEX_6, SexualDysfunctionalDisorders.SEX_7, SexualDysfunctionalDisorders.SEX_8, SexualDysfunctionalDisorders.SEX_9, SexualDysfunctionalDisorders.SEX_10);
	}

	// Obsessive-compulsive & related disorders
	public static final class ObsessiveCompulsiveAndRelatedDisorders {
		public static final String OCD_1 = "132611AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OCD_2 = "112196AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OCD_3 = "18ed68d3-f797-4b0a-8863-a52443c07e8a";
		public static final String OCD_4 = "d1a5e49e-4f57-4dc7-b1f9-41368123723d";
		public static final String OCD_5 = "147101AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OCD_6 = "96c7d622-8fea-4655-a366-3be1be7adf11";
		public static final String OCD_7 = "5aba3382-2a26-45be-976c-c5466669909e";
		public static final String OCD_8 = "138075AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String OCD_9 = "3e1280d1-28fc-49b4-97d6-97e26535f584";
		public static final String OCD_10 = "6db5c372-12e1-4817-b28e-2d4bb11a134e";
		public static final String OCD_11 = "d8bb3f3c-fcde-496b-8dc6-05057411377e";
		public static final String OCD_12 = "a0ea1038-65ea-4fe8-9166-812c6ace9fb6";
		public static final String OCD_13 = "e670c97e-5a5f-42af-ab89-07dd7673cb28";
	}

	public static List<String> getObsessiveCompulsiveAndRelatedDisordersUuidsList() {
		return Arrays.asList(ObsessiveCompulsiveAndRelatedDisorders.OCD_1, ObsessiveCompulsiveAndRelatedDisorders.OCD_2, ObsessiveCompulsiveAndRelatedDisorders.OCD_3, ObsessiveCompulsiveAndRelatedDisorders.OCD_4, ObsessiveCompulsiveAndRelatedDisorders.OCD_5, ObsessiveCompulsiveAndRelatedDisorders.OCD_6, ObsessiveCompulsiveAndRelatedDisorders.OCD_7, ObsessiveCompulsiveAndRelatedDisorders.OCD_8, ObsessiveCompulsiveAndRelatedDisorders.OCD_9, ObsessiveCompulsiveAndRelatedDisorders.OCD_10, ObsessiveCompulsiveAndRelatedDisorders.OCD_11, ObsessiveCompulsiveAndRelatedDisorders.OCD_12, ObsessiveCompulsiveAndRelatedDisorders.OCD_13);
	}

	// Disruptive impulse control & conduct disorder
	public static final class DisruptiveImpulseControlAndConductDisorder {
		public static final String DICD_1 = "131765AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DICD_2 = "457bb553-85dc-4f18-a49e-8eb2a16dea23";
		public static final String DICD_3 = "8cad54ad-8a4b-4902-bd76-7f56e097cb7f";
		public static final String DICD_4 = "cd4d04b7-565f-4286-addb-72b66a492f1a";
		public static final String DICD_5 = "bcab3a97-9f8f-4351-91c7-0e1a7cf4e24c";
		public static final String DICD_6 = "136746AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DICD_7 = "127984AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DICD_8 = "136254AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		public static final String DICD_9 = "33148022-6fb1-4085-b21c-80ab09d6b4a2";
		public static final String DICD_10 = "71f6a43a-9ff2-4b81-92d3-5579c0dbef79";
		public static final String DICD_11 = "c95cbd2a-8ab3-42c7-ba83-6958bdb02fd6";
		public static final String DICD_12 = "c34f7591-0ba8-4395-bbbc-721590380531";
	}

	public static List<String> getDisruptiveImpulseControlAndConductDisorderUuidsList() {
		return Arrays.asList(DisruptiveImpulseControlAndConductDisorder.DICD_1, DisruptiveImpulseControlAndConductDisorder.DICD_2, DisruptiveImpulseControlAndConductDisorder.DICD_3, DisruptiveImpulseControlAndConductDisorder.DICD_4, DisruptiveImpulseControlAndConductDisorder.DICD_5, DisruptiveImpulseControlAndConductDisorder.DICD_6, DisruptiveImpulseControlAndConductDisorder.DICD_7, DisruptiveImpulseControlAndConductDisorder.DICD_8, DisruptiveImpulseControlAndConductDisorder.DICD_9, DisruptiveImpulseControlAndConductDisorder.DICD_10, DisruptiveImpulseControlAndConductDisorder.DICD_11, DisruptiveImpulseControlAndConductDisorder.DICD_12);
	}

}