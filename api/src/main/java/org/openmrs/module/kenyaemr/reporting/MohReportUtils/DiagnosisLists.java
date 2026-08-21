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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.openmrs.module.kenyaemr.reporting.MohReportUtils.DiagnosisConcepts.getConcept;

public class DiagnosisLists {

	public static List<Integer> getDiarrheaWithNoDehydrationDiagnosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DWSOD_1,
				DiagnosisConcepts._DiagnosisConcepts.DWSOD_2);

	}

	public static List<Integer> getDiarrheaWithSomeDehydrationDiagnosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DWSD_1,
				DiagnosisConcepts._DiagnosisConcepts.DWSOD_2);

	}

	public static List<Integer> getDiarrheaWithSevereDehydrationDiagnosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DWSD_1,
				DiagnosisConcepts._DiagnosisConcepts.DWSD_2);

	}

	public static List<Integer> getGastroenteritisList() {
		return Arrays
				.asList(DiagnosisConcepts._DiagnosisConcepts.GAS_1,
						DiagnosisConcepts._DiagnosisConcepts.GAS_2,
						DiagnosisConcepts._DiagnosisConcepts.GAS_3,
						DiagnosisConcepts._DiagnosisConcepts.GAS_4,
						DiagnosisConcepts._DiagnosisConcepts.GAS_5,
						DiagnosisConcepts._DiagnosisConcepts.GAS_6,
						DiagnosisConcepts._DiagnosisConcepts.GAS_7,
						DiagnosisConcepts._DiagnosisConcepts.GAS_8,
						DiagnosisConcepts._DiagnosisConcepts.GAS_9,
						DiagnosisConcepts._DiagnosisConcepts.GAS_10,
						DiagnosisConcepts._DiagnosisConcepts.GAS_11,
						DiagnosisConcepts._DiagnosisConcepts.GAS_12,
						DiagnosisConcepts._DiagnosisConcepts.GAS_13,
						DiagnosisConcepts._DiagnosisConcepts.GAS_14,
						DiagnosisConcepts._DiagnosisConcepts.GAS_15,
						DiagnosisConcepts._DiagnosisConcepts.GAS_16,
						DiagnosisConcepts._DiagnosisConcepts.GAS_17,
						DiagnosisConcepts._DiagnosisConcepts.GAS_18,
						DiagnosisConcepts._DiagnosisConcepts.GAS_19,
						DiagnosisConcepts._DiagnosisConcepts.GAS_20,
						DiagnosisConcepts._DiagnosisConcepts.GAS_21,
						DiagnosisConcepts._DiagnosisConcepts.GAS_22,
						DiagnosisConcepts._DiagnosisConcepts.GAS_23,
						DiagnosisConcepts._DiagnosisConcepts.GAS_24,
						DiagnosisConcepts._DiagnosisConcepts.GAS_25,
						DiagnosisConcepts._DiagnosisConcepts.GAS_26

				);
	}

	public static List<Integer> getSeverePneumoniaList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.SP_1,
				DiagnosisConcepts._DiagnosisConcepts.SP_2,
				DiagnosisConcepts._DiagnosisConcepts.SP_3,
				DiagnosisConcepts._DiagnosisConcepts.SP_4,
				DiagnosisConcepts._DiagnosisConcepts.SP_5,
				DiagnosisConcepts._DiagnosisConcepts.SP_6,
				DiagnosisConcepts._DiagnosisConcepts.SP_7,
				DiagnosisConcepts._DiagnosisConcepts.SP_8,
				DiagnosisConcepts._DiagnosisConcepts.SP_9,
				DiagnosisConcepts._DiagnosisConcepts.SP_10
		);
	}

	public static List<Integer> getUpperPeadRespiratoryTractInfectionsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.URTI_1,
				DiagnosisConcepts._DiagnosisConcepts.URTI_2,
				DiagnosisConcepts._DiagnosisConcepts.URTI_3,
				DiagnosisConcepts._DiagnosisConcepts.URTI_4,
				DiagnosisConcepts._DiagnosisConcepts.URTI_5,
				DiagnosisConcepts._DiagnosisConcepts.URTI_6
		);
	}

	public static List<Integer> getLowerPeadRespiratoryTractInfectionsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.LRTI_1,
				DiagnosisConcepts._DiagnosisConcepts.LRTI_2,
				DiagnosisConcepts._DiagnosisConcepts.LRTI_3
		);
	}
	public static List<Integer> getUpperRespiratoryTractInfectionsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.URC_1,
				DiagnosisConcepts._DiagnosisConcepts.URC_2,
				DiagnosisConcepts._DiagnosisConcepts.URC_3,
				DiagnosisConcepts._DiagnosisConcepts.URC_4,
				DiagnosisConcepts._DiagnosisConcepts.URC_5,
				DiagnosisConcepts._DiagnosisConcepts.URC_6,
				DiagnosisConcepts._DiagnosisConcepts.URC_7,
				DiagnosisConcepts._DiagnosisConcepts.URC_8,
				DiagnosisConcepts._DiagnosisConcepts.URC_9,
				DiagnosisConcepts._DiagnosisConcepts.URC_10,
				DiagnosisConcepts._DiagnosisConcepts.URC_11,
				DiagnosisConcepts._DiagnosisConcepts.URC_12,
				DiagnosisConcepts._DiagnosisConcepts.URC_13,
				DiagnosisConcepts._DiagnosisConcepts.URC_14,
				DiagnosisConcepts._DiagnosisConcepts.URC_15,
				DiagnosisConcepts._DiagnosisConcepts.URC_16,
				DiagnosisConcepts._DiagnosisConcepts.URC_17,
				DiagnosisConcepts._DiagnosisConcepts.URC_18,
				DiagnosisConcepts._DiagnosisConcepts.URC_19,
				DiagnosisConcepts._DiagnosisConcepts.URC_20,
				DiagnosisConcepts._DiagnosisConcepts.URC_21,
				DiagnosisConcepts._DiagnosisConcepts.URC_22,
				DiagnosisConcepts._DiagnosisConcepts.URC_23,
				DiagnosisConcepts._DiagnosisConcepts.URC_24,
				DiagnosisConcepts._DiagnosisConcepts.URC_25,
				DiagnosisConcepts._DiagnosisConcepts.URC_26,
				DiagnosisConcepts._DiagnosisConcepts.URC_27,
				DiagnosisConcepts._DiagnosisConcepts.URC_28,
				DiagnosisConcepts._DiagnosisConcepts.URC_29,
				DiagnosisConcepts._DiagnosisConcepts.URC_30,
				DiagnosisConcepts._DiagnosisConcepts.URC_31,
				DiagnosisConcepts._DiagnosisConcepts.URC_32,
				DiagnosisConcepts._DiagnosisConcepts.URC_33,
				DiagnosisConcepts._DiagnosisConcepts.URC_34,
				DiagnosisConcepts._DiagnosisConcepts.URC_35,
				DiagnosisConcepts._DiagnosisConcepts.URC_36,
				DiagnosisConcepts._DiagnosisConcepts.URC_37,
				DiagnosisConcepts._DiagnosisConcepts.URC_38,
				DiagnosisConcepts._DiagnosisConcepts.URC_39,
				DiagnosisConcepts._DiagnosisConcepts.URC_40,
				DiagnosisConcepts._DiagnosisConcepts.URC_41,
				DiagnosisConcepts._DiagnosisConcepts.URC_42,
				DiagnosisConcepts._DiagnosisConcepts.URC_43,
				DiagnosisConcepts._DiagnosisConcepts.URC_44,
				DiagnosisConcepts._DiagnosisConcepts.URC_45,
				DiagnosisConcepts._DiagnosisConcepts.URC_46,
				DiagnosisConcepts._DiagnosisConcepts.URC_47,
				DiagnosisConcepts._DiagnosisConcepts.URC_48,
				DiagnosisConcepts._DiagnosisConcepts.URC_49,
				DiagnosisConcepts._DiagnosisConcepts.URC_50,
				DiagnosisConcepts._DiagnosisConcepts.URC_51,
				DiagnosisConcepts._DiagnosisConcepts.URC_52,
				DiagnosisConcepts._DiagnosisConcepts.URC_53,
				DiagnosisConcepts._DiagnosisConcepts.URC_54,
				DiagnosisConcepts._DiagnosisConcepts.URC_55,
				DiagnosisConcepts._DiagnosisConcepts.URC_56
		);
	}

	public static List<Integer> getPresumedTuberculosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.TCP_1,
				DiagnosisConcepts._DiagnosisConcepts.TCP_2

		);
	}

	public static List<Integer> getOtherMenigitisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.OMC_1,
				DiagnosisConcepts._DiagnosisConcepts.OMC_2,
				DiagnosisConcepts._DiagnosisConcepts.OMC_3,
				DiagnosisConcepts._DiagnosisConcepts.OMC_4,
				DiagnosisConcepts._DiagnosisConcepts.OMC_5,
				DiagnosisConcepts._DiagnosisConcepts.OMC_6,
				DiagnosisConcepts._DiagnosisConcepts.OMC_7,
				DiagnosisConcepts._DiagnosisConcepts.OMC_8,
				DiagnosisConcepts._DiagnosisConcepts.OMC_9,
				DiagnosisConcepts._DiagnosisConcepts.OMC_10,
				DiagnosisConcepts._DiagnosisConcepts.OMC_11,
				DiagnosisConcepts._DiagnosisConcepts.OMC_12,
				DiagnosisConcepts._DiagnosisConcepts.OMC_13,
				DiagnosisConcepts._DiagnosisConcepts.OMC_14,
				DiagnosisConcepts._DiagnosisConcepts.OMC_15,
				DiagnosisConcepts._DiagnosisConcepts.OMC_16,
				DiagnosisConcepts._DiagnosisConcepts.OMC_17,
				DiagnosisConcepts._DiagnosisConcepts.OMC_18,
				DiagnosisConcepts._DiagnosisConcepts.OMC_19,
				DiagnosisConcepts._DiagnosisConcepts.OMC_20,
				DiagnosisConcepts._DiagnosisConcepts.OMC_21,
				DiagnosisConcepts._DiagnosisConcepts.OMC_22,
				DiagnosisConcepts._DiagnosisConcepts.OMC_23,
				DiagnosisConcepts._DiagnosisConcepts.OMC_24,
				DiagnosisConcepts._DiagnosisConcepts.OMC_25,
				DiagnosisConcepts._DiagnosisConcepts.OMC_26,
				DiagnosisConcepts._DiagnosisConcepts.OMC_27,
				DiagnosisConcepts._DiagnosisConcepts.OMC_28,
				DiagnosisConcepts._DiagnosisConcepts.OMC_29,
				DiagnosisConcepts._DiagnosisConcepts.OMC_30,
				DiagnosisConcepts._DiagnosisConcepts.OMC_31,
				DiagnosisConcepts._DiagnosisConcepts.OMC_32,
				DiagnosisConcepts._DiagnosisConcepts.OMC_33,
				DiagnosisConcepts._DiagnosisConcepts.OMC_34,
				DiagnosisConcepts._DiagnosisConcepts.OMC_35,
				DiagnosisConcepts._DiagnosisConcepts.OMC_36,
				DiagnosisConcepts._DiagnosisConcepts.OMC_37,
				DiagnosisConcepts._DiagnosisConcepts.OMC_38,
				DiagnosisConcepts._DiagnosisConcepts.OMC_39,
				DiagnosisConcepts._DiagnosisConcepts.OMC_40,
				DiagnosisConcepts._DiagnosisConcepts.OMC_41,
				DiagnosisConcepts._DiagnosisConcepts.OMC_42,
				DiagnosisConcepts._DiagnosisConcepts.OMC_43,
				DiagnosisConcepts._DiagnosisConcepts.OMC_44

		);
	}

	public static List<Integer> getPoliomyelitisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.PMC_1,
				DiagnosisConcepts._DiagnosisConcepts.PMC_2

		);
	}
	public static List<Integer> getChickenPoxList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.CPC_1,
				DiagnosisConcepts._DiagnosisConcepts.CPC_2,
				DiagnosisConcepts._DiagnosisConcepts.CPC_3,
				DiagnosisConcepts._DiagnosisConcepts.CPC_4,
				DiagnosisConcepts._DiagnosisConcepts.CPC_5,
				DiagnosisConcepts._DiagnosisConcepts.CPC_6,
				DiagnosisConcepts._DiagnosisConcepts.CPC_7,
				DiagnosisConcepts._DiagnosisConcepts.CPC_8);
	}

	public static List<Integer> getMeaslesListA() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MSC_1,
				DiagnosisConcepts._DiagnosisConcepts.MSC_2,
				DiagnosisConcepts._DiagnosisConcepts.MSC_3,
				DiagnosisConcepts._DiagnosisConcepts.MSC_4,
				DiagnosisConcepts._DiagnosisConcepts.MSC_5,
				DiagnosisConcepts._DiagnosisConcepts.MSC_6

		);
	}

	public static List<Integer> getHepatitisListA() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.HPC_1,
				DiagnosisConcepts._DiagnosisConcepts.HPC_2,
				DiagnosisConcepts._DiagnosisConcepts.HPC_3,
				DiagnosisConcepts._DiagnosisConcepts.HPC_4,
				DiagnosisConcepts._DiagnosisConcepts.HPC_5,
				DiagnosisConcepts._DiagnosisConcepts.HPC_6,
				DiagnosisConcepts._DiagnosisConcepts.HPC_7,
				DiagnosisConcepts._DiagnosisConcepts.HPC_8,
				DiagnosisConcepts._DiagnosisConcepts.HPC_9,
				DiagnosisConcepts._DiagnosisConcepts.HPC_10,
				DiagnosisConcepts._DiagnosisConcepts.HPC_11,
				DiagnosisConcepts._DiagnosisConcepts.HPC_12,
				DiagnosisConcepts._DiagnosisConcepts.HPC_13,
				DiagnosisConcepts._DiagnosisConcepts.HPC_14,
				DiagnosisConcepts._DiagnosisConcepts.HPC_15,
				DiagnosisConcepts._DiagnosisConcepts.HPC_16,
				DiagnosisConcepts._DiagnosisConcepts.HPC_17,
				DiagnosisConcepts._DiagnosisConcepts.HPC_18,
				DiagnosisConcepts._DiagnosisConcepts.HPC_19,
				DiagnosisConcepts._DiagnosisConcepts.HPC_20,
				DiagnosisConcepts._DiagnosisConcepts.HPC_21,
				DiagnosisConcepts._DiagnosisConcepts.HPC_22,
				DiagnosisConcepts._DiagnosisConcepts.HPC_23,
				DiagnosisConcepts._DiagnosisConcepts.HPC_24,
				DiagnosisConcepts._DiagnosisConcepts.HPC_25,
				DiagnosisConcepts._DiagnosisConcepts.HPC_26,
				DiagnosisConcepts._DiagnosisConcepts.HPC_27,
				DiagnosisConcepts._DiagnosisConcepts.HPC_28,
				DiagnosisConcepts._DiagnosisConcepts.HPC_29,
				DiagnosisConcepts._DiagnosisConcepts.HPC_30,
				DiagnosisConcepts._DiagnosisConcepts.HPC_31,
				DiagnosisConcepts._DiagnosisConcepts.HPC_32,
				DiagnosisConcepts._DiagnosisConcepts.HPC_33,
				DiagnosisConcepts._DiagnosisConcepts.HPC_34

		);
	}

	public static List<Integer> getAmoebiasis() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.AM_1,
				DiagnosisConcepts._DiagnosisConcepts.AM_2,
				DiagnosisConcepts._DiagnosisConcepts.AM_3,
				DiagnosisConcepts._DiagnosisConcepts.AM_4,
				DiagnosisConcepts._DiagnosisConcepts.AM_5
		);
	}

	public static List<Integer> getMumpsListA() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MPC_1,
				DiagnosisConcepts._DiagnosisConcepts.MPC_2,
				DiagnosisConcepts._DiagnosisConcepts.MPC_3,
				DiagnosisConcepts._DiagnosisConcepts.MPC_4
		);
	}

	public static List<Integer> getTyphoidList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.TYC_1,
				DiagnosisConcepts._DiagnosisConcepts.TYC_2,
				DiagnosisConcepts._DiagnosisConcepts.TYC_3,
				DiagnosisConcepts._DiagnosisConcepts.TYC_4
		);
	}

	public static List<Integer> getBilharziaList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.BLC_1,
				DiagnosisConcepts._DiagnosisConcepts.BLC_2,
				DiagnosisConcepts._DiagnosisConcepts.BLC_3,
				DiagnosisConcepts._DiagnosisConcepts.BLC_4,
				DiagnosisConcepts._DiagnosisConcepts.BLC_5,
				DiagnosisConcepts._DiagnosisConcepts.BLC_6
		);
	}

	public static List<Integer> getInterstinalwormsListA() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.IWC_1,
				DiagnosisConcepts._DiagnosisConcepts.IWC_2,
				DiagnosisConcepts._DiagnosisConcepts.IWC_3,
				DiagnosisConcepts._DiagnosisConcepts.IWC_4,
				DiagnosisConcepts._DiagnosisConcepts.IWC_5,
				DiagnosisConcepts._DiagnosisConcepts.IWC_6,
				DiagnosisConcepts._DiagnosisConcepts.IWC_7,
				DiagnosisConcepts._DiagnosisConcepts.IWC_8
		);
	}

	public static List<Integer> getTonsilitiesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.TSC_1,
				DiagnosisConcepts._DiagnosisConcepts.TSC_2,
				DiagnosisConcepts._DiagnosisConcepts.TSC_3,
				DiagnosisConcepts._DiagnosisConcepts.TSC_4,
				DiagnosisConcepts._DiagnosisConcepts.TSC_5,
				DiagnosisConcepts._DiagnosisConcepts.TSC_6,
				DiagnosisConcepts._DiagnosisConcepts.TSC_7,
				DiagnosisConcepts._DiagnosisConcepts.TSC_8,
				DiagnosisConcepts._DiagnosisConcepts.TSC_9,
				DiagnosisConcepts._DiagnosisConcepts.TSC_10
		);
	}

	public static List<Integer> getUrinaryTractInfectionListA() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.UTI1,
				DiagnosisConcepts._DiagnosisConcepts.UTI2,
				DiagnosisConcepts._DiagnosisConcepts.UTI3,
				DiagnosisConcepts._DiagnosisConcepts.UTI4,
				DiagnosisConcepts._DiagnosisConcepts.UTI5,
				DiagnosisConcepts._DiagnosisConcepts.UTI6,
				DiagnosisConcepts._DiagnosisConcepts.UTI7

		);
	}

	public static List<Integer> getMentalDisordersList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MDC_1,
				DiagnosisConcepts._DiagnosisConcepts.MDC_2, DiagnosisConcepts._DiagnosisConcepts.MDC_3,
				DiagnosisConcepts._DiagnosisConcepts.MDC_4, DiagnosisConcepts._DiagnosisConcepts.MDC_5,
				DiagnosisConcepts._DiagnosisConcepts.MDC_6, DiagnosisConcepts._DiagnosisConcepts.MDC_7,
				DiagnosisConcepts._DiagnosisConcepts.MDC_8, DiagnosisConcepts._DiagnosisConcepts.MDC_9,
				DiagnosisConcepts._DiagnosisConcepts.MDC_10, DiagnosisConcepts._DiagnosisConcepts.MDC_11,
				DiagnosisConcepts._DiagnosisConcepts.MDC_12, DiagnosisConcepts._DiagnosisConcepts.MDC_13,
				DiagnosisConcepts._DiagnosisConcepts.MDC_14, DiagnosisConcepts._DiagnosisConcepts.MDC_15,
				DiagnosisConcepts._DiagnosisConcepts.MDC_16, DiagnosisConcepts._DiagnosisConcepts.MDC_17,
				DiagnosisConcepts._DiagnosisConcepts.MDC_18, DiagnosisConcepts._DiagnosisConcepts.MDC_19,
				DiagnosisConcepts._DiagnosisConcepts.MDC_20, DiagnosisConcepts._DiagnosisConcepts.MDC_21,
				DiagnosisConcepts._DiagnosisConcepts.MDC_22, DiagnosisConcepts._DiagnosisConcepts.MDC_23,
				DiagnosisConcepts._DiagnosisConcepts.MDC_24, DiagnosisConcepts._DiagnosisConcepts.MDC_25,
				DiagnosisConcepts._DiagnosisConcepts.MDC_26, DiagnosisConcepts._DiagnosisConcepts.MDC_27,
				DiagnosisConcepts._DiagnosisConcepts.MDC_28,
				DiagnosisConcepts._DiagnosisConcepts.MDC_30, DiagnosisConcepts._DiagnosisConcepts.MDC_31,
				DiagnosisConcepts._DiagnosisConcepts.MDC_32, DiagnosisConcepts._DiagnosisConcepts.MDC_33,
				DiagnosisConcepts._DiagnosisConcepts.MDC_34, DiagnosisConcepts._DiagnosisConcepts.MDC_35,
				DiagnosisConcepts._DiagnosisConcepts.MDC_36, DiagnosisConcepts._DiagnosisConcepts.MDC_37,
				DiagnosisConcepts._DiagnosisConcepts.MDC_38, DiagnosisConcepts._DiagnosisConcepts.MDC_39,
				DiagnosisConcepts._DiagnosisConcepts.MDC_40, DiagnosisConcepts._DiagnosisConcepts.MDC_41,
				DiagnosisConcepts._DiagnosisConcepts.MDC_42, DiagnosisConcepts._DiagnosisConcepts.MDC_43,
				DiagnosisConcepts._DiagnosisConcepts.MDC_44, DiagnosisConcepts._DiagnosisConcepts.MDC_45,
				DiagnosisConcepts._DiagnosisConcepts.MDC_46, DiagnosisConcepts._DiagnosisConcepts.MDC_47,
				DiagnosisConcepts._DiagnosisConcepts.MDC_48, DiagnosisConcepts._DiagnosisConcepts.MDC_49,
				DiagnosisConcepts._DiagnosisConcepts.MDC_50, DiagnosisConcepts._DiagnosisConcepts.MDC_51,
				DiagnosisConcepts._DiagnosisConcepts.MDC_52, DiagnosisConcepts._DiagnosisConcepts.MDC_53,
				DiagnosisConcepts._DiagnosisConcepts.MDC_54, DiagnosisConcepts._DiagnosisConcepts.MDC_55,
				DiagnosisConcepts._DiagnosisConcepts.MDC_56, DiagnosisConcepts._DiagnosisConcepts.MDC_57,
				DiagnosisConcepts._DiagnosisConcepts.MDC_58, DiagnosisConcepts._DiagnosisConcepts.MDC_59,
				DiagnosisConcepts._DiagnosisConcepts.MDC_60, DiagnosisConcepts._DiagnosisConcepts.MDC_61,
				DiagnosisConcepts._DiagnosisConcepts.MDC_62, DiagnosisConcepts._DiagnosisConcepts.MDC_63,
				DiagnosisConcepts._DiagnosisConcepts.MDC_64, DiagnosisConcepts._DiagnosisConcepts.MDC_65,
				DiagnosisConcepts._DiagnosisConcepts.MDC_66, DiagnosisConcepts._DiagnosisConcepts.MDC_67,
				DiagnosisConcepts._DiagnosisConcepts.MDC_68, DiagnosisConcepts._DiagnosisConcepts.MDC_69,
				DiagnosisConcepts._DiagnosisConcepts.MDC_70, DiagnosisConcepts._DiagnosisConcepts.MDC_71,
				DiagnosisConcepts._DiagnosisConcepts.MDC_72, DiagnosisConcepts._DiagnosisConcepts.MDC_73,
				DiagnosisConcepts._DiagnosisConcepts.MDC_74, DiagnosisConcepts._DiagnosisConcepts.MDC_75,
				DiagnosisConcepts._DiagnosisConcepts.MDC_76, DiagnosisConcepts._DiagnosisConcepts.MDC_77,
				DiagnosisConcepts._DiagnosisConcepts.MDC_78, DiagnosisConcepts._DiagnosisConcepts.MDC_79,
				DiagnosisConcepts._DiagnosisConcepts.MDC_80, DiagnosisConcepts._DiagnosisConcepts.MDC_81,
				DiagnosisConcepts._DiagnosisConcepts.MDC_82, DiagnosisConcepts._DiagnosisConcepts.MDC_83,
				DiagnosisConcepts._DiagnosisConcepts.MDC_84, DiagnosisConcepts._DiagnosisConcepts.MDC_85,
				DiagnosisConcepts._DiagnosisConcepts.MDC_86, DiagnosisConcepts._DiagnosisConcepts.MDC_87,
				DiagnosisConcepts._DiagnosisConcepts.MDC_88, DiagnosisConcepts._DiagnosisConcepts.MDC_89,
				DiagnosisConcepts._DiagnosisConcepts.MDC_90, DiagnosisConcepts._DiagnosisConcepts.MDC_91,
				DiagnosisConcepts._DiagnosisConcepts.MDC_92, DiagnosisConcepts._DiagnosisConcepts.MDC_93,
				DiagnosisConcepts._DiagnosisConcepts.MDC_94, DiagnosisConcepts._DiagnosisConcepts.MDC_95,
				DiagnosisConcepts._DiagnosisConcepts.MDC_96, DiagnosisConcepts._DiagnosisConcepts.MDC_97,
				DiagnosisConcepts._DiagnosisConcepts.MDC_98, DiagnosisConcepts._DiagnosisConcepts.MDC_99,
				DiagnosisConcepts._DiagnosisConcepts.MDC_100, DiagnosisConcepts._DiagnosisConcepts.MDC_101,
				DiagnosisConcepts._DiagnosisConcepts.MDC_102, DiagnosisConcepts._DiagnosisConcepts.MDC_103,
				DiagnosisConcepts._DiagnosisConcepts.MDC_104, DiagnosisConcepts._DiagnosisConcepts.MDC_105,
				DiagnosisConcepts._DiagnosisConcepts.MDC_106, DiagnosisConcepts._DiagnosisConcepts.MDC_107,
				DiagnosisConcepts._DiagnosisConcepts.MDC_108, DiagnosisConcepts._DiagnosisConcepts.MDC_109,
				DiagnosisConcepts._DiagnosisConcepts.MDC_110, DiagnosisConcepts._DiagnosisConcepts.MDC_111,
				DiagnosisConcepts._DiagnosisConcepts.MDC_112, DiagnosisConcepts._DiagnosisConcepts.MDC_113,
				DiagnosisConcepts._DiagnosisConcepts.MDC_114, DiagnosisConcepts._DiagnosisConcepts.MDC_115,
				DiagnosisConcepts._DiagnosisConcepts.MDC_116, DiagnosisConcepts._DiagnosisConcepts.MDC_117,
				DiagnosisConcepts._DiagnosisConcepts.MDC_118, DiagnosisConcepts._DiagnosisConcepts.MDC_119,
				DiagnosisConcepts._DiagnosisConcepts.MDC_120, DiagnosisConcepts._DiagnosisConcepts.MDC_121,
				DiagnosisConcepts._DiagnosisConcepts.MDC_122, DiagnosisConcepts._DiagnosisConcepts.MDC_123,
				DiagnosisConcepts._DiagnosisConcepts.MDC_124, DiagnosisConcepts._DiagnosisConcepts.MDC_125,
				DiagnosisConcepts._DiagnosisConcepts.MDC_126, DiagnosisConcepts._DiagnosisConcepts.MDC_127,
				DiagnosisConcepts._DiagnosisConcepts.MDC_128, DiagnosisConcepts._DiagnosisConcepts.MDC_129,
				DiagnosisConcepts._DiagnosisConcepts.MDC_130, DiagnosisConcepts._DiagnosisConcepts.MDC_131

		);

	}


	public static List<Integer> getDentalDisordersList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DDC_1, DiagnosisConcepts._DiagnosisConcepts.DDC_2,
				DiagnosisConcepts._DiagnosisConcepts.DDC_3, DiagnosisConcepts._DiagnosisConcepts.DDC_4,
				DiagnosisConcepts._DiagnosisConcepts.DDC_5, DiagnosisConcepts._DiagnosisConcepts.DDC_6,
				DiagnosisConcepts._DiagnosisConcepts.DDC_7, DiagnosisConcepts._DiagnosisConcepts.DDC_8,
				DiagnosisConcepts._DiagnosisConcepts.DDC_9, DiagnosisConcepts._DiagnosisConcepts.DDC_10,
				DiagnosisConcepts._DiagnosisConcepts.DDC_11, DiagnosisConcepts._DiagnosisConcepts.DDC_12,
				DiagnosisConcepts._DiagnosisConcepts.DDC_13, DiagnosisConcepts._DiagnosisConcepts.DDC_14,
				DiagnosisConcepts._DiagnosisConcepts.DDC_15, DiagnosisConcepts._DiagnosisConcepts.DDC_16,
				DiagnosisConcepts._DiagnosisConcepts.DDC_17, DiagnosisConcepts._DiagnosisConcepts.DDC_18,
				DiagnosisConcepts._DiagnosisConcepts.DDC_19, DiagnosisConcepts._DiagnosisConcepts.DDC_20,
				DiagnosisConcepts._DiagnosisConcepts.DDC_21, DiagnosisConcepts._DiagnosisConcepts.DDC_22,
				DiagnosisConcepts._DiagnosisConcepts.DDC_23, DiagnosisConcepts._DiagnosisConcepts.DDC_24,
				DiagnosisConcepts._DiagnosisConcepts.DDC_25, DiagnosisConcepts._DiagnosisConcepts.DDC_26,
				DiagnosisConcepts._DiagnosisConcepts.DDC_27, DiagnosisConcepts._DiagnosisConcepts.DDC_28,
				DiagnosisConcepts._DiagnosisConcepts.DDC_29, DiagnosisConcepts._DiagnosisConcepts.DDC_30,
				DiagnosisConcepts._DiagnosisConcepts.DDC_32,
				DiagnosisConcepts._DiagnosisConcepts.DDC_33, DiagnosisConcepts._DiagnosisConcepts.DDC_34,
				DiagnosisConcepts._DiagnosisConcepts.DDC_35, DiagnosisConcepts._DiagnosisConcepts.DDC_36,
				DiagnosisConcepts._DiagnosisConcepts.DDC_37, DiagnosisConcepts._DiagnosisConcepts.DDC_38,
				DiagnosisConcepts._DiagnosisConcepts.DDC_39, DiagnosisConcepts._DiagnosisConcepts.DDC_40,
				DiagnosisConcepts._DiagnosisConcepts.DDC_41, DiagnosisConcepts._DiagnosisConcepts.DDC_42,
				DiagnosisConcepts._DiagnosisConcepts.DDC_43, DiagnosisConcepts._DiagnosisConcepts.DDC_44,
				DiagnosisConcepts._DiagnosisConcepts.DDC_45, DiagnosisConcepts._DiagnosisConcepts.DDC_46,
				DiagnosisConcepts._DiagnosisConcepts.DDC_47, DiagnosisConcepts._DiagnosisConcepts.DDC_48,
				DiagnosisConcepts._DiagnosisConcepts.DDC_49, DiagnosisConcepts._DiagnosisConcepts.DDC_50,
				DiagnosisConcepts._DiagnosisConcepts.DDC_50,
				DiagnosisConcepts._DiagnosisConcepts.DDC_51,
				DiagnosisConcepts._DiagnosisConcepts.DDC_52,
				DiagnosisConcepts._DiagnosisConcepts.DDC_53,
				DiagnosisConcepts._DiagnosisConcepts.DDC_54,
				DiagnosisConcepts._DiagnosisConcepts.DDC_55,
				DiagnosisConcepts._DiagnosisConcepts.DDC_56,
				DiagnosisConcepts._DiagnosisConcepts.DDC_57,
				DiagnosisConcepts._DiagnosisConcepts.DDC_58,
				DiagnosisConcepts._DiagnosisConcepts.DDC_59,
				DiagnosisConcepts._DiagnosisConcepts.DDC_60,
				DiagnosisConcepts._DiagnosisConcepts.DDC_61,
				DiagnosisConcepts._DiagnosisConcepts.DDC_62,
				DiagnosisConcepts._DiagnosisConcepts.DDC_63,
				DiagnosisConcepts._DiagnosisConcepts.DDC_64,
				DiagnosisConcepts._DiagnosisConcepts.DDC_65,
				DiagnosisConcepts._DiagnosisConcepts.DDC_66,
				DiagnosisConcepts._DiagnosisConcepts.DDC_67,
				DiagnosisConcepts._DiagnosisConcepts.DDC_68,
				DiagnosisConcepts._DiagnosisConcepts.DDC_69,
				DiagnosisConcepts._DiagnosisConcepts.DDC_70,
				DiagnosisConcepts._DiagnosisConcepts.DDC_71,
				DiagnosisConcepts._DiagnosisConcepts.DDC_72,
				DiagnosisConcepts._DiagnosisConcepts.DDC_71,
				DiagnosisConcepts._DiagnosisConcepts.DDC_72,
				DiagnosisConcepts._DiagnosisConcepts.DDC_73,
				DiagnosisConcepts._DiagnosisConcepts.DDC_74,
				DiagnosisConcepts._DiagnosisConcepts.DDC_75,
				DiagnosisConcepts._DiagnosisConcepts.DDC_76,
				DiagnosisConcepts._DiagnosisConcepts.DDC_77,
				DiagnosisConcepts._DiagnosisConcepts.DDC_78,
				DiagnosisConcepts._DiagnosisConcepts.DDC_79,
				DiagnosisConcepts._DiagnosisConcepts.DDC_80,
				DiagnosisConcepts._DiagnosisConcepts.DDC_81,
				DiagnosisConcepts._DiagnosisConcepts.DDC_82,
				DiagnosisConcepts._DiagnosisConcepts.DDC_83,
				DiagnosisConcepts._DiagnosisConcepts.DDC_84,
				DiagnosisConcepts._DiagnosisConcepts.DDC_85,
				DiagnosisConcepts._DiagnosisConcepts.DDC_86,
				DiagnosisConcepts._DiagnosisConcepts.DDC_87,
				DiagnosisConcepts._DiagnosisConcepts.DDC_88,
				DiagnosisConcepts._DiagnosisConcepts.DDC_89,
				DiagnosisConcepts._DiagnosisConcepts.DDC_90,
				DiagnosisConcepts._DiagnosisConcepts.DDC_91,
				DiagnosisConcepts._DiagnosisConcepts.DDC_92,
				DiagnosisConcepts._DiagnosisConcepts.DDC_93,
				DiagnosisConcepts._DiagnosisConcepts.DDC_94,
				DiagnosisConcepts._DiagnosisConcepts.DDC_95,
				DiagnosisConcepts._DiagnosisConcepts.DDC_96
		);
	}

	public static List<Integer> getRoadTrafficInjuriesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.RTC_1, DiagnosisConcepts._DiagnosisConcepts.RTC_2,
				DiagnosisConcepts._DiagnosisConcepts.RTC_3,DiagnosisConcepts._DiagnosisConcepts.RTC_4,
				DiagnosisConcepts._DiagnosisConcepts.RTC_5,DiagnosisConcepts._DiagnosisConcepts.RTC_6,
				DiagnosisConcepts._DiagnosisConcepts.RTC_7,DiagnosisConcepts._DiagnosisConcepts.RTC_8,
				DiagnosisConcepts._DiagnosisConcepts.RTC_9,DiagnosisConcepts._DiagnosisConcepts.RTC_10,
				DiagnosisConcepts._DiagnosisConcepts.RTC_11,DiagnosisConcepts._DiagnosisConcepts.RTC_12,
				DiagnosisConcepts._DiagnosisConcepts.RTC_13,DiagnosisConcepts._DiagnosisConcepts.RTC_14,
				DiagnosisConcepts._DiagnosisConcepts.RTC_15,DiagnosisConcepts._DiagnosisConcepts.RTC_16,
				DiagnosisConcepts._DiagnosisConcepts.RTC_17,DiagnosisConcepts._DiagnosisConcepts.RTC_18,
				DiagnosisConcepts._DiagnosisConcepts.RTC_19,DiagnosisConcepts._DiagnosisConcepts.RTC_20,
				DiagnosisConcepts._DiagnosisConcepts.RTC_21
		);
	}

	public static List<Integer> getViolenceRelatedInjuriesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.VRC_1, DiagnosisConcepts._DiagnosisConcepts.VRC_2,
				DiagnosisConcepts._DiagnosisConcepts.VRC_3, DiagnosisConcepts._DiagnosisConcepts.VRC_4,
				DiagnosisConcepts._DiagnosisConcepts.VRC_5, DiagnosisConcepts._DiagnosisConcepts.VRC_6,
				DiagnosisConcepts._DiagnosisConcepts.VRC_7

		);
	}

	public static List<Integer> getOtherInjuriesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.OIC_1, DiagnosisConcepts._DiagnosisConcepts.OIC_2,
				DiagnosisConcepts._DiagnosisConcepts.OIC_3, DiagnosisConcepts._DiagnosisConcepts.OIC_4,
				DiagnosisConcepts._DiagnosisConcepts.OIC_5, DiagnosisConcepts._DiagnosisConcepts.OIC_6,
				DiagnosisConcepts._DiagnosisConcepts.OIC_7, DiagnosisConcepts._DiagnosisConcepts.OIC_8,
				DiagnosisConcepts._DiagnosisConcepts.OIC_9, DiagnosisConcepts._DiagnosisConcepts.OIC_10,
				DiagnosisConcepts._DiagnosisConcepts.OIC_11, DiagnosisConcepts._DiagnosisConcepts.OIC_12
		);

	}

	public static List<Integer> getSexualAssaultList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.SAC_1,
				DiagnosisConcepts._DiagnosisConcepts.SAC_2,
				DiagnosisConcepts._DiagnosisConcepts.SAC_4,
				DiagnosisConcepts._DiagnosisConcepts.SAC_5,
				DiagnosisConcepts._DiagnosisConcepts.SAC_6

		);
	}
	public static List<Integer> getBurnsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.BC_1, DiagnosisConcepts._DiagnosisConcepts.BC_2,
				DiagnosisConcepts._DiagnosisConcepts.BC_3, DiagnosisConcepts._DiagnosisConcepts.BC_4,
				DiagnosisConcepts._DiagnosisConcepts.BC_5, DiagnosisConcepts._DiagnosisConcepts.BC_6,
				DiagnosisConcepts._DiagnosisConcepts.BC_7, DiagnosisConcepts._DiagnosisConcepts.BC_8,
				DiagnosisConcepts._DiagnosisConcepts.BC_9, DiagnosisConcepts._DiagnosisConcepts.BC_10,
				DiagnosisConcepts._DiagnosisConcepts.BC_11, DiagnosisConcepts._DiagnosisConcepts.BC_12,
				DiagnosisConcepts._DiagnosisConcepts.BC_13, DiagnosisConcepts._DiagnosisConcepts.BC_13,
				DiagnosisConcepts._DiagnosisConcepts.BC_14, DiagnosisConcepts._DiagnosisConcepts.BC_15,
				DiagnosisConcepts._DiagnosisConcepts.BC_16, DiagnosisConcepts._DiagnosisConcepts.BC_17,
				DiagnosisConcepts._DiagnosisConcepts.BC_18, DiagnosisConcepts._DiagnosisConcepts.BC_19,
				DiagnosisConcepts._DiagnosisConcepts.BC_20, DiagnosisConcepts._DiagnosisConcepts.BC_21,
				DiagnosisConcepts._DiagnosisConcepts.BC_22, DiagnosisConcepts._DiagnosisConcepts.BC_23,
				DiagnosisConcepts._DiagnosisConcepts.BC_24, DiagnosisConcepts._DiagnosisConcepts.BC_25,
				DiagnosisConcepts._DiagnosisConcepts.BC_26, DiagnosisConcepts._DiagnosisConcepts.BC_27,
				DiagnosisConcepts._DiagnosisConcepts.BC_28, DiagnosisConcepts._DiagnosisConcepts.BC_29,
				DiagnosisConcepts._DiagnosisConcepts.BC_30, DiagnosisConcepts._DiagnosisConcepts.BC_31,
				DiagnosisConcepts._DiagnosisConcepts.BC_32, DiagnosisConcepts._DiagnosisConcepts.BC_33,
				DiagnosisConcepts._DiagnosisConcepts.BC_34, DiagnosisConcepts._DiagnosisConcepts.BC_35,
				DiagnosisConcepts._DiagnosisConcepts.BC_36, DiagnosisConcepts._DiagnosisConcepts.BC_37,
				DiagnosisConcepts._DiagnosisConcepts.BC_38, DiagnosisConcepts._DiagnosisConcepts.BC_39,
				DiagnosisConcepts._DiagnosisConcepts.BC_40, DiagnosisConcepts._DiagnosisConcepts.BC_41,
				DiagnosisConcepts._DiagnosisConcepts.BC_42, DiagnosisConcepts._DiagnosisConcepts.BC_43,
				DiagnosisConcepts._DiagnosisConcepts.BC_44, DiagnosisConcepts._DiagnosisConcepts.BC_45,
				DiagnosisConcepts._DiagnosisConcepts.BC_46, DiagnosisConcepts._DiagnosisConcepts.BC_47,
				DiagnosisConcepts._DiagnosisConcepts.BC_48, DiagnosisConcepts._DiagnosisConcepts.BC_49,
				DiagnosisConcepts._DiagnosisConcepts.BC_50, DiagnosisConcepts._DiagnosisConcepts.BC_51,
				DiagnosisConcepts._DiagnosisConcepts.BC_52, DiagnosisConcepts._DiagnosisConcepts.BC_53,
				DiagnosisConcepts._DiagnosisConcepts.BC_54, DiagnosisConcepts._DiagnosisConcepts.BC_55,
				DiagnosisConcepts._DiagnosisConcepts.BC_56, DiagnosisConcepts._DiagnosisConcepts.BC_57,
				DiagnosisConcepts._DiagnosisConcepts.BC_58, DiagnosisConcepts._DiagnosisConcepts.BC_59,
				DiagnosisConcepts._DiagnosisConcepts.BC_60, DiagnosisConcepts._DiagnosisConcepts.BC_61,
				DiagnosisConcepts._DiagnosisConcepts.BC_61, DiagnosisConcepts._DiagnosisConcepts.BC_62,
				DiagnosisConcepts._DiagnosisConcepts.BC_63, DiagnosisConcepts._DiagnosisConcepts.BC_64,
				DiagnosisConcepts._DiagnosisConcepts.BC_65, DiagnosisConcepts._DiagnosisConcepts.BC_66,
				DiagnosisConcepts._DiagnosisConcepts.BC_67, DiagnosisConcepts._DiagnosisConcepts.BC_68,
				DiagnosisConcepts._DiagnosisConcepts.BC_69, DiagnosisConcepts._DiagnosisConcepts.BC_70,
				DiagnosisConcepts._DiagnosisConcepts.BC_71, DiagnosisConcepts._DiagnosisConcepts.BC_72,
				DiagnosisConcepts._DiagnosisConcepts.BC_73, DiagnosisConcepts._DiagnosisConcepts.BC_74,
				DiagnosisConcepts._DiagnosisConcepts.BC_75, DiagnosisConcepts._DiagnosisConcepts.BC_76,
				DiagnosisConcepts._DiagnosisConcepts.BC_77, DiagnosisConcepts._DiagnosisConcepts.BC_78,
				DiagnosisConcepts._DiagnosisConcepts.BC_79, DiagnosisConcepts._DiagnosisConcepts.BC_80,
				DiagnosisConcepts._DiagnosisConcepts.BC_81, DiagnosisConcepts._DiagnosisConcepts.BC_82,
				DiagnosisConcepts._DiagnosisConcepts.BC_83, DiagnosisConcepts._DiagnosisConcepts.BC_84,
				DiagnosisConcepts._DiagnosisConcepts.BC_85, DiagnosisConcepts._DiagnosisConcepts.BC_86,
				DiagnosisConcepts._DiagnosisConcepts.BC_87, DiagnosisConcepts._DiagnosisConcepts.BC_88,
				DiagnosisConcepts._DiagnosisConcepts.BC_89, DiagnosisConcepts._DiagnosisConcepts.BC_90,
				DiagnosisConcepts._DiagnosisConcepts.BC_91, DiagnosisConcepts._DiagnosisConcepts.BC_92,
				DiagnosisConcepts._DiagnosisConcepts.BC_93, DiagnosisConcepts._DiagnosisConcepts.BC_94,
				DiagnosisConcepts._DiagnosisConcepts.BC_95, DiagnosisConcepts._DiagnosisConcepts.BC_96,
				DiagnosisConcepts._DiagnosisConcepts.BC_97, DiagnosisConcepts._DiagnosisConcepts.BC_98,
				DiagnosisConcepts._DiagnosisConcepts.BC_99, DiagnosisConcepts._DiagnosisConcepts.BC_100,
				DiagnosisConcepts._DiagnosisConcepts.BC_101, DiagnosisConcepts._DiagnosisConcepts.BC_102,
				DiagnosisConcepts._DiagnosisConcepts.BC_103, DiagnosisConcepts._DiagnosisConcepts.BC_104,
				DiagnosisConcepts._DiagnosisConcepts.BC_105
		);
	}


	public static List<Integer> getSnakeBitesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.SBC_1,
				DiagnosisConcepts._DiagnosisConcepts.SBC_2, DiagnosisConcepts._DiagnosisConcepts.SBC_3,
				DiagnosisConcepts._DiagnosisConcepts.SBC_4, DiagnosisConcepts._DiagnosisConcepts.SBC_5,
				DiagnosisConcepts._DiagnosisConcepts.SBC_6
		);
	}

	public static List<Integer> getDogBitesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DBC_1, DiagnosisConcepts._DiagnosisConcepts.DBC_2,
				DiagnosisConcepts._DiagnosisConcepts.DBC_3, DiagnosisConcepts._DiagnosisConcepts.DBC_4
		);

	}
	public static List<Integer> getOtherBitesList() {

		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.OBC_1, DiagnosisConcepts._DiagnosisConcepts.OBC_2,
				DiagnosisConcepts._DiagnosisConcepts.OBC_3, DiagnosisConcepts._DiagnosisConcepts.OBC_4,
				DiagnosisConcepts._DiagnosisConcepts.OBC_5, DiagnosisConcepts._DiagnosisConcepts.OBC_6,
				DiagnosisConcepts._DiagnosisConcepts.OBC_7
		);

	}

	public static List<Integer> getDiabetesList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DTC_1, DiagnosisConcepts._DiagnosisConcepts.DTC_2,
				DiagnosisConcepts._DiagnosisConcepts.DTC_3, DiagnosisConcepts._DiagnosisConcepts.DTC_4,
				DiagnosisConcepts._DiagnosisConcepts.DTC_5, DiagnosisConcepts._DiagnosisConcepts.DTC_6,
				DiagnosisConcepts._DiagnosisConcepts.DTC_7, DiagnosisConcepts._DiagnosisConcepts.DTC_8,
				DiagnosisConcepts._DiagnosisConcepts.DTC_9,	DiagnosisConcepts._DiagnosisConcepts.DTC_10,
				DiagnosisConcepts._DiagnosisConcepts.DTC_11,DiagnosisConcepts._DiagnosisConcepts.DTC_12,
				DiagnosisConcepts._DiagnosisConcepts.DTC_13, DiagnosisConcepts._DiagnosisConcepts.DTC_14,
				DiagnosisConcepts._DiagnosisConcepts.DTC_15, DiagnosisConcepts._DiagnosisConcepts.DTC_16,
				DiagnosisConcepts._DiagnosisConcepts.DTC_17, DiagnosisConcepts._DiagnosisConcepts.DTC_18,
				DiagnosisConcepts._DiagnosisConcepts.DTC_19, DiagnosisConcepts._DiagnosisConcepts.DTC_20,
				DiagnosisConcepts._DiagnosisConcepts.DTC_21
		);
	}

	public static List<Integer> getEpilepsyList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.EPC_1,
				DiagnosisConcepts._DiagnosisConcepts.EPC_2, DiagnosisConcepts._DiagnosisConcepts.EPC_3,
				DiagnosisConcepts._DiagnosisConcepts.EPC_4, DiagnosisConcepts._DiagnosisConcepts.EPC_5,
				DiagnosisConcepts._DiagnosisConcepts.EPC_6, DiagnosisConcepts._DiagnosisConcepts.EPC_7,
				DiagnosisConcepts._DiagnosisConcepts.EPC_8, DiagnosisConcepts._DiagnosisConcepts.EPC_9,
				DiagnosisConcepts._DiagnosisConcepts.EPC_10, DiagnosisConcepts._DiagnosisConcepts.EPC_11,
				DiagnosisConcepts._DiagnosisConcepts.EPC_12, DiagnosisConcepts._DiagnosisConcepts.EPC_13,
				DiagnosisConcepts._DiagnosisConcepts.EPC_14, DiagnosisConcepts._DiagnosisConcepts.EPC_15,
				DiagnosisConcepts._DiagnosisConcepts.EPC_16, DiagnosisConcepts._DiagnosisConcepts.EPC_17,
				DiagnosisConcepts._DiagnosisConcepts.EPC_18, DiagnosisConcepts._DiagnosisConcepts.EPC_19,
				DiagnosisConcepts._DiagnosisConcepts.EPC_20, DiagnosisConcepts._DiagnosisConcepts.EPC_21,
				DiagnosisConcepts._DiagnosisConcepts.EPC_22, DiagnosisConcepts._DiagnosisConcepts.EPC_23,
				DiagnosisConcepts._DiagnosisConcepts.EPC_24
		);
	}

	public static List<Integer> getOtherConvulsiveDisordersList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.OCDC_1,
				DiagnosisConcepts._DiagnosisConcepts.OCDC_2,
				DiagnosisConcepts._DiagnosisConcepts.OCDC_3
		);
	}

	public static List<Integer> getReumonicFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.RF_1,
				DiagnosisConcepts._DiagnosisConcepts.RF_2, DiagnosisConcepts._DiagnosisConcepts.RF_3,
				DiagnosisConcepts._DiagnosisConcepts.RF_4

		);
	}

	public static List<Integer> getRicketsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.RKC_1, DiagnosisConcepts._DiagnosisConcepts.RKC_2,
				DiagnosisConcepts._DiagnosisConcepts.RKC_3, DiagnosisConcepts._DiagnosisConcepts.RKC_4,
				DiagnosisConcepts._DiagnosisConcepts.RKC_5
		);
	}

	public static List<Integer> getCerebralPalsyList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.CRPC_1, DiagnosisConcepts._DiagnosisConcepts.CRPC_2,
				DiagnosisConcepts._DiagnosisConcepts.CRPC_3, DiagnosisConcepts._DiagnosisConcepts.CRPC_4,
				DiagnosisConcepts._DiagnosisConcepts.CRPC_5, DiagnosisConcepts._DiagnosisConcepts.CRPC_6,
				DiagnosisConcepts._DiagnosisConcepts.CRPC_7, DiagnosisConcepts._DiagnosisConcepts.CRPC_8,
				DiagnosisConcepts._DiagnosisConcepts.CRPC_9, DiagnosisConcepts._DiagnosisConcepts.CRPC_10,
				DiagnosisConcepts._DiagnosisConcepts.CRPC_11,DiagnosisConcepts._DiagnosisConcepts.CRPC_12

		);
	}

	public static List<Integer> getAutismList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.ATC_1,
				DiagnosisConcepts._DiagnosisConcepts.ATC_2, DiagnosisConcepts._DiagnosisConcepts.ATC_3,
				DiagnosisConcepts._DiagnosisConcepts.ATC_4, DiagnosisConcepts._DiagnosisConcepts.ATC_5,
				DiagnosisConcepts._DiagnosisConcepts.ATC_6, DiagnosisConcepts._DiagnosisConcepts.ATC_7,
				DiagnosisConcepts._DiagnosisConcepts.ATC_8
		);
	}

	public static List<Integer> getTryponomiasisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.TRC_1,
				DiagnosisConcepts._DiagnosisConcepts.TRC_2, DiagnosisConcepts._DiagnosisConcepts.TRC_3,
				DiagnosisConcepts._DiagnosisConcepts.TRC_4, DiagnosisConcepts._DiagnosisConcepts.TRC_5,
				DiagnosisConcepts._DiagnosisConcepts.TRC_6, DiagnosisConcepts._DiagnosisConcepts.TRC_7,
				DiagnosisConcepts._DiagnosisConcepts.TRC_8, DiagnosisConcepts._DiagnosisConcepts.TRC_9,
				DiagnosisConcepts._DiagnosisConcepts.TRC_10


		);
	}
	public static List<Integer> getYellowFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.YFC_1,
				DiagnosisConcepts._DiagnosisConcepts.YFC_2
		);
	}

	public static List<Integer> getRiftValleyFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.RVF_1,
				DiagnosisConcepts._DiagnosisConcepts.RVF_2
		);
	}

	public static List<Integer> getChikungunyaFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.CKU_1,
				DiagnosisConcepts._DiagnosisConcepts.CKU_2);
	}

	public static List<Integer> getDengueFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DENF_1,
				DiagnosisConcepts._DiagnosisConcepts.DENF_2, DiagnosisConcepts._DiagnosisConcepts.DENF_3,
				DiagnosisConcepts._DiagnosisConcepts.DENF_4, DiagnosisConcepts._DiagnosisConcepts.DENF_5,
				DiagnosisConcepts._DiagnosisConcepts.DENF_6

		);
	}

	public static List<Integer> getKalazarLeishmaniasisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.KLC_1,
				DiagnosisConcepts._DiagnosisConcepts.KLC_2,
				DiagnosisConcepts._DiagnosisConcepts.KLC_3,
				DiagnosisConcepts._DiagnosisConcepts.KLC_4
		);
	}

	public static List<Integer> getChildHoodCancerist() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.SCC_1, DiagnosisConcepts._DiagnosisConcepts.SCC_2,
				DiagnosisConcepts._DiagnosisConcepts.SCC_3, DiagnosisConcepts._DiagnosisConcepts.SCC_4,
				DiagnosisConcepts._DiagnosisConcepts.SCC_5, DiagnosisConcepts._DiagnosisConcepts.SCC_6,
				DiagnosisConcepts._DiagnosisConcepts.SCC_7, DiagnosisConcepts._DiagnosisConcepts.SCC_8,
				DiagnosisConcepts._DiagnosisConcepts.SCC_9, DiagnosisConcepts._DiagnosisConcepts.SCC_10,
				DiagnosisConcepts._DiagnosisConcepts.SCC_11, DiagnosisConcepts._DiagnosisConcepts.SCC_12,
				DiagnosisConcepts._DiagnosisConcepts.SCC_13, DiagnosisConcepts._DiagnosisConcepts.SCC_14,
				DiagnosisConcepts._DiagnosisConcepts.SCC_15, DiagnosisConcepts._DiagnosisConcepts.SCC_16,
				DiagnosisConcepts._DiagnosisConcepts.SCC_17, DiagnosisConcepts._DiagnosisConcepts.SCC_18,
				DiagnosisConcepts._DiagnosisConcepts.SCC_19, DiagnosisConcepts._DiagnosisConcepts.SCC_20,
				DiagnosisConcepts._DiagnosisConcepts.SCC_21, DiagnosisConcepts._DiagnosisConcepts.SCC_22,
				DiagnosisConcepts._DiagnosisConcepts.SCC_23, DiagnosisConcepts._DiagnosisConcepts.SCC_24,
				DiagnosisConcepts._DiagnosisConcepts.SCC_25, DiagnosisConcepts._DiagnosisConcepts.SCC_26,
				DiagnosisConcepts._DiagnosisConcepts.SCC_27, DiagnosisConcepts._DiagnosisConcepts.SCC_28,
				DiagnosisConcepts._DiagnosisConcepts.SCC_29, DiagnosisConcepts._DiagnosisConcepts.SCC_30,
				DiagnosisConcepts._DiagnosisConcepts.SCC_31, DiagnosisConcepts._DiagnosisConcepts.SCC_32,
				DiagnosisConcepts._DiagnosisConcepts.SCC_33, DiagnosisConcepts._DiagnosisConcepts.SCC_34,
				DiagnosisConcepts._DiagnosisConcepts.SCC_35, DiagnosisConcepts._DiagnosisConcepts.SCC_36,
				DiagnosisConcepts._DiagnosisConcepts.SCC_37, DiagnosisConcepts._DiagnosisConcepts.SCC_38,
				DiagnosisConcepts._DiagnosisConcepts.SCC_39, DiagnosisConcepts._DiagnosisConcepts.SCC_40,
				DiagnosisConcepts._DiagnosisConcepts.SCC_41, DiagnosisConcepts._DiagnosisConcepts.SCC_42,
				DiagnosisConcepts._DiagnosisConcepts.SCC_43, DiagnosisConcepts._DiagnosisConcepts.SCC_44,
				DiagnosisConcepts._DiagnosisConcepts.SCC_45, DiagnosisConcepts._DiagnosisConcepts.SCC_46,
				DiagnosisConcepts._DiagnosisConcepts.SCC_47, DiagnosisConcepts._DiagnosisConcepts.SCC_48,
				DiagnosisConcepts._DiagnosisConcepts.SCC_49, DiagnosisConcepts._DiagnosisConcepts.SCC_50,
				DiagnosisConcepts._DiagnosisConcepts.SCC_51, DiagnosisConcepts._DiagnosisConcepts.SCC_52,
				DiagnosisConcepts._DiagnosisConcepts.SCC_53, DiagnosisConcepts._DiagnosisConcepts.SCC_54,
				DiagnosisConcepts._DiagnosisConcepts.SCC_55, DiagnosisConcepts._DiagnosisConcepts.SCC_56,
				DiagnosisConcepts._DiagnosisConcepts.SCC_57, DiagnosisConcepts._DiagnosisConcepts.SCC_58,
				DiagnosisConcepts._DiagnosisConcepts.SCC_59, DiagnosisConcepts._DiagnosisConcepts.SCC_60,
				DiagnosisConcepts._DiagnosisConcepts.SCC_61, DiagnosisConcepts._DiagnosisConcepts.SCC_62,
				DiagnosisConcepts._DiagnosisConcepts.SCC_63, DiagnosisConcepts._DiagnosisConcepts.SCC_64,
				DiagnosisConcepts._DiagnosisConcepts.SCC_65, DiagnosisConcepts._DiagnosisConcepts.SCC_66,
				DiagnosisConcepts._DiagnosisConcepts.SCC_67, DiagnosisConcepts._DiagnosisConcepts.SCC_68,
				DiagnosisConcepts._DiagnosisConcepts.SCC_69, DiagnosisConcepts._DiagnosisConcepts.SCC_70,
				DiagnosisConcepts._DiagnosisConcepts.SCC_71, DiagnosisConcepts._DiagnosisConcepts.SCC_72,
				DiagnosisConcepts._DiagnosisConcepts.SCC_73, DiagnosisConcepts._DiagnosisConcepts.SCC_74,
				DiagnosisConcepts._DiagnosisConcepts.SCC_75, DiagnosisConcepts._DiagnosisConcepts.SCC_76,
				DiagnosisConcepts._DiagnosisConcepts.SCC_77, DiagnosisConcepts._DiagnosisConcepts.SCC_78,
				DiagnosisConcepts._DiagnosisConcepts.SCC_79, DiagnosisConcepts._DiagnosisConcepts.SCC_80,
				DiagnosisConcepts._DiagnosisConcepts.SCC_81, DiagnosisConcepts._DiagnosisConcepts.SCC_82,
				DiagnosisConcepts._DiagnosisConcepts.SCC_83, DiagnosisConcepts._DiagnosisConcepts.SCC_84,
				DiagnosisConcepts._DiagnosisConcepts.SCC_85, DiagnosisConcepts._DiagnosisConcepts.SCC_86,
				DiagnosisConcepts._DiagnosisConcepts.SCC_87, DiagnosisConcepts._DiagnosisConcepts.SCC_88,
				DiagnosisConcepts._DiagnosisConcepts.SCC_89, DiagnosisConcepts._DiagnosisConcepts.SCC_90,
				DiagnosisConcepts._DiagnosisConcepts.SCC_91, DiagnosisConcepts._DiagnosisConcepts.SCC_92,
				DiagnosisConcepts._DiagnosisConcepts.SCC_93, DiagnosisConcepts._DiagnosisConcepts.SCC_94,
				DiagnosisConcepts._DiagnosisConcepts.SCC_95, DiagnosisConcepts._DiagnosisConcepts.SCC_96,
				DiagnosisConcepts._DiagnosisConcepts.SCC_97, DiagnosisConcepts._DiagnosisConcepts.SCC_98,
				DiagnosisConcepts._DiagnosisConcepts.SCC_99, DiagnosisConcepts._DiagnosisConcepts.SCC_100,
				DiagnosisConcepts._DiagnosisConcepts.SCC_101, DiagnosisConcepts._DiagnosisConcepts.SCC_102,
				DiagnosisConcepts._DiagnosisConcepts.SCC_103, DiagnosisConcepts._DiagnosisConcepts.SCC_104,
				DiagnosisConcepts._DiagnosisConcepts.SCC_105, DiagnosisConcepts._DiagnosisConcepts.SCC_106,
				DiagnosisConcepts._DiagnosisConcepts.SCC_107, DiagnosisConcepts._DiagnosisConcepts.SCC_108,
				DiagnosisConcepts._DiagnosisConcepts.SCC_109, DiagnosisConcepts._DiagnosisConcepts.SCC_110,
				DiagnosisConcepts._DiagnosisConcepts.SCC_111, DiagnosisConcepts._DiagnosisConcepts.SCC_112,
				DiagnosisConcepts._DiagnosisConcepts.SCC_113, DiagnosisConcepts._DiagnosisConcepts.SCC_114,
				DiagnosisConcepts._DiagnosisConcepts.SCC_115, DiagnosisConcepts._DiagnosisConcepts.SCC_116,
				DiagnosisConcepts._DiagnosisConcepts.SCC_117, DiagnosisConcepts._DiagnosisConcepts.SCC_118,
				DiagnosisConcepts._DiagnosisConcepts.SCC_119, DiagnosisConcepts._DiagnosisConcepts.SCC_120,
				DiagnosisConcepts._DiagnosisConcepts.SCC_121, DiagnosisConcepts._DiagnosisConcepts.SCC_122,
				DiagnosisConcepts._DiagnosisConcepts.SCC_123, DiagnosisConcepts._DiagnosisConcepts.SCC_124,
				DiagnosisConcepts._DiagnosisConcepts.SCC_125, DiagnosisConcepts._DiagnosisConcepts.SCC_126,
				DiagnosisConcepts._DiagnosisConcepts.SCC_127, DiagnosisConcepts._DiagnosisConcepts.SCC_128,
				DiagnosisConcepts._DiagnosisConcepts.SCC_129, DiagnosisConcepts._DiagnosisConcepts.SCC_130,
				DiagnosisConcepts._DiagnosisConcepts.SCC_131, DiagnosisConcepts._DiagnosisConcepts.SCC_132,
				DiagnosisConcepts._DiagnosisConcepts.SCC_133, DiagnosisConcepts._DiagnosisConcepts.SCC_134,
				DiagnosisConcepts._DiagnosisConcepts.SCC_135, DiagnosisConcepts._DiagnosisConcepts.SCC_136,
				DiagnosisConcepts._DiagnosisConcepts.SCC_137, DiagnosisConcepts._DiagnosisConcepts.SCC_138,
				DiagnosisConcepts._DiagnosisConcepts.SCC_139, DiagnosisConcepts._DiagnosisConcepts.SCC_140,
				DiagnosisConcepts._DiagnosisConcepts.SCC_141, DiagnosisConcepts._DiagnosisConcepts.SCC_142,
				DiagnosisConcepts._DiagnosisConcepts.SCC_143, DiagnosisConcepts._DiagnosisConcepts.SCC_144,
				DiagnosisConcepts._DiagnosisConcepts.SCC_145, DiagnosisConcepts._DiagnosisConcepts.SCC_146,
				DiagnosisConcepts._DiagnosisConcepts.SCC_147, DiagnosisConcepts._DiagnosisConcepts.SCC_148,
				DiagnosisConcepts._DiagnosisConcepts.SCC_149, DiagnosisConcepts._DiagnosisConcepts.SCC_150,
				DiagnosisConcepts._DiagnosisConcepts.SCC_151, DiagnosisConcepts._DiagnosisConcepts.SCC_152,
				DiagnosisConcepts._DiagnosisConcepts.SCC_153, DiagnosisConcepts._DiagnosisConcepts.SCC_154,
				DiagnosisConcepts._DiagnosisConcepts.SCC_155, DiagnosisConcepts._DiagnosisConcepts.SCC_156,
				DiagnosisConcepts._DiagnosisConcepts.SCC_157, DiagnosisConcepts._DiagnosisConcepts.SCC_158,
				DiagnosisConcepts._DiagnosisConcepts.SCC_159, DiagnosisConcepts._DiagnosisConcepts.SCC_160,
				DiagnosisConcepts._DiagnosisConcepts.SCC_161, DiagnosisConcepts._DiagnosisConcepts.SCC_162,
				DiagnosisConcepts._DiagnosisConcepts.SCC_163, DiagnosisConcepts._DiagnosisConcepts.SCC_164,
				DiagnosisConcepts._DiagnosisConcepts.SCC_165, DiagnosisConcepts._DiagnosisConcepts.SCC_166,
				DiagnosisConcepts._DiagnosisConcepts.SCC_167, DiagnosisConcepts._DiagnosisConcepts.SCC_168,
				DiagnosisConcepts._DiagnosisConcepts.SCC_169, DiagnosisConcepts._DiagnosisConcepts.SCC_170,
				DiagnosisConcepts._DiagnosisConcepts.SCC_171, DiagnosisConcepts._DiagnosisConcepts.SCC_172,
				DiagnosisConcepts._DiagnosisConcepts.SCC_173, DiagnosisConcepts._DiagnosisConcepts.SCC_174,
				DiagnosisConcepts._DiagnosisConcepts.SCC_175, DiagnosisConcepts._DiagnosisConcepts.SCC_176,
				DiagnosisConcepts._DiagnosisConcepts.SCC_177, DiagnosisConcepts._DiagnosisConcepts.SCC_178,
				DiagnosisConcepts._DiagnosisConcepts.SCC_179, DiagnosisConcepts._DiagnosisConcepts.SCC_180,
				DiagnosisConcepts._DiagnosisConcepts.SCC_181, DiagnosisConcepts._DiagnosisConcepts.SCC_182,
				DiagnosisConcepts._DiagnosisConcepts.SCC_183, DiagnosisConcepts._DiagnosisConcepts.SCC_184,
				DiagnosisConcepts._DiagnosisConcepts.SCC_185, DiagnosisConcepts._DiagnosisConcepts.SCC_186,
				DiagnosisConcepts._DiagnosisConcepts.SCC_187, DiagnosisConcepts._DiagnosisConcepts.SCC_188,
				DiagnosisConcepts._DiagnosisConcepts.SCC_189, DiagnosisConcepts._DiagnosisConcepts.SCC_190,
				DiagnosisConcepts._DiagnosisConcepts.SCC_191, DiagnosisConcepts._DiagnosisConcepts.SCC_192,
				DiagnosisConcepts._DiagnosisConcepts.SCC_193, DiagnosisConcepts._DiagnosisConcepts.SCC_194,
				DiagnosisConcepts._DiagnosisConcepts.SCC_195, DiagnosisConcepts._DiagnosisConcepts.SCC_196,
				DiagnosisConcepts._DiagnosisConcepts.SCC_197, DiagnosisConcepts._DiagnosisConcepts.SCC_198,
				DiagnosisConcepts._DiagnosisConcepts.SCC_199, DiagnosisConcepts._DiagnosisConcepts.SCC_200,
				DiagnosisConcepts._DiagnosisConcepts.SCC_201, DiagnosisConcepts._DiagnosisConcepts.SCC_202,
				DiagnosisConcepts._DiagnosisConcepts.SCC_203, DiagnosisConcepts._DiagnosisConcepts.SCC_204,
				DiagnosisConcepts._DiagnosisConcepts.SCC_205, DiagnosisConcepts._DiagnosisConcepts.SCC_206,
				DiagnosisConcepts._DiagnosisConcepts.SCC_207, DiagnosisConcepts._DiagnosisConcepts.SCC_208,
				DiagnosisConcepts._DiagnosisConcepts.SCC_209, DiagnosisConcepts._DiagnosisConcepts.SCC_210,
				DiagnosisConcepts._DiagnosisConcepts.SCC_211, DiagnosisConcepts._DiagnosisConcepts.SCC_212
		);
	}

	//moh705B

	public static List<Integer> getDiarrheaDiagnosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DA_1,
				DiagnosisConcepts._DiagnosisConcepts.DA_2,
				DiagnosisConcepts._DiagnosisConcepts.DA_3,
				DiagnosisConcepts._DiagnosisConcepts.DA_4,
				DiagnosisConcepts._DiagnosisConcepts.DA_5
		);
	}

	public static List<Integer> getTuberculosisDiagnosisList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.TBA_1,
				DiagnosisConcepts._DiagnosisConcepts.TBA_2,
				DiagnosisConcepts._DiagnosisConcepts.TBA_3,
				DiagnosisConcepts._DiagnosisConcepts.TBA_4,
				DiagnosisConcepts._DiagnosisConcepts.TBA_5,
				DiagnosisConcepts._DiagnosisConcepts.TBA_6,
				DiagnosisConcepts._DiagnosisConcepts.TBA_7,
				DiagnosisConcepts._DiagnosisConcepts.TBA_8,
				DiagnosisConcepts._DiagnosisConcepts.TBA_9,
				DiagnosisConcepts._DiagnosisConcepts.TBA_10,
				DiagnosisConcepts._DiagnosisConcepts.TBA_11,
				DiagnosisConcepts._DiagnosisConcepts.TBA_12,
				DiagnosisConcepts._DiagnosisConcepts.TBA_13,
				DiagnosisConcepts._DiagnosisConcepts.TBA_14,
				DiagnosisConcepts._DiagnosisConcepts.TBA_15,
				DiagnosisConcepts._DiagnosisConcepts.TBA_16,
				DiagnosisConcepts._DiagnosisConcepts.TBA_17,
				DiagnosisConcepts._DiagnosisConcepts.TBA_18,
				DiagnosisConcepts._DiagnosisConcepts.TBA_19,
				DiagnosisConcepts._DiagnosisConcepts.TBA_20,
				DiagnosisConcepts._DiagnosisConcepts.TBA_21,
				DiagnosisConcepts._DiagnosisConcepts.TBA_22,
				DiagnosisConcepts._DiagnosisConcepts.TBA_23,
				DiagnosisConcepts._DiagnosisConcepts.TBA_24,
				DiagnosisConcepts._DiagnosisConcepts.TBA_25,
				DiagnosisConcepts._DiagnosisConcepts.TBA_26,
				DiagnosisConcepts._DiagnosisConcepts.TBA_27,
				DiagnosisConcepts._DiagnosisConcepts.TBA_28,
				DiagnosisConcepts._DiagnosisConcepts.TBA_29,
				DiagnosisConcepts._DiagnosisConcepts.TBA_30,
				DiagnosisConcepts._DiagnosisConcepts.TBA_31,
				DiagnosisConcepts._DiagnosisConcepts.TBA_32,
				DiagnosisConcepts._DiagnosisConcepts.TBA_33,
				DiagnosisConcepts._DiagnosisConcepts.TBA_34,
				DiagnosisConcepts._DiagnosisConcepts.TBA_35,
				DiagnosisConcepts._DiagnosisConcepts.TBA_36,
				DiagnosisConcepts._DiagnosisConcepts.TBA_37,
				DiagnosisConcepts._DiagnosisConcepts.TBA_38,
				DiagnosisConcepts._DiagnosisConcepts.TBA_39,
				DiagnosisConcepts._DiagnosisConcepts.TBA_40,
				DiagnosisConcepts._DiagnosisConcepts.TBA_41
		);
	}

	public static List<Integer> getDysenteryList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DYA_1,
				DiagnosisConcepts._DiagnosisConcepts.DYA_2);
	}

	public static List<Integer> getCholeraList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.CLA_1,
				DiagnosisConcepts._DiagnosisConcepts.CLA_2,
				DiagnosisConcepts._DiagnosisConcepts.CLA_3,
				DiagnosisConcepts._DiagnosisConcepts.CLA_4,
				DiagnosisConcepts._DiagnosisConcepts.CLA_5,
				DiagnosisConcepts._DiagnosisConcepts.CLA_6,
				DiagnosisConcepts._DiagnosisConcepts.CLA_7

		);
	}

	public static List<Integer> getOtherMenigitisListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MOA_1,
				DiagnosisConcepts._DiagnosisConcepts.MOA_2, DiagnosisConcepts._DiagnosisConcepts.MOA_3,
				DiagnosisConcepts._DiagnosisConcepts.MOA_4, DiagnosisConcepts._DiagnosisConcepts.MOA_5,
				DiagnosisConcepts._DiagnosisConcepts.MOA_6, DiagnosisConcepts._DiagnosisConcepts.MOA_7,
				DiagnosisConcepts._DiagnosisConcepts.MOA_8, DiagnosisConcepts._DiagnosisConcepts.MOA_9,
				DiagnosisConcepts._DiagnosisConcepts.MOA_10, DiagnosisConcepts._DiagnosisConcepts.MOA_11,
				DiagnosisConcepts._DiagnosisConcepts.MOA_12, DiagnosisConcepts._DiagnosisConcepts.MOA_13,
				DiagnosisConcepts._DiagnosisConcepts.MOA_14, DiagnosisConcepts._DiagnosisConcepts.MOA_15,
				DiagnosisConcepts._DiagnosisConcepts.MOA_16, DiagnosisConcepts._DiagnosisConcepts.MOA_17,
				DiagnosisConcepts._DiagnosisConcepts.MOA_18, DiagnosisConcepts._DiagnosisConcepts.MOA_19,
				DiagnosisConcepts._DiagnosisConcepts.MOA_20, DiagnosisConcepts._DiagnosisConcepts.MOA_21,
				DiagnosisConcepts._DiagnosisConcepts.MOA_22, DiagnosisConcepts._DiagnosisConcepts.MOA_23,
				DiagnosisConcepts._DiagnosisConcepts.MOA_24, DiagnosisConcepts._DiagnosisConcepts.MOA_25,
				DiagnosisConcepts._DiagnosisConcepts.MOA_26, DiagnosisConcepts._DiagnosisConcepts.MOA_27,
				DiagnosisConcepts._DiagnosisConcepts.MOA_28, DiagnosisConcepts._DiagnosisConcepts.MOA_29,
				DiagnosisConcepts._DiagnosisConcepts.MOA_30, DiagnosisConcepts._DiagnosisConcepts.MOA_31,
				DiagnosisConcepts._DiagnosisConcepts.MOA_32, DiagnosisConcepts._DiagnosisConcepts.MOA_33,
				DiagnosisConcepts._DiagnosisConcepts.MOA_34, DiagnosisConcepts._DiagnosisConcepts.MOA_35,
				DiagnosisConcepts._DiagnosisConcepts.MOA_36, DiagnosisConcepts._DiagnosisConcepts.MOA_37,
				DiagnosisConcepts._DiagnosisConcepts.MOA_38, DiagnosisConcepts._DiagnosisConcepts.MOA_39,
				DiagnosisConcepts._DiagnosisConcepts.MOA_40, DiagnosisConcepts._DiagnosisConcepts.MOA_41,
				DiagnosisConcepts._DiagnosisConcepts.MOA_42, DiagnosisConcepts._DiagnosisConcepts.MOA_43,
				DiagnosisConcepts._DiagnosisConcepts.MOA_44

		);
	}

	public static List<Integer> getTetanusList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.NTA_1, DiagnosisConcepts._DiagnosisConcepts.NTA_2,
				DiagnosisConcepts._DiagnosisConcepts.NTA_3, DiagnosisConcepts._DiagnosisConcepts.NTA_4,
				DiagnosisConcepts._DiagnosisConcepts.NTA_5, DiagnosisConcepts._DiagnosisConcepts.NTA_6,
				DiagnosisConcepts._DiagnosisConcepts.NTA_7, DiagnosisConcepts._DiagnosisConcepts.NTA_8,
				DiagnosisConcepts._DiagnosisConcepts.NTA_9, DiagnosisConcepts._DiagnosisConcepts.NTA_10,
				DiagnosisConcepts._DiagnosisConcepts.NTA_11, DiagnosisConcepts._DiagnosisConcepts.NTA_12,
				DiagnosisConcepts._DiagnosisConcepts.NTA_13, DiagnosisConcepts._DiagnosisConcepts.NTA_14,
				DiagnosisConcepts._DiagnosisConcepts.NTA_15, DiagnosisConcepts._DiagnosisConcepts.NTA_16,
				DiagnosisConcepts._DiagnosisConcepts.NTA_17, DiagnosisConcepts._DiagnosisConcepts.NTA_18,
				DiagnosisConcepts._DiagnosisConcepts.NTA_19, DiagnosisConcepts._DiagnosisConcepts.NTA_20,
				DiagnosisConcepts._DiagnosisConcepts.NTA_21, DiagnosisConcepts._DiagnosisConcepts.NTA_22,
				DiagnosisConcepts._DiagnosisConcepts.NTA_23, DiagnosisConcepts._DiagnosisConcepts.NTA_24,
				DiagnosisConcepts._DiagnosisConcepts.NTA_25, DiagnosisConcepts._DiagnosisConcepts.NTA_26,
				DiagnosisConcepts._DiagnosisConcepts.NTA_27, DiagnosisConcepts._DiagnosisConcepts.NTA_28,
				DiagnosisConcepts._DiagnosisConcepts.NTA_29

		);
	}


	public static List<Integer> getPoliomyelitisListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.PMA_1,
				DiagnosisConcepts._DiagnosisConcepts.PMA_2,
				DiagnosisConcepts._DiagnosisConcepts.PMA_3,
				DiagnosisConcepts._DiagnosisConcepts.PMA_4,
				DiagnosisConcepts._DiagnosisConcepts.PMA_5,
				DiagnosisConcepts._DiagnosisConcepts.PMA_6,
				DiagnosisConcepts._DiagnosisConcepts.PMA_7,
				DiagnosisConcepts._DiagnosisConcepts.PMA_8
		);
	}

	public static List<Integer> getChickenPoxListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.CPA_1, DiagnosisConcepts._DiagnosisConcepts.CPA_2,
				DiagnosisConcepts._DiagnosisConcepts.CPA_3, DiagnosisConcepts._DiagnosisConcepts.CPA_4,
				DiagnosisConcepts._DiagnosisConcepts.CPA_5, DiagnosisConcepts._DiagnosisConcepts.CPA_6,
				DiagnosisConcepts._DiagnosisConcepts.CPA_7, DiagnosisConcepts._DiagnosisConcepts.CPA_8,
				DiagnosisConcepts._DiagnosisConcepts.CPA_9, DiagnosisConcepts._DiagnosisConcepts.CPA_10

		);
	}


	public static List<Integer> getMeaslesListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MEA_1, DiagnosisConcepts._DiagnosisConcepts.MEA_2,
				DiagnosisConcepts._DiagnosisConcepts.MEA_3, DiagnosisConcepts._DiagnosisConcepts.MEA_4,
				DiagnosisConcepts._DiagnosisConcepts.MEA_5, DiagnosisConcepts._DiagnosisConcepts.MEA_6,
				DiagnosisConcepts._DiagnosisConcepts.MEA_7, DiagnosisConcepts._DiagnosisConcepts.MEA_8,
				DiagnosisConcepts._DiagnosisConcepts.MEA_9

		);
	}


	public static List<Integer> getHepatitisListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.HEA_1, DiagnosisConcepts._DiagnosisConcepts.HEA_2,
				DiagnosisConcepts._DiagnosisConcepts.HEA_3, DiagnosisConcepts._DiagnosisConcepts.HEA_4,
				DiagnosisConcepts._DiagnosisConcepts.HEA_5, DiagnosisConcepts._DiagnosisConcepts.HEA_6,
				DiagnosisConcepts._DiagnosisConcepts.HEA_7, DiagnosisConcepts._DiagnosisConcepts.HEA_8,
				DiagnosisConcepts._DiagnosisConcepts.HEA_9, DiagnosisConcepts._DiagnosisConcepts.HEA_10,
				DiagnosisConcepts._DiagnosisConcepts.HEA_11, DiagnosisConcepts._DiagnosisConcepts.HEA_12,
				DiagnosisConcepts._DiagnosisConcepts.HEA_13, DiagnosisConcepts._DiagnosisConcepts.HEA_14,
				DiagnosisConcepts._DiagnosisConcepts.HEA_15, DiagnosisConcepts._DiagnosisConcepts.HEA_16,
				DiagnosisConcepts._DiagnosisConcepts.HEA_17, DiagnosisConcepts._DiagnosisConcepts.HEA_18,
				DiagnosisConcepts._DiagnosisConcepts.HEA_19, DiagnosisConcepts._DiagnosisConcepts.HEA_20,
				DiagnosisConcepts._DiagnosisConcepts.HEA_21, DiagnosisConcepts._DiagnosisConcepts.HEA_22,
				DiagnosisConcepts._DiagnosisConcepts.HEA_23, DiagnosisConcepts._DiagnosisConcepts.HEA_24,
				DiagnosisConcepts._DiagnosisConcepts.HEA_25, DiagnosisConcepts._DiagnosisConcepts.HEA_26,
				DiagnosisConcepts._DiagnosisConcepts.HEA_27, DiagnosisConcepts._DiagnosisConcepts.HEA_28,
				DiagnosisConcepts._DiagnosisConcepts.HEA_29, DiagnosisConcepts._DiagnosisConcepts.HEA_30,
				DiagnosisConcepts._DiagnosisConcepts.HEA_31, DiagnosisConcepts._DiagnosisConcepts.HEA_32,
				DiagnosisConcepts._DiagnosisConcepts.HEA_33, DiagnosisConcepts._DiagnosisConcepts.HEA_34,
				DiagnosisConcepts._DiagnosisConcepts.HEA_35, DiagnosisConcepts._DiagnosisConcepts.HEA_36,
				DiagnosisConcepts._DiagnosisConcepts.HEA_37

		);
	}


	public static List<Integer> getMumpsListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MPSA_1, DiagnosisConcepts._DiagnosisConcepts.MPSA_2,
				DiagnosisConcepts._DiagnosisConcepts.MPSA_3,
				DiagnosisConcepts._DiagnosisConcepts.MPSA_4,
				DiagnosisConcepts._DiagnosisConcepts.MPSA_5,
				DiagnosisConcepts._DiagnosisConcepts.MPSA_6,
				DiagnosisConcepts._DiagnosisConcepts.MPSA_7
		);
	}
	public static List<Integer> getTestedMalariaConcepts() {
		return Arrays
				.asList(
						DiagnosisConcepts._DiagnosisConcepts.TMC_1,
						DiagnosisConcepts._DiagnosisConcepts.TMC_2,
						DiagnosisConcepts._DiagnosisConcepts.TMC_3
				);
	}

	public static List<Integer> getConfirmedMalariaList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.COA_1,
				DiagnosisConcepts._DiagnosisConcepts.COA_2,
				DiagnosisConcepts._DiagnosisConcepts.COA_3,
				DiagnosisConcepts._DiagnosisConcepts.COA_4,
				DiagnosisConcepts._DiagnosisConcepts.COA_5
		);
	}

	//Malaria in pregnancy
	public static List<Integer> getMalariaInPregnancyList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.MPA_1,
				DiagnosisConcepts._DiagnosisConcepts.MPA_2
		);
	}

	public static List<Integer> getMalnutritionList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.MALA_54,
				DiagnosisConcepts._DiagnosisConcepts.MALA_55,
				DiagnosisConcepts._DiagnosisConcepts.MALA_56,
				DiagnosisConcepts._DiagnosisConcepts.MALA_57,
				DiagnosisConcepts._DiagnosisConcepts.MALA_58,
				DiagnosisConcepts._DiagnosisConcepts.MALA_59,
				DiagnosisConcepts._DiagnosisConcepts.MALA_60,
				DiagnosisConcepts._DiagnosisConcepts.MALA_61,
				DiagnosisConcepts._DiagnosisConcepts.MALA_62,
				DiagnosisConcepts._DiagnosisConcepts.MALA_63
		);
	}


	public static List<Integer> getSexuallyTransmittedInfectionsList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.STIA_1, DiagnosisConcepts._DiagnosisConcepts.STIA_2,
				DiagnosisConcepts._DiagnosisConcepts.STIA_3, DiagnosisConcepts._DiagnosisConcepts.STIA_4,
				DiagnosisConcepts._DiagnosisConcepts.STIA_5, DiagnosisConcepts._DiagnosisConcepts.STIA_6,
				DiagnosisConcepts._DiagnosisConcepts.STIA_7, DiagnosisConcepts._DiagnosisConcepts.STIA_8,
				DiagnosisConcepts._DiagnosisConcepts.STIA_9, DiagnosisConcepts._DiagnosisConcepts.STIA_10,
				DiagnosisConcepts._DiagnosisConcepts.STIA_11, DiagnosisConcepts._DiagnosisConcepts.STIA_12,
				DiagnosisConcepts._DiagnosisConcepts.STIA_13, DiagnosisConcepts._DiagnosisConcepts.STIA_14,
				DiagnosisConcepts._DiagnosisConcepts.STIA_15, DiagnosisConcepts._DiagnosisConcepts.STIA_16,
				DiagnosisConcepts._DiagnosisConcepts.STIA_17, DiagnosisConcepts._DiagnosisConcepts.STIA_18,
				DiagnosisConcepts._DiagnosisConcepts.STIA_19, DiagnosisConcepts._DiagnosisConcepts.STIA_20,
				DiagnosisConcepts._DiagnosisConcepts.STIA_21, DiagnosisConcepts._DiagnosisConcepts.STIA_22,
				DiagnosisConcepts._DiagnosisConcepts.STIA_23, DiagnosisConcepts._DiagnosisConcepts.STIA_24,
				DiagnosisConcepts._DiagnosisConcepts.STIA_25, DiagnosisConcepts._DiagnosisConcepts.STIA_26,
				DiagnosisConcepts._DiagnosisConcepts.STIA_27, DiagnosisConcepts._DiagnosisConcepts.STIA_28,
				DiagnosisConcepts._DiagnosisConcepts.STIA_29, DiagnosisConcepts._DiagnosisConcepts.STIA_30,
				DiagnosisConcepts._DiagnosisConcepts.STIA_31, DiagnosisConcepts._DiagnosisConcepts.STIA_32,
				DiagnosisConcepts._DiagnosisConcepts.STIA_33, DiagnosisConcepts._DiagnosisConcepts.STIA_34,
				DiagnosisConcepts._DiagnosisConcepts.STIA_35, DiagnosisConcepts._DiagnosisConcepts.STIA_36,
				DiagnosisConcepts._DiagnosisConcepts.STIA_37, DiagnosisConcepts._DiagnosisConcepts.STIA_38,
				DiagnosisConcepts._DiagnosisConcepts.STIA_39, DiagnosisConcepts._DiagnosisConcepts.STIA_40,
				DiagnosisConcepts._DiagnosisConcepts.STIA_41, DiagnosisConcepts._DiagnosisConcepts.STIA_42,
				DiagnosisConcepts._DiagnosisConcepts.STIA_43, DiagnosisConcepts._DiagnosisConcepts.STIA_44,
				DiagnosisConcepts._DiagnosisConcepts.STIA_45, DiagnosisConcepts._DiagnosisConcepts.STIA_46,
				DiagnosisConcepts._DiagnosisConcepts.STIA_47, DiagnosisConcepts._DiagnosisConcepts.STIA_48,
				DiagnosisConcepts._DiagnosisConcepts.STIA_49, DiagnosisConcepts._DiagnosisConcepts.STIA_50,
				DiagnosisConcepts._DiagnosisConcepts.STIA_51, DiagnosisConcepts._DiagnosisConcepts.STIA_52,
				DiagnosisConcepts._DiagnosisConcepts.STIA_53, DiagnosisConcepts._DiagnosisConcepts.STIA_54,
				DiagnosisConcepts._DiagnosisConcepts.STIA_55, DiagnosisConcepts._DiagnosisConcepts.STIA_60,
				DiagnosisConcepts._DiagnosisConcepts.STIA_63, DiagnosisConcepts._DiagnosisConcepts.STIA_70
		);
	}


	public static List<Integer> getUrinaryTractInfectionListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.URA_1, DiagnosisConcepts._DiagnosisConcepts.URA_2,
				DiagnosisConcepts._DiagnosisConcepts.URA_3, DiagnosisConcepts._DiagnosisConcepts.URA_4,
				DiagnosisConcepts._DiagnosisConcepts.URA_5, DiagnosisConcepts._DiagnosisConcepts.URA_6,
				DiagnosisConcepts._DiagnosisConcepts.URA_6, DiagnosisConcepts._DiagnosisConcepts.URA_7,
				DiagnosisConcepts._DiagnosisConcepts.URA_8


		);
	}

	public static List<Integer> getInterstinalwormsListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.INA_1, DiagnosisConcepts._DiagnosisConcepts.INA_2,
				DiagnosisConcepts._DiagnosisConcepts.INA_3, DiagnosisConcepts._DiagnosisConcepts.INA_4,
				DiagnosisConcepts._DiagnosisConcepts.INA_5, DiagnosisConcepts._DiagnosisConcepts.INA_6,
				DiagnosisConcepts._DiagnosisConcepts.INA_7, DiagnosisConcepts._DiagnosisConcepts.INA_8
		);
	}

	public static List<Integer> getAnaemiaListB() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.ANEA_1, DiagnosisConcepts._DiagnosisConcepts.ANEA_2,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_3, DiagnosisConcepts._DiagnosisConcepts.ANEA_4,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_5, DiagnosisConcepts._DiagnosisConcepts.ANEA_6,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_7,DiagnosisConcepts._DiagnosisConcepts.ANEA_8,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_9,DiagnosisConcepts._DiagnosisConcepts.ANEA_10,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_11,DiagnosisConcepts._DiagnosisConcepts.ANEA_12,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_13,DiagnosisConcepts._DiagnosisConcepts.ANEA_14,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_15,DiagnosisConcepts._DiagnosisConcepts.ANEA_16,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_17,DiagnosisConcepts._DiagnosisConcepts.ANEA_18,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_19,DiagnosisConcepts._DiagnosisConcepts.ANEA_20,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_21,DiagnosisConcepts._DiagnosisConcepts.ANEA_22,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_23,DiagnosisConcepts._DiagnosisConcepts.ANEA_24,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_25,DiagnosisConcepts._DiagnosisConcepts.ANEA_26,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_27,DiagnosisConcepts._DiagnosisConcepts.ANEA_28,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_29,DiagnosisConcepts._DiagnosisConcepts.ANEA_30,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_31,DiagnosisConcepts._DiagnosisConcepts.ANEA_32,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_33,DiagnosisConcepts._DiagnosisConcepts.ANEA_34,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_35,DiagnosisConcepts._DiagnosisConcepts.ANEA_36,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_37,DiagnosisConcepts._DiagnosisConcepts.ANEA_38,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_39,DiagnosisConcepts._DiagnosisConcepts.ANEA_40,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_41,DiagnosisConcepts._DiagnosisConcepts.ANEA_42,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_43,DiagnosisConcepts._DiagnosisConcepts.ANEA_44,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_45,DiagnosisConcepts._DiagnosisConcepts.ANEA_46,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_47,DiagnosisConcepts._DiagnosisConcepts.ANEA_48,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_49,DiagnosisConcepts._DiagnosisConcepts.ANEA_50,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_51,DiagnosisConcepts._DiagnosisConcepts.ANEA_52,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_53,DiagnosisConcepts._DiagnosisConcepts.ANEA_54,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_55,DiagnosisConcepts._DiagnosisConcepts.ANEA_56,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_57,DiagnosisConcepts._DiagnosisConcepts.ANEA_58,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_59,DiagnosisConcepts._DiagnosisConcepts.ANEA_60,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_61,DiagnosisConcepts._DiagnosisConcepts.ANEA_62,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_63,DiagnosisConcepts._DiagnosisConcepts.ANEA_64,
				DiagnosisConcepts._DiagnosisConcepts.ANEA_65

		);
	}

	//EYE INFECTION B

	public static List<Integer> getEyeInfectionsList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.EYA_1,
				DiagnosisConcepts._DiagnosisConcepts.EYA_2,
				DiagnosisConcepts._DiagnosisConcepts.EYA_3,
				DiagnosisConcepts._DiagnosisConcepts.EYA_4,
				DiagnosisConcepts._DiagnosisConcepts.EYA_5,
				DiagnosisConcepts._DiagnosisConcepts.EYA_6,
				DiagnosisConcepts._DiagnosisConcepts.EYA_7,
				DiagnosisConcepts._DiagnosisConcepts.EYA_8,
				DiagnosisConcepts._DiagnosisConcepts.EYA_9,
				DiagnosisConcepts._DiagnosisConcepts.EYA_10,
				DiagnosisConcepts._DiagnosisConcepts.EYA_11,
				DiagnosisConcepts._DiagnosisConcepts.EYA_12,
				DiagnosisConcepts._DiagnosisConcepts.EYA_13,
				DiagnosisConcepts._DiagnosisConcepts.EYA_14,
				DiagnosisConcepts._DiagnosisConcepts.EYA_15,
				DiagnosisConcepts._DiagnosisConcepts.EYA_16,
				DiagnosisConcepts._DiagnosisConcepts.EYA_17,
				DiagnosisConcepts._DiagnosisConcepts.EYA_18,
				DiagnosisConcepts._DiagnosisConcepts.EYA_19,
				DiagnosisConcepts._DiagnosisConcepts.EYA_20,
				DiagnosisConcepts._DiagnosisConcepts.EYA_21,
				DiagnosisConcepts._DiagnosisConcepts.EYA_22,
				DiagnosisConcepts._DiagnosisConcepts.EYA_23,
				DiagnosisConcepts._DiagnosisConcepts.EYA_24,
				DiagnosisConcepts._DiagnosisConcepts.EYA_25,
				DiagnosisConcepts._DiagnosisConcepts.EYA_26,
				DiagnosisConcepts._DiagnosisConcepts.EYA_27,
				DiagnosisConcepts._DiagnosisConcepts.EYA_28,
				DiagnosisConcepts._DiagnosisConcepts.EYA_29,
				DiagnosisConcepts._DiagnosisConcepts.EYA_30,
				DiagnosisConcepts._DiagnosisConcepts.EYA_31,
				DiagnosisConcepts._DiagnosisConcepts.EYA_32,
				DiagnosisConcepts._DiagnosisConcepts.EYA_33,
				DiagnosisConcepts._DiagnosisConcepts.EYA_34,
				DiagnosisConcepts._DiagnosisConcepts.EYA_35,
				DiagnosisConcepts._DiagnosisConcepts.EYA_36,
				DiagnosisConcepts._DiagnosisConcepts.EYA_37,
				DiagnosisConcepts._DiagnosisConcepts.EYA_38,
				DiagnosisConcepts._DiagnosisConcepts.EYA_39,
				DiagnosisConcepts._DiagnosisConcepts.EYA_40,
				DiagnosisConcepts._DiagnosisConcepts.EYA_41,
				DiagnosisConcepts._DiagnosisConcepts.EYA_42,
				DiagnosisConcepts._DiagnosisConcepts.EYA_43,
				DiagnosisConcepts._DiagnosisConcepts.EYA_44,
				DiagnosisConcepts._DiagnosisConcepts.EYA_45,
				DiagnosisConcepts._DiagnosisConcepts.EYA_46,
				DiagnosisConcepts._DiagnosisConcepts.EYA_47,
				DiagnosisConcepts._DiagnosisConcepts.EYA_48,
				DiagnosisConcepts._DiagnosisConcepts.EYA_49,
				DiagnosisConcepts._DiagnosisConcepts.EYA_50,
				DiagnosisConcepts._DiagnosisConcepts.EYA_51,
				DiagnosisConcepts._DiagnosisConcepts.EYA_52,
				DiagnosisConcepts._DiagnosisConcepts.EYA_53,
				DiagnosisConcepts._DiagnosisConcepts.EYA_54,
				DiagnosisConcepts._DiagnosisConcepts.EYA_55,
				DiagnosisConcepts._DiagnosisConcepts.EYA_56,
				DiagnosisConcepts._DiagnosisConcepts.EYA_57,
				DiagnosisConcepts._DiagnosisConcepts.EYA_58,
				DiagnosisConcepts._DiagnosisConcepts.EYA_59,
				DiagnosisConcepts._DiagnosisConcepts.EYA_60,
				DiagnosisConcepts._DiagnosisConcepts.EYA_61,
				DiagnosisConcepts._DiagnosisConcepts.EYA_62,
				DiagnosisConcepts._DiagnosisConcepts.EYA_63,
				DiagnosisConcepts._DiagnosisConcepts.EYA_64,
				DiagnosisConcepts._DiagnosisConcepts.EYA_65,
				DiagnosisConcepts._DiagnosisConcepts.EYA_66,
				DiagnosisConcepts._DiagnosisConcepts.EYA_67,
				DiagnosisConcepts._DiagnosisConcepts.EYA_68,
				DiagnosisConcepts._DiagnosisConcepts.EYA_69,
				DiagnosisConcepts._DiagnosisConcepts.EYA_70,
				DiagnosisConcepts._DiagnosisConcepts.EYA_71,
				DiagnosisConcepts._DiagnosisConcepts.EYA_72,
				DiagnosisConcepts._DiagnosisConcepts.EYA_73,
				DiagnosisConcepts._DiagnosisConcepts.EYA_74,
				DiagnosisConcepts._DiagnosisConcepts.EYA_75,
				DiagnosisConcepts._DiagnosisConcepts.EYA_76,
				DiagnosisConcepts._DiagnosisConcepts.EYA_77,
				DiagnosisConcepts._DiagnosisConcepts.EYA_78,
				DiagnosisConcepts._DiagnosisConcepts.EYA_79,
				DiagnosisConcepts._DiagnosisConcepts.EYA_80,
				DiagnosisConcepts._DiagnosisConcepts.EYA_81,
				DiagnosisConcepts._DiagnosisConcepts.EYA_82,
				DiagnosisConcepts._DiagnosisConcepts.EYA_83,
				DiagnosisConcepts._DiagnosisConcepts.EYA_84,
				DiagnosisConcepts._DiagnosisConcepts.EYA_85,
				DiagnosisConcepts._DiagnosisConcepts.EYA_86,
				DiagnosisConcepts._DiagnosisConcepts.EYA_87,
				DiagnosisConcepts._DiagnosisConcepts.EYA_88,
				DiagnosisConcepts._DiagnosisConcepts.EYA_89,
				DiagnosisConcepts._DiagnosisConcepts.EYA_90,
				DiagnosisConcepts._DiagnosisConcepts.EYA_91,
				DiagnosisConcepts._DiagnosisConcepts.EYA_92,
				DiagnosisConcepts._DiagnosisConcepts.EYA_93,
				DiagnosisConcepts._DiagnosisConcepts.EYA_94,
				DiagnosisConcepts._DiagnosisConcepts.EYA_95,
				DiagnosisConcepts._DiagnosisConcepts.EYA_96,
				DiagnosisConcepts._DiagnosisConcepts.EYA_97,
				DiagnosisConcepts._DiagnosisConcepts.EYA_98,
				DiagnosisConcepts._DiagnosisConcepts.EYA_99,
				DiagnosisConcepts._DiagnosisConcepts.EYA_100,
				DiagnosisConcepts._DiagnosisConcepts.EYA_101,
				DiagnosisConcepts._DiagnosisConcepts.EYA_102,
				DiagnosisConcepts._DiagnosisConcepts.EYA_103,
				DiagnosisConcepts._DiagnosisConcepts.EYA_104,
				DiagnosisConcepts._DiagnosisConcepts.EYA_105,
				DiagnosisConcepts._DiagnosisConcepts.EYA_106,
				DiagnosisConcepts._DiagnosisConcepts.EYA_107,
				DiagnosisConcepts._DiagnosisConcepts.EYA_108,
				DiagnosisConcepts._DiagnosisConcepts.EYA_109,
				DiagnosisConcepts._DiagnosisConcepts.EYA_110,
				DiagnosisConcepts._DiagnosisConcepts.EYA_111,
				DiagnosisConcepts._DiagnosisConcepts.EYA_112,
				DiagnosisConcepts._DiagnosisConcepts.EYA_113,
				DiagnosisConcepts._DiagnosisConcepts.EYA_114,
				DiagnosisConcepts._DiagnosisConcepts.EYA_115,
				DiagnosisConcepts._DiagnosisConcepts.EYA_116,
				DiagnosisConcepts._DiagnosisConcepts.EYA_117,
				DiagnosisConcepts._DiagnosisConcepts.EYA_118,
				DiagnosisConcepts._DiagnosisConcepts.EYA_119,
				DiagnosisConcepts._DiagnosisConcepts.EYA_120,
				DiagnosisConcepts._DiagnosisConcepts.EYA_121,
				DiagnosisConcepts._DiagnosisConcepts.EYA_122,
				DiagnosisConcepts._DiagnosisConcepts.EYA_123,
				DiagnosisConcepts._DiagnosisConcepts.EYA_124,
				DiagnosisConcepts._DiagnosisConcepts.EYA_125,
				DiagnosisConcepts._DiagnosisConcepts.EYA_126,
				DiagnosisConcepts._DiagnosisConcepts.EYA_127,
				DiagnosisConcepts._DiagnosisConcepts.EYA_128,
				DiagnosisConcepts._DiagnosisConcepts.EYA_129,
				DiagnosisConcepts._DiagnosisConcepts.EYA_130,
				DiagnosisConcepts._DiagnosisConcepts.EYA_131,
				DiagnosisConcepts._DiagnosisConcepts.EYA_132,
				DiagnosisConcepts._DiagnosisConcepts.EYA_133,
				DiagnosisConcepts._DiagnosisConcepts.EYA_134,
				DiagnosisConcepts._DiagnosisConcepts.EYA_135,
				DiagnosisConcepts._DiagnosisConcepts.EYA_136,
				DiagnosisConcepts._DiagnosisConcepts.EYA_137,
				DiagnosisConcepts._DiagnosisConcepts.EYA_138,
				DiagnosisConcepts._DiagnosisConcepts.EYA_139,
				DiagnosisConcepts._DiagnosisConcepts.EYA_140,
				DiagnosisConcepts._DiagnosisConcepts.EYA_141,
				DiagnosisConcepts._DiagnosisConcepts.EYA_142,
				DiagnosisConcepts._DiagnosisConcepts.EYA_143,
				DiagnosisConcepts._DiagnosisConcepts.EYA_144,
				DiagnosisConcepts._DiagnosisConcepts.EYA_145,
				DiagnosisConcepts._DiagnosisConcepts.EYA_146,
				DiagnosisConcepts._DiagnosisConcepts.EYA_147,
				DiagnosisConcepts._DiagnosisConcepts.EYA_148,
				DiagnosisConcepts._DiagnosisConcepts.EYA_149,
				DiagnosisConcepts._DiagnosisConcepts.EYA_150,
				DiagnosisConcepts._DiagnosisConcepts.EYA_151,
				DiagnosisConcepts._DiagnosisConcepts.EYA_152,
				DiagnosisConcepts._DiagnosisConcepts.EYA_153,
				DiagnosisConcepts._DiagnosisConcepts.EYA_154,
				DiagnosisConcepts._DiagnosisConcepts.EYA_155,
				DiagnosisConcepts._DiagnosisConcepts.EYA_156,
				DiagnosisConcepts._DiagnosisConcepts.EYA_157,
				DiagnosisConcepts._DiagnosisConcepts.EYA_158,
				DiagnosisConcepts._DiagnosisConcepts.EYA_159,
				DiagnosisConcepts._DiagnosisConcepts.EYA_160,
				DiagnosisConcepts._DiagnosisConcepts.EYA_161,
				DiagnosisConcepts._DiagnosisConcepts.EYA_162,
				DiagnosisConcepts._DiagnosisConcepts.EYA_163,
				DiagnosisConcepts._DiagnosisConcepts.EYA_164,
				DiagnosisConcepts._DiagnosisConcepts.EYA_165,
				DiagnosisConcepts._DiagnosisConcepts.EYA_166,
				DiagnosisConcepts._DiagnosisConcepts.EYA_167,
				DiagnosisConcepts._DiagnosisConcepts.EYA_168,
				DiagnosisConcepts._DiagnosisConcepts.EYA_169,
				DiagnosisConcepts._DiagnosisConcepts.EYA_170,
				DiagnosisConcepts._DiagnosisConcepts.EYA_171,
				DiagnosisConcepts._DiagnosisConcepts.EYA_172,
				DiagnosisConcepts._DiagnosisConcepts.EYA_173,
				DiagnosisConcepts._DiagnosisConcepts.EYA_174,
				DiagnosisConcepts._DiagnosisConcepts.EYA_175,
				DiagnosisConcepts._DiagnosisConcepts.EYA_176,
				DiagnosisConcepts._DiagnosisConcepts.EYA_177,
				DiagnosisConcepts._DiagnosisConcepts.EYA_178,
				DiagnosisConcepts._DiagnosisConcepts.EYA_179,
				DiagnosisConcepts._DiagnosisConcepts.EYA_180,
				DiagnosisConcepts._DiagnosisConcepts.EYA_181,
				DiagnosisConcepts._DiagnosisConcepts.EYA_182,
				DiagnosisConcepts._DiagnosisConcepts.EYA_183,
				DiagnosisConcepts._DiagnosisConcepts.EYA_184,
				DiagnosisConcepts._DiagnosisConcepts.EYA_185,
				DiagnosisConcepts._DiagnosisConcepts.EYA_186,
				DiagnosisConcepts._DiagnosisConcepts.EYA_187,
				DiagnosisConcepts._DiagnosisConcepts.EYA_188,
				DiagnosisConcepts._DiagnosisConcepts.EYA_189,
				DiagnosisConcepts._DiagnosisConcepts.EYA_190,
				DiagnosisConcepts._DiagnosisConcepts.EYA_191,
				DiagnosisConcepts._DiagnosisConcepts.EYA_192,
				DiagnosisConcepts._DiagnosisConcepts.EYA_193,
				DiagnosisConcepts._DiagnosisConcepts.EYA_194,
				DiagnosisConcepts._DiagnosisConcepts.EYA_195,
				DiagnosisConcepts._DiagnosisConcepts.EYA_196,
				DiagnosisConcepts._DiagnosisConcepts.EYA_197,
				DiagnosisConcepts._DiagnosisConcepts.EYA_198,
				DiagnosisConcepts._DiagnosisConcepts.EYA_199,
				DiagnosisConcepts._DiagnosisConcepts.EYA_200,
				DiagnosisConcepts._DiagnosisConcepts.EYA_201,
				DiagnosisConcepts._DiagnosisConcepts.EYA_202,
				DiagnosisConcepts._DiagnosisConcepts.EYA_203,
				DiagnosisConcepts._DiagnosisConcepts.EYA_204,
				DiagnosisConcepts._DiagnosisConcepts.EYA_205,
				DiagnosisConcepts._DiagnosisConcepts.EYA_206,
				DiagnosisConcepts._DiagnosisConcepts.EYA_207,
				DiagnosisConcepts._DiagnosisConcepts.EYA_208,
				DiagnosisConcepts._DiagnosisConcepts.EYA_209,
				DiagnosisConcepts._DiagnosisConcepts.EYA_210,
				DiagnosisConcepts._DiagnosisConcepts.EYA_211,
				DiagnosisConcepts._DiagnosisConcepts.EYA_212,
				DiagnosisConcepts._DiagnosisConcepts.EYA_213,
				DiagnosisConcepts._DiagnosisConcepts.EYA_214,
				DiagnosisConcepts._DiagnosisConcepts.EYA_215,
				DiagnosisConcepts._DiagnosisConcepts.EYA_216,
				DiagnosisConcepts._DiagnosisConcepts.EYA_217,
				DiagnosisConcepts._DiagnosisConcepts.EYA_218,
				DiagnosisConcepts._DiagnosisConcepts.EYA_219,
				DiagnosisConcepts._DiagnosisConcepts.EYA_220,
				DiagnosisConcepts._DiagnosisConcepts.EYA_221,
				DiagnosisConcepts._DiagnosisConcepts.EYA_222,
				DiagnosisConcepts._DiagnosisConcepts.EYA_223,
				DiagnosisConcepts._DiagnosisConcepts.EYA_224,
				DiagnosisConcepts._DiagnosisConcepts.EYA_225,
				DiagnosisConcepts._DiagnosisConcepts.EYA_226,
				DiagnosisConcepts._DiagnosisConcepts.EYA_227,
				DiagnosisConcepts._DiagnosisConcepts.EYA_228,
				DiagnosisConcepts._DiagnosisConcepts.EYA_229,
				DiagnosisConcepts._DiagnosisConcepts.EYA_230,
				DiagnosisConcepts._DiagnosisConcepts.EYA_231,
				DiagnosisConcepts._DiagnosisConcepts.EYA_232,
				DiagnosisConcepts._DiagnosisConcepts.EYA_233,
				DiagnosisConcepts._DiagnosisConcepts.EYA_234,
				DiagnosisConcepts._DiagnosisConcepts.EYA_235,
				DiagnosisConcepts._DiagnosisConcepts.EYA_236,
				DiagnosisConcepts._DiagnosisConcepts.EYA_237,
				DiagnosisConcepts._DiagnosisConcepts.EYA_238,
				DiagnosisConcepts._DiagnosisConcepts.EYA_239,
				DiagnosisConcepts._DiagnosisConcepts.EYA_240,
				DiagnosisConcepts._DiagnosisConcepts.EYA_241,
				DiagnosisConcepts._DiagnosisConcepts.EYA_242,
				DiagnosisConcepts._DiagnosisConcepts.EYA_243,
				DiagnosisConcepts._DiagnosisConcepts.EYA_244,
				DiagnosisConcepts._DiagnosisConcepts.EYA_245,
				DiagnosisConcepts._DiagnosisConcepts.EYA_246,
				DiagnosisConcepts._DiagnosisConcepts.EYA_247,
				DiagnosisConcepts._DiagnosisConcepts.EYA_248,
				DiagnosisConcepts._DiagnosisConcepts.EYA_249,
				DiagnosisConcepts._DiagnosisConcepts.EYA_250,
				DiagnosisConcepts._DiagnosisConcepts.EYA_251,
				DiagnosisConcepts._DiagnosisConcepts.EYA_252,
				DiagnosisConcepts._DiagnosisConcepts.EYA_253,
				DiagnosisConcepts._DiagnosisConcepts.EYA_254,
				DiagnosisConcepts._DiagnosisConcepts.EYA_255,
				DiagnosisConcepts._DiagnosisConcepts.EYA_256,
				DiagnosisConcepts._DiagnosisConcepts.EYA_257,
				DiagnosisConcepts._DiagnosisConcepts.EYA_258,
				DiagnosisConcepts._DiagnosisConcepts.EYA_259,
				DiagnosisConcepts._DiagnosisConcepts.EYA_260,
				DiagnosisConcepts._DiagnosisConcepts.EYA_261,
				DiagnosisConcepts._DiagnosisConcepts.EYA_262,
				DiagnosisConcepts._DiagnosisConcepts.EYA_263,
				DiagnosisConcepts._DiagnosisConcepts.EYA_264,
				DiagnosisConcepts._DiagnosisConcepts.EYA_265,
				DiagnosisConcepts._DiagnosisConcepts.EYA_266,
				DiagnosisConcepts._DiagnosisConcepts.EYA_267,
				DiagnosisConcepts._DiagnosisConcepts.EYA_268,
				DiagnosisConcepts._DiagnosisConcepts.EYA_269,
				DiagnosisConcepts._DiagnosisConcepts.EYA_270,
				DiagnosisConcepts._DiagnosisConcepts.EYA_271,
				DiagnosisConcepts._DiagnosisConcepts.EYA_272,
				DiagnosisConcepts._DiagnosisConcepts.EYA_273,
				DiagnosisConcepts._DiagnosisConcepts.EYA_274,
				DiagnosisConcepts._DiagnosisConcepts.EYA_275,
				DiagnosisConcepts._DiagnosisConcepts.EYA_276,
				DiagnosisConcepts._DiagnosisConcepts.EYA_277,
				DiagnosisConcepts._DiagnosisConcepts.EYA_278,
				DiagnosisConcepts._DiagnosisConcepts.EYA_279,
				DiagnosisConcepts._DiagnosisConcepts.EYA_280,
				DiagnosisConcepts._DiagnosisConcepts.EYA_281,
				DiagnosisConcepts._DiagnosisConcepts.EYA_282,
				DiagnosisConcepts._DiagnosisConcepts.EYA_283,
				DiagnosisConcepts._DiagnosisConcepts.EYA_284,
				DiagnosisConcepts._DiagnosisConcepts.EYA_285,
				DiagnosisConcepts._DiagnosisConcepts.EYA_286,
				DiagnosisConcepts._DiagnosisConcepts.EYA_287,
				DiagnosisConcepts._DiagnosisConcepts.EYA_288,
				DiagnosisConcepts._DiagnosisConcepts.EYA_289,
				DiagnosisConcepts._DiagnosisConcepts.EYA_290,
				DiagnosisConcepts._DiagnosisConcepts.EYA_291,
				DiagnosisConcepts._DiagnosisConcepts.EYA_292,
				DiagnosisConcepts._DiagnosisConcepts.EYA_293,
				DiagnosisConcepts._DiagnosisConcepts.EYA_294,
				DiagnosisConcepts._DiagnosisConcepts.EYA_295,
				DiagnosisConcepts._DiagnosisConcepts.EYA_296,
				DiagnosisConcepts._DiagnosisConcepts.EYA_297,
				DiagnosisConcepts._DiagnosisConcepts.EYA_298,
				DiagnosisConcepts._DiagnosisConcepts.EYA_299,
				DiagnosisConcepts._DiagnosisConcepts.EYA_300,
				DiagnosisConcepts._DiagnosisConcepts.EYA_301,
				DiagnosisConcepts._DiagnosisConcepts.EYA_302,
				DiagnosisConcepts._DiagnosisConcepts.EYA_303,
				DiagnosisConcepts._DiagnosisConcepts.EYA_304,
				DiagnosisConcepts._DiagnosisConcepts.EYA_305,
				DiagnosisConcepts._DiagnosisConcepts.EYA_306,
				DiagnosisConcepts._DiagnosisConcepts.EYA_307,
				DiagnosisConcepts._DiagnosisConcepts.EYA_308,
				DiagnosisConcepts._DiagnosisConcepts.EYA_309,
				DiagnosisConcepts._DiagnosisConcepts.EYA_310,
				DiagnosisConcepts._DiagnosisConcepts.EYA_311,
				DiagnosisConcepts._DiagnosisConcepts.EYA_312,
				DiagnosisConcepts._DiagnosisConcepts.EYA_313,
				DiagnosisConcepts._DiagnosisConcepts.EYA_314,
				DiagnosisConcepts._DiagnosisConcepts.EYA_315,
				DiagnosisConcepts._DiagnosisConcepts.EYA_316,
				DiagnosisConcepts._DiagnosisConcepts.EYA_317,
				DiagnosisConcepts._DiagnosisConcepts.EYA_318,
				DiagnosisConcepts._DiagnosisConcepts.EYA_319,
				DiagnosisConcepts._DiagnosisConcepts.EYA_320,
				DiagnosisConcepts._DiagnosisConcepts.EYA_321,
				DiagnosisConcepts._DiagnosisConcepts.EYA_322,
				DiagnosisConcepts._DiagnosisConcepts.EYA_323,
				DiagnosisConcepts._DiagnosisConcepts.EYA_324,
				DiagnosisConcepts._DiagnosisConcepts.EYA_325,
				DiagnosisConcepts._DiagnosisConcepts.EYA_326,
				DiagnosisConcepts._DiagnosisConcepts.EYA_327,
				DiagnosisConcepts._DiagnosisConcepts.EYA_328,
				DiagnosisConcepts._DiagnosisConcepts.EYA_329,
				DiagnosisConcepts._DiagnosisConcepts.EYA_330,
				DiagnosisConcepts._DiagnosisConcepts.EYA_331,
				DiagnosisConcepts._DiagnosisConcepts.EYA_332,
				DiagnosisConcepts._DiagnosisConcepts.EYA_333,
				DiagnosisConcepts._DiagnosisConcepts.EYA_334,
				DiagnosisConcepts._DiagnosisConcepts.EYA_335,
				DiagnosisConcepts._DiagnosisConcepts.EYA_336,
				DiagnosisConcepts._DiagnosisConcepts.EYA_337,
				DiagnosisConcepts._DiagnosisConcepts.EYA_338,
				DiagnosisConcepts._DiagnosisConcepts.EYA_339,
				DiagnosisConcepts._DiagnosisConcepts.EYA_340,
				DiagnosisConcepts._DiagnosisConcepts.EYA_340,
				DiagnosisConcepts._DiagnosisConcepts.EYA_341,
				DiagnosisConcepts._DiagnosisConcepts.EYA_342,
				DiagnosisConcepts._DiagnosisConcepts.EYA_343,
				DiagnosisConcepts._DiagnosisConcepts.EYA_344,
				DiagnosisConcepts._DiagnosisConcepts.EYA_345,
				DiagnosisConcepts._DiagnosisConcepts.EYA_346,
				DiagnosisConcepts._DiagnosisConcepts.EYA_347,
				DiagnosisConcepts._DiagnosisConcepts.EYA_348,
				DiagnosisConcepts._DiagnosisConcepts.EYA_349,
				DiagnosisConcepts._DiagnosisConcepts.EYA_350,
				DiagnosisConcepts._DiagnosisConcepts.EYA_351,
				DiagnosisConcepts._DiagnosisConcepts.EYA_352,
				DiagnosisConcepts._DiagnosisConcepts.EYA_353,
				DiagnosisConcepts._DiagnosisConcepts.EYA_354,
				DiagnosisConcepts._DiagnosisConcepts.EYA_355,
				DiagnosisConcepts._DiagnosisConcepts.EYA_356,
				DiagnosisConcepts._DiagnosisConcepts.EYA_357,
				DiagnosisConcepts._DiagnosisConcepts.EYA_358,
				DiagnosisConcepts._DiagnosisConcepts.EYA_359,
				DiagnosisConcepts._DiagnosisConcepts.EYA_360,
				DiagnosisConcepts._DiagnosisConcepts.EYA_361,
				DiagnosisConcepts._DiagnosisConcepts.EYA_362,
				DiagnosisConcepts._DiagnosisConcepts.EYA_363,
				DiagnosisConcepts._DiagnosisConcepts.EYA_364,
				DiagnosisConcepts._DiagnosisConcepts.EYA_365,
				DiagnosisConcepts._DiagnosisConcepts.EYA_366,
				DiagnosisConcepts._DiagnosisConcepts.EYA_367,
				DiagnosisConcepts._DiagnosisConcepts.EYA_368,
				DiagnosisConcepts._DiagnosisConcepts.EYA_369,
				DiagnosisConcepts._DiagnosisConcepts.EYA_370,
				DiagnosisConcepts._DiagnosisConcepts.EYA_371,
				DiagnosisConcepts._DiagnosisConcepts.EYA_372,
				DiagnosisConcepts._DiagnosisConcepts.EYA_373,
				DiagnosisConcepts._DiagnosisConcepts.EYA_374,
				DiagnosisConcepts._DiagnosisConcepts.EYA_375,
				DiagnosisConcepts._DiagnosisConcepts.EYA_376,
				DiagnosisConcepts._DiagnosisConcepts.EYA_377,
				DiagnosisConcepts._DiagnosisConcepts.EYA_378,
				DiagnosisConcepts._DiagnosisConcepts.EYA_379,
				DiagnosisConcepts._DiagnosisConcepts.EYA_380,
				DiagnosisConcepts._DiagnosisConcepts.EYA_381,
				DiagnosisConcepts._DiagnosisConcepts.EYA_382,
				DiagnosisConcepts._DiagnosisConcepts.EYA_383,
				DiagnosisConcepts._DiagnosisConcepts.EYA_384,
				DiagnosisConcepts._DiagnosisConcepts.EYA_385,
				DiagnosisConcepts._DiagnosisConcepts.EYA_386,
				DiagnosisConcepts._DiagnosisConcepts.EYA_387,
				DiagnosisConcepts._DiagnosisConcepts.EYA_388,
				DiagnosisConcepts._DiagnosisConcepts.EYA_389,
				DiagnosisConcepts._DiagnosisConcepts.EYA_390,
				DiagnosisConcepts._DiagnosisConcepts.EYA_391,
				DiagnosisConcepts._DiagnosisConcepts.EYA_392,
				DiagnosisConcepts._DiagnosisConcepts.EYA_393,
				DiagnosisConcepts._DiagnosisConcepts.EYA_394,
				DiagnosisConcepts._DiagnosisConcepts.EYA_395,
				DiagnosisConcepts._DiagnosisConcepts.EYA_396,
				DiagnosisConcepts._DiagnosisConcepts.EYA_397,
				DiagnosisConcepts._DiagnosisConcepts.EYA_398,
				DiagnosisConcepts._DiagnosisConcepts.EYA_399,
				DiagnosisConcepts._DiagnosisConcepts.EYA_400,
				DiagnosisConcepts._DiagnosisConcepts.EYA_401,
				DiagnosisConcepts._DiagnosisConcepts.EYA_402,
				DiagnosisConcepts._DiagnosisConcepts.EYA_403,
				DiagnosisConcepts._DiagnosisConcepts.EYA_404,
				DiagnosisConcepts._DiagnosisConcepts.EYA_405,
				DiagnosisConcepts._DiagnosisConcepts.EYA_406,
				DiagnosisConcepts._DiagnosisConcepts.EYA_407,
				DiagnosisConcepts._DiagnosisConcepts.EYA_408,
				DiagnosisConcepts._DiagnosisConcepts.EYA_409,
				DiagnosisConcepts._DiagnosisConcepts.EYA_410,
				DiagnosisConcepts._DiagnosisConcepts.EYA_411,
				DiagnosisConcepts._DiagnosisConcepts.EYA_412,
				DiagnosisConcepts._DiagnosisConcepts.EYA_413,
				DiagnosisConcepts._DiagnosisConcepts.EYA_414,
				DiagnosisConcepts._DiagnosisConcepts.EYA_415,
				DiagnosisConcepts._DiagnosisConcepts.EYA_416,
				DiagnosisConcepts._DiagnosisConcepts.EYA_417,
				DiagnosisConcepts._DiagnosisConcepts.EYA_418,
				DiagnosisConcepts._DiagnosisConcepts.EYA_419,
				DiagnosisConcepts._DiagnosisConcepts.EYA_420,
				DiagnosisConcepts._DiagnosisConcepts.EYA_422,
				DiagnosisConcepts._DiagnosisConcepts.EYA_423,
				DiagnosisConcepts._DiagnosisConcepts.EYA_424,
				DiagnosisConcepts._DiagnosisConcepts.EYA_425,
				DiagnosisConcepts._DiagnosisConcepts.EYA_426,
				DiagnosisConcepts._DiagnosisConcepts.EYA_427,
				DiagnosisConcepts._DiagnosisConcepts.EYA_428,
				DiagnosisConcepts._DiagnosisConcepts.EYA_429,
				DiagnosisConcepts._DiagnosisConcepts.EYA_421,
				DiagnosisConcepts._DiagnosisConcepts.EYA_430,
				DiagnosisConcepts._DiagnosisConcepts.EYA_431,
				DiagnosisConcepts._DiagnosisConcepts.EYA_432,
				DiagnosisConcepts._DiagnosisConcepts.EYA_433,
				DiagnosisConcepts._DiagnosisConcepts.EYA_434,
				DiagnosisConcepts._DiagnosisConcepts.EYA_435,
				DiagnosisConcepts._DiagnosisConcepts.EYA_436,
				DiagnosisConcepts._DiagnosisConcepts.EYA_437,
				DiagnosisConcepts._DiagnosisConcepts.EYA_438,
				DiagnosisConcepts._DiagnosisConcepts.EYA_439,
				DiagnosisConcepts._DiagnosisConcepts.EYA_440,
				DiagnosisConcepts._DiagnosisConcepts.EYA_441,
				DiagnosisConcepts._DiagnosisConcepts.EYA_442,
				DiagnosisConcepts._DiagnosisConcepts.EYA_443,
				DiagnosisConcepts._DiagnosisConcepts.EYA_444,
				DiagnosisConcepts._DiagnosisConcepts.EYA_445,
				DiagnosisConcepts._DiagnosisConcepts.EYA_446,
				DiagnosisConcepts._DiagnosisConcepts.EYA_447,
				DiagnosisConcepts._DiagnosisConcepts.EYA_448,
				DiagnosisConcepts._DiagnosisConcepts.EYA_449,
				DiagnosisConcepts._DiagnosisConcepts.EYA_450,
				DiagnosisConcepts._DiagnosisConcepts.EYA_451,
				DiagnosisConcepts._DiagnosisConcepts.EYA_452,
				DiagnosisConcepts._DiagnosisConcepts.EYA_453,
				DiagnosisConcepts._DiagnosisConcepts.EYA_454,
				DiagnosisConcepts._DiagnosisConcepts.EYA_455,
				DiagnosisConcepts._DiagnosisConcepts.EYA_456,
				DiagnosisConcepts._DiagnosisConcepts.EYA_457,
				DiagnosisConcepts._DiagnosisConcepts.EYA_458,
				DiagnosisConcepts._DiagnosisConcepts.EYA_459,
				DiagnosisConcepts._DiagnosisConcepts.EYA_460,
				DiagnosisConcepts._DiagnosisConcepts.EYA_461,
				DiagnosisConcepts._DiagnosisConcepts.EYA_462,
				DiagnosisConcepts._DiagnosisConcepts.EYA_463,
				DiagnosisConcepts._DiagnosisConcepts.EYA_464,
				DiagnosisConcepts._DiagnosisConcepts.EYA_465,
				DiagnosisConcepts._DiagnosisConcepts.EYA_466,
				DiagnosisConcepts._DiagnosisConcepts.EYA_467,
				DiagnosisConcepts._DiagnosisConcepts.EYA_468,
				DiagnosisConcepts._DiagnosisConcepts.EYA_469,
				DiagnosisConcepts._DiagnosisConcepts.EYA_470,
				DiagnosisConcepts._DiagnosisConcepts.EYA_471,
				DiagnosisConcepts._DiagnosisConcepts.EYA_472,
				DiagnosisConcepts._DiagnosisConcepts.EYA_473,
				DiagnosisConcepts._DiagnosisConcepts.EYA_474,
				DiagnosisConcepts._DiagnosisConcepts.EYA_475,
				DiagnosisConcepts._DiagnosisConcepts.EYA_476,
				DiagnosisConcepts._DiagnosisConcepts.EYA_477,
				DiagnosisConcepts._DiagnosisConcepts.EYA_478,
				DiagnosisConcepts._DiagnosisConcepts.EYA_479,
				DiagnosisConcepts._DiagnosisConcepts.EYA_480,
				DiagnosisConcepts._DiagnosisConcepts.EYA_481,
				DiagnosisConcepts._DiagnosisConcepts.EYA_482,
				DiagnosisConcepts._DiagnosisConcepts.EYA_483,
				DiagnosisConcepts._DiagnosisConcepts.EYA_484,
				DiagnosisConcepts._DiagnosisConcepts.EYA_485,
				DiagnosisConcepts._DiagnosisConcepts.EYA_486,
				DiagnosisConcepts._DiagnosisConcepts.EYA_487,
				DiagnosisConcepts._DiagnosisConcepts.EYA_488,
				DiagnosisConcepts._DiagnosisConcepts.EYA_489,
				DiagnosisConcepts._DiagnosisConcepts.EYA_490,
				DiagnosisConcepts._DiagnosisConcepts.EYA_491,
				DiagnosisConcepts._DiagnosisConcepts.EYA_492,
				DiagnosisConcepts._DiagnosisConcepts.EYA_493,
				DiagnosisConcepts._DiagnosisConcepts.EYA_494,
				DiagnosisConcepts._DiagnosisConcepts.EYA_495,
				DiagnosisConcepts._DiagnosisConcepts.EYA_496,
				DiagnosisConcepts._DiagnosisConcepts.EYA_497,
				DiagnosisConcepts._DiagnosisConcepts.EYA_498,
				DiagnosisConcepts._DiagnosisConcepts.EYA_499,
				DiagnosisConcepts._DiagnosisConcepts.EYA_501,
				DiagnosisConcepts._DiagnosisConcepts.EYA_502,
				DiagnosisConcepts._DiagnosisConcepts.EYA_503,
				DiagnosisConcepts._DiagnosisConcepts.EYA_504,
				DiagnosisConcepts._DiagnosisConcepts.EYA_505,
				DiagnosisConcepts._DiagnosisConcepts.EYA_506,
				DiagnosisConcepts._DiagnosisConcepts.EYA_507,
				DiagnosisConcepts._DiagnosisConcepts.EYA_508,
				DiagnosisConcepts._DiagnosisConcepts.EYA_509,
				DiagnosisConcepts._DiagnosisConcepts.EYA_510,
				DiagnosisConcepts._DiagnosisConcepts.EYA_511,
				DiagnosisConcepts._DiagnosisConcepts.EYA_512,
				DiagnosisConcepts._DiagnosisConcepts.EYA_513,
				DiagnosisConcepts._DiagnosisConcepts.EYA_514,
				DiagnosisConcepts._DiagnosisConcepts.EYA_515,
				DiagnosisConcepts._DiagnosisConcepts.EYA_516,
				DiagnosisConcepts._DiagnosisConcepts.EYA_517,
				DiagnosisConcepts._DiagnosisConcepts.EYA_518,
				DiagnosisConcepts._DiagnosisConcepts.EYA_519,
				DiagnosisConcepts._DiagnosisConcepts.EYA_520,
				DiagnosisConcepts._DiagnosisConcepts.EYA_521,
				DiagnosisConcepts._DiagnosisConcepts.EYA_522,
				DiagnosisConcepts._DiagnosisConcepts.EYA_523,
				DiagnosisConcepts._DiagnosisConcepts.EYA_524,
				DiagnosisConcepts._DiagnosisConcepts.EYA_525,
				DiagnosisConcepts._DiagnosisConcepts.EYA_526,
				DiagnosisConcepts._DiagnosisConcepts.EYA_527,
				DiagnosisConcepts._DiagnosisConcepts.EYA_528,
				DiagnosisConcepts._DiagnosisConcepts.EYA_529,
				DiagnosisConcepts._DiagnosisConcepts.EYA_530,
				DiagnosisConcepts._DiagnosisConcepts.EYA_531,
				DiagnosisConcepts._DiagnosisConcepts.EYA_532,
				DiagnosisConcepts._DiagnosisConcepts.EYA_533,
				DiagnosisConcepts._DiagnosisConcepts.EYA_534,
				DiagnosisConcepts._DiagnosisConcepts.EYA_535,
				DiagnosisConcepts._DiagnosisConcepts.EYA_536,
				DiagnosisConcepts._DiagnosisConcepts.EYA_537,
				DiagnosisConcepts._DiagnosisConcepts.EYA_538,
				DiagnosisConcepts._DiagnosisConcepts.EYA_539,
				DiagnosisConcepts._DiagnosisConcepts.EYA_540,
				DiagnosisConcepts._DiagnosisConcepts.EYA_541,
				DiagnosisConcepts._DiagnosisConcepts.EYA_542,
				DiagnosisConcepts._DiagnosisConcepts.EYA_543,
				DiagnosisConcepts._DiagnosisConcepts.EYA_544,
				DiagnosisConcepts._DiagnosisConcepts.EYA_545,
				DiagnosisConcepts._DiagnosisConcepts.EYA_546,
				DiagnosisConcepts._DiagnosisConcepts.EYA_547,
				DiagnosisConcepts._DiagnosisConcepts.EYA_548,
				DiagnosisConcepts._DiagnosisConcepts.EYA_549,
				DiagnosisConcepts._DiagnosisConcepts.EYA_550,
				DiagnosisConcepts._DiagnosisConcepts.EYA_551,
				DiagnosisConcepts._DiagnosisConcepts.EYA_552,
				DiagnosisConcepts._DiagnosisConcepts.EYA_553,
				DiagnosisConcepts._DiagnosisConcepts.EYA_554,
				DiagnosisConcepts._DiagnosisConcepts.EYA_555,
				DiagnosisConcepts._DiagnosisConcepts.EYA_556,
				DiagnosisConcepts._DiagnosisConcepts.EYA_557,
				DiagnosisConcepts._DiagnosisConcepts.EYA_558,
				DiagnosisConcepts._DiagnosisConcepts.EYA_559,
				DiagnosisConcepts._DiagnosisConcepts.EYA_560,
				DiagnosisConcepts._DiagnosisConcepts.EYA_561,
				DiagnosisConcepts._DiagnosisConcepts.EYA_562,
				DiagnosisConcepts._DiagnosisConcepts.EYA_563,
				DiagnosisConcepts._DiagnosisConcepts.EYA_564,
				DiagnosisConcepts._DiagnosisConcepts.EYA_565,
				DiagnosisConcepts._DiagnosisConcepts.EYA_566,
				DiagnosisConcepts._DiagnosisConcepts.EYA_567,
				DiagnosisConcepts._DiagnosisConcepts.EYA_568,
				DiagnosisConcepts._DiagnosisConcepts.EYA_569,
				DiagnosisConcepts._DiagnosisConcepts.EYA_570,
				DiagnosisConcepts._DiagnosisConcepts.EYA_571,
				DiagnosisConcepts._DiagnosisConcepts.EYA_572,
				DiagnosisConcepts._DiagnosisConcepts.EYA_573,
				DiagnosisConcepts._DiagnosisConcepts.EYA_574,
				DiagnosisConcepts._DiagnosisConcepts.EYA_575,
				DiagnosisConcepts._DiagnosisConcepts.EYA_576,
				DiagnosisConcepts._DiagnosisConcepts.EYA_577,
				DiagnosisConcepts._DiagnosisConcepts.EYA_578,
				DiagnosisConcepts._DiagnosisConcepts.EYA_579,
				DiagnosisConcepts._DiagnosisConcepts.EYA_580,
				DiagnosisConcepts._DiagnosisConcepts.EYA_581,
				DiagnosisConcepts._DiagnosisConcepts.EYA_582,
				DiagnosisConcepts._DiagnosisConcepts.EYA_583,
				DiagnosisConcepts._DiagnosisConcepts.EYA_584,
				DiagnosisConcepts._DiagnosisConcepts.EYA_585,
				DiagnosisConcepts._DiagnosisConcepts.EYA_590,
				DiagnosisConcepts._DiagnosisConcepts.EYA_591,
				DiagnosisConcepts._DiagnosisConcepts.EYA_592,
				DiagnosisConcepts._DiagnosisConcepts.EYA_593,
				DiagnosisConcepts._DiagnosisConcepts.EYA_594,
				DiagnosisConcepts._DiagnosisConcepts.EYA_595,
				DiagnosisConcepts._DiagnosisConcepts.EYA_596,
				DiagnosisConcepts._DiagnosisConcepts.EYA_597,
				DiagnosisConcepts._DiagnosisConcepts.EYA_598,
				DiagnosisConcepts._DiagnosisConcepts.EYA_599,
				DiagnosisConcepts._DiagnosisConcepts.EYA_600,
				DiagnosisConcepts._DiagnosisConcepts.EYA_601,
				DiagnosisConcepts._DiagnosisConcepts.EYA_602,
				DiagnosisConcepts._DiagnosisConcepts.EYA_603,
				DiagnosisConcepts._DiagnosisConcepts.EYA_604,
				DiagnosisConcepts._DiagnosisConcepts.EYA_605,
				DiagnosisConcepts._DiagnosisConcepts.EYA_606,
				DiagnosisConcepts._DiagnosisConcepts.EYA_607,
				DiagnosisConcepts._DiagnosisConcepts.EYA_608,
				DiagnosisConcepts._DiagnosisConcepts.EYA_609,
				DiagnosisConcepts._DiagnosisConcepts.EYA_610,
				DiagnosisConcepts._DiagnosisConcepts.EYA_611,
				DiagnosisConcepts._DiagnosisConcepts.EYA_612,
				DiagnosisConcepts._DiagnosisConcepts.EYA_613,
				DiagnosisConcepts._DiagnosisConcepts.EYA_614,
				DiagnosisConcepts._DiagnosisConcepts.EYA_615,
				DiagnosisConcepts._DiagnosisConcepts.EYA_616,
				DiagnosisConcepts._DiagnosisConcepts.EYA_617,
				DiagnosisConcepts._DiagnosisConcepts.EYA_618,
				DiagnosisConcepts._DiagnosisConcepts.EYA_619,
				DiagnosisConcepts._DiagnosisConcepts.EYA_620,
				DiagnosisConcepts._DiagnosisConcepts.EYA_621,
				DiagnosisConcepts._DiagnosisConcepts.EYA_622,
				DiagnosisConcepts._DiagnosisConcepts.EYA_623,
				DiagnosisConcepts._DiagnosisConcepts.EYA_624,
				DiagnosisConcepts._DiagnosisConcepts.EYA_625,
				DiagnosisConcepts._DiagnosisConcepts.EYA_626,
				DiagnosisConcepts._DiagnosisConcepts.EYA_627,
				DiagnosisConcepts._DiagnosisConcepts.EYA_628,
				DiagnosisConcepts._DiagnosisConcepts.EYA_629,
				DiagnosisConcepts._DiagnosisConcepts.EYA_630,
				DiagnosisConcepts._DiagnosisConcepts.EYA_631,
				DiagnosisConcepts._DiagnosisConcepts.EYA_632,
				DiagnosisConcepts._DiagnosisConcepts.EYA_633,
				DiagnosisConcepts._DiagnosisConcepts.EYA_634,
				DiagnosisConcepts._DiagnosisConcepts.EYA_635,
				DiagnosisConcepts._DiagnosisConcepts.EYA_636,
				DiagnosisConcepts._DiagnosisConcepts.EYA_637,
				DiagnosisConcepts._DiagnosisConcepts.EYA_638,
				DiagnosisConcepts._DiagnosisConcepts.EYA_639,
				DiagnosisConcepts._DiagnosisConcepts.EYA_640,
				DiagnosisConcepts._DiagnosisConcepts.EYA_641,
				DiagnosisConcepts._DiagnosisConcepts.EYA_642,
				DiagnosisConcepts._DiagnosisConcepts.EYA_643,
				DiagnosisConcepts._DiagnosisConcepts.EYA_644,
				DiagnosisConcepts._DiagnosisConcepts.EYA_645,
				DiagnosisConcepts._DiagnosisConcepts.EYA_646,
				DiagnosisConcepts._DiagnosisConcepts.EYA_647,
				DiagnosisConcepts._DiagnosisConcepts.EYA_648,
				DiagnosisConcepts._DiagnosisConcepts.EYA_649,
				DiagnosisConcepts._DiagnosisConcepts.EYA_650,
				DiagnosisConcepts._DiagnosisConcepts.EYA_651,
				DiagnosisConcepts._DiagnosisConcepts.EYA_652,
				DiagnosisConcepts._DiagnosisConcepts.EYA_653,
				DiagnosisConcepts._DiagnosisConcepts.EYA_654,
				DiagnosisConcepts._DiagnosisConcepts.EYA_655,
				DiagnosisConcepts._DiagnosisConcepts.EYA_656,
				DiagnosisConcepts._DiagnosisConcepts.EYA_657,
				DiagnosisConcepts._DiagnosisConcepts.EYA_658,
				DiagnosisConcepts._DiagnosisConcepts.EYA_659,
				DiagnosisConcepts._DiagnosisConcepts.EYA_660,
				DiagnosisConcepts._DiagnosisConcepts.EYA_661
		);
	}


//Ear Infections

	public static List<Integer> getEarInfectionConditionsList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.EIA_1,
				DiagnosisConcepts._DiagnosisConcepts.EIA_2,
				DiagnosisConcepts._DiagnosisConcepts.EIA_3,
				DiagnosisConcepts._DiagnosisConcepts.EIA_4,
				DiagnosisConcepts._DiagnosisConcepts.EIA_5,
				DiagnosisConcepts._DiagnosisConcepts.EIA_6,
				DiagnosisConcepts._DiagnosisConcepts.EIA_7,
				DiagnosisConcepts._DiagnosisConcepts.EIA_8,
				DiagnosisConcepts._DiagnosisConcepts.EIA_9,
				DiagnosisConcepts._DiagnosisConcepts.EIA_10,
				DiagnosisConcepts._DiagnosisConcepts.EIA_11,
				DiagnosisConcepts._DiagnosisConcepts.EIA_12,
				DiagnosisConcepts._DiagnosisConcepts.EIA_13,
				DiagnosisConcepts._DiagnosisConcepts.EIA_14,
				DiagnosisConcepts._DiagnosisConcepts.EIA_15,
				DiagnosisConcepts._DiagnosisConcepts.EIA_16,
				DiagnosisConcepts._DiagnosisConcepts.EIA_17,
				DiagnosisConcepts._DiagnosisConcepts.EIA_18,
				DiagnosisConcepts._DiagnosisConcepts.EIA_19,
				DiagnosisConcepts._DiagnosisConcepts.EIA_20,
				DiagnosisConcepts._DiagnosisConcepts.EIA_21,
				DiagnosisConcepts._DiagnosisConcepts.EIA_22,
				DiagnosisConcepts._DiagnosisConcepts.EIA_23,
				DiagnosisConcepts._DiagnosisConcepts.EIA_24,
				DiagnosisConcepts._DiagnosisConcepts.EIA_25,
				DiagnosisConcepts._DiagnosisConcepts.EIA_26,
				DiagnosisConcepts._DiagnosisConcepts.EIA_27,
				DiagnosisConcepts._DiagnosisConcepts.EIA_28,
				DiagnosisConcepts._DiagnosisConcepts.EIA_29,
				DiagnosisConcepts._DiagnosisConcepts.EIA_30,
				DiagnosisConcepts._DiagnosisConcepts.EIA_31,
				DiagnosisConcepts._DiagnosisConcepts.EIA_32,
				DiagnosisConcepts._DiagnosisConcepts.EIA_33,
				DiagnosisConcepts._DiagnosisConcepts.EIA_34,
				DiagnosisConcepts._DiagnosisConcepts.EIA_35,
				DiagnosisConcepts._DiagnosisConcepts.EIA_36,
				DiagnosisConcepts._DiagnosisConcepts.EIA_37,
				DiagnosisConcepts._DiagnosisConcepts.EIA_38,
				DiagnosisConcepts._DiagnosisConcepts.EIA_39,
				DiagnosisConcepts._DiagnosisConcepts.EIA_40,
				DiagnosisConcepts._DiagnosisConcepts.EIA_41,
				DiagnosisConcepts._DiagnosisConcepts.EIA_42,
				DiagnosisConcepts._DiagnosisConcepts.EIA_43,
				DiagnosisConcepts._DiagnosisConcepts.EIA_44,
				DiagnosisConcepts._DiagnosisConcepts.EIA_45,
				DiagnosisConcepts._DiagnosisConcepts.EIA_46,
				DiagnosisConcepts._DiagnosisConcepts.EIA_47,
				DiagnosisConcepts._DiagnosisConcepts.EIA_48,
				DiagnosisConcepts._DiagnosisConcepts.EIA_49,
				DiagnosisConcepts._DiagnosisConcepts.EIA_50,
				DiagnosisConcepts._DiagnosisConcepts.EIA_51,
				DiagnosisConcepts._DiagnosisConcepts.EIA_52,
				DiagnosisConcepts._DiagnosisConcepts.EIA_53,
				DiagnosisConcepts._DiagnosisConcepts.EIA_54,
				DiagnosisConcepts._DiagnosisConcepts.EIA_55,
				DiagnosisConcepts._DiagnosisConcepts.EIA_56,
				DiagnosisConcepts._DiagnosisConcepts.EIA_57,
				DiagnosisConcepts._DiagnosisConcepts.EIA_58,
				DiagnosisConcepts._DiagnosisConcepts.EIA_59,
				DiagnosisConcepts._DiagnosisConcepts.EIA_60,
				DiagnosisConcepts._DiagnosisConcepts.EIA_61,
				DiagnosisConcepts._DiagnosisConcepts.EIA_62,
				DiagnosisConcepts._DiagnosisConcepts.EIA_63,
				DiagnosisConcepts._DiagnosisConcepts.EIA_64,
				DiagnosisConcepts._DiagnosisConcepts.EIA_65
		);
	}

	public static List<Integer> getPneumoniaList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.PNA_1,
				DiagnosisConcepts._DiagnosisConcepts.PNA_2,
				DiagnosisConcepts._DiagnosisConcepts.PNA_3,
				DiagnosisConcepts._DiagnosisConcepts.PNA_4,
				DiagnosisConcepts._DiagnosisConcepts.PNA_5,
				DiagnosisConcepts._DiagnosisConcepts.PNA_6,
				DiagnosisConcepts._DiagnosisConcepts.PNA_7,
				DiagnosisConcepts._DiagnosisConcepts.PNA_8,
				DiagnosisConcepts._DiagnosisConcepts.PNA_9,
				DiagnosisConcepts._DiagnosisConcepts.PNA_10,
				DiagnosisConcepts._DiagnosisConcepts.PNA_11,
				DiagnosisConcepts._DiagnosisConcepts.PNA_12,
				DiagnosisConcepts._DiagnosisConcepts.PNA_13,
				DiagnosisConcepts._DiagnosisConcepts.PNA_14,
				DiagnosisConcepts._DiagnosisConcepts.PNA_15,
				DiagnosisConcepts._DiagnosisConcepts.PNA_16,
				DiagnosisConcepts._DiagnosisConcepts.PNA_17,
				DiagnosisConcepts._DiagnosisConcepts.PNA_18,
				DiagnosisConcepts._DiagnosisConcepts.PNA_19,
				DiagnosisConcepts._DiagnosisConcepts.PNA_20,
				DiagnosisConcepts._DiagnosisConcepts.PNA_21,
				DiagnosisConcepts._DiagnosisConcepts.PNA_22,
				DiagnosisConcepts._DiagnosisConcepts.PNA_23,
				DiagnosisConcepts._DiagnosisConcepts.PNA_24,
				DiagnosisConcepts._DiagnosisConcepts.PNA_25,
				DiagnosisConcepts._DiagnosisConcepts.PNA_26
		);
	}
	// Method to get all skin disease conditions
	public static List<Integer> getDiseasesOfSkin() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.DS_1,
				DiagnosisConcepts._DiagnosisConcepts.DS_2,
				DiagnosisConcepts._DiagnosisConcepts.DS_3,
				DiagnosisConcepts._DiagnosisConcepts.DS_4,
				DiagnosisConcepts._DiagnosisConcepts.DS_5,
				DiagnosisConcepts._DiagnosisConcepts.DS_6,
				DiagnosisConcepts._DiagnosisConcepts.DS_7,
				DiagnosisConcepts._DiagnosisConcepts.DS_8,
				DiagnosisConcepts._DiagnosisConcepts.DS_9,
				DiagnosisConcepts._DiagnosisConcepts.DS_10,
				DiagnosisConcepts._DiagnosisConcepts.DS_11,
				DiagnosisConcepts._DiagnosisConcepts.DS_12,
				DiagnosisConcepts._DiagnosisConcepts.DS_13,
				DiagnosisConcepts._DiagnosisConcepts.DS_14,
				DiagnosisConcepts._DiagnosisConcepts.DS_15,
				DiagnosisConcepts._DiagnosisConcepts.DS_16,
				DiagnosisConcepts._DiagnosisConcepts.DS_17,
				DiagnosisConcepts._DiagnosisConcepts.DS_18,
				DiagnosisConcepts._DiagnosisConcepts.DS_19,
				DiagnosisConcepts._DiagnosisConcepts.DS_20,
				DiagnosisConcepts._DiagnosisConcepts.DS_21,
				DiagnosisConcepts._DiagnosisConcepts.DS_22,
				DiagnosisConcepts._DiagnosisConcepts.DS_23,
				DiagnosisConcepts._DiagnosisConcepts.DS_24,
				DiagnosisConcepts._DiagnosisConcepts.DS_25,
				DiagnosisConcepts._DiagnosisConcepts.DS_26,
				DiagnosisConcepts._DiagnosisConcepts.DS_27,
				DiagnosisConcepts._DiagnosisConcepts.DS_28,
				DiagnosisConcepts._DiagnosisConcepts.DS_29,
				DiagnosisConcepts._DiagnosisConcepts.DS_30,
				DiagnosisConcepts._DiagnosisConcepts.DS_31,
				DiagnosisConcepts._DiagnosisConcepts.DS_32,
				DiagnosisConcepts._DiagnosisConcepts.DS_33,
				DiagnosisConcepts._DiagnosisConcepts.DS_34,
				DiagnosisConcepts._DiagnosisConcepts.DS_35,
				DiagnosisConcepts._DiagnosisConcepts.DS_36,
				DiagnosisConcepts._DiagnosisConcepts.DS_37,
				DiagnosisConcepts._DiagnosisConcepts.DS_38,
				DiagnosisConcepts._DiagnosisConcepts.DS_39,
				DiagnosisConcepts._DiagnosisConcepts.DS_40,
				DiagnosisConcepts._DiagnosisConcepts.DS_41,
				DiagnosisConcepts._DiagnosisConcepts.DS_42,
				DiagnosisConcepts._DiagnosisConcepts.DS_43,
				DiagnosisConcepts._DiagnosisConcepts.DS_44,
				DiagnosisConcepts._DiagnosisConcepts.DS_45,
				DiagnosisConcepts._DiagnosisConcepts.DS_46,
				DiagnosisConcepts._DiagnosisConcepts.DS_47,
				DiagnosisConcepts._DiagnosisConcepts.DS_48,
				DiagnosisConcepts._DiagnosisConcepts.DS_49,
				DiagnosisConcepts._DiagnosisConcepts.DS_50,
				DiagnosisConcepts._DiagnosisConcepts.DS_51,
				DiagnosisConcepts._DiagnosisConcepts.DS_52,
				DiagnosisConcepts._DiagnosisConcepts.DS_53,
				DiagnosisConcepts._DiagnosisConcepts.DS_54,
				DiagnosisConcepts._DiagnosisConcepts.DS_55,
				DiagnosisConcepts._DiagnosisConcepts.DS_56,
				DiagnosisConcepts._DiagnosisConcepts.DS_57,
				DiagnosisConcepts._DiagnosisConcepts.DS_58,
				DiagnosisConcepts._DiagnosisConcepts.DS_59,
				DiagnosisConcepts._DiagnosisConcepts.DS_60,
				DiagnosisConcepts._DiagnosisConcepts.DS_61,
				DiagnosisConcepts._DiagnosisConcepts.DS_62,
				DiagnosisConcepts._DiagnosisConcepts.DS_63,
				DiagnosisConcepts._DiagnosisConcepts.DS_64,
				DiagnosisConcepts._DiagnosisConcepts.DS_65,
				DiagnosisConcepts._DiagnosisConcepts.DS_66,
				DiagnosisConcepts._DiagnosisConcepts.DS_67,
				DiagnosisConcepts._DiagnosisConcepts.DS_68,
				DiagnosisConcepts._DiagnosisConcepts.DS_69,
				DiagnosisConcepts._DiagnosisConcepts.DS_70,
				DiagnosisConcepts._DiagnosisConcepts.DS_71,
				DiagnosisConcepts._DiagnosisConcepts.DS_72,
				DiagnosisConcepts._DiagnosisConcepts.DS_73,
				DiagnosisConcepts._DiagnosisConcepts.DS_74,
				DiagnosisConcepts._DiagnosisConcepts.DS_75,
				DiagnosisConcepts._DiagnosisConcepts.DS_76,
				DiagnosisConcepts._DiagnosisConcepts.DS_77
		);
	}


	public static List<Integer> getOtherDisOfRespiratorySystemList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.OTRA_2,
				DiagnosisConcepts._DiagnosisConcepts.OTRA_3

		);
	}


	public static List<Integer> getAbortionList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.ABA_1,
				DiagnosisConcepts._DiagnosisConcepts.ABA_2,
				DiagnosisConcepts._DiagnosisConcepts.ABA_3,
				DiagnosisConcepts._DiagnosisConcepts.ABA_4,
				DiagnosisConcepts._DiagnosisConcepts.ABA_5,
				DiagnosisConcepts._DiagnosisConcepts.ABA_6,
				DiagnosisConcepts._DiagnosisConcepts.ABA_7,
				DiagnosisConcepts._DiagnosisConcepts.ABA_8,
				DiagnosisConcepts._DiagnosisConcepts.ABA_9,
				DiagnosisConcepts._DiagnosisConcepts.ABA_10,
				DiagnosisConcepts._DiagnosisConcepts.ABA_11,
				DiagnosisConcepts._DiagnosisConcepts.ABA_12,
				DiagnosisConcepts._DiagnosisConcepts.ABA_13,
				DiagnosisConcepts._DiagnosisConcepts.ABA_14,
				DiagnosisConcepts._DiagnosisConcepts.ABA_15,
				DiagnosisConcepts._DiagnosisConcepts.ABA_16,
				DiagnosisConcepts._DiagnosisConcepts.ABA_17,
				DiagnosisConcepts._DiagnosisConcepts.ABA_18,
				DiagnosisConcepts._DiagnosisConcepts.ABA_19,
				DiagnosisConcepts._DiagnosisConcepts.ABA_20,
				DiagnosisConcepts._DiagnosisConcepts.ABA_21,
				DiagnosisConcepts._DiagnosisConcepts.ABA_22,
				DiagnosisConcepts._DiagnosisConcepts.ABA_23,
				DiagnosisConcepts._DiagnosisConcepts.ABA_24,
				DiagnosisConcepts._DiagnosisConcepts.ABA_25,
				DiagnosisConcepts._DiagnosisConcepts.ABA_26,
				DiagnosisConcepts._DiagnosisConcepts.ABA_27,
				DiagnosisConcepts._DiagnosisConcepts.ABA_28,
				DiagnosisConcepts._DiagnosisConcepts.ABA_29,
				DiagnosisConcepts._DiagnosisConcepts.ABA_30,
				DiagnosisConcepts._DiagnosisConcepts.ABA_31,
				DiagnosisConcepts._DiagnosisConcepts.ABA_32,
				DiagnosisConcepts._DiagnosisConcepts.ABA_33,
				DiagnosisConcepts._DiagnosisConcepts.ABA_34,
				DiagnosisConcepts._DiagnosisConcepts.ABA_35,
				DiagnosisConcepts._DiagnosisConcepts.ABA_36,
				DiagnosisConcepts._DiagnosisConcepts.ABA_37,
				DiagnosisConcepts._DiagnosisConcepts.ABA_38,
				DiagnosisConcepts._DiagnosisConcepts.ABA_39,
				DiagnosisConcepts._DiagnosisConcepts.ABA_40,
				DiagnosisConcepts._DiagnosisConcepts.ABA_41,
				DiagnosisConcepts._DiagnosisConcepts.ABA_42,
				DiagnosisConcepts._DiagnosisConcepts.ABA_43,
				DiagnosisConcepts._DiagnosisConcepts.ABA_44,
				DiagnosisConcepts._DiagnosisConcepts.ABA_45,
				DiagnosisConcepts._DiagnosisConcepts.ABA_46,
				DiagnosisConcepts._DiagnosisConcepts.ABA_47,
				DiagnosisConcepts._DiagnosisConcepts.ABA_48,
				DiagnosisConcepts._DiagnosisConcepts.ABA_49,
				DiagnosisConcepts._DiagnosisConcepts.ABA_50,
				DiagnosisConcepts._DiagnosisConcepts.ABA_51,
				DiagnosisConcepts._DiagnosisConcepts.ABA_52,
				DiagnosisConcepts._DiagnosisConcepts.ABA_53,
				DiagnosisConcepts._DiagnosisConcepts.ABA_54,
				DiagnosisConcepts._DiagnosisConcepts.ABA_55,
				DiagnosisConcepts._DiagnosisConcepts.ABA_56,
				DiagnosisConcepts._DiagnosisConcepts.ABA_57,
				DiagnosisConcepts._DiagnosisConcepts.ABA_58,
				DiagnosisConcepts._DiagnosisConcepts.ABA_59,
				DiagnosisConcepts._DiagnosisConcepts.ABA_60,
				DiagnosisConcepts._DiagnosisConcepts.ABA_61,
				DiagnosisConcepts._DiagnosisConcepts.ABA_62,
				DiagnosisConcepts._DiagnosisConcepts.ABA_63,
				DiagnosisConcepts._DiagnosisConcepts.ABA_64,
				DiagnosisConcepts._DiagnosisConcepts.ABA_65,
				DiagnosisConcepts._DiagnosisConcepts.ABA_66,
				DiagnosisConcepts._DiagnosisConcepts.ABA_67

		);
	}

//Dis. of Puerperium & Childbirth


	public static List<Integer> getDisOfPuerperiumChildbathList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.DPA_1,
				DiagnosisConcepts._DiagnosisConcepts.DPA_2,
				DiagnosisConcepts._DiagnosisConcepts.DPA_3,
				DiagnosisConcepts._DiagnosisConcepts.DPA_4,
				DiagnosisConcepts._DiagnosisConcepts.DPA_5,
				DiagnosisConcepts._DiagnosisConcepts.DPA_6,
				DiagnosisConcepts._DiagnosisConcepts.DPA_7,
				DiagnosisConcepts._DiagnosisConcepts.DPA_8,
				DiagnosisConcepts._DiagnosisConcepts.DPA_9,
				DiagnosisConcepts._DiagnosisConcepts.DPA_10,
				DiagnosisConcepts._DiagnosisConcepts.DPA_11,
				DiagnosisConcepts._DiagnosisConcepts.DPA_12,
				DiagnosisConcepts._DiagnosisConcepts.DPA_13,
				DiagnosisConcepts._DiagnosisConcepts.DPA_14,
				DiagnosisConcepts._DiagnosisConcepts.DPA_15,
				DiagnosisConcepts._DiagnosisConcepts.DPA_16,
				DiagnosisConcepts._DiagnosisConcepts.DPA_17,
				DiagnosisConcepts._DiagnosisConcepts.DPA_18,
				DiagnosisConcepts._DiagnosisConcepts.DPA_19,
				DiagnosisConcepts._DiagnosisConcepts.DPA_20


		);
	}

	public static List<Integer> getHypertensionList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.HYA_1,
				DiagnosisConcepts._DiagnosisConcepts.HYA_2,
				DiagnosisConcepts._DiagnosisConcepts.HYA_3,
				DiagnosisConcepts._DiagnosisConcepts.HYA_4,
				DiagnosisConcepts._DiagnosisConcepts.HYA_5,
				DiagnosisConcepts._DiagnosisConcepts.HYA_6,
				DiagnosisConcepts._DiagnosisConcepts.HYA_7,
				DiagnosisConcepts._DiagnosisConcepts.HYA_8,
				DiagnosisConcepts._DiagnosisConcepts.HYA_9,
				DiagnosisConcepts._DiagnosisConcepts.HYA_10,
				DiagnosisConcepts._DiagnosisConcepts.HYA_11,
				DiagnosisConcepts._DiagnosisConcepts.HYA_12,
				DiagnosisConcepts._DiagnosisConcepts.HYA_13,
				DiagnosisConcepts._DiagnosisConcepts.HYA_14,
				DiagnosisConcepts._DiagnosisConcepts.HYA_15,
				DiagnosisConcepts._DiagnosisConcepts.HYA_16,
				DiagnosisConcepts._DiagnosisConcepts.HYA_17,
				DiagnosisConcepts._DiagnosisConcepts.HYA_18,
				DiagnosisConcepts._DiagnosisConcepts.HYA_19,
				DiagnosisConcepts._DiagnosisConcepts.HYA_20,
				DiagnosisConcepts._DiagnosisConcepts.HYA_21,
				DiagnosisConcepts._DiagnosisConcepts.HYA_22,
				DiagnosisConcepts._DiagnosisConcepts.HYA_23,
				DiagnosisConcepts._DiagnosisConcepts.HYA_24,
				DiagnosisConcepts._DiagnosisConcepts.HYA_25,
				DiagnosisConcepts._DiagnosisConcepts.HYA_26,
				DiagnosisConcepts._DiagnosisConcepts.HYA_27,
				DiagnosisConcepts._DiagnosisConcepts.HYA_28,
				DiagnosisConcepts._DiagnosisConcepts.HYA_29,
				DiagnosisConcepts._DiagnosisConcepts.HYA_30,
				DiagnosisConcepts._DiagnosisConcepts.HYA_31,
				DiagnosisConcepts._DiagnosisConcepts.HYA_32,
				DiagnosisConcepts._DiagnosisConcepts.HYA_33,
				DiagnosisConcepts._DiagnosisConcepts.HYA_34,
				DiagnosisConcepts._DiagnosisConcepts.HYA_35

		);
	}


	public static List<Integer> getAnthritisJointPainsList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.AJPA_1,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_2,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_3,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_4,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_5,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_6,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_7,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_8,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_9,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_10,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_11,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_12,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_13,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_14,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_15,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_16,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_17,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_18,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_19,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_20,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_21,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_22,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_23,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_24,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_25,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_26,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_27,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_28,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_29,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_30,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_31,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_32,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_33,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_34,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_35,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_36,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_37,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_38,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_39,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_40,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_41,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_42,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_43,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_44,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_45,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_46,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_47,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_48,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_49,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_50,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_51,
				DiagnosisConcepts._DiagnosisConcepts.AJPA_52
		);
	}

	public static List<Integer> getOtherCentralNervousSystemConditionsList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.CNSA_1,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_2,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_3,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_4,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_5,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_6,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_7,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_8,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_9,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_10,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_11,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_12,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_13,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_14,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_15,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_16,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_17,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_18,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_19,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_20,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_21,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_22,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_23,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_24,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_25,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_26,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_27,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_28,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_29,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_30,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_31,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_32,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_33,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_34,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_35,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_36,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_37,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_38,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_39,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_40,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_41,
				DiagnosisConcepts._DiagnosisConcepts.CNSA_42
		);
	}


	public static List<Integer> getOverweightList() {
		return Arrays.asList(
				DiagnosisConcepts._DiagnosisConcepts.OVA_1,
				DiagnosisConcepts._DiagnosisConcepts.OVA_2,
				DiagnosisConcepts._DiagnosisConcepts.OVA_3,
				DiagnosisConcepts._DiagnosisConcepts.OVA_4,
				DiagnosisConcepts._DiagnosisConcepts.OVA_5,
				DiagnosisConcepts._DiagnosisConcepts.OVA_6
		);
	}

	public static List<Integer> getPoisoningList() {
		return Arrays.asList( DiagnosisConcepts._DiagnosisConcepts.POA_1,
				DiagnosisConcepts._DiagnosisConcepts.POA_2,
				DiagnosisConcepts._DiagnosisConcepts.POA_3,
				DiagnosisConcepts._DiagnosisConcepts.POA_4,
				DiagnosisConcepts._DiagnosisConcepts.POA_5,
				DiagnosisConcepts._DiagnosisConcepts.POA_6,
				DiagnosisConcepts._DiagnosisConcepts.POA_7,
				DiagnosisConcepts._DiagnosisConcepts.POA_8,
				DiagnosisConcepts._DiagnosisConcepts.POA_9,
				DiagnosisConcepts._DiagnosisConcepts.POA_10

		);
	}


	public static List<Integer> getCardiovascularConditionsList() {
		return Arrays.asList( DiagnosisConcepts._DiagnosisConcepts.CAA_1,
				DiagnosisConcepts._DiagnosisConcepts.CAA_2,
				DiagnosisConcepts._DiagnosisConcepts.CAA_3,
				DiagnosisConcepts._DiagnosisConcepts.CAA_4

		);
	}

	public static List<Integer> getAsthmaList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.ASTHMA1, DiagnosisConcepts._DiagnosisConcepts.ASTHMA2,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA3, DiagnosisConcepts._DiagnosisConcepts.ASTHMA4,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA5, DiagnosisConcepts._DiagnosisConcepts.ASTHMA6,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA6, DiagnosisConcepts._DiagnosisConcepts.ASTHMA7,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA8, DiagnosisConcepts._DiagnosisConcepts.ASTHMA9,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA10, DiagnosisConcepts._DiagnosisConcepts.ASTHMA11,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA12, DiagnosisConcepts._DiagnosisConcepts.ASTHMA13,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA14, DiagnosisConcepts._DiagnosisConcepts.ASTHMA15,
				DiagnosisConcepts._DiagnosisConcepts.ASTHMA16, DiagnosisConcepts._DiagnosisConcepts.ASTHMA17

		);
	}

	public static List<Integer> getMuscularSkeletalConditionsList() {
		return Arrays.asList(MascularDiagnosisConstants.Muscular1, MascularDiagnosisConstants.Muscular2,
				MascularDiagnosisConstants.Muscular3, MascularDiagnosisConstants.Muscular4,
				MascularDiagnosisConstants.Muscular5, MascularDiagnosisConstants.Muscular6,
				MascularDiagnosisConstants.Muscular7, MascularDiagnosisConstants.Muscular8,
				MascularDiagnosisConstants.Muscular9, MascularDiagnosisConstants.Muscular10,
				MascularDiagnosisConstants.Muscular11, MascularDiagnosisConstants.Muscular12,
				MascularDiagnosisConstants.Muscular13, MascularDiagnosisConstants.Muscular14,
				MascularDiagnosisConstants.Muscular15, MascularDiagnosisConstants.Muscular16,
				MascularDiagnosisConstants.Muscular17, MascularDiagnosisConstants.Muscular18,
				MascularDiagnosisConstants.Muscular19, MascularDiagnosisConstants.Muscular20,
				MascularDiagnosisConstants.Muscular21, MascularDiagnosisConstants.Muscular22,
				MascularDiagnosisConstants.Muscular23, MascularDiagnosisConstants.Muscular24,
				MascularDiagnosisConstants.Muscular25, MascularDiagnosisConstants.Muscular26,
				MascularDiagnosisConstants.Muscular27, MascularDiagnosisConstants.Muscular28,
				MascularDiagnosisConstants.Muscular29, MascularDiagnosisConstants.Muscular30,
				MascularDiagnosisConstants.Muscular31, MascularDiagnosisConstants.Muscular32,
				MascularDiagnosisConstants.Muscular33, MascularDiagnosisConstants.Muscular34,
				MascularDiagnosisConstants.Muscular35, MascularDiagnosisConstants.Muscular36,
				MascularDiagnosisConstants.Muscular37, MascularDiagnosisConstants.Muscular38,
				MascularDiagnosisConstants.Muscular39, MascularDiagnosisConstants.Muscular40,
				MascularDiagnosisConstants.Muscular41, MascularDiagnosisConstants.Muscular42,
				MascularDiagnosisConstants.Muscular43, MascularDiagnosisConstants.Muscular44,
				MascularDiagnosisConstants.Muscular45, MascularDiagnosisConstants.Muscular46,
				MascularDiagnosisConstants.Muscular47, MascularDiagnosisConstants.Muscular48,
				MascularDiagnosisConstants.Muscular49, MascularDiagnosisConstants.Muscular50,
				MascularDiagnosisConstants.Muscular51, MascularDiagnosisConstants.Muscular52

		);
	}

	public static List<Integer> getFistulaBirthRelatedList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.FISTULA1, DiagnosisConcepts._DiagnosisConcepts.FISTULA2,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA3, DiagnosisConcepts._DiagnosisConcepts.FISTULA4,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA5, DiagnosisConcepts._DiagnosisConcepts.FISTULA6,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA7, DiagnosisConcepts._DiagnosisConcepts.FISTULA8,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA9, DiagnosisConcepts._DiagnosisConcepts.FISTULA10,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA11, DiagnosisConcepts._DiagnosisConcepts.FISTULA12,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA13, DiagnosisConcepts._DiagnosisConcepts.FISTULA14,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA15, DiagnosisConcepts._DiagnosisConcepts.FISTULA16,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA17, DiagnosisConcepts._DiagnosisConcepts.FISTULA18,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA19, DiagnosisConcepts._DiagnosisConcepts.FISTULA20,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA21, DiagnosisConcepts._DiagnosisConcepts.FISTULA22,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA23, DiagnosisConcepts._DiagnosisConcepts.FISTULA24,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA25, DiagnosisConcepts._DiagnosisConcepts.FISTULA26,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA27, DiagnosisConcepts._DiagnosisConcepts.FISTULA28,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA29, DiagnosisConcepts._DiagnosisConcepts.FISTULA30,
				DiagnosisConcepts._DiagnosisConcepts.FISTULA31

		);
	}

	public static List<Integer> getNeoplamsList() {
		return Arrays.stream(OncologyDiagnosisConstants.class.getDeclaredFields())
				.filter(f -> f.getName().startsWith("CANCER_"))
				.filter(f -> f.getType().equals(Integer.class))
				.map(f -> {
					try {
						return (Integer) f.get(null);
					} catch (IllegalAccessException e) {
						throw new RuntimeException("Failed to access field: " + f.getName(), e);
					}
				})
				.collect(Collectors.toList());
	}

	public static List<Integer> getViralHaemorrhagicFeverList() {
		return Arrays.asList(DiagnosisConcepts._DiagnosisConcepts.HAEMORRHAGICFEVER_1,
				DiagnosisConcepts._DiagnosisConcepts.HAEMORRHAGICFEVER_2);
	}

	//Reported diseases not in any list but standing as singulars for under 5 years old. We create a list for them
	public static ArrayList<Integer> scalarDiseasesListA = new ArrayList<>(Arrays.asList(2007117,2014248,2014827,2002391,1226,134369,226,124954,
			166567,2009210,144481,2027745,121005,2002647,143074,121555,2006141,2002652,114100,121375,149609,2004984));

	//Reported diseases not in any list but standing as singulars for 5+ years old. We create a list for them
	public static ArrayList<Integer> scalarDiseasesListB = new ArrayList<>(Arrays.asList(134369,166567,121005,2014582,2002647,143074,121555));

	public static List<Integer> getAllOtherUnder5DiseasesList() {
		List<Integer> allListedDiagnosisForUnder5 = new ArrayList<>();
		allListedDiagnosisForUnder5.addAll(getDiarrheaWithSomeDehydrationDiagnosisList());
		allListedDiagnosisForUnder5.addAll(getDiarrheaWithSevereDehydrationDiagnosisList());
		allListedDiagnosisForUnder5.addAll(getPneumoniaList());
		allListedDiagnosisForUnder5.addAll(getGastroenteritisList());
		allListedDiagnosisForUnder5.addAll(getSeverePneumoniaList());
		allListedDiagnosisForUnder5.addAll(getUpperRespiratoryTractInfectionsList());
		allListedDiagnosisForUnder5.addAll(getOtherDisOfRespiratorySystemList());
		allListedDiagnosisForUnder5.addAll(getPresumedTuberculosisList());
		allListedDiagnosisForUnder5.addAll(getAsthmaList());
		allListedDiagnosisForUnder5.addAll(getConfirmedMalariaList());
		allListedDiagnosisForUnder5.addAll(getMalnutritionList());
		allListedDiagnosisForUnder5.addAll(getOtherMenigitisList());
		allListedDiagnosisForUnder5.addAll(getPoliomyelitisList());
		allListedDiagnosisForUnder5.addAll(getChickenPoxList());
		allListedDiagnosisForUnder5.addAll(getMeaslesListA());
		allListedDiagnosisForUnder5.addAll(getHepatitisListA());
		allListedDiagnosisForUnder5.addAll(getAmoebiasis());
		allListedDiagnosisForUnder5.addAll(getMumpsListA());
		allListedDiagnosisForUnder5.addAll(getTyphoidList());
		allListedDiagnosisForUnder5.addAll(getBilharziaList());
		allListedDiagnosisForUnder5.addAll(getInterstinalwormsListA());
		allListedDiagnosisForUnder5.addAll(getEyeInfectionsList());
		allListedDiagnosisForUnder5.addAll(getEarInfectionConditionsList());
		allListedDiagnosisForUnder5.addAll(getTonsilitiesList());
		allListedDiagnosisForUnder5.addAll(getPoliomyelitisList());
		allListedDiagnosisForUnder5.addAll(getChickenPoxList());
		allListedDiagnosisForUnder5.addAll(getBilharziaList());
		allListedDiagnosisForUnder5.addAll(getTonsilitiesList());
		allListedDiagnosisForUnder5.addAll(getUrinaryTractInfectionListA());
		allListedDiagnosisForUnder5.addAll(getMentalDisordersList());
		allListedDiagnosisForUnder5.addAll(getDentalDisordersList());
		allListedDiagnosisForUnder5.addAll(getViolenceRelatedInjuriesList());
		allListedDiagnosisForUnder5.addAll(getOtherInjuriesList());
		allListedDiagnosisForUnder5.addAll(getSexualAssaultList());
		allListedDiagnosisForUnder5.addAll(getBurnsList());
		allListedDiagnosisForUnder5.addAll(getSnakeBitesList());
		allListedDiagnosisForUnder5.addAll(getDogBitesList());
		allListedDiagnosisForUnder5.addAll(getOtherBitesList());
		allListedDiagnosisForUnder5.addAll(getDiabetesList());
		allListedDiagnosisForUnder5.addAll(getEpilepsyList());
		allListedDiagnosisForUnder5.addAll(getOtherConvulsiveDisordersList());
		allListedDiagnosisForUnder5.addAll(getReumonicFeverList());
		allListedDiagnosisForUnder5.addAll(getRicketsList());
		allListedDiagnosisForUnder5.addAll(getCerebralPalsyList());
		allListedDiagnosisForUnder5.addAll(getAutismList());
		allListedDiagnosisForUnder5.addAll(getTryponomiasisList());
		allListedDiagnosisForUnder5.addAll(getYellowFeverList());
		allListedDiagnosisForUnder5.addAll(getRiftValleyFeverList());
		allListedDiagnosisForUnder5.addAll(getChikungunyaFeverList());
		allListedDiagnosisForUnder5.addAll(getDengueFeverList());
		allListedDiagnosisForUnder5.addAll(getKalazarLeishmaniasisList());
		allListedDiagnosisForUnder5.addAll(getChildHoodCancerist());
		allListedDiagnosisForUnder5.addAll(getTuberculosisDiagnosisList());
		allListedDiagnosisForUnder5.addAll(getOverweightList());
		allListedDiagnosisForUnder5.addAll(getRoadTrafficInjuriesList());
		allListedDiagnosisForUnder5.addAll(scalarDiseasesListA);
		return allListedDiagnosisForUnder5;
	}

	public static List<Integer> getAllOtherAbove5DiseasesList() {
		List<Integer> allListedDiagnosisForAbove5 = new ArrayList<>();
		allListedDiagnosisForAbove5.addAll(getDiarrheaDiagnosisList());
		allListedDiagnosisForAbove5.addAll(getTuberculosisDiagnosisList());
		allListedDiagnosisForAbove5.addAll(getCholeraList());
		allListedDiagnosisForAbove5.addAll(getDysenteryList());
		allListedDiagnosisForAbove5.addAll(getOtherMenigitisListB());
		allListedDiagnosisForAbove5.addAll(getPoliomyelitisListB());
		allListedDiagnosisForAbove5.addAll(getChickenPoxListB());
		allListedDiagnosisForAbove5.addAll(getMeaslesListB());
		allListedDiagnosisForAbove5.addAll(getHepatitisListB());
		allListedDiagnosisForAbove5.addAll(getMumpsListB());
		allListedDiagnosisForAbove5.addAll(getUrinaryTractInfectionListB());
		allListedDiagnosisForAbove5.addAll(getInterstinalwormsListB());
		allListedDiagnosisForAbove5.addAll(getMalnutritionList());
		allListedDiagnosisForAbove5.addAll(getAnaemiaListB());
		allListedDiagnosisForAbove5.addAll(getEyeInfectionsList());
		allListedDiagnosisForAbove5.addAll(getEarInfectionConditionsList());
		allListedDiagnosisForAbove5.addAll(getUpperRespiratoryTractInfectionsList());
		allListedDiagnosisForAbove5.addAll(getAsthmaList());
		allListedDiagnosisForAbove5.addAll(getPneumoniaList());
		allListedDiagnosisForAbove5.addAll(getDiseasesOfSkin());
		allListedDiagnosisForAbove5.addAll(getOtherDisOfRespiratorySystemList());
		allListedDiagnosisForAbove5.addAll(getMentalDisordersList());
		allListedDiagnosisForAbove5.addAll(getDentalDisordersList());
		allListedDiagnosisForAbove5.addAll(getTetanusList());
		allListedDiagnosisForAbove5.addAll(getConfirmedMalariaList());
		allListedDiagnosisForAbove5.addAll(getTyphoidList());
		allListedDiagnosisForAbove5.addAll(getAmoebiasis());
		allListedDiagnosisForAbove5.addAll(getSexuallyTransmittedInfectionsList());
		allListedDiagnosisForAbove5.addAll(getBilharziaList());
		allListedDiagnosisForAbove5.addAll(getAbortionList());
		allListedDiagnosisForAbove5.addAll(getDisOfPuerperiumChildbathList());
		allListedDiagnosisForAbove5.addAll(getHypertensionList());
		allListedDiagnosisForAbove5.addAll(getAnthritisJointPainsList());
		allListedDiagnosisForAbove5.addAll(getPoisoningList());
		allListedDiagnosisForAbove5.addAll(getRoadTrafficInjuriesList());
		allListedDiagnosisForAbove5.addAll(getOtherInjuriesList());
		allListedDiagnosisForAbove5.addAll(getViolenceRelatedInjuriesList());
		allListedDiagnosisForAbove5.addAll(getSexualAssaultList());
		allListedDiagnosisForAbove5.addAll(getBurnsList());
		allListedDiagnosisForAbove5.addAll(getSnakeBitesList());
		allListedDiagnosisForAbove5.addAll(getDogBitesList());
		allListedDiagnosisForAbove5.addAll(getOtherBitesList());
		allListedDiagnosisForAbove5.addAll(getDiabetesList());
		allListedDiagnosisForAbove5.addAll(getEpilepsyList());
		allListedDiagnosisForAbove5.addAll(getCardiovascularConditionsList());
		allListedDiagnosisForAbove5.addAll(getOtherCentralNervousSystemConditionsList());
		allListedDiagnosisForAbove5.addAll(getOverweightList());
		allListedDiagnosisForAbove5.addAll(getTryponomiasisList());
		allListedDiagnosisForAbove5.addAll(getYellowFeverList());
		allListedDiagnosisForAbove5.addAll(getViralHaemorrhagicFeverList());
		allListedDiagnosisForAbove5.addAll(getRiftValleyFeverList());
		allListedDiagnosisForAbove5.addAll(getChikungunyaFeverList());
		allListedDiagnosisForAbove5.addAll(getDengueFeverList());
		allListedDiagnosisForAbove5.addAll(getKalazarLeishmaniasisList());
		allListedDiagnosisForAbove5.addAll(getMuscularSkeletalConditionsList());
		allListedDiagnosisForAbove5.addAll(getFistulaBirthRelatedList());
		allListedDiagnosisForAbove5.addAll(getNeoplamsList());
		allListedDiagnosisForAbove5.addAll(scalarDiseasesListB);

		return allListedDiagnosisForAbove5;
	}
}
