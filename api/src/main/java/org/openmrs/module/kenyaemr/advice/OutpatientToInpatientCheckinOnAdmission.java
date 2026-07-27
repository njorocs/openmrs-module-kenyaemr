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
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Automates the process of updating a patient visit type from OPD and starting an
 * inpatient visit on admission form submission.
 * 1. Send the patient to admission queue through clinical encounter admit sub form or direct admission
 * 2. On the Ward app fill the admit patient form
 * 3. Patient visit changes from OPD to IPD
 * 4. Removes patient from OPD queue
 * NOTE:Runs only when the admission form is filled and not admission request
 */
public class OutpatientToInpatientCheckinOnAdmission implements AfterReturningAdvice {

	private Log log = LogFactory.getLog(this.getClass());	
	public static final String INPATIENT_ADMISSION_FORM = "49f3686d-b83c-4263-a5a1-89040f643a78";
	public static final String INPATIENT_VISIT_TYPE_UUID = "a73e2ac6-263b-47fc-99fc-e0f2c09fc914";
	
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

			if (enc != null
				&& enc.getVisit() != null
				&& enc.getVisit().getVisitType() != null
				&& enc.getVisit().getStopDatetime() == null
				&& CommonMetadata._VisitType.OUTPATIENT.equals(enc.getVisit().getVisitType().getUuid())
				&& enc.getForm() != null
				&& INPATIENT_ADMISSION_FORM.equalsIgnoreCase(enc.getForm().getUuid())) {

				//This will support generation of requisite attachments from visit summary and procedure notes from the existing unclosed visit
				//Changing only the visit type will allow admission to inpatient	
			
				Date inpatientStart = enc.getEncounterDatetime() != null
					? enc.getEncounterDatetime()
					: new Date();
				Date opdStop = new Date(inpatientStart.getTime() - 1L);
				Visit visit = enc.getVisit();

				//Update the outpatient visit type to inpatient
				VisitType inpatient = visitService.getVisitTypeByUuid(INPATIENT_VISIT_TYPE_UUID);
				visit.setVisitType(inpatient);
				visitService.saveVisit(visit);

				//Remove the patient from outpatient queue
				removePatientFromOpdQueue(enc.getPatient(), visit, opdStop);
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
