/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.builder.malaria;

import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({ "kenyaemr.ehrReports.report.moh643" })
public class Moh643ReportBuilder extends AbstractReportBuilder {

	static final int mRDT = 161465;

	String indParams = "startDate=${startDate},endDate=${endDate}";

	@Override
	protected List<Parameter> getParameters(ReportDescriptor reportDescriptor) {
		return Arrays.asList(
				new Parameter("startDate", "Start Date", Date.class),
				new Parameter("endDate", "End Date", Date.class),
				new Parameter("dateBasedReporting", "", String.class)
		);
	}
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor reportDescriptor,
	                                                        ReportDefinition reportDefinition) {
		return Arrays.asList(
				ReportUtils.map(getDataSetDefinition("Malaria Rapid Diagnostic Test (mRDT)", mRDT), "startDate=${startDate},endDate=${endDate}")
		);
	}

	private DataSetDefinition getDataSetDefinition(String datasetName, int commodityConceptId) {
		SqlDataSetDefinition sqlDataSetDefinition = new SqlDataSetDefinition();
		sqlDataSetDefinition.setName(datasetName);
		sqlDataSetDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		sqlDataSetDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		sqlDataSetDefinition.setSqlQuery(getFCDRRForLabCommodities(commodityConceptId));
		return sqlDataSetDefinition;
	}

	private String getFCDRRForLabCommodities(int commodityConceptId) {
		String query = "WITH RECURSIVE\n" +
				"-- calendar of every day in the period (for days-out-of-stock)\n" +
				"cal AS (\n" +
				"    SELECT CAST(:startDate AS DATE) AS d\n" +
				"    UNION ALL\n" +
				"    SELECT d + INTERVAL 1 DAY FROM cal WHERE d + INTERVAL 1 DAY <= :endDate\n" +
				"),\n" +
				"-- single normalised pass over the ledger for this concept\n" +
				"led AS (\n" +
				"    SELECT\n" +
				"        t.stock_item_packaging_uom_id                      AS uom_id,\n" +
				"        t.quantity                                         AS qty,\n" +
				"        t.stock_batch_id,\n" +
				"        DATE(COALESCE(op.operation_date, t.date_created))  AS tx_date,\n" +
				"        ot.operation_type,\n" +
				"        t.patient_id,\n" +
				"        t.order_id,\n" +
				"        ss.name                                            AS source_name\n" +
				"    FROM stockmgmt_stock_item_transaction t\n" +
				"    JOIN stockmgmt_stock_item i\n" +
				"         ON i.stock_item_id = t.stock_item_id AND i.concept_id = "+commodityConceptId+" AND i.voided = 0\n" +
				"    LEFT JOIN stockmgmt_stock_operation op\n" +
				"         ON op.stock_operation_id = t.stock_operation_id AND op.voided = 0\n" +
				"    LEFT JOIN stockmgmt_stock_operation_type ot\n" +
				"         ON ot.stock_operation_type_id = op.operation_type_id\n" +
				"    LEFT JOIN stockmgmt_party pty       ON pty.party_id = op.source_id\n" +
				"    LEFT JOIN stockmgmt_stock_source ss ON ss.stock_source_id = pty.stock_source_id\n" +
				"    WHERE t.stock_operation_id IS NULL OR op.stock_operation_id IS NOT NULL\n" +
				"),\n" +
				"-- lines 1b,1c,2-8 per UOM\n" +
				"agg AS (\n" +
				"    SELECT\n" +
				"        uom_id,\n" +
				"        ROUND(SUM(CASE WHEN tx_date < :startDate THEN qty END), 2)                       AS beginning_balance,\n" +
				"        ROUND(SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                        AND operation_type = 'initial' THEN qty END), 2)                 AS opening_stock_booked,\n" +
				"        ROUND(SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                        AND operation_type = 'receipt'\n" +
				"                        AND source_name IN ('KEMSA','Kenya Medical Supplies Authority')\n" +
				"                       THEN qty END), 2)                                                 AS recd_central_kemsa,\n" +
				"        ROUND(SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                        AND operation_type = 'receipt'\n" +
				"                        AND (source_name IS NULL\n" +
				"                             OR source_name NOT IN ('KEMSA','Kenya Medical Supplies Authority'))\n" +
				"                       THEN qty END), 2)                                                 AS recd_other_sources,\n" +
				"        ROUND(-SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                         AND (patient_id IS NOT NULL OR order_id IS NOT NULL)\n" +
				"                        THEN qty END), 2)                                                AS quantity_used,\n" +
				"        ROUND(-SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                         AND (patient_id IS NOT NULL OR order_id IS NOT NULL)\n" +
				"                        THEN qty END), 2)                                                AS tests_done_proxy,\n" +
				"        ROUND(-SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                         AND operation_type IN ('loss','disposed') THEN qty END), 2)     AS losses_wastage,\n" +
				"        ROUND(SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                        AND operation_type IN ('adjustment','stocktake')\n" +
				"                        AND qty > 0 THEN qty END), 2)                                    AS adjustments_positive,\n" +
				"        ROUND(-SUM(CASE WHEN tx_date BETWEEN :startDate AND :endDate\n" +
				"                         AND operation_type IN ('adjustment','stocktake')\n" +
				"                         AND qty < 0 THEN qty END), 2)                                   AS adjustments_negative,\n" +
				"        ROUND(SUM(CASE WHEN tx_date <= :endDate THEN qty END), 2)                        AS closing_balance\n" +
				"    FROM led\n" +
				"    GROUP BY uom_id\n" +
				"),\n" +
				"-- line 9: on-hand at :endDate in batches expiring within 6 months, summed per UOM\n" +
				"expiring AS (\n" +
				"    SELECT uom_id, ROUND(SUM(batch_qty), 2) AS qty_expiring_lt_6m\n" +
				"    FROM (\n" +
				"        SELECT l.uom_id, l.stock_batch_id, SUM(l.qty) AS batch_qty\n" +
				"        FROM led l\n" +
				"        JOIN stockmgmt_stock_batch b ON b.stock_batch_id = l.stock_batch_id AND b.voided = 0\n" +
				"        WHERE l.tx_date <= :endDate\n" +
				"          AND b.expiration >  :endDate\n" +
				"          AND b.expiration <= DATE_ADD(:endDate, INTERVAL 6 MONTH)\n" +
				"        GROUP BY l.uom_id, l.stock_batch_id\n" +
				"        HAVING SUM(l.qty) > 0\n" +
				"    ) bx\n" +
				"    GROUP BY uom_id\n" +
				"),\n" +
				"-- line 10: days the per-UOM running balance was <= 0\n" +
				"oos AS (\n" +
				"    SELECT u.uom_id, COUNT(*) AS days_out_of_stock\n" +
				"    FROM (SELECT DISTINCT uom_id FROM led) u\n" +
				"    CROSS JOIN cal\n" +
				"    WHERE (SELECT COALESCE(SUM(qty), 0)\n" +
				"           FROM led WHERE led.uom_id = u.uom_id AND led.tx_date <= cal.d) <= 0\n" +
				"    GROUP BY u.uom_id\n" +
				"),\n" +
				"-- line 11: quantity requested for re-supply per requested-UOM\n" +
				"req AS (\n" +
				"    SELECT COALESCE(oi.qty_req_packaging_uom_id, oi.stock_item_packaging_uom_id) AS uom_id,\n" +
				"           ROUND(SUM(COALESCE(oi.quantity_requested, oi.quantity)), 2)           AS qty_requested\n" +
				"    FROM stockmgmt_stock_operation o\n" +
				"    JOIN stockmgmt_stock_operation_type ot ON ot.stock_operation_type_id = o.operation_type_id\n" +
				"    JOIN stockmgmt_stock_operation_item oi ON oi.stock_operation_id = o.stock_operation_id AND oi.voided = 0\n" +
				"    JOIN stockmgmt_stock_item i ON i.stock_item_id = oi.stock_item_id AND i.concept_id = "+commodityConceptId+" AND i.voided = 0\n" +
				"    WHERE o.voided = 0\n" +
				"      AND ot.operation_type IN ('requisition','externalrequisition')\n" +
				"      AND DATE(o.operation_date) BETWEEN :startDate AND :endDate\n" +
				"    GROUP BY uom_id\n" +
				"),\n" +
				"-- all UOMs that appear anywhere (ledger or requisitions)\n" +
				"uoms AS (\n" +
				"    SELECT uom_id FROM agg\n" +
				"    UNION\n" +
				"    SELECT uom_id FROM req\n" +
				")\n" +
				"SELECT\n" +
				"    -- Unit of Issue (dispensing unit of the commodity)\n" +
				"    ucn.name                                         AS uom,\n" +
				"    CAST(COALESCE(a.beginning_balance,    0) AS SIGNED) AS beginning_balance,     -- 1\n" +
				"    CAST(COALESCE(a.opening_stock_booked, 0) AS SIGNED) AS opening_stock_booked,  -- 1c\n" +
				"    CAST(COALESCE(a.recd_central_kemsa,   0) AS SIGNED) AS recd_central_kemsa,    -- 2\n" +
				"    CAST(COALESCE(a.recd_other_sources,   0) AS SIGNED) AS recd_other_sources,    -- 3\n" +
				"    CAST(COALESCE(a.quantity_used,        0) AS SIGNED) AS quantity_used,         -- 4\n" +
				"    CAST(COALESCE(a.tests_done_proxy,     0) AS SIGNED) AS tests_done_proxy,      -- 5 (proxy; true counts live in lab obs)\n" +
				"    CAST(COALESCE(a.losses_wastage,       0) AS SIGNED) AS losses_wastage,        -- 6\n" +
				"    CAST(COALESCE(a.adjustments_positive, 0) AS SIGNED) AS adjustments_positive,  -- 7 (+)\n" +
				"    CAST(COALESCE(a.adjustments_negative, 0) AS SIGNED) AS adjustments_negative,  -- 7 (-)\n" +
				"    CAST(COALESCE(a.closing_balance,      0) AS SIGNED) AS closing_balance,       -- 8\n" +
				"    CAST(COALESCE(e.qty_expiring_lt_6m,   0) AS SIGNED) AS qty_expiring_lt_6m,    -- 9\n" +
				"    CAST(COALESCE(o.days_out_of_stock,    0) AS SIGNED) AS days_out_of_stock,     -- 10\n" +
				"    CAST(COALESCE(r.qty_requested,        0) AS SIGNED) AS qty_requested          -- 11\n" +
				"FROM uoms uu\n" +
				"LEFT JOIN agg      a ON a.uom_id = uu.uom_id\n" +
				"LEFT JOIN expiring e ON e.uom_id = uu.uom_id\n" +
				"LEFT JOIN oos      o ON o.uom_id = uu.uom_id\n" +
				"LEFT JOIN req      r ON r.uom_id = uu.uom_id\n" +
				"LEFT JOIN stockmgmt_stock_item_packaging_uom pu ON pu.stock_item_packaging_uom_id = uu.uom_id\n" +
				"LEFT JOIN concept_name ucn\n" +
				"       ON ucn.concept_id = pu.packaging_uom_id\n" +
				"          AND ucn.concept_name_type = 'FULLY_SPECIFIED' AND ucn.locale = 'en' AND ucn.voided = 0\n" +
				"ORDER BY uu.uom_id;";

		return query;
	}
}

