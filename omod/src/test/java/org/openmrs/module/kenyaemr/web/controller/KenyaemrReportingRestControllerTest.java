/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.kenyaemr.web.controller;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.DateOfEnrollmentArtCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.LastViralLoadResultCalculation;
import org.openmrs.module.reportingcompatibility.service.ReportingCompatibilityService;
import java.util.Collections;
import org.mockito.Mockito;
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.MapDataSet;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.indicator.dimension.CohortIndicatorAndDimensionResult;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KenyaemrReportingRestControllerTest {

	private final KenyaemrReportingRestController controller = new KenyaemrReportingRestController();

	@Test
	public void shouldRejectInvalidPaginationBeforeLoadingReport() {
		assertEquals(HttpStatus.BAD_REQUEST, controller.getReportPatients(1, "ds", "col", -1, 50).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, controller.getReportPatients(1, "ds", "col", 0, 0).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, controller.getReportPatients(1, "ds", "col", 0, 201).getStatusCode());
	}

	@Test
	public void shouldResolveBothSavedCohortRepresentations() {
		Cohort cohort = new Cohort();
		assertSame(cohort, KenyaemrReportingRestController.getResultCohort(cohort));
		CohortIndicatorAndDimensionResult result = mock(CohortIndicatorAndDimensionResult.class);
		when(result.getCohortIndicatorAndDimensionCohort()).thenReturn(cohort);
		assertSame(cohort, KenyaemrReportingRestController.getResultCohort(result));
		assertNull(KenyaemrReportingRestController.getResultCohort(12));
		assertNull(KenyaemrReportingRestController.getResultCohort(null));
	}

	@Test
	public void shouldRequireExistingCompletedRequest() {
		try (MockedStatic<Context> context = Mockito.mockStatic(Context.class)) {
			ReportService service = mock(ReportService.class);
			context.when(() -> Context.getService(ReportService.class)).thenReturn(service);
			assertEquals(HttpStatus.NOT_FOUND, controller.getReportPatients(1, "ds", "col", 0, 50).getStatusCode());
			ReportRequest request = new ReportRequest();
			request.setStatus(ReportRequest.Status.PROCESSING);
			when(service.getReportRequest(1)).thenReturn(request);
			assertEquals(HttpStatus.CONFLICT, controller.getReportPatients(1, "ds", "col", 0, 50).getStatusCode());
			verify(service, never()).loadReportData(any(ReportRequest.class));
		}
	}

	@Test
	public void shouldReturnEmptyPageWithoutLoadingPatientsAndRejectMissingColumn() {
		try (MockedStatic<Context> context = Mockito.mockStatic(Context.class)) {
			ReportService service = mock(ReportService.class);
			context.when(() -> Context.getService(ReportService.class)).thenReturn(service);
			ReportRequest request = new ReportRequest();
			request.setStatus(ReportRequest.Status.COMPLETED);
			when(service.getReportRequest(1)).thenReturn(request);
			ReportData data = new ReportData(new ReportDefinition(), new EvaluationContext());
			MapDataSet dataset = new MapDataSet(null, new EvaluationContext());
			dataset.addData(new DataSetColumn("col", "Patients", Cohort.class), new Cohort());
			dataset.addData(new DataSetColumn("number", "Count only", Integer.class), 12);
			data.getDataSets().put("ds", dataset);
			when(service.loadReportData(request)).thenReturn(data);
			ResponseEntity<?> response = controller.getReportPatients(1, "ds", "col", Integer.MAX_VALUE, 50);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			SimpleObject body = (SimpleObject) response.getBody();
			assertEquals(Integer.valueOf(0), body.get("totalCount"));
			assertEquals(false, body.get("hasMore"));
			assertTrue(((List<?>) body.get("results")).isEmpty());
			assertEquals(HttpStatus.NOT_FOUND, controller.getReportPatients(1, "ds", "missing", 0, 50).getStatusCode());
			assertEquals(HttpStatus.BAD_REQUEST, controller.getReportPatients(1, "ds", "number", 0, 50).getStatusCode());
		}
	}
	@Test
	public void shouldPageSavedMembersInPatientIdOrderAndReturnPatientUuid() {
		try (MockedStatic<Context> context = Mockito.mockStatic(Context.class);
				MockedConstruction<DateOfEnrollmentArtCalculation> enrollment = mockConstruction(
						DateOfEnrollmentArtCalculation.class, (mock, construction) ->
						when(mock.evaluate(anyCollection(), isNull(), any(PatientCalculationContext.class)))
								.thenReturn(new CalculationResultMap()));
				MockedConstruction<InitialArtStartDateCalculation> art = mockConstruction(
						InitialArtStartDateCalculation.class, (mock, construction) ->
						when(mock.evaluate(anyCollection(), isNull(), any(PatientCalculationContext.class)))
								.thenReturn(new CalculationResultMap()));
				MockedConstruction<LastViralLoadResultCalculation> vl = mockConstruction(
						LastViralLoadResultCalculation.class, (mock, construction) ->
						when(mock.evaluate(anyCollection(), isNull(), any(PatientCalculationContext.class)))
								.thenReturn(new CalculationResultMap()))) {
			ReportService service = mock(ReportService.class);
			ReportingCompatibilityService patients = mock(ReportingCompatibilityService.class);
			PatientCalculationService calculations = mock(PatientCalculationService.class);
			context.when(() -> Context.getService(ReportService.class)).thenReturn(service);
			context.when(() -> Context.getService(ReportingCompatibilityService.class)).thenReturn(patients);
			context.when(() -> Context.getService(PatientCalculationService.class)).thenReturn(calculations);
			when(calculations.createCalculationContext()).thenReturn(mock(PatientCalculationContext.class));
			ReportRequest request = new ReportRequest();
			request.setStatus(ReportRequest.Status.COMPLETED);
			when(service.getReportRequest(1)).thenReturn(request);
			ReportData data = new ReportData(new ReportDefinition(), new EvaluationContext());
			Cohort cohort = new Cohort();
			cohort.addMember(9);
			cohort.addMember(2);
			cohort.addMember(5);
			MapDataSet dataset = new MapDataSet(null, new EvaluationContext());
			dataset.addData(new DataSetColumn("col", "Patients", Cohort.class), cohort);
			data.getDataSets().put("ds", dataset);
			when(service.loadReportData(request)).thenReturn(data);
			Patient patient = new Patient(5);
			patient.setUuid("patient-five");
			patient.addName(new PersonName("Test", null, "Patient"));
			when(patients.getPatients(Collections.singleton(5))).thenReturn(Collections.singletonList(patient));
			SimpleObject body = (SimpleObject) controller.getReportPatients(1, "ds", "col", 1, 1).getBody();
			assertEquals(Integer.valueOf(3), body.get("totalCount"));
			assertEquals(true, body.get("hasMore"));
			List<SimpleObject> rows = (List<SimpleObject>) body.get("results");
			assertEquals(1, rows.size());
			assertEquals("patient-five", rows.get(0).get("uuid"));
			assertNull(rows.get(0).get("lastViralLoad"));
			verify(patients).getPatients(Collections.singleton(5));
			verify(enrollment.constructed().get(0)).evaluate(eq(Collections.singleton(5)), isNull(),
					any(PatientCalculationContext.class));
		}
	}

}
