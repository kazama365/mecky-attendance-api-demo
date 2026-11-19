package com.mecky.attendance.service;

import com.fasterxml.jackson.databind.*;
import com.mecky.attendance.api.ApiDtos.Scan;
import com.mecky.attendance.config.JwtService;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@org.springframework.test.context.TestExecutionListeners(listeners = {org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class, org.springframework.test.context.transaction.TransactionalTestExecutionListener.class}, mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional
class ManagementWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired EmployeeRepository employees;
    @Autowired ScheduleRepository schedules;
    @Autowired AttendanceRepository attendance;
    @Autowired DeductionPolicyRepository policies;
    @Autowired PayrollRepository payroll;
    @Autowired PayrollCalculator calculator;
    @Autowired AttendanceService scanner;

    private String admin() { return "Bearer " + jwt.issue("admin", "ADMIN"); }
    private Employee employee(String id, int slot) {
        Employee e = new Employee();
        e.employeeId = id;
        e.fullName = id;
        e.sensorSlotId = slot;
        e.dailyRate = new BigDecimal("620.00");
        return employees.saveAndFlush(e);
    }

    private void lateDay(Employee e, LocalDate date, int minutes) {
        Attendance row = new Attendance(e, date);
        row.timeIn = date.atTime(9, 5).plusMinutes(minutes);
        row.lateMinutes = minutes;
        row.status = "LATE";
        attendance.saveAndFlush(row);
    }

    private DeductionPolicy policy(LocalDate date, String rate) {
        DeductionPolicy p = new DeductionPolicy();
        p.effectiveDate = date;
        p.ratePerMinute = new BigDecimal(rate);
        p.createdBy = "admin";
        return policies.saveAndFlush(p);
    }

    @Test void reactivationRetainsTemplateAndDeviceCanScanAgain() throws Exception {
        Employee e = employee("TEST-REACTIVATE", 901);
        LocalDate date = LocalDate.now();
        mvc.perform(delete("/api/employees/" + e.employeeId).header("Authorization", admin())).andExpect(status().isOk());
        assertEquals("UNREGISTERED", scanner.process(new Scan(UUID.randomUUID().toString(), 901, date.atTime(9,0))).status());
        mvc.perform(get("/api/employees?active=false").header("Authorization", admin()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/employees/" + e.employeeId + "/reactivate").header("Authorization", admin()))
                .andExpect(status().isOk());
        assertEquals(901, employees.findById(e.employeeId).orElseThrow().sensorSlotId);
        assertEquals("TIME_IN", scanner.process(new Scan(UUID.randomUUID().toString(), 901, date.atTime(9,1))).status());
    }

    @Test void lateScanRetryDoesNotAccumulateMinutesOrDeductions() {
        Employee e = employee("TEST-RETRY", 902);
        schedules.saveAndFlush(new WorkSchedule(e.employeeId));
        Scan scan = new Scan(UUID.randomUUID().toString(), 902, LocalDate.now().atTime(9,18));
        assertEquals("TIME_IN", scanner.process(scan).status());
        assertEquals("DUPLICATE", scanner.process(scan).status());
        assertEquals(13, attendance.findByEmployeeEmployeeIdAndAttendanceDate(e.employeeId, LocalDate.now()).orElseThrow().lateMinutes);
        policy(LocalDate.now(), "2.50");
        var first = calculator.calculate(List.of(e), LocalDate.now(), LocalDate.now()).get(0);
        var second = calculator.calculate(List.of(e), LocalDate.now(), LocalDate.now()).get(0);
        assertEquals(new BigDecimal("32.50"), first.lateDeduction);
        assertEquals(first.netPay, second.netPay);
    }

    @Test void policyEffectiveDatesAndSavedSnapshotStayFixed() throws Exception {
        Employee e = employee("TEST-SNAPSHOT", 903);
        LocalDate from = LocalDate.of(2025,1,1), to = from.plusDays(2);
        lateDay(e, from, 10);
        lateDay(e, to, 20);
        policy(from, "1.00");
        policy(from.plusDays(1), "2.00");
        var calculated = calculator.calculate(List.of(e), from, to).get(0);
        assertEquals(new BigDecimal("50.00"), calculated.lateDeduction);
        String request = "{\"from\":\"2025-01-01\",\"to\":\"2025-01-03\"}";
        mvc.perform(post("/api/payroll/run").header("Authorization", admin()).contentType("application/json").content(request))
                .andExpect(status().isOk());
        policy(to, "99.00");
        assertNotEquals(calculated.lateDeduction, calculator.calculate(List.of(e), from, to).get(0).lateDeduction);
        e.dailyRate = new BigDecimal("999.00");
        employees.saveAndFlush(e);
        String response = mvc.perform(get("/api/payroll/preview?from=2025-01-01&to=2025-01-03").header("Authorization", admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var rows = json.readTree(response).get("rows");
        JsonNode row = null;
        for (JsonNode item : rows) if (item.get("employeeId").asText().equals(e.employeeId)) row = item;
        assertNotNull(row);
        assertEquals(620, row.get("dailyRate").asInt());
        assertEquals(50, row.get("lateDeduction").asInt());
        long count = payroll.count();
        mvc.perform(post("/api/payroll/run").header("Authorization", admin()).contentType("application/json").content(request))
                .andExpect(status().isOk());
        assertEquals(count, payroll.count());
    }

    @Test void missingPolicyBlocksSavingAndFormulaNeedsConfirmation() throws Exception {
        Employee e = employee("TEST-PENDING", 904);
        LocalDate date = LocalDate.of(2024,1,1);
        lateDay(e, date, 25);
        assertTrue(PayrollCalculator.awaitingPolicy(calculator.calculate(List.of(e), date, date).get(0)));
        mvc.perform(post("/api/payroll/run").header("Authorization", admin()).contentType("application/json")
                .content("{\"from\":\"2024-01-01\",\"to\":\"2024-01-01\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/payroll/policies").header("Authorization", admin()).contentType("application/json")
                .content("{\"effectiveDate\":\"2024-01-01\",\"ratePerMinute\":1.25,\"confirmed\":false}")).andExpect(status().isBadRequest());
    }

    @Test void dashboardUsesDatabaseAndBoundsItsRange() throws Exception {
        Employee e = employee("TEST-GRAPH", 905);
        lateDay(e, LocalDate.now(), 8);
        String response = mvc.perform(get("/api/dashboard?from=" + LocalDate.now() + "&to=" + LocalDate.now())
                .header("Authorization", admin())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode trend = json.readTree(response).get("trend");
        assertEquals(1, trend.size());
        assertTrue(trend.get(0).get("late").asInt() >= 1);
        mvc.perform(get("/api/dashboard?from=2020-01-01&to=2025-01-01").header("Authorization", admin()))
                .andExpect(status().isBadRequest());
    }
}
