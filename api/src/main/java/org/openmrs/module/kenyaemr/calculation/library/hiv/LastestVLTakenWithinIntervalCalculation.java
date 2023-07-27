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
import org.openmrs.*;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyacore.calculation.*;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.ActiveInMCHProgramCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.LastViralLoadResultCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.OnArtCalculation;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.MchMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.ui.framework.SimpleObject;

import java.util.*;

import static org.openmrs.module.kenyaemrorderentry.util.Utils.daysBetween;
import static org.openmrs.module.kenyaemrorderentry.util.Utils.getLatestObs;

public class LastestVLTakenWithinIntervalCalculation extends AbstractPatientCalculation {
    protected static final Log log = LogFactory.getLog(StablePatientsCalculation.class);

    /**
     * Calculation to determine whether latest VL was taken within the recommended interval
     * ---------------------------------------------------------------------------------------
     * Immediately = Pregnant + Breastfeeding mothers On ART
     * After 3 months = All unsuppressed + All Newly on ART (Including Pregnant and Breastfeeding mothers)
     * After 6 months = Children (0-24) with suppressed VL or Pregnant_Breastfeeding with suppressed initial VL after Pregnancy/BF status is recorded
     * After 12 months = Adults aged 25+ years with suppressed VL (upto 200 cps/ml)
     */
    @Override
    public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
        Program hivProgram = MetadataUtils.existing(Program.class, HivMetadata._Program.HIV);
        Program mchProgram = MetadataUtils.existing(Program.class, MchMetadata._Program.MCHMS);
        PatientService patientService = Context.getPatientService();
        CalculationResultMap ret = new CalculationResultMap();
        Map<String, Object> params = new HashMap<String, Object>();
        Set<Integer> alive = Filters.alive(cohort, context);
        Set<Integer> inHivProgram = Filters.inProgram(hivProgram, alive, context);
        Set<Integer> inMCHProgram = Filters.inProgram(mchProgram, alive, context);

        // All on ART already
        Set<Integer> allOnArt = CalculationUtils.patientsThatPass(calculate(new OnArtCalculation(), cohort, context));
        Concept mixedFeeding = Dictionary.getConcept(Dictionary.MIXED_FEEDING);
        Concept exclusiveBreastFeeding = Dictionary.getConcept(Dictionary.BREASTFED_EXCLUSIVELY);

        //get the initial art start date
        CalculationResultMap dateInitiatedART = calculate(new InitialArtStartDateCalculation(), cohort, context);
        //check for last vl and date
        SecondLastVLCalculation secondLastVLCalculation = new SecondLastVLCalculation();
        LastViralLoadResultCalculation lastVlResultCalculation = new LastViralLoadResultCalculation();
        CalculationResultMap secondLastVl = secondLastVLCalculation.evaluate(cohort, null, context);
        CalculationResultMap lastVlResults = lastVlResultCalculation.evaluate(cohort, null, context);

        //Checks for ltfu
        Set<Integer> ltfu = CalculationUtils.patientsThatPass(calculate(new LostToFollowUpCalculation(), cohort, context));
        //Returns active in MCH clients
        Set<Integer> activeInMCH = CalculationUtils.patientsThatPass(calculate(new ActiveInMCHProgramCalculation(), cohort, context));

        Set<Integer> aliveAndFemale = Filters.female(Filters.alive(cohort, context), context);
        EncounterType mchEnrollmentEncType = MetadataUtils.existing(EncounterType.class, MchMetadata._EncounterType.MCHMS_ENROLLMENT);
        Concept yes = Dictionary.getConcept(Dictionary.YES);
        Date dateNineMonthsBefore = DateUtil.adjustDate(context.getNow(), -9, DurationUnit.MONTHS);
        System.out.println("*********************dateNineMonthsBefore " + dateNineMonthsBefore);
        CalculationResultMap pregStatusMap = Calculations.lastObs(Dictionary.getConcept(Dictionary.PREGNANCY_STATUS), aliveAndFemale, context);
        CalculationResultMap infantFeedingMap = Calculations.lastObs(Dictionary.getConcept(Dictionary.INFANT_FEEDING_METHOD), aliveAndFemale, context);
        CalculationResultMap currentlyBreastFeedingMap = Calculations.lastObs(Dictionary.getConcept(Dictionary.CURRENTLY_BREASTFEEDING), aliveAndFemale, context);
        CalculationResultMap enrollmentMap = Calculations.lastEncounter(mchEnrollmentEncType, aliveAndFemale, context);

        for (Integer ptId : cohort) {
            // Confirm that patient is on hiv and there are no pending vls
            if (inHivProgram.contains(ptId) && /*!pendingVlResults.contains(ptId) &&*/ allOnArt.contains(ptId) && !ltfu.contains(ptId)) {
                Patient patient = patientService.getPatient(ptId);
                boolean vlDoneWithinInterval = false;
                String previousVLResult = null, lastVlResult = null, previousVLResultLDL = null, lastVlResultLDL = null;
                Double previousVLResultValue = null, lastVlResultValue = null;
                Date previousVLOrderDate = null, lastVLDate = null, activeVLDate = null, lastBFStartDate = null, lastPregStartDate = null, mchEnrollmentDate = null;
                Order lastOrder = null;
                Date artStartDate = dateInitiatedART != null ? EmrCalculationUtils.datetimeResultForPatient(dateInitiatedART, ptId) : null;
                System.out.println("*****************Patient_id is : " + ptId);
                Obs lastPregStartObs = EmrCalculationUtils.obsResultForPatient(pregStatusMap, ptId);
                if (lastPregStartObs != null && lastPregStartObs.getValueCoded().equals(yes) && !lastPregStartObs.getObsDatetime().before(dateNineMonthsBefore)) {
                    System.out.println("********lastPregStartObs.getValueCoded().getConceptId():" + lastPregStartObs.getValueCoded().getConceptId());
                    lastPregStartDate = lastPregStartObs.getObsDatetime();
                    System.out.println("********lastPregStartDate: " + lastPregStartDate);
                }

                Obs lastBFObs = EmrCalculationUtils.obsResultForPatient(infantFeedingMap, ptId);
                Obs currentlyBFObs = EmrCalculationUtils.obsResultForPatient(currentlyBreastFeedingMap, ptId);

                Concept lastBFStatus = null;
                Concept currentlyBFStatus = null;
                boolean isInfantBreastfeeding = false;
                if (lastBFObs != null) {
                    lastBFStatus = lastBFObs.getValueCoded();
                    isInfantBreastfeeding = lastBFStatus.equals(exclusiveBreastFeeding) || lastBFStatus.equals(mixedFeeding);
                }
                if (currentlyBFObs != null) {
                    currentlyBFStatus = currentlyBFObs.getValueCoded();
                }
                //   boolean isInfantBreastfeeding = lastBFStatus.equals(exclusiveBreastFeeding) || lastBFStatus.equals(mixedFeeding);
                if (lastBFObs != null && currentlyBFObs != null) {

                    if (lastBFObs.getObsDatetime().after(currentlyBFObs.getObsDatetime()) && !lastBFObs.getObsDatetime().before(dateNineMonthsBefore)) {
                        lastBFStartDate = isInfantBreastfeeding ? lastBFObs.getObsDatetime() : null;
                    } else if (currentlyBFObs.getObsDatetime().after(lastBFObs.getObsDatetime()) && !currentlyBFObs.getObsDatetime().before(dateNineMonthsBefore)) {
                        lastBFStartDate = currentlyBFStatus.equals(yes) ? currentlyBFObs.getObsDatetime() : null;
                    }
                } else if (lastBFObs != null && !lastBFObs.getObsDatetime().before(dateNineMonthsBefore)) {
                    lastBFStartDate = isInfantBreastfeeding ? lastBFObs.getObsDatetime() : null;
                } else if (currentlyBFObs != null && !currentlyBFObs.getObsDatetime().before(dateNineMonthsBefore)) {
                    lastBFStartDate = currentlyBFStatus.equals(yes) ? currentlyBFObs.getObsDatetime() : null;
                }
                if (lastBFStartDate != null) {
                    System.out.println("********lastBFStartDate: " + lastBFStartDate);
                }
                //Get the latest of either Breastfeeding or Pregnant status

                //Date lastMixedFeedingStartDate =EmrCalculationUtils.obsResultForPatient(infantFeedingMap, ptId).getObsDatetime();
                if (!enrollmentMap.isEmpty()) {
                    Encounter mchEncounterResult = EmrCalculationUtils.encounterResultForPatient(enrollmentMap, ptId);
                    mchEnrollmentDate = mchEncounterResult != null ? mchEncounterResult.getEncounterDatetime() : null;
                    System.out.println("-----------------mchEnrollmentDate------" + mchEnrollmentDate);
                }
                OrderService orderService = Context.getOrderService();
                OrderType patientLabOrders = orderService.getOrderTypeByUuid(OrderType.TEST_ORDER_TYPE_UUID);
                CareSetting careSetting = orderService.getCareSetting(1);

                List<Order> labOrders = new ArrayList<>();
                List<Order> activeLabOrders = new ArrayList<>();
                List<Order> allVLOrders = new ArrayList<>();

                if (patientLabOrders != null) {
                    labOrders = orderService.getOrders(patient, careSetting, patientLabOrders, true);
                    activeLabOrders = orderService.getActiveOrders(patient, patientLabOrders, careSetting, null);
                    //     labOrders = orderService.getOrders(patientService.getPatient(ptId), careSetting, patientLabOrders, true);

                    for (Order order : activeLabOrders) {
                        // only get active vl orders
                        if (order.getConcept().getConceptId() == 856 || order.getConcept().getConceptId() == 1305) {
                            allVLOrders.add(order);
                            System.out.println("+++++++++Active VL >: Order ID " + order.getOrderId() + "Date:" + order.getDateActivated());
                        }
                    }
                    for (Order order : labOrders) {
                        // only get old vl orders
                        if (order.getConcept().getConceptId() == 856 || order.getConcept().getConceptId() == 1305) {
                            allVLOrders.add(order);
                        }
                    }
                    if (allVLOrders.size() > 0) {
                        lastOrder = orderService.getOrder(allVLOrders.get(0).getOrderId());
                        activeVLDate = lastOrder.getDateActivated();
                    }
                }

                CalculationResult previousVL = secondLastVl != null ? secondLastVl.get(ptId) : null;
                CalculationResult lastVL = lastVlResults != null ? lastVlResults.get(ptId) : null;

                if (previousVL != null && previousVL.getValue() != null) {
                    Object prevVl = previousVL.getValue();
                    SimpleObject res = (SimpleObject) prevVl;
                    previousVLResult = res.get("vl").toString();
                    previousVLOrderDate = (Date) res.get("vlDate");
                    // Differentiate between LDL and values for Viral load results
                    if (previousVLResult == "LDL") {
                        previousVLResultLDL = "LDL";
                    } else {
                        previousVLResultValue = Double.parseDouble(previousVLResult);
                    }
                    System.out.println("***********previousVLOrderDate**: " + previousVLOrderDate);
                }
                if (lastVL != null && lastVL.getValue() != null) {
                    Object lastViralLoad = lastVL.getValue();
                    SimpleObject res = (SimpleObject) lastViralLoad;
                    lastVlResult = res.get("lastVl").toString();
                    lastVLDate = (Date) res.get("lastVlDate");
                    // Differentiate between LDL and values for Viral load results
                    if (lastVlResult == "LDL") {
                        lastVlResultLDL = "LDL";
                    } else {
                        lastVlResultValue = Double.parseDouble(lastVlResult);
                    }
                    System.out.println("***********Last vl order date**: " + lastVLDate);
                }
                if (activeLabOrders.size() > 0) {
                    previousVLOrderDate = lastVLDate;
                    lastVLDate = activeVLDate;
                    previousVLResultLDL = lastVlResultLDL;
                    previousVLResultValue = lastVlResultValue;
                }
                System.out.println("--------activeLabOrders.size()--------: " + activeLabOrders.size());
                System.out.println("--------lastVLDate--------: " + lastVLDate);
                System.out.println("--------previousVLOrderDate--------: " + previousVLOrderDate);
                System.out.println("--------previousVLResultLDL--------: " + previousVLResultLDL);
                System.out.println("--------previousVLResultValue--------: " + previousVLResultValue);

                int daysToMCHSinceARTStart = -1, daysPregObsSinceARTStart = -1, daysBtwLastVlAndBFDate = -1, daysBFObsSinceARTStart = -1, daysBtwLastVlAndMCHDate = -1, daysBtwLastVlAndPregDate = -1, daysBtwLastAndPrevVlDates = 0, daysBetweenARTStartAndLastVL = -1, daysBetweenPregGreenCardAndARTStartDate = -1, daysBetweenBFGreenCardAndARTStartDate = -1;

                if (artStartDate != null && lastVLDate != null) {
                    daysBetweenARTStartAndLastVL = daysBetween(artStartDate, lastVLDate);
                    System.out.println("=================daysBetweenARTStartAndLastVL: " + daysBetweenARTStartAndLastVL);
                }
                if (mchEnrollmentDate != null && artStartDate != null) {
                    daysToMCHSinceARTStart = daysBetween(artStartDate, mchEnrollmentDate);
                    System.out.println("-----------------daysToMCHSinceARTStart: " + daysToMCHSinceARTStart);
                }
                if (artStartDate != null && lastPregStartDate != null) {
                    daysPregObsSinceARTStart = daysBetween(artStartDate, lastPregStartDate);
                    System.out.println("-----------------daysPregObsSinceARTStart: " + daysPregObsSinceARTStart);
                }

                if (artStartDate != null && lastBFStartDate != null) {
                    daysBFObsSinceARTStart = daysBetween(artStartDate, lastBFStartDate);
                    System.out.println("-----------------daysBFObsSinceARTStart: " + daysBFObsSinceARTStart);
                }

                // int daysToExcBFObsSinceARTStart = daysBetween(artStartDate, lastExclusiveBFStartDate);
                if (lastVLDate != null && mchEnrollmentDate != null) {
                    daysBtwLastVlAndMCHDate = daysBetween(mchEnrollmentDate, lastVLDate);
                    System.out.println("-----------------daysBtwLastVlAndMCHDate: " + daysBtwLastVlAndMCHDate);
                }
                if (lastVLDate != null && lastBFStartDate != null) {
                    daysBtwLastVlAndBFDate = daysBetween(lastBFStartDate, lastVLDate);
                    System.out.println("-----------------daysBtwLastVlAndBFDate: " + daysBtwLastVlAndBFDate);
                }
                // int daysBtwLastVlAndMxFeedingDate = lastVLResultDate != null ? daysBetween(lastMixedFeedingStartDate, lastVLResultDate) : null;
                if (lastVLDate != null && lastPregStartDate != null) {
                    daysBtwLastVlAndPregDate = daysBetween(lastPregStartDate, lastVLDate);
                    System.out.println("-----------------daysBtwLastVlAndPregDate: " + daysBtwLastVlAndPregDate);
                }
                if (lastVLDate != null && previousVLOrderDate != null && lastVLDate != previousVLOrderDate) {
                    daysBtwLastAndPrevVlDates = daysBetween(previousVLOrderDate, lastVLDate);
                    System.out.println("***********daysBtwLastAndPrevVlDates: " + daysBtwLastAndPrevVlDates);
                    System.out.println("-----------------daysBtwLastAndPrevVlDates: " + daysBtwLastAndPrevVlDates);
                }

                //Immediate: Pregnant and previously on ART
                if (daysPregObsSinceARTStart >= 92 && lastPregStartObs != null && lastPregStartObs.getValueCoded().equals(yes) && daysBtwLastVlAndPregDate >= 0 && daysBtwLastVlAndPregDate <= 30) {
                    System.out.println("---------------Line 229---->" + ptId + "-daysPregObsSinceARTStart:" + daysPregObsSinceARTStart + "-lastPregStartObs:" + lastPregStartObs + "-lastPregStartObs.getValueCoded().equals(yes):" + lastPregStartObs.getValueCoded().equals(yes) + "-daysBtwLastVlAndPregDate:" + daysBtwLastVlAndPregDate);
                    vlDoneWithinInterval = true;

                }
                //Immediate: Breastfeeding and previously on ART
                else if (daysBFObsSinceARTStart >= 92 && (daysBtwLastVlAndBFDate >= 0 && daysBtwLastVlAndBFDate <= 30)) {
                    System.out.println("---------------Line 235--->" + ptId + "-daysBFObsSinceARTStart:" + daysBFObsSinceARTStart + "-daysBtwLastVlAndBFDate:" + daysBtwLastVlAndBFDate);
                    vlDoneWithinInterval = true;
                }
                //Immediate: in MCH and previously on ART
                else if (activeInMCH.contains(ptId) && daysToMCHSinceARTStart >= 92 && daysBtwLastVlAndMCHDate >= 0 && daysBtwLastVlAndMCHDate <= 30) {
                    System.out.println("---------------Line 240--->" + ptId + "-activeInMCH.contains(ptId):" + activeInMCH.contains(ptId) + ":daysToMCHSinceARTStart:" + daysToMCHSinceARTStart + "-daysBtwLastVlAndMCHDate:" + daysBtwLastVlAndMCHDate);
                    vlDoneWithinInterval = true;
                }
                //After 3 months: All with unsuppressed VL (>200 cps/ml)
                else if (previousVLResultValue != null && daysBtwLastAndPrevVlDates > 0 && daysBtwLastAndPrevVlDates <= 92 && (int) previousVLResultValue.doubleValue() > 200) {
                    System.out.println("---------------Line 245--->" + ptId + "-previousVLResultValue:" + previousVLResultValue + "-daysBtwLastAndPrevVlDates:" + daysBtwLastAndPrevVlDates);
                    vlDoneWithinInterval = true;
                }
                //After 3 Months: New positives with no previous VL
                else if (daysBetweenARTStartAndLastVL >= 0 && daysBetweenARTStartAndLastVL <= 92 && allVLOrders.size() == 1) {
                    System.out.println("---------------Line 251--->" + ptId + "-daysBetweenARTStartAndLastVL:" + daysBetweenARTStartAndLastVL);
                    //System.out.println("---------------Line 251--->" + ptId + "-daysToMCHSinceARTStart:" + daysToMCHSinceARTStart + "-daysPregObsSinceARTStart:" + daysPregObsSinceARTStart + "-daysBFObsSinceARTStart:" + daysBFObsSinceARTStart + "-daysBtwLastVlAndMCHDate:" + daysBtwLastVlAndMCHDate + "-daysBtwLastVlAndBFDate:" + daysBtwLastVlAndBFDate + "-daysBtwLastVlAndPregDate:" + daysBtwLastVlAndPregDate);
                    vlDoneWithinInterval = true;
                }
                //After 6 months: In MCH program with a suppressed previous VL or BF with a suppressed previous VL or pregnant with a suppressed previous VL or 0-24 years old with a previous suppressed or LDLVL,
                //1. In MCH program
                else if (activeInMCH.contains(ptId) && lastVLDate != null && mchEnrollmentDate != null && lastVLDate.after(mchEnrollmentDate) && (previousVLOrderDate != null && daysBtwLastAndPrevVlDates < 183 && (previousVLResultLDL != null || (previousVLResultValue != null && previousVLResultValue < 200)))) {
                    System.out.println("---------------Line 257--->" + ptId + "-activeInMCH.contains(ptId):" + activeInMCH.contains(ptId) + "-lastVLDate:" + lastVLDate + "-mchEnrollmentDate:" + mchEnrollmentDate + "-previousVLOrderDate" + previousVLOrderDate + "-daysBtwLastAndPrevVlDates:" + daysBtwLastAndPrevVlDates + "-previousVLResultLDL:" + previousVLResultLDL + "-previousVLResultValue:" + previousVLResultValue);
                    vlDoneWithinInterval = true;
                }
                //2. BF with a suppressed VL
                else if ((lastBFObs != null && ((lastBFObs.getValueCoded().equals(mixedFeeding)) || lastBFObs.getValueCoded().equals(exclusiveBreastFeeding)) && lastBFStartDate != null && lastVLDate != null && lastVLDate.after(lastBFStartDate)) && (previousVLOrderDate != null && daysBtwLastAndPrevVlDates < 183 && (previousVLResultLDL != null || (previousVLResultValue != null && previousVLResultValue < 200)))) {
                    System.out.println("---------------Line 262--->" + ptId + "-lastBFObs:" + lastBFObs + "-lastBFObs.getValueCoded().equals(mixedFeeding):" + lastBFObs.getValueCoded().equals(mixedFeeding) + "-lastBFObs.getValueCoded().equals(exclusiveBreastFeeding)" + lastBFObs.getValueCoded().equals(exclusiveBreastFeeding) + "-lastBFStartDate:" + lastBFStartDate + "-previousVLOrderDate:" + previousVLOrderDate + "-daysBtwLastAndPrevVlDates" + daysBtwLastAndPrevVlDates + "-previousVLResultLDL:" + previousVLResultLDL + "-previousVLResultValue:" + previousVLResultValue);
                    vlDoneWithinInterval = true;
                }
                //3. Pregnant with a suppressed Previous VL
                else if (lastPregStartObs != null && lastPregStartObs.getValueCoded().equals(yes) && lastVLDate != null && lastVLDate.after(lastPregStartDate) && (previousVLOrderDate != null && daysBtwLastAndPrevVlDates < 183 && (previousVLResultLDL != null || (previousVLResultValue != null && previousVLResultValue < 200)))) {
                    System.out.println("---------------Line 267--->" + ptId + "-lastPregStartObs:" + lastPregStartObs + "-lastPregStartObs.getValueCoded().equals(yes)" + lastPregStartObs.getValueCoded().equals(yes) + "-lastVLDate" + lastVLDate + "-lastVLDate.after(lastPregStartDate):" + lastVLDate.after(lastPregStartDate) + "-previousVLOrderDate:" + previousVLOrderDate + "-daysBtwLastAndPrevVlDates:" + "-daysBtwLastAndPrevVlDates:" + daysBtwLastAndPrevVlDates + "-previousVLResultLDL:" + previousVLResultLDL + "-previousVLResultValue:" + previousVLResultValue);
                    vlDoneWithinInterval = true;
                }
                //4. 0-24 years old with a suppressed or LDL previous VL
                else if (patient.getAge() <= 24 && (lastVLDate != null && previousVLOrderDate != null && daysBtwLastAndPrevVlDates < 183 && (previousVLResultLDL != null || (previousVLResultValue != null && previousVLResultValue < 200)))) {
                    System.out.println("---------------Line 272--->" + ptId + "-patient.getAge():" + patient.getAge() + "-lastVLDate:" + lastVLDate + "-previousVLOrderDate:" + previousVLOrderDate + "-daysBtwLastAndPrevVlDates:" + daysBtwLastAndPrevVlDates + "-previousVLResultLDL:" + previousVLResultLDL + "-previousVLResultValue" + previousVLResultValue);
                    vlDoneWithinInterval = true;
                }
                //After 12 Months: > 25 years old with suppressed VL or LDL
                else if (/*lastPregStartObs == null && lastBFObs == null && */!activeInMCH.contains(ptId) && lastVLDate != null && previousVLOrderDate != null && daysBtwLastAndPrevVlDates > 0 && daysBtwLastAndPrevVlDates <= 366 && (previousVLResultLDL != null || (previousVLResultValue != null && previousVLResultValue < 200)) && patient.getAge() >= 25) {
                    System.out.println("---------------Line 277--->" + ptId + "-!activeInMCH.contains(ptId):" + !activeInMCH.contains(ptId) + "-lastVLDate" + lastVLDate + "-previousVLOrderDate:" + previousVLOrderDate + "-daysBtwLastAndPrevVlDates:" + daysBtwLastAndPrevVlDates + "-previousVLResultLDL:" + previousVLResultLDL + "-previousVLResultValue:" + previousVLResultValue + "-patient.getAge():" + patient.getAge());
                    vlDoneWithinInterval = true;
                }
                ret.put(ptId, new BooleanResult(vlDoneWithinInterval, this));
            }
        }
        return ret;
    }
}
