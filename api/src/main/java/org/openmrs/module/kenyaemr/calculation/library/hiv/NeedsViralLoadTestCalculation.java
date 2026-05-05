/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.calculation.library.hiv;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.CareSetting;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.CalculationUtils;
import org.openmrs.module.kenyacore.calculation.Filters;
import org.openmrs.module.kenyacore.calculation.PatientFlagCalculation;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.ActiveInMCHProgramCalculation;
import org.openmrs.module.kenyaemr.calculation.library.BreastFeedingStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.IsBreastFeedingCalculation;
import org.openmrs.module.kenyaemr.calculation.library.IsPregnantCalculation;
import org.openmrs.module.kenyaemr.calculation.library.PregnancyStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.LastViralLoadResultCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.OnArtCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.util.HtsConstants;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils.daysSince;
import static org.openmrs.module.kenyaemr.util.EmrUtils.*;
import static org.openmrs.module.kenyaemrorderentry.util.Utils.getLatestObs;

public class NeedsViralLoadTestCalculation extends AbstractPatientCalculation implements PatientFlagCalculation {
    protected static final Log log = LogFactory.getLog(NeedsViralLoadTestCalculation.class);
    String flagMessage = null;
    public static final Integer HTS_PMTCT_MAT_ENTRY_POINT_CONCEPT_ID = 160456;
    public static final Integer HTSENTRYPOINT_QUESTION_CONCEPT_ID = 160540;
    public static final Integer PMTCT_NP = 163718;
    public static final Integer PMTCT_KP = 2001237;

    /**
     * Needs vl test calculation criteria: New EMR guidelines March 2023
     * -----------------------------------------------------------------
     * Immediately = Pregnant + Breastfeeding mothers On ART
     * After 3 months = All unsuppressed + All Newly on ART (Including Pregnant and Breastfeeding mothers)
     * After 6 months = Children (0-24) with suppressed VL or Pregnant_Breastfeeding with suppressed initial VL after Pregnancy/BF status is recorded
     * After 12 months = Adults aged 25+ years with suppressed VL (upto 200 cps/ml)
     *
     * @see org.openmrs.module.kenyacore.calculation.PatientFlagCalculation#getFlagMessage()
     */
    @Override
    public String getFlagMessage() {
        return flagMessage;
    }

    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        PatientService patientService = Context.getPatientService();
        ConceptService cs = Context.getConceptService();
        OrderService orderService = Context.getOrderService();
        EncounterService encounterService = Context.getEncounterService();
        EncounterType htsEncounterType = safeGetMetadata(EncounterType.class, CommonMetadata._EncounterType.HTS);
        List<Form> htsPreArtForms = Arrays.asList(
                safeGetMetadata(Form.class, CommonMetadata._Form.HTS_INITIAL_TEST),
                safeGetMetadata(Form.class, CommonMetadata._Form.HTS_CONFIRMATORY_TEST)
        );

        Concept htsFinalTestQuestion = cs.getConcept(HtsConstants.HTS_FINAL_TEST_CONCEPT_ID);
        Concept htsPositiveResult = cs.getConcept(HtsConstants.HTS_POSITIVE_RESULT_CONCEPT_ID);
        Concept htsEntryPointQuestion = cs.getConcept(HTSENTRYPOINT_QUESTION_CONCEPT_ID);
        Concept pmtctMatEntryPoint = cs.getConcept(HTS_PMTCT_MAT_ENTRY_POINT_CONCEPT_ID);
        Concept YES = Dictionary.getConcept(Dictionary.YES);
        List<Concept> vlOrderConcepts = Arrays.asList(cs.getConceptByUuid(Dictionary.HIV_VIRAL_LOAD), cs.getConceptByUuid(Dictionary.HIV_VIRAL_LOAD_QUALITATIVE));

        Set<Integer> alive = Filters.alive(cohort, context);
        Set<Integer> inHivProgram = Filters.inProgram(hivProgram, alive, context);
        //Cohorts to consider
         // All on ART already
        Set<Integer> allOnArt = CalculationUtils.patientsThatPass(calculate(new OnArtCalculation(), cohort, context));
        // Patients with pending vl results
        Set<Integer> pendingVlResults = CalculationUtils.patientsThatPass(calculate(new PendingViralLoadResultCalculation(), cohort, context));
        //check for last pregnancy start date
        CalculationResultMap pregnancyStartDate = calculate(new PregnancyStartDateCalculation(), cohort, context);
        //check for last breastfeeding start date
        CalculationResultMap breastFeedingStarDate = calculate(new BreastFeedingStartDateCalculation(), cohort, context);
        //get the initial art start date
        CalculationResultMap dateInitiatedART = calculate(new InitialArtStartDateCalculation(), cohort, context);
        //check for last vl and date
        LastViralLoadResultCalculation lastVlResultCalculation = new LastViralLoadResultCalculation();
        CalculationResultMap lastVlResults = lastVlResultCalculation.evaluate(cohort, null, context);
        //Checks for ltfu
        Set<Integer> ltfu = CalculationUtils.patientsThatPass(calculate(new LostToFollowUpCalculation(), cohort, context));
        //Returns active in MCH clients
        Set<Integer> activeInMCH = CalculationUtils.patientsThatPass(calculate(new ActiveInMCHProgramCalculation(), cohort, context));
        Set<Integer> pregnantPatients = CalculationUtils.patientsThatPass(calculate(new IsPregnantCalculation(), cohort, context));
        Set<Integer> breastFeedingPatients = CalculationUtils.patientsThatPass(calculate(new IsBreastFeedingCalculation(), cohort, context));
        EncounterType mchConsultationEncounterType = safeGetMetadata(EncounterType.class, MchMetadata._EncounterType.MCHMS_CONSULTATION);
        Form ancForm = safeGetMetadata(Form.class, MchMetadata._Form.MCHMS_ANTENATAL_VISIT);
        Form deliveryForm = safeGetMetadata(Form.class, MchMetadata._Form.MCHMS_DELIVERY);
        Form pncForm = safeGetMetadata(Form.class, MchMetadata._Form.MCHMS_POSTNATAL_VISIT);
        CalculationResultMap ret = new CalculationResultMap();
        for (Integer ptId : cohort) {
            Patient patient = patientService.getPatient(ptId);
            boolean needsViralLoadTest = false;
            String lastVlResult = null;
            String lastVlResultLDL = null;
            Double lastVlResultValue = null;
            Date lastVLResultDate = null;
            Date artStartDate = EmrCalculationUtils.datetimeResultForPatient(dateInitiatedART, ptId);
            Date lastPregStartDate = EmrCalculationUtils.datetimeResultForPatient(pregnancyStartDate, ptId);
            Date lastBFStartDate = EmrCalculationUtils.datetimeResultForPatient(breastFeedingStarDate, ptId);

            Obs latestVlObs = getLatestObs(patient, Dictionary.HIV_VIRAL_LOAD);
            Order order = latestVlObs != null ? latestVlObs.getOrder() : null;
            Integer lastVlOrderReason = order != null ? order.getOrderReason().getConceptId() : null;

            //Check for latest vl and if it exists (vl is only valid if its for the last 12 months)
            CalculationResult lastvlresult = lastVlResults.get(ptId);
            if (lastvlresult != null && lastvlresult.getValue() != null) {
                Object lastVl = lastvlresult.getValue();
                SimpleObject res = (SimpleObject) lastVl;
                lastVlResult = res.get("lastVl").toString();
                lastVLResultDate = (Date) res.get("lastVlDate");
                // Differentiate between LDL and values for Viral load results
                if ("LDL".equals(lastVlResult)) {
                    lastVlResultLDL = "LDL";
                } else {
                    try {
                        lastVlResultValue = Double.parseDouble(lastVlResult);
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse viral load result as number: " + lastVlResult + " for patient " + ptId);
                    }
                }
            }

            // Confirm that patient is on hiv and there are no pending vls
            if (inHivProgram.contains(ptId) && !pendingVlResults.contains(ptId) && allOnArt.contains(ptId) && !ltfu.contains(ptId)) {

                Obs savedPregnancyStatus = getLatestObs(patient, Dictionary.PREGNANCY_STATUS);
                Obs savedBFStatus = getLatestObs(patient, Dictionary.CURRENTLY_BREASTFEEDING);

                Date obsPregStatusDate = savedPregnancyStatus != null && savedPregnancyStatus.getValueCoded().equals(YES) ? savedPregnancyStatus.getObsDatetime() : null;
                Date obsBFStatusDate = savedBFStatus != null && savedBFStatus.getValueCoded().equals(YES) ? savedBFStatus.getObsDatetime() : null;

                // Check if patient is currently pregnant or breastfeeding
                boolean isCurrentlyPregnantOrBreastfeeding = (activeInMCH.contains(ptId) &&
                        ((lastPregStartDate != null && (obsPregStatusDate == null || lastPregStartDate.after(obsPregStatusDate) || lastPregStartDate.equals(obsPregStatusDate))) ||
                                (lastBFStartDate != null && (obsBFStatusDate == null || lastBFStartDate.after(obsBFStatusDate) || lastBFStartDate.equals(obsBFStatusDate))))) ||
                        obsPregStatusDate != null || obsBFStatusDate != null;

                //Immediate: Pregnant or breastfeeding On ART - Only apply to currently pregnant/breastfeeding patients
                if (isCurrentlyPregnantOrBreastfeeding && artStartDate != null && lastVLResultDate != null &&
                        ((lastPregStartDate != null && lastPregStartDate.after(lastVLResultDate)) ||
                                (lastBFStartDate != null && lastBFStartDate.after(lastVLResultDate)) ||
                                (obsPregStatusDate != null && obsPregStatusDate.after(lastVLResultDate)) ||
                                (obsBFStatusDate != null && obsBFStatusDate.after(lastVLResultDate)))) {

                    needsViralLoadTest = true;
                    flagMessage = "Due for Viral Load";
                }
                //After 3 months: All with unsuppressed VL (>200 cps/ml)
                else if (lastVlResultValue != null && lastVLResultDate != null && daysSince(lastVLResultDate, context) >= 90 && lastVlResultValue > 200) {
                    needsViralLoadTest = true;
                    flagMessage = "Due for Viral Load";
                }
                //After 3 Months: New positives with no previous VL
                else if (artStartDate != null && daysSince(artStartDate, context) >= 90 && lastVLResultDate == null) {
                    needsViralLoadTest = true;
                    flagMessage = "Due for Viral Load";
                }
                //After 6 months:
                // Pregnant and BF with a suppressed VL pregnancy test or during BF.
                //0-24 years old with a suppressed or LDL previous VL
                else if ((isCurrentlyPregnantOrBreastfeeding || patient.getAge() <= 24) &&
                        (lastVLResultDate != null && daysSince(lastVLResultDate, context) >= 183 &&
                                (lastVlResultLDL != null || (lastVlResultValue != null && lastVlResultValue < 200)))) {

                    // For pregnant/breastfeeding mothers, check if VL was done during current pregnancy/BF period
                    boolean vlDuringCurrentPeriod = false;
                    if (isCurrentlyPregnantOrBreastfeeding) {
                        vlDuringCurrentPeriod = (lastPregStartDate != null && lastVLResultDate != null && lastPregStartDate.before(lastVLResultDate)) ||
                                (lastBFStartDate != null && lastVLResultDate != null && lastBFStartDate.before(lastVLResultDate)) ||
                                (obsPregStatusDate != null && lastVLResultDate != null && obsPregStatusDate.before(lastVLResultDate)) ||
                                (obsBFStatusDate != null && lastVLResultDate != null && obsBFStatusDate.before(lastVLResultDate));
                    }

                    if (vlDuringCurrentPeriod || patient.getAge() <= 24) {
                        needsViralLoadTest = true;
                        flagMessage = "Due for Viral Load";
                    }
                }
                //After 12 Months: > 25 years old with suppressed VL or LDL
                else if (lastVLResultDate != null && daysSince(lastVLResultDate, context) >= 365 && patient.getAge() >= 25 &&
                        (lastVlResultLDL != null || (lastVlResultValue != null && lastVlResultValue <= 200))) {
                    needsViralLoadTest = true;
                    flagMessage = "Due for Viral Load";
                }

            }

            boolean activeInMchWorkflow = pregnantPatients.contains(ptId)
                    || breastFeedingPatients.contains(ptId)
                    || activeInMCH.contains(ptId);

            if (needsPreArtViralLoad(patient, artStartDate, lastVLResultDate, activeInMchWorkflow,
                    encounterService, orderService, htsEncounterType, htsPreArtForms,
                    mchConsultationEncounterType, ancForm, deliveryForm, pncForm,
                    htsFinalTestQuestion, htsPositiveResult,
                    htsEntryPointQuestion, pmtctMatEntryPoint, vlOrderConcepts)) {
                needsViralLoadTest = true;
                flagMessage = "Due for Pre-ART Viral Load";
            }

            ret.put(ptId, new BooleanResult(needsViralLoadTest, this));
        }

        return ret;
    }

    private boolean hasConfirmedPositiveAtANC(Patient patient, EncounterService encounterService,
                                               EncounterType htsEncounterType, List<Form> htsForms,
                                               EncounterType mchConsultationType, Form ancForm,
                                               Concept testQuestion, Concept positiveResult) {
        List<Encounter> allAncHtsEncounters = new ArrayList<>(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(htsEncounterType)).setEnteredViaForms(htsForms)
                        .createEncounterSearchCriteria()));
        allAncHtsEncounters.addAll(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(mchConsultationType)).setEnteredViaForms(Arrays.asList(ancForm))
                        .createEncounterSearchCriteria()));
        Encounter latest = latestEncounter(allAncHtsEncounters);
        return latest != null && EmrUtils.encounterThatPassCodedAnswer(latest, testQuestion, positiveResult);
    }

    private boolean hasConfirmedPositiveAtDelivery(Patient patient, EncounterService encounterService,
                                                    EncounterType htsEncounterType, List<Form> htsForms,
                                                    EncounterType mchConsultationType, Form deliveryForm,
                                                    Concept testQuestion, Concept positiveResult,
                                                    Concept entryPointQuestion, Concept pmtctMatEntryPoint) {
        Encounter latestHts = latestEncounter(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(htsEncounterType)).setEnteredViaForms(htsForms)
                        .createEncounterSearchCriteria()));
        if (latestHts != null &&
                EmrUtils.encounterThatPassCodedAnswer(latestHts, entryPointQuestion, pmtctMatEntryPoint) &&
                EmrUtils.encounterThatPassCodedAnswer(latestHts, testQuestion, positiveResult)) {
            return true;
        }

        Encounter latestDelivery = latestEncounter(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(mchConsultationType)).setEnteredViaForms(Arrays.asList(deliveryForm))
                        .createEncounterSearchCriteria()));
        return latestDelivery != null && EmrUtils.encounterThatPassCodedAnswer(latestDelivery, testQuestion, positiveResult);
    }

    private boolean hasConfirmedPositiveAtPNC(Patient patient, EncounterService encounterService,
                                               EncounterType htsEncounterType, List<Form> htsForms,
                                               EncounterType mchConsultationType, Form pncForm,
                                               Concept testQuestion, Concept positiveResult) {
        List<Encounter> allPncHtsEncounters = new ArrayList<>(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(htsEncounterType)).setEnteredViaForms(htsForms)
                        .createEncounterSearchCriteria()));
        allPncHtsEncounters.addAll(encounterService.getEncounters(
                new EncounterSearchCriteriaBuilder().setPatient(patient)
                        .setEncounterTypes(Arrays.asList(mchConsultationType)).setEnteredViaForms(Arrays.asList(pncForm))
                        .createEncounterSearchCriteria()));
        Encounter latest = latestEncounter(allPncHtsEncounters);
        return latest != null && EmrUtils.encounterThatPassCodedAnswer(latest, testQuestion, positiveResult);
    }

    private boolean needsPreArtViralLoad(Patient patient, Date artStartDate, Date lastVLResultDate, boolean activeInMch,
                                         EncounterService encounterService, OrderService orderService,
                                         EncounterType htsEncounterType, List<Form> htsForms,
                                         EncounterType mchConsultationType, Form ancForm, Form deliveryForm, Form pncForm,
                                         Concept htsFinalTestQuestion, Concept htsPositiveResult,
                                         Concept htsEntryPointQuestion, Concept pmtctMatEntryPoint,
                                         List<Concept> vlOrderConcepts) {
        if (artStartDate != null || lastVLResultDate != null || hasExistingVlOrder(patient, orderService, vlOrderConcepts)) {
            return false;
        }

        // Delivery patients may not be enrolled in any MCH program — confirmed positive at delivery is their identifier
        if (hasConfirmedPositiveAtDelivery(patient, encounterService, htsEncounterType, htsForms,
                mchConsultationType, deliveryForm, htsFinalTestQuestion, htsPositiveResult,
                htsEntryPointQuestion, pmtctMatEntryPoint)) {
            return true;
        }

        // ANC and PNC pathways require MCH program enrollment or pregnancy/BF obs identification
        if (!activeInMch) {
            return false;
        }

        return hasConfirmedPositiveAtANC(patient, encounterService, htsEncounterType, htsForms,
                       mchConsultationType, ancForm, htsFinalTestQuestion, htsPositiveResult)
                || hasConfirmedPositiveAtPNC(patient, encounterService, htsEncounterType, htsForms,
                       mchConsultationType, pncForm, htsFinalTestQuestion, htsPositiveResult);
    }

}
