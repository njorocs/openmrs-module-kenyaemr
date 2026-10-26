/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.malaria;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Drug ids (drug.drug_id) of the malaria commodities, shared by the MOH 743 commodity rows, the
 * MOH 743 patient counts and the MOH 643 ACT courses, so the three count the same products.
 * Each list has the current drug and, for counting past orders, the retired brand entries of the
 * same product. The RDT kit and nets are stock items without a drug, so they are named by concept.
 */
public final class MalariaDrugs {

	/** Concept of the stock item "Rapid diagnostic test kit for malaria" */
	public static final int RAPID_DIAGNOSTIC_TEST_CONCEPT = 161465;

	/** Concept of the stock item "Long-lasting insecticidal net" */
	public static final int LONG_LASTING_INSECTICIDAL_NET_CONCEPT = 160428;

	public static final List<Integer> ARTEMETHER_LUMEFANTRINE_20_120MG_6S = Collections.singletonList(1403);

	public static final List<Integer> ARTEMETHER_LUMEFANTRINE_20_120MG_12S = Collections.singletonList(18010);

	public static final List<Integer> ARTEMETHER_LUMEFANTRINE_20_120MG_18S = Collections.singletonList(18011);

	public static final List<Integer> ARTEMETHER_LUMEFANTRINE_20_120MG_24S = Collections.singletonList(18012);

	/** Other artemether-lumefantrine tablets: 40/480mg, and retired brand entries of 20/120mg */
	public static final List<Integer> ARTEMETHER_LUMEFANTRINE_OTHER = Arrays.asList(8197, 8580, 14009, 14010, 14011, 14018,
			14019, 14020, 14021, 14022, 14023, 14024, 14025, 14026, 14027, 14028, 14029, 14030, 14031, 14032, 14033, 14034,
			14035, 14036, 14037, 14038, 14039, 14040, 14041, 14042, 14043, 14044, 14049, 14050, 15092, 16260, 16266, 16267,
			16269, 16272, 16273, 16274, 16275, 16276, 16281, 16282, 16284, 16303);

	public static final List<Integer> ARTESUNATE_PYRONARIDINE_20_60MG_GRANULES = Collections.singletonList(7520);

	public static final List<Integer> ARTESUNATE_PYRONARIDINE_60_180MG_TAB = Collections.singletonList(7518);

	public static final List<Integer> DIHYDROARTEMISININ_PIPERAQUINE_20_160MG_TAB = Collections.singletonList(5601);

	public static final List<Integer> DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_DISPERSIBLE_TAB = Collections.singletonList(17998);

	public static final List<Integer> DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_TAB = Collections.singletonList(5600);

	/** Artesunate-amodiaquine: retired brand entries only, kept for past orders */
	public static final List<Integer> ARTESUNATE_AMODIAQUINE_67_25MG_TAB = Arrays.asList(14490, 14491, 14492, 14493, 15095,
			16297, 16301, 20267);

	public static final List<Integer> ARTESUNATE_AMODIAQUINE_135_50MG_TAB = Arrays.asList(14494, 14495, 14496, 14497, 15096,
			16293, 16296, 16300, 20263);

	public static final List<Integer> ARTESUNATE_AMODIAQUINE_270_100MG_TAB = Arrays.asList(14486, 14487, 14488, 14489, 15097,
			16291, 16295, 20261);

	/** Artesunate injection 60mg and 30mg, and retired brand entries of 60mg */
	public static final List<Integer> ARTESUNATE_INJECTION = Arrays.asList(8045, 8044, 9580, 9581, 9583, 9584, 9593, 9595,
			9596, 9598, 9604, 9605, 9607, 9608, 15620, 16288, 16289, 16298);

	/** Sulphadoxine-pyrimethamine 500/25mg, and retired brand entries */
	public static final List<Integer> SULPHADOXINE_PYRIMETHAMINE_500_25MG_TAB = Arrays.asList(2070, 14145, 14147, 14148,
			14149, 14150, 14151, 17699, 17700, 17841);

	/** Artemether injection 80mg/ml, and retired entries of artemether injection and oral suspension */
	public static final List<Integer> ARTEMETHER = Arrays.asList(8006, 1404, 9552, 9554, 9555, 9557, 9558, 9560, 9561, 9563,
			9564, 9566, 9568, 9569, 9571, 9572, 16243, 16244, 16245, 16246, 16247, 16248, 16249, 16250, 16251, 16252, 16253,
			16254, 16255, 16256, 16257, 16258, 16268, 16270, 16280, 18071, 20114, 20235, 20237, 20239, 20251);

	/** Rectal artesunate 100mg (pre-referral), and retired entries of 50, 100 and 200mg */
	public static final List<Integer> ARTESUNATE_SUPPOSITORY = Arrays.asList(8046, 1407, 1412, 9586, 9587, 9589, 9590, 16299,
			20268, 21209);

	/** Quinine tablet 300mg and injection 300mg/ml, and retired entries of tablets, syrup and injection */
	public static final List<Integer> QUININE = Arrays.asList(2072, 2074, 2073, 6807, 6808, 6809, 6810, 6811, 6812, 6813, 8841,
			8842, 8858, 8860, 8868, 8871, 8878, 8879, 8885, 8888, 9550, 9551, 9615, 17575, 17576, 17577, 17578, 17579, 17580,
			17581, 17582, 17583, 17584, 17585, 17586, 17587, 18984, 18986, 18987, 20657, 20660);

	public static final List<Integer> PRIMAQUINE_7_5MG_TAB = Collections.singletonList(2048);

	public static final List<Integer> PRIMAQUINE_15MG_TAB = Collections.singletonList(2047);

	/** Every artemether-lumefantrine tablet */
	public static final List<Integer> ARTEMETHER_LUMEFANTRINE = union(ARTEMETHER_LUMEFANTRINE_20_120MG_6S,
			ARTEMETHER_LUMEFANTRINE_20_120MG_12S, ARTEMETHER_LUMEFANTRINE_20_120MG_18S, ARTEMETHER_LUMEFANTRINE_20_120MG_24S,
			ARTEMETHER_LUMEFANTRINE_OTHER);

	/** Artemisinin-based combination therapies: the "Patients given an ACT" section of MOH 743 */
	public static final List<Integer> ACTS = union(ARTEMETHER_LUMEFANTRINE, ARTESUNATE_PYRONARIDINE_20_60MG_GRANULES, ARTESUNATE_PYRONARIDINE_60_180MG_TAB,
			DIHYDROARTEMISININ_PIPERAQUINE_20_160MG_TAB, DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_DISPERSIBLE_TAB,
			DIHYDROARTEMISININ_PIPERAQUINE_40_320MG_TAB, ARTESUNATE_AMODIAQUINE_67_25MG_TAB,
			ARTESUNATE_AMODIAQUINE_135_50MG_TAB, ARTESUNATE_AMODIAQUINE_270_100MG_TAB);

	/**
	 * Drugs that treat malaria, for "treated for malaria" (T22-04): the MOH 743 drugs except the RDT,
	 * sulphadoxine-pyrimethamine (prevention in pregnancy, IPTp) and nets, and the treatments MOH 743
	 * does not list: artemether, rectal artesunate and quinine
	 */
	public static final List<Integer> TREATMENT = union(ACTS, ARTESUNATE_INJECTION, PRIMAQUINE_7_5MG_TAB, PRIMAQUINE_15MG_TAB,
			ARTEMETHER, ARTESUNATE_SUPPOSITORY, QUININE);

	private MalariaDrugs() {
	}

	/**
	 * @return the ids as a SQL list, e.g. (1, 2, 3)
	 */
	public static String sql(List<Integer> drugIds) {
		return "(" + StringUtils.join(drugIds, ", ") + ")";
	}

	@SafeVarargs
	private static List<Integer> union(List<Integer>... lists) {
		LinkedHashSet<Integer> ids = new LinkedHashSet<Integer>();
		for (List<Integer> list : lists) {
			ids.addAll(list);
		}
		return Collections.unmodifiableList(new ArrayList<Integer>(ids));
	}
}
