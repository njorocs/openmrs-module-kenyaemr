/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.visit.EmrVisitAssignmentHandler;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.aop.AfterReturningAdvice;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Automates the process of checking out a patient from OPD and starting an
 * inpatient visit on admission form submission.
 *
 * 1. Write OPD.stop = T-1ms directly via SQL (no VisitValidator).
 * 2. Update the in-memory Visit entity to match so Hibernate stays consistent.
 * 3. flushSession — force the Hibernate dirty-entity write for OPD to the DB,
 * ensuring the overlap check for the inpatient visit sees OPD as stopped.
 * 4. End queue entry at T-1ms (endedAt == OPD.stop → queue constraint
 * satisfied).
 * 5. Create inpatient visit starting at T via visitService.saveVisit()
 * (validator runs normally; overlap check passes because OPD.stop < T).
 */
public class OutpatientToInpatientCheckinOnAdmission implements AfterReturningAdvice {

    private Log log = LogFactory.getLog(this.getClass());
    public static final String INPATIENT_ADMISSION_REQUEST_QUESTION_CONCEPT = "160433AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String INPATIENT_ADMISSION_ANSWER_CONCEPT = "1654AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String INPATIENT_ADMISSION_FORM = "49f3686d-b83c-4263-a5a1-89040f643a78";

    String conditionsConfig = Context.getAdministrationService().getGlobalProperty("kenyaemr.conditions.config");
    Set<Integer> CONDITIONS_CONCEPTS = new HashSet<>();
    {
        if (conditionsConfig != null && !conditionsConfig.trim().isEmpty()) {
            for (String id : conditionsConfig.split("\\s*,\\s*")) {
                try {
                    CONDITIONS_CONCEPTS.add(Integer.parseInt(id));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {

        if (method.getName().equals("saveEncounter")) {
            Encounter enc = (Encounter) args[0];
            VisitService visitService = Context.getVisitService();

            if (enc != null && enc.getVisit() != null
                    && enc.getVisit().getVisitType().getUuid().equals(CommonMetadata._VisitType.OUTPATIENT)
                    && enc.getForm() != null && INPATIENT_ADMISSION_FORM.equalsIgnoreCase(enc.getForm().getUuid())) {

                /*
                 * inpatientStart = enc.getEncounterDatetime()
                 * The inpatient visit starts exactly when the encounter was recorded.
                 * This guarantees the encounter is never "before" its visit's startDatetime
                 * (constraint C), which would block the user from ever ending the visit.
                 *
                 * opdStop = inpatientStart - 1 ms
                 * One millisecond before the inpatient start. OpenMRS overlap check is
                 * strict (uses Date.before(), so equal timestamps overlap), so we need at
                 * least 1 ms gap (constraint B).
                 * Written via SQL — NOT via visitService.saveVisit() — to avoid the
                 * VisitValidator's encountersCannotBeAfterStopDate check (constraint A
                 * deadlock described above).
                 */
                Date inpatientStart = enc.getEncounterDatetime() != null
                        ? enc.getEncounterDatetime()
                        : new Date();
                Date opdStop = new Date(inpatientStart.getTime() - 1L);

                Visit opdVisit = enc.getVisit();
                String opdStopSql = String.format(
                        "UPDATE visit SET date_stopped = '%s', date_changed = '%s' WHERE visit_id = %d",
                        new Timestamp(opdStop.getTime()),
                        new Timestamp(new Date().getTime()),
                        opdVisit.getVisitId());
                Context.getAdministrationService().executeSQL(opdStopSql, false);
                opdVisit.setStopDatetime(opdStop);

                log.info("OPD visit " + opdVisit.getUuid() + " stopped at " + opdStop
                        + " (SQL) for patient " + enc.getPatient().getPatientId());
                Context.flushSession();
                removePatientFromOpdQueue(enc.getPatient(), opdVisit, opdStop);
                Visit inpatientVisit = new Visit();
                inpatientVisit.setStartDatetime(inpatientStart);
                inpatientVisit.setLocation(enc.getLocation());
                inpatientVisit.setPatient(enc.getPatient());
                inpatientVisit.setVisitType(visitService.getVisitTypeByUuid(CommonMetadata._VisitType.INPATIENT));
                enc.setVisit(inpatientVisit);
                Context.getVisitService().saveVisit(inpatientVisit);

                log.info("Inpatient visit " + inpatientVisit.getUuid() + " started at " + inpatientStart
                        + " for patient " + enc.getPatient().getPatientId());

                // Prevent re-attachment of the encounter to the closed OPD visit.
                EmrVisitAssignmentHandler.setVisitOfEncounter(inpatientVisit, enc);
            }

            // Auto-populate conditions table from clinical diagnoses
            if (enc != null && enc.getForm() != null
                    && (CommonMetadata._Form.CLINICAL_ENCOUNTER.equalsIgnoreCase(enc.getForm().getUuid())
                            || HivMetadata._Form.HIV_GREEN_CARD.equalsIgnoreCase(enc.getForm().getUuid()))) {

                Integer encounterId = enc.getEncounterId();
                Integer patientId = enc.getPatient().getId();
                User creator = enc.getCreator();
                Date now = new Date();

                String diagnosisQuery = String.format(
                        "SELECT diagnosis_coded FROM encounter_diagnosis " +
                                "WHERE encounter_id = %d AND voided = 0",
                        encounterId);
                try {
                    List<List<Object>> diagnosisResults = Context.getAdministrationService()
                            .executeSQL(diagnosisQuery, true);
                    for (List<Object> row : diagnosisResults) {
                        if (row != null && !row.isEmpty()) {
                            Integer diagnosisCoded = (Integer) row.get(0);
                            if (CONDITIONS_CONCEPTS.contains(diagnosisCoded)) {
                                String checkConditionQuery = String.format(
                                        "SELECT condition_id FROM conditions " +
                                                "WHERE patient_id = %d AND condition_coded = %d AND voided = 0",
                                        patientId, diagnosisCoded);
                                List<List<Object>> conditionResults = Context.getAdministrationService()
                                        .executeSQL(checkConditionQuery, true);
                                if (conditionResults.isEmpty()) {
                                    String insertConditionQuery = String.format(
                                            "INSERT INTO conditions (" +
                                                    "patient_id, encounter_id, condition_coded, clinical_status, " +
                                                    "onset_date, date_created, creator, uuid, voided) " +
                                                    "VALUES (%d, %d, %d, 'ACTIVE', '%s', '%s', %d, '%s', 0)",
                                            patientId,
                                            encounterId,
                                            diagnosisCoded,
                                            new Timestamp(now.getTime()),
                                            new Timestamp(now.getTime()),
                                            creator.getUserId(),
                                            UUID.randomUUID().toString());
                                    Context.getAdministrationService().executeSQL(insertConditionQuery, false);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error auto-populating conditions for encounter " + encounterId, e);
                }
            }
        }
    }

    /**
     * Ends all active OPD queue entries for the given patient and visit.
     *
     * <p>
     * {@code endTime} MUST equal the OPD visit's {@code date_stopped}.
     * The queue module enforces {@code queueEntry.endedAt <= visit.date_stopped}.
     *
     * <p>
     * Must be called after the OPD stop time is already committed to the DB
     * (either via SQL in step 1 or a flush), so the queue validator finds a
     * valid {@code date_stopped} on the visit.
     *
     * <p>
     * Exceptions are caught so that a missing queue entry never blocks admission.
     */
    private void removePatientFromOpdQueue(Patient patient, Visit visit, Date endTime) {
        try {
            QueueEntryService queueEntryService = Context.getService(QueueEntryService.class);
            QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
            criteria.setPatient(patient);
            criteria.setVisit(visit);
            criteria.setIsEnded(false);
            List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
            if (!queueEntries.isEmpty()) {
                log.info("Ending " + queueEntries.size() + " OPD queue entry/entries"
                        + " for patient " + patient.getPatientId() + " at " + endTime);
                for (QueueEntry queueEntry : queueEntries) {
                    queueEntry.setEndedAt(endTime);
                    queueEntryService.saveQueueEntry(queueEntry);
                }
            } else {
                log.info("No active OPD queue entries found for patient "
                        + patient.getPatientId() + " — skipping queue removal");
            }
        } catch (Exception e) {
            log.warn("Could not end OPD queue entry for patient " + patient.getPatientId()
                    + " (admission will still proceed): " + e.getMessage(), e);
        }
    }
}