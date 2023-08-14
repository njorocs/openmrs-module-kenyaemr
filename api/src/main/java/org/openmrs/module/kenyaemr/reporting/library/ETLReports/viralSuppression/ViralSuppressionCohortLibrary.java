/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.reporting.library.ETLReports.viralSuppression;

import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyaemr.reporting.library.ETLReports.RevisedDatim.DatimCohortLibrary;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * Library of cohort definitions for viral suppression
 */
@Component
public class ViralSuppressionCohortLibrary {

    @Autowired
    private DatimCohortLibrary datimCohorts;
    public CohortDefinition suppressedVl(){
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery = "select a.patient_id from (select\n" +
                "       b.patient_id,\n" +
                "       max(b.visit_date) as vl_date,\n" +
                "       date_add(date_sub(date(:endDate), interval 12 MONTH), INTERVAL 1 DAY),\n" +
                "       mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n" +
                "       if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n" +
                "       mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n" +
                "from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n" +
                "      from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n" +
                "      group by x.patient_id,x.visit_date order by visit_date desc)b\n" +
                "group by patient_id\n" +
                "having max(visit_date) between\n" +
                "           date_add(date_sub(date(:endDate), interval 12 MONTH), INTERVAL 1 DAY) and date(:endDate) and (vl_result < 200 or vl_result='LDL'))a;";
        cd.setName("suppressed");
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setQuery(sqlQuery);
        cd.setDescription("Suppressed");

        return cd;
    }
    public CohortDefinition suppressed() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("txcurr", ReportUtils.map(datimCohorts.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("suppressedVl", ReportUtils.map(suppressedVl(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("txcurr AND suppressedVl");
        return cd;
    }
    public  CohortDefinition unsuppressedVl() {
        SqlCohortDefinition cd = new SqlCohortDefinition();
        String sqlQuery="select a.patient_id from (select\n" +
                "       b.patient_id,\n" +
                "       max(b.visit_date) as vl_date,\n" +
                "       date_add(date_sub(date(:endDate), interval 12 MONTH), INTERVAL 1 DAY),\n" +
                "       mid(max(concat(b.visit_date,b.lab_test)),11) as lab_test,\n" +
                "       if(mid(max(concat(b.visit_date,b.lab_test)),11) = 856, mid(max(concat(b.visit_date,b.test_result)),11), if(mid(max(concat(b.visit_date,b.lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302, \"LDL\",\"\")) as vl_result,\n" +
                "       mid(max(concat(b.visit_date,b.urgency)),11) as urgency\n" +
                "from (select x.patient_id as patient_id,x.visit_date as visit_date,x.lab_test as lab_test, x.test_result as test_result,urgency as urgency\n" +
                "      from kenyaemr_etl.etl_laboratory_extract x where x.lab_test in (1305,856)\n" +
                "      group by x.patient_id,x.visit_date order by visit_date desc)b\n" +
                "group by patient_id\n" +
                "having max(visit_date) between\n" +
                "date_add(date_sub(date(:endDate), interval 12 MONTH), INTERVAL 1 DAY) and date(:endDate) and vl_result >= 200)a;" ;

        cd.setName("unsuppressed");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("Unsuppressed");

        return cd;
    }
    public CohortDefinition unsuppressed() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("txcurr", ReportUtils.map(datimCohorts.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("unsuppressedVl", ReportUtils.map(unsuppressedVl(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("txcurr AND unsuppressedVl");
        return cd;
    }
    public  CohortDefinition noCurrentVL() {
        String sqlQuery="    select\n" +
                "                 patient_id,encounter_id,\n" +
                "                 max(visit_date) as vl_date,\n" +
                "                     if(mid(max(concat(visit_date,lab_test)),11) = 856, mid(max(concat(visit_date,test_result)),11), if(mid(max(concat(visit_date,lab_test)),11)=1305 and mid(max(concat(visit_date,test_result)),11) = 1302,\n" +
                "\"LDL\",\"\")) as vl_result,\n" +
                "                 mid(max(concat(visit_date,urgency)),11) as urgency\n" +
                "            from kenyaemr_etl.etl_laboratory_extract\n" +
                "            group by patient_id\n" +
                "            having mid(max(concat(visit_date,lab_test)),11) in (1305,856) and max(visit_date) < date_add(date_sub(date(:endDate), interval 12 MONTH), INTERVAL 1 DAY)\n" +
                "            ;";
        SqlCohortDefinition cd = new SqlCohortDefinition();
        cd.setName("noCurrentVLResults");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("No current VL Results");
        return cd;
    }
    public CohortDefinition noCurrentVLResults() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("txcurr", ReportUtils.map(datimCohorts.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("noCurrentVL", ReportUtils.map(noCurrentVL(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("txcurr AND noCurrentVL");
        return cd;
    }
    public  CohortDefinition allWithVL() {
        String sqlQuery="select patient_id from kenyaemr_etl.etl_laboratory_extract where lab_test in (1305,856)\n" +
                "group by patient_id;";
        SqlCohortDefinition cd = new SqlCohortDefinition();
        cd.setName("noVLResults");
        cd.setQuery(sqlQuery);
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.setDescription("No VL Results");
        return cd;
    }
    public CohortDefinition noVLResults() {
        CompositionCohortDefinition cd = new CompositionCohortDefinition();
        cd.addParameter(new Parameter("startDate", "Start Date", Date.class));
        cd.addParameter(new Parameter("endDate", "End Date", Date.class));
        cd.addSearch("txcurr", ReportUtils.map(datimCohorts.currentlyOnArt(), "startDate=${startDate},endDate=${endDate}"));
        cd.addSearch("allWithVL", ReportUtils.map(allWithVL(), "startDate=${startDate},endDate=${endDate}"));
        cd.setCompositionString("txcurr AND NOT allWithVL");
        return cd;
    }

}
