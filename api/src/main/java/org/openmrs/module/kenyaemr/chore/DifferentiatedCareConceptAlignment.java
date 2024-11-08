/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.chore;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * handles alignment of Differentiated Care concepts for data collected using the deprecated greencard
 */
@Component("kenyaemr.chore.DifferentiatedCareConceptAlignment")
public class DifferentiatedCareConceptAlignment extends AbstractChore {

    /**
     * @see AbstractChore#perform(PrintWriter)
     */

    @Override
    public void perform(PrintWriter out) {
        EncounterService encounterService = Context.getEncounterService();
        ConceptService conceptService = Context.getConceptService();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Integer> artDistributionConceptIds = Arrays.asList(1537, 163488);
        List<Integer> diffCareModelConceptIds = Arrays.asList(164946, 165287);

        List<Concept> artDistributionAnswerConcepts = new ArrayList<>();
        List<Concept> diffCareModelConcepts = new ArrayList<>();
        for (Integer id : artDistributionConceptIds) {
            Concept concept = conceptService.getConcept(id);
            if (concept != null) {
                artDistributionAnswerConcepts.add(concept);
            }
        }

        for (Integer id : diffCareModelConceptIds) {
            Concept concept = conceptService.getConcept(id);
            if (concept != null) {
                diffCareModelConcepts.add(concept);
            }
        }

        Date fromDate = new Date();
        try {
            fromDate = sdf.parse("2024-01-01");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, fromDate, null, null, Collections.singleton(MetadataUtils.existing(Form.class, HivMetadata._Form.HIV_GREEN_CARD)), Collections.singleton(MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_CONSULTATION)), null, null, null, false);
        List<Encounter> hivEncounters = encounterService.getEncounters(encounterSearchCriteria);
        List<Obs> artDistributionObs = Context.getObsService().getObservations(null, hivEncounters, Collections.singletonList(conceptService.getConcept(164947)), artDistributionAnswerConcepts, null, null, null, null, null, null, null, false);
        List<Obs> diffCareModelObs = Context.getObsService().getObservations(null, hivEncounters, diffCareModelConcepts, null, null, null, null, null, null, null, null, false);

        for (Obs oldArtDistributionObs : artDistributionObs) {
            Obs newArtDistributionObs = new Obs();
            Date obsDate = oldArtDistributionObs.getObsDatetime();

            newArtDistributionObs.setPerson(oldArtDistributionObs.getPerson());
            newArtDistributionObs.setEncounter(oldArtDistributionObs.getEncounter());
            newArtDistributionObs.setObsDatetime(obsDate);
            newArtDistributionObs.setConcept(conceptService.getConcept(166448));
            newArtDistributionObs.setValueCoded(oldArtDistributionObs.getValueCoded());
            newArtDistributionObs.setLocation(oldArtDistributionObs.getLocation());
            newArtDistributionObs.setCreator(oldArtDistributionObs.getCreator());
            newArtDistributionObs.setDateCreated(new Date());
            newArtDistributionObs.setObsGroup(oldArtDistributionObs.getObsGroup());

            try {
                Context.getObsService().saveObs(newArtDistributionObs, "Replaced wrongly used concept for ART distribution group");

                try {
                    Context.getObsService().voidObs(oldArtDistributionObs, "Replaced by new obs with new ART distribution group concept");

                } catch (Exception voidException) {
                    Context.getObsService().voidObs(newArtDistributionObs, "Rollback due to failure to void old observation");
                    out.println("Failed to void old observation for patient " + oldArtDistributionObs.getPerson().getId() + ". Rolled back new observation.");
                    voidException.printStackTrace(out);
                }

            } catch (Exception saveException) {
                out.println("Failed to save new observation for patient " + oldArtDistributionObs.getPerson().getId() + ": " + saveException.getMessage());
                saveException.printStackTrace(out);
            }
        }

        for (Obs oldDiffCareModelObs : diffCareModelObs) {
            Obs newDiffCareModelObs = new Obs();
            Date obsDate = oldDiffCareModelObs.getObsDatetime();

            newDiffCareModelObs.setPerson(oldDiffCareModelObs.getPerson());
            newDiffCareModelObs.setEncounter(oldDiffCareModelObs.getEncounter());
            newDiffCareModelObs.setObsDatetime(obsDate);
            newDiffCareModelObs.setConcept(conceptService.getConcept(164947));
            newDiffCareModelObs.setValueCoded(oldDiffCareModelObs.getValueCoded());
            newDiffCareModelObs.setLocation(oldDiffCareModelObs.getLocation());
            newDiffCareModelObs.setCreator(oldDiffCareModelObs.getCreator());
            newDiffCareModelObs.setDateCreated(new Date());
            newDiffCareModelObs.setObsGroup(oldDiffCareModelObs.getObsGroup());

            try {
                Context.getObsService().saveObs(newDiffCareModelObs, "Replaced wrongly used differentiated care model model concept");

                try {
                    Context.getObsService().voidObs(oldDiffCareModelObs, "Replaced by new obs with new differentiated care model concept");

                } catch (Exception voidException) {
                    Context.getObsService().voidObs(newDiffCareModelObs, "Rollback due to failure to void old observation");
                    out.println("Failed to void old observation for patient " + oldDiffCareModelObs.getPerson().getId() + ". Rolled back new observation.");
                    voidException.printStackTrace(out);
                }

            } catch (Exception saveException) {
                out.println("Failed to save new observation for patient " + oldDiffCareModelObs.getPerson().getId() + ": " + saveException.getMessage());
                saveException.printStackTrace(out);
            }
        }

        out.println("Completed Updating concepts for DCM and ART distribution group");
    }
}
