/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.util;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openmrs.Allergy;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Condition;
import org.openmrs.Diagnosis;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.Visit;
import org.openmrs.api.ConditionService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.DiagnosisService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.orderexpansion.api.ProcedureService;
import org.openmrs.module.orderexpansion.api.model.Procedure;
import org.openmrs.module.orderexpansion.api.util.HtmlToITextUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisitSummaryPdfGenerator {

    // SimpleDateFormat is not thread-safe — must be instance fields
    private final SimpleDateFormat dateFmt     = new SimpleDateFormat("dd-MMM-yyyy");
    private final SimpleDateFormat datetimeFmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
    private final SimpleDateFormat printFmt    = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

    private static final String WEIGHT               = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String HEIGHT               = "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String TEMPERATURE          = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String PULSE_RATE           = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String BP_SYSTOLIC          = "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String BP_DIASTOLIC         = "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String RESPIRATORY_RATE     = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String OXYGEN_SAT           = "5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String COMPLAINT_GROUP      = "160531AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String COMPLAINT_CONCEPT    = "5219AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String COMPLAINT_DURATION   = "159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String LAB_ORDER_CONCEPT    = "9a6f10d6-7fc5-4fb7-9428-24ef7b8d01f7";
    private static final String PROCEDURE_ORDER_TYPE = "b4a7c280-369e-4d12-9ce8-18e36783fed6";
    private static final String IMAGING_CLASS_UUID   = "8caa332c-efe4-4025-8b18-3398328e1323";

    private static final DeviceGray C_RULE = new DeviceGray(0.65f);

    private PdfFont regular;
    private PdfFont bold;


    public byte[] generate(Visit visit) throws IOException {
        regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        String printedBy = resolveCurrentUser();
        String printDate = printFmt.format(new Date());
        FacilityInfo facility = parseFacilityInfo();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        generateContent(visit, buf, facility);
        return addPageFooters(buf.toByteArray(), printedBy, printDate, facility);
    }

    private String resolveCurrentUser() {
        try {
            if (Context.getAuthenticatedUser() != null
                    && Context.getAuthenticatedUser().getPersonName() != null) {
                return Context.getAuthenticatedUser().getPersonName().getFullName();
            }
        } catch (Exception ignored) {
            // context unavailable — fall through to default
        }
        return "Unknown";
    }


    private void generateContent(Visit visit, OutputStream out, FacilityInfo facility) throws IOException {
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(18, 18, 48, 18); // compact margins; 48pt bottom reserved for two-line footer
        doc.setFont(regular).setFontSize(8);

        Patient patient = visit.getPatient();
        ConceptService cs = Context.getConceptService();
        Date visitEnd = visit.getStopDatetime() != null ? visit.getStopDatetime() : new Date();

        addPageHeader(doc, visit, patient, visitEnd, cs, facility);
        addDiagnoses(doc, visit);
        addComplaints(doc, visit, cs);
        addVitals(doc, visit, visitEnd, cs);
        addConditions(doc, visit, visitEnd, patient);
        addLabResults(doc, visit, visitEnd, cs);
        addAllergies(doc, visit, visitEnd, patient);
        addMedications(doc, visit);
        addProceduresAndImaging(doc, visit);
        addClinicalNotes(doc, visit);
        doc.close();
    }


    private byte[] addPageFooters(byte[] content, String printedBy, String printDate,
                                  FacilityInfo facility) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(
            new PdfReader(new ByteArrayInputStream(content)),
            new PdfWriter(out));
        int total = pdfDoc.getNumberOfPages();
        PdfFont footerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        for (int i = 1; i <= total; i++) {
            writeFooter(pdfDoc.getPage(i), pdfDoc, footerFont, printedBy, printDate, i, total, facility);
        }
        pdfDoc.close();
        return out.toByteArray();
    }

    private void writeFooter(PdfPage page, PdfDocument pdfDoc, PdfFont font,
                             String printedBy, String printDate, int pageNum, int total,
                             FacilityInfo facility) {
        Rectangle ps = page.getPageSize();
        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

        pdfCanvas.setStrokeColor(C_RULE).setLineWidth(0.5f)
                 .moveTo(18, 34).lineTo(ps.getWidth() - 18, 34).stroke();

        String left  = "Printed by: " + printedBy + "  •  " + printDate;
        String right = "Page " + pageNum + " of " + total;

        Canvas canvas = new Canvas(pdfCanvas, ps);
        canvas.setFont(font).setFontColor(ColorConstants.BLACK);
        canvas.setFontSize(6.5f);
        canvas.showTextAligned(left,  18, 22, TextAlignment.LEFT);
        canvas.showTextAligned(right, ps.getWidth() - 18, 22, TextAlignment.RIGHT);

        String facilityLine = buildFacilityFooterLine(facility);
        if (!facilityLine.isEmpty()) {
            canvas.setFontSize(5.5f);
            canvas.showTextAligned(facilityLine, 18, 13, TextAlignment.LEFT);
        }
        canvas.close();
    }

    private String buildFacilityFooterLine(FacilityInfo facility) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, facility.name,    "",        "");
        appendPart(sb, facility.address, ", ",      "");
        appendPart(sb, facility.tel,     "  |  ",   "Tel: ");
        appendPart(sb, facility.email,   "  |  ",   "");
        appendPart(sb, facility.web,     "  |  ",   "");
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String value, String sep, String prefix) {
        if (value == null || value.isEmpty()) return;
        if (sb.length() > 0) sb.append(sep);
        sb.append(prefix).append(value);
    }

    private void addPageHeader(Document doc, Visit visit, Patient patient, Date visitEnd,
                               ConceptService cs, FacilityInfo facility) {
        String visitDate = visit.getStartDatetime() != null
            ? dateFmt.format(visit.getStartDatetime()).toUpperCase() : "";
        doc.add(new Paragraph("VISIT SUMMARY  •  " + visitDate)
            .setFontSize(7).setFontColor(ColorConstants.BLACK).setMarginBottom(1));

        // Row 1: patient name (left) flush with facility name + address (right)
        String name = patient.getPersonName() != null ? patient.getPersonName().getFullName() : "";
        Table nameRow = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(2);

        nameRow.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .add(new Paragraph(name)
                .setFont(bold).setFontSize(11).setFontColor(ColorConstants.BLACK)));

        Paragraph facilityPara = new Paragraph()
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontColor(ColorConstants.BLACK);
        if (!facility.name.isEmpty()) {
            facilityPara.add(new Text(facility.name).setFont(bold).setFontSize(9));
        }
        if (!facility.address.isEmpty()) {
            if (!facility.name.isEmpty()) facilityPara.add(new Text("\n"));
            facilityPara.add(new Text(facility.address).setFont(regular).setFontSize(7));
        }
        nameRow.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .add(facilityPara));

        doc.add(nameRow);

        // Row 2: demographics line with bottom separator
        doc.add(new Paragraph(buildDemographicsLine(patient, visit, visitEnd, cs))
            .setFontSize(8).setFontColor(ColorConstants.BLACK)
            .setBorderBottom(new SolidBorder(C_RULE, 0.5f))
            .setMarginBottom(4).setPaddingBottom(3));
    }

    private String buildDemographicsLine(Patient patient, Visit visit, Date visitEnd, ConceptService cs) {
        StringBuilder sb = new StringBuilder();
        PatientIdentifier pid = patient.getPatientIdentifier();
        if (pid != null) sb.append("MRN: ").append(pid.getIdentifier()).append("    ");
        if (patient.getBirthdate() != null) {
            sb.append("DOB: ").append(dateFmt.format(patient.getBirthdate()))
              .append(" (").append(patient.getAge()).append("y)").append("    ");
        }
        sb.append("Gender: ").append("M".equals(patient.getGender()) ? "Male" : "Female").append("    ");
        String w = getLatestNumericObsValue(patient, visit.getStartDatetime(), visitEnd, cs, WEIGHT);
        if (w != null) sb.append("Weight: ").append(w).append(" kg");
        return sb.toString();
    }


    private void addDiagnoses(Document doc, Visit visit) {
        List<Diagnosis> list = Context.getService(DiagnosisService.class)
            .getDiagnosesByVisit(visit, false, false);
        if (list.isEmpty()) return;
        addSectionHeader(doc, "DIAGNOSES");
        for (Diagnosis d : list) {
            doc.add(bulletItem(buildDiagnosisLabel(d)));
        }
        doc.add(tiny());
    }

    private String buildDiagnosisLabel(Diagnosis d) {
        String name = d.getDiagnosis().getCoded() != null
            ? d.getDiagnosis().getCoded().getName().getName()
            : d.getDiagnosis().getNonCoded();
        if (name == null) name = "";
        String certainty = d.getCertainty() != null ? d.getCertainty().toString().toLowerCase() : "";
        return certainty.isEmpty() ? name : name + " (" + certainty + ")";
    }


    private void addComplaints(Document doc, Visit visit, ConceptService cs) {
        List<String> items = collectComplaintItems(visit, cs);
        if (items.isEmpty()) return;
        addSectionHeader(doc, "CHIEF COMPLAINTS");
        for (String item : items) doc.add(bulletItem(item));
        doc.add(tiny());
    }

    private List<String> collectComplaintItems(Visit visit, ConceptService cs) {
        Concept groupConcept = cs.getConceptByUuid(COMPLAINT_GROUP);
        Concept complaintCon = cs.getConceptByUuid(COMPLAINT_CONCEPT);
        Concept durationCon  = cs.getConceptByUuid(COMPLAINT_DURATION);
        if (groupConcept == null || complaintCon == null) return Collections.emptyList();

        EncounterType triageType = Context.getEncounterService()
            .getEncounterTypeByUuid(CommonMetadata._EncounterType.TRIAGE);
        List<String> items = new ArrayList<>();
        for (Encounter enc : visit.getEncounters()) {
            if (!isActiveEncounterOfType(enc, triageType)) continue;
            for (Obs top : enc.getObsAtTopLevel(false)) {
                String item = extractComplaintItem(top, groupConcept, complaintCon, durationCon);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    private boolean isActiveEncounterOfType(Encounter enc, EncounterType type) {
        return !Boolean.TRUE.equals(enc.getVoided())
            && (type == null || enc.getEncounterType().equals(type));
    }

    private String extractComplaintItem(Obs top, Concept groupConcept, Concept complaintCon, Concept durationCon) {
        if (!top.getConcept().equals(groupConcept) || !top.hasGroupMembers()) return null;
        String complaint = null;
        String duration  = null;
        for (Obs m : top.getGroupMembers(false)) {
            if (m.getConcept().equals(complaintCon)) {
                complaint = m.getValueCoded() != null
                    ? m.getValueCoded().getName().getName() : m.getValueText();
            } else if (durationCon != null && m.getConcept().equals(durationCon)
                       && m.getValueNumeric() != null) {
                duration = m.getValueNumeric().intValue() + " days";
            }
        }
        if (complaint == null) return null;
        return duration != null ? complaint + " (" + duration + ")" : complaint;
    }


    private void addVitals(Document doc, Visit visit, Date visitEnd, ConceptService cs) {
        VitalValues v = fetchVitals(visit, visitEnd, cs);
        if (v.isEmpty()) return;

        addSectionHeader(doc, "LATEST VITALS");

        // Compact 4-column grid: label + bold value side by side
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);

        addCompactVitalCell(table, "Temp",          v.temp,   "°C");
        addCompactVitalCell(table, "Pulse",          v.pulse,  "bpm");
        addCompactVitalCell(table, "Blood Pressure", v.bp,     "mmHg");
        addCompactVitalCell(table, "Resp. Rate",     v.rr,     "br/min");
        addCompactVitalCell(table, "SpO2",           v.spo2,   "%");
        addCompactVitalCell(table, "Weight",         v.weight, "kg");
        addCompactVitalCell(table, "Height",         v.height, "cm");
        addCompactVitalCell(table, "BMI",            v.bmi,    "kg/m²");
        doc.add(table);
    }

    private void addCompactVitalCell(Table table, String label, String value, String unit) {
        Paragraph p = new Paragraph()
            .add(new Text(label + ": ").setFontSize(7))
            .add(new Text(value != null ? value + " " + unit : "—").setFont(bold).setFontSize(8));
        table.addCell(new Cell()
            .setBorderTop(new SolidBorder(C_RULE, 0.5f))
            .setBorderBottom(new SolidBorder(C_RULE, 0.5f))
            .setBorderLeft(Border.NO_BORDER)
            .setBorderRight(Border.NO_BORDER)
            .setPaddingTop(3).setPaddingBottom(3)
            .add(p));
    }

    private VitalValues fetchVitals(Visit visit, Date visitEnd, ConceptService cs) {
        Patient p  = visit.getPatient();
        Date from  = visit.getStartDatetime();
        VitalValues v = new VitalValues();
        v.temp   = getLatestNumericObsValue(p, from, visitEnd, cs, TEMPERATURE);
        v.pulse  = getLatestNumericObsValue(p, from, visitEnd, cs, PULSE_RATE);
        v.rr     = getLatestNumericObsValue(p, from, visitEnd, cs, RESPIRATORY_RATE);
        v.spo2   = getLatestNumericObsValue(p, from, visitEnd, cs, OXYGEN_SAT);
        v.weight = getLatestNumericObsValue(p, from, visitEnd, cs, WEIGHT);
        v.height = getLatestNumericObsValue(p, from, visitEnd, cs, HEIGHT);
        String sbp = getLatestNumericObsValue(p, from, visitEnd, cs, BP_SYSTOLIC);
        String dbp = getLatestNumericObsValue(p, from, visitEnd, cs, BP_DIASTOLIC);
        v.bp  = (sbp != null && dbp != null) ? sbp + "/" + dbp : sbp;
        v.bmi = calculateBmi(v.weight, v.height);
        return v;
    }

    private static class VitalValues {
        String temp;
        String pulse;
        String bp;
        String rr;
        String spo2;
        String weight;
        String height;
        String bmi;

        boolean isEmpty() {
            return temp == null && pulse == null && bp == null
                && rr == null && spo2 == null && weight == null && height == null;
        }
    }

    private String calculateBmi(String weight, String height) {
        if (weight == null || height == null) return null;
        try {
            double w = Double.parseDouble(weight);
            double h = Double.parseDouble(height) / 100.0;
            return String.format("%.0f", w / (h * h));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private void addConditions(Document doc, Visit visit, Date visitEnd, Patient patient) {
        List<Condition> inVisit = getConditionsForVisit(visit, visitEnd, patient);
        if (inVisit.isEmpty()) return;
        addSectionHeader(doc, "CONDITIONS");
        Table table = buildTable(new float[]{55, 30, 15}, "Condition", "Date Noted", "Status");
        for (Condition c : inVisit) {
            table.addCell(dataCell(getConditionName(c)));
            String dateStr = c.getOnsetDate() != null ? datetimeFmt.format(c.getOnsetDate()) : "";
            String status  = c.getClinicalStatus() != null ? c.getClinicalStatus().toString() : "";
            table.addCell(dataCell(dateStr));
            table.addCell(dataCell(status));
        }
        doc.add(table);
    }

    private List<Condition> getConditionsForVisit(Visit visit, Date visitEnd, Patient patient) {
        List<Condition> all = Context.getService(ConditionService.class).getActiveConditions(patient);
        List<Condition> result = new ArrayList<>();
        for (Condition c : all) {
            Date ref = c.getOnsetDate() != null ? c.getOnsetDate() : c.getDateCreated();
            if (ref != null && !ref.before(visit.getStartDatetime()) && !ref.after(visitEnd)) {
                result.add(c);
            }
        }
        return result;
    }

    private String getConditionName(Condition c) {
        String name = c.getCondition().getCoded() != null
            ? c.getCondition().getCoded().getName().getName()
            : c.getCondition().getNonCoded();
        return name != null ? name : "";
    }


    private void addLabResults(Document doc, Visit visit, Date visitEnd, ConceptService cs) {
        List<Obs> valid = fetchLabObs(visit, visitEnd, cs);
        if (valid.isEmpty()) return;
        addSectionHeader(doc, "LAB RESULTS");
        Table table = buildTable(new float[]{50, 30, 20}, "Test", "Result", "Ref Range");
        Set<Integer> addedPanels = new HashSet<>();
        for (Obs obs : valid) {
            if (obs.hasGroupMembers()) {
                renderLabPanel(table, obs, addedPanels, cs);
            } else if (obs.getObsGroup() == null) {
                addLabRow(table, obs, cs);
            }
        }
        doc.add(table);
    }

    private List<Obs> fetchLabObs(Visit visit, Date visitEnd, ConceptService cs) {
        Concept labOrder = cs.getConceptByUuid(LAB_ORDER_CONCEPT);
        if (labOrder == null) return Collections.emptyList();
        List<Concept> concepts = labOrder.getSetMembers();
        if (concepts.isEmpty()) return Collections.emptyList();
        Person person = Context.getPersonService().getPerson(visit.getPatient().getPersonId());
        List<Obs> raw = Context.getObsService().getObservations(
            Collections.singletonList(person), null, concepts, null, null, null,
            null, null, null, visit.getStartDatetime(), visitEnd, false);
        List<Obs> valid = new ArrayList<>();
        for (Obs o : raw) {
            if (!Boolean.TRUE.equals(o.getVoided())) valid.add(o);
        }
        return valid;
    }

    private void renderLabPanel(Table table, Obs panel, Set<Integer> seen, ConceptService cs) {
        if (seen.contains(panel.getObsId())) return;
        seen.add(panel.getObsId());
        table.addCell(new Cell(1, 3)
            .setBorder(new SolidBorder(C_RULE, 0.5f))
            .setPadding(3)
            .add(new Paragraph(panel.getConcept().getName().getName().toUpperCase())
                .setFont(bold).setFontSize(7)));
        for (Obs child : panel.getGroupMembers(false)) {
            if (!Boolean.TRUE.equals(child.getVoided())) addLabRow(table, child, cs);
        }
    }

    private void addLabRow(Table table, Obs obs, ConceptService cs) {
        LabRowData d = buildLabRowData(obs, cs);
        table.addCell(dataCell(d.testName));
        table.addCell(labResultCell(d));
        table.addCell(dataCell(d.refRange));
    }

    private static class LabRowData {
        String testName;
        String value;
        String refRange  = "—";
        String interpCode;
    }

    private LabRowData buildLabRowData(Obs obs, ConceptService cs) {
        LabRowData d = new LabRowData();
        d.testName = obs.getConcept().getName().getName();
        d.value    = obsValueAsString(obs);
        if (obs.getConcept().isNumeric() && obs.getValueNumeric() != null) {
            ConceptNumeric cn = cs.getConceptNumeric(obs.getConcept().getConceptId());
            if (cn != null) enrichLabRowData(d, obs, cn);
        }
        return d;
    }

    private void enrichLabRowData(LabRowData d, Obs obs, ConceptNumeric cn) {
        if (cn.getUnits() != null && !cn.getUnits().isEmpty()) {
            d.value = d.value + " " + cn.getUnits();
        }
        if (cn.getLowNormal() != null || cn.getHiNormal() != null) {
            String lo = cn.getLowNormal() != null ? formatNum(cn.getLowNormal()) : "";
            String hi = cn.getHiNormal() != null ? formatNum(cn.getHiNormal()) : "";
            d.refRange = lo + " – " + hi;
            d.interpCode = interpCode(obs.getValueNumeric(),
                cn.getLowCritical(), cn.getLowNormal(), cn.getHiNormal(), cn.getHiCritical());
        }
    }

    private Cell labResultCell(LabRowData d) {
        String indicator = interpIndicator(d.interpCode);
        Paragraph p = new Paragraph().add(new Text(d.value));
        if (!indicator.isEmpty()) {
            p.add(new Text("  " + indicator).setFont(bold).setFontSize(6.5f));
        }
        return new Cell().setBorder(new SolidBorder(C_RULE, 0.5f)).setPadding(2).add(p.setFontSize(8));
    }

    private String interpIndicator(String code) {
        if ("HH".equals(code)) return "⇑⇑ Critical High";
        if ("H".equals(code))  return "↑ High";
        if ("LL".equals(code)) return "⇓⇓ Critical Low";
        if ("L".equals(code))  return "↓ Low";
        return "";
    }


    private void addAllergies(Document doc, Visit visit, Date visitEnd, Patient patient) {
        List<Allergy> inVisit = getAllergiesForVisit(visit, visitEnd, patient);
        if (inVisit.isEmpty()) return;
        addSectionHeader(doc, "ALLERGIES");
        Table table = buildTable(new float[]{70, 30}, "Allergen", "Severity");
        for (Allergy a : inVisit) {
            table.addCell(dataCell(getAllergenName(a)));
            table.addCell(dataCell(getAllergySeverity(a)));
        }
        doc.add(table);
    }

    private List<Allergy> getAllergiesForVisit(Visit visit, Date visitEnd, Patient patient) {
        List<Allergy> all = Context.getPatientService().getAllergies(patient);
        List<Allergy> result = new ArrayList<>();
        for (Allergy a : all) {
            Date recorded = a.getDateCreated();
            if (recorded != null && !recorded.before(visit.getStartDatetime()) && !recorded.after(visitEnd)) {
                result.add(a);
            }
        }
        return result;
    }

    private String getAllergenName(Allergy a) {
        String name = a.getAllergen().getCodedAllergen() != null
            ? a.getAllergen().getCodedAllergen().getName().getName()
            : a.getAllergen().getNonCodedAllergen();
        return name != null ? name : "";
    }

    private String getAllergySeverity(Allergy a) {
        if (a.getSeverity() == null || a.getSeverity().getName() == null) return "";
        return a.getSeverity().getName().getName();
    }


    private void addMedications(Document doc, Visit visit) {
        List<DrugOrder> drugs = collectDrugOrders(visit);
        if (drugs.isEmpty()) return;
        addSectionHeader(doc, "MEDICATIONS");
        Table table = buildTable(new float[]{55, 30, 15}, "Drug", "Details", "Status");
        for (DrugOrder d : drugs) {
            table.addCell(dataCell(resolveDrugName(d)));
            table.addCell(dataCell(buildMedicationDetails(d)));
            table.addCell(dataCell(isMedicationActive(d) ? "Active" : "Stopped"));
        }
        doc.add(table);
    }

    private List<DrugOrder> collectDrugOrders(Visit visit) {
        List<DrugOrder> result = new ArrayList<>();
        for (Encounter enc : visit.getEncounters()) {
            if (Boolean.TRUE.equals(enc.getVoided())) continue;
            for (Order o : enc.getOrders()) {
                if (!Boolean.TRUE.equals(o.getVoided()) && o instanceof DrugOrder) {
                    result.add((DrugOrder) o);
                }
            }
        }
        return result;
    }

    private String resolveDrugName(DrugOrder d) {
        if (d.getDrug() != null) return d.getDrug().getName();
        if (d.getConcept() != null) return d.getConcept().getName().getName();
        return "";
    }

    private String buildMedicationDetails(DrugOrder d) {
        StringBuilder sb = new StringBuilder();
        if (d.getDose() != null) {
            sb.append(d.getDose());
            if (d.getDoseUnits() != null) sb.append(" ").append(d.getDoseUnits().getName().getName());
        }
        if (d.getFrequency() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(d.getFrequency().getName());
        }
        if (d.getRoute() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(d.getRoute().getName().getName());
        }
        return sb.toString();
    }

    private boolean isMedicationActive(DrugOrder d) {
        return d.getDateStopped() == null
            && (d.getAutoExpireDate() == null || !d.getAutoExpireDate().before(new Date()));
    }


    private void addProceduresAndImaging(Document doc, Visit visit) {
        List<Order> procedures = new ArrayList<>();
        List<Order> imaging    = new ArrayList<>();
        for (Encounter enc : visit.getEncounters()) {
            if (!Boolean.TRUE.equals(enc.getVoided())) {
                collectOrdersByType(enc, procedures, imaging);
            }
        }
        if (procedures.isEmpty() && imaging.isEmpty()) return;
        addSectionHeader(doc, "PROCEDURES & IMAGING");
        for (Order o : procedures) addOrderBlock(doc, o, false);
        for (Order o : imaging)    addOrderBlock(doc, o, true);
    }

    private void collectOrdersByType(Encounter enc, List<Order> procedures, List<Order> imaging) {
        for (Order o : enc.getOrders()) {
            if (!Boolean.TRUE.equals(o.getVoided())
                    && o.getOrderType() != null
                    && PROCEDURE_ORDER_TYPE.equals(o.getOrderType().getUuid())) {
                if (isImagingOrder(o)) imaging.add(o);
                else procedures.add(o);
            }
        }
    }

    private void addOrderBlock(Document doc, Order order, boolean isImaging) {
        doc.add(new Paragraph(order.getConcept().getName().getName().toUpperCase())
            .setFont(bold).setFontSize(9).setMarginBottom(1).setMarginTop(2));
        doc.add(new Paragraph(buildOrderSubtitle(order))
            .setFontSize(7).setMarginBottom(2));
        Procedure procedure = getLinkedProcedure(order);
        if (procedure != null) renderProcedureReport(doc, procedure, isImaging);
    }

    private String buildOrderSubtitle(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getOrderer() != null) sb.append(order.getOrderer().getName());
        if (order.getDateActivated() != null) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(dateFmt.format(order.getDateActivated()));
        }
        if (order.getFulfillerStatus() != null) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(order.getFulfillerStatus().name());
        }
        return sb.toString();
    }

    private void renderProcedureReport(Document doc, Procedure procedure, boolean isImaging) {
        String report = procedure.getProcedureReport();
        if (report != null && !report.isEmpty()) {
            doc.add(new Paragraph(isImaging ? "Report:" : "Procedure Report:")
                .setFont(bold).setFontSize(7).setMarginBottom(1));
            addHtmlAwareTextBox(doc, report);
        }
        if (!isImaging) return;
        String impression = procedure.getImpressions();
        if (impression != null && !impression.isEmpty()) {
            doc.add(new Paragraph("Impression:")
                .setFont(bold).setFontSize(7).setMarginTop(2).setMarginBottom(1));
            addHtmlAwareTextBox(doc, impression);
        }
    }


    private void addClinicalNotes(Document doc, Visit visit) {
        String notes = buildClinicalNotesText(visit);
        if (notes.isEmpty()) return;
        addSectionHeader(doc, "CLINICAL NOTES & PLAN");
        doc.add(new Paragraph(notes).setFontSize(8).setMarginBottom(4));
    }

    private String buildClinicalNotesText(Visit visit) {
        Set<String> noteTypes = new HashSet<>(Arrays.asList(
            CommonMetadata._EncounterType.CONSULTATION,
            CommonMetadata._EncounterType.NURSING_CARE_PLAN));
        StringBuilder sb = new StringBuilder();
        for (Encounter enc : visit.getEncounters()) {
            if (!Boolean.TRUE.equals(enc.getVoided())
                    && noteTypes.contains(enc.getEncounterType().getUuid())) {
                appendEncounterNotes(sb, enc);
            }
        }
        return sb.toString();
    }

    private void appendEncounterNotes(StringBuilder sb, Encounter enc) {
        for (Obs obs : enc.getObs()) {
            String val = obs.getValueText();
            if (!Boolean.TRUE.equals(obs.getVoided()) && val != null && !val.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(obs.getConcept().getName().getName()).append(": ").append(val);
            }
        }
    }


    /** Bold uppercase section title with a full-width rule below — no ink fill. */
    private void addSectionHeader(Document doc, String title) {
        doc.add(new Paragraph(title)
            .setFont(bold).setFontSize(8)
            .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 0.75f))
            .setMarginTop(4).setMarginBottom(3).setPaddingBottom(1));
    }

    /** Compact bullet-point item. */
    private Paragraph bulletItem(String text) {
        return new Paragraph("•  " + text).setFontSize(8).setMarginLeft(6).setMarginBottom(1);
    }

    /** Bordered text block for plain text content. */
    private void addTextBox(Document doc, String text) {
        doc.add(new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(2)
            .addCell(new Cell()
                .setBorder(new SolidBorder(C_RULE, 0.5f))
                .setPadding(4)
                .add(new Paragraph(text).setFontSize(8))));
    }

    /**
     * Bordered text box that handles HTML content from procedure reports.
     * Uses HtmlToITextUtil only for String→String operations (safe across
     * module classloader boundaries): entities are decoded and HTML tags are
     * stripped before rendering, so no cross-classloader iText objects are used.
     */
    private void addHtmlAwareTextBox(Document doc, String raw) {
        // stripTags: iteratively decodes HTML entities then extracts plain text via Jsoup.
        // All types that cross the classloader boundary are plain Strings.
        String plain = HtmlToITextUtil.stripTags(raw);
        addTextBox(doc, plain.isEmpty() ? HtmlToITextUtil.decodeHtmlEntities(raw) : plain);
    }

    /** Creates a table pre-loaded with header cells. */
    private Table buildTable(float[] widths, String... headers) {
        Table table = new Table(UnitValue.createPercentArray(widths))
            .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);
        for (String h : headers) {
            table.addCell(new Cell()
                .setBorder(new SolidBorder(C_RULE, 0.5f))
                .setPadding(2)
                .add(new Paragraph(h).setFont(bold).setFontSize(7)));
        }
        return table;
    }

    private Cell dataCell(String text) {
        return new Cell().setBorder(new SolidBorder(C_RULE, 0.5f)).setPadding(2)
            .add(new Paragraph(text != null ? text : "").setFontSize(8));
    }

    /** Small vertical gap without visible content. */
    private Paragraph tiny() {
        return new Paragraph("").setFontSize(3).setMarginBottom(0);
    }


    private String getLatestNumericObsValue(Patient patient, Date from, Date to,
                                            ConceptService cs, String uuid) {
        Concept concept = cs.getConceptByUuid(uuid);
        if (concept == null) return null;
        Person person = Context.getPersonService().getPerson(patient.getPersonId());
        List<Obs> obs = Context.getObsService().getObservations(
            Collections.singletonList(person), null, Collections.singletonList(concept),
            null, null, null, Collections.singletonList("obsId"), null, null, from, to, false);
        if (obs.isEmpty() || obs.get(0).getValueNumeric() == null) return null;
        return formatNum(obs.get(0).getValueNumeric());
    }

    private String obsValueAsString(Obs obs) {
        if (obs.getValueNumeric() != null)  return formatNum(obs.getValueNumeric());
        if (obs.getValueCoded() != null)    return obs.getValueCoded().getName().getName();
        if (obs.getValueText() != null)     return obs.getValueText();
        if (obs.getValueDate() != null)     return dateFmt.format(obs.getValueDate());
        if (obs.getValueDatetime() != null) return datetimeFmt.format(obs.getValueDatetime());
        return "";
    }

    private String formatNum(double v) {
        return (v == Math.floor(v) && !Double.isInfinite(v)) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private String interpCode(double value, Double ll, Double l, Double h, Double hh) {
        if (ll != null && value < ll) return "LL";
        if (l  != null && value < l)  return "L";
        if (hh != null && value > hh) return "HH";
        if (h  != null && value > h)  return "H";
        return "N";
    }

    private boolean isImagingOrder(Order order) {
        return order.getConcept() != null
            && order.getConcept().getConceptClass() != null
            && IMAGING_CLASS_UUID.equals(order.getConcept().getConceptClass().getUuid());
    }

    private Procedure getLinkedProcedure(Order order) {
        try {
            List<Procedure> list = Context.getService(ProcedureService.class)
                .getProceduresByOrderUuid(order.getUuid());
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception ignored) {
            // orderexpansion module may not be available
            return null;
        }
    }


    private FacilityInfo parseFacilityInfo() {
        FacilityInfo info = new FacilityInfo();
        try {
            String json = Context.getAdministrationService()
                .getGlobalProperty("kenyaemr.cashier.receipt.facilityInformation");
            if (json == null || json.isEmpty()) return info;
            JsonNode root = new ObjectMapper().readTree(json);
            if (root.has("facilityName")) info.name = root.get("facilityName").asText("");
            JsonNode contacts = root.path("contacts");
            if (!contacts.isMissingNode()) {
                info.address = contacts.path("address").asText("");
                info.tel     = contacts.path("tel").asText("");
                info.email   = contacts.path("email").asText("");
                info.web     = contacts.path("web").asText("");
            }
        } catch (Exception ignored) {
            // global property not set or JSON malformed — return empty info
        }
        return info;
    }

    private static class FacilityInfo {
        String name    = "";
        String address = "";
        String tel     = "";
        String email   = "";
        String web     = "";
    }
}
