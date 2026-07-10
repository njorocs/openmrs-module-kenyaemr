/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.task;

import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.evaluation.querybuilder.SqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Scheduled task to void expired Ebola flag observations.
 * Implemented as an OpenMRS AbstractTask so it can be scheduled by the
 * module's scheduler (same mechanism as other DMI tasks).
 */
public class EbolaFlagCleanupTask extends AbstractTask {

    private static final Logger log = LoggerFactory.getLogger(EbolaFlagCleanupTask.class);

    /**
     * Find obs with value_text like 'ebola_flag:%' and void those whose expiry timestamp has passed.
     */
    public static void runCleanup() {
        EvaluationService evaluationService = Context.getService(EvaluationService.class);
        String qry = "select o.obs_id, o.value_text from obs o where o.voided = 0 and o.value_text like 'ebola_flag:%'";
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.append(qry);
        List<Object> rows = evaluationService.evaluateToList(builder, Object.class, null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date now = new Date();
        for (Object r : rows) {
            if (r instanceof Object[]) {
                Object[] cols = (Object[]) r;
                Integer obsId = ((Number) cols[0]).intValue();
                String valueText = (String) cols[1];
                try {
                    String[] parts = valueText.split("expires=");
                    if (parts.length == 2) {
                        String ts = parts[1].split(";")[0];
                        Date expiry = sdf.parse(ts);
                        if (expiry.before(now)) {
                            Obs o = Context.getObsService().getObs(obsId);
                            if (o != null && !o.getVoided()) {
                                o.setVoided(true);
                                o.setVoidReason("expired ebola flag");
                                Context.getObsService().saveObs(o, "void expired by cleanup");
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore parse errors for safety
                }
            }
        }
    }

    @Override
    public void execute() {
        if (log.isDebugEnabled()) {
            log.debug("Executing EbolaFlagCleanupTask");
        }
        runCleanup();
    }
}

