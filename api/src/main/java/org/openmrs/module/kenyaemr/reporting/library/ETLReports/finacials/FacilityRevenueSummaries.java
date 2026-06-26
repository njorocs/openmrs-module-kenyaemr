/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.ETLReports.finacials;

public class FacilityRevenueSummaries {
    public static final String getMonthlySummaryQueryM1() {
        return
			"SELECT transaction_date,\n" +
				"total_sales,\n" +
				"op_total,\n" +
				"op_cash,\n" +
				"op_mobile_money,\n" +
				"op_insurance,\n" +
				"op_waiver,\n" +
				"lab_total,\n" +
				"lab_cash,\n" +
				"lab_mobile_money,\n" +
				"lab_insurance,\n" +
				"lab_waiver,\n" +
				"pharmacy_total,\n" +
				"pharmacy_cash,\n" +
				"pharmacy_mobile_money,\n" +
				"pharmacy_insurance,\n" +
				"pharmacy_waiver,\n" +
				"procedure_total,\n" +
				"procedure_cash,\n" +
				"procedure_mobile_money,\n" +
				"procedure_insurance,\n" +
				"procedure_waiver,\n" +
				"dental_total,\n" +
				"dental_cash,\n" +
				"dental_mobile_money,\n" +
				"dental_insurance,\n" +
				"dental_waiver,\n" +
				"ent_total,\n" +
				"ent_cash,\n" +
				"ent_mobile_money,\n" +
				"ent_insurance,\n" +
				"ent_waiver,\n" +
				"mat_total,\n" +
				"mat_cash,\n" +
				"mat_mobile_money,\n" +
				"mat_insurance,\n" +
				"mat_waiver,\n" +
				"ipd_total,\n" +
				"ipd_cash,\n" +
				"ipd_mobile_money,\n" +
				"ipd_insurance,\n" +
				"ipd_waiver,\n" +
				"physio_total,\n" +
				"physio_cash,\n" +
				"physio_mobile_money,\n" +
				"physio_insurance,\n" +
				"physio_waiver,\n" +
				"theatre_total,\n" +
				"theatre_cash,\n" +
				"theatre_mobile_money,\n" +
				"theatre_insurance,\n" +
				"theatre_waiver,\n" +
				"amb_total,\n" +
				"amb_cash,\n" +
				"amb_mobile_money,\n" +
				"amb_insurance,\n" +
				"amb_waiver,\n" +
				"occ_total,\n" +
				"occ_cash,\n" +
				"occ_mobile_money,\n" +
				"occ_insurance,\n" +
				"occ_waiver,\n" +
				"medExam_total,\n" +
				"medExam_cash,\n" +
				"medExam_mobile_money,\n" +
				"medExam_insurance,\n" +
				"medExam_waiver,\n" +
				"medRecords_total,\n" +
				"medRecords_cash,\n" +
				"medRecords_mobile_money,\n" +
				"medRecords_insurance,\n" +
				"medRecords_waiver,\n" +
				"mortuary_total,\n" +
				"mortuary_cash,\n" +
				"mortuary_mobile_money,\n" +
				"mortuary_insurance,\n" +
				"mortuary_waiver,\n" +
				"other_total,\n" +
				"other_cash,\n" +
				"other_mobile_money,\n" +
				"other_insurance,\n" +
				"other_waiver,\n" +
				"cash_receipts_cash,\n" +
				"cash_receipts_mobile_money,\n" +
				"cash_receipts_insurance,\n" +
				"cash_receipts_waiver,\n" +
				"cash_receipt_other_debtors_receipt,\n" +
				"revenue_not_collected_patient_not_yet_paid_sha_patients,\n" +
				"revenue_not_collected_patient_not_yet_paid_other_debtors,\n" +
				"revenue_not_collected_patient_not_yet_paid_waivers,\n" +
				"revenue_not_collected_write_offs_exemptions,\n" +
				"revenue_not_collected_write_offs_absconders\n" +
				"   FROM kenyaemr_etl.etl_daily_revenue_summary  dr WHERE dr.transaction_date BETWEEN :startDate AND :endDate;";
    }
}
