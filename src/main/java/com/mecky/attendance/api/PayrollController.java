package com.mecky.attendance.api;

import com.mecky.attendance.domain.Employee;
import com.mecky.attendance.domain.PayrollRecord;
import com.mecky.attendance.repo.AbsenceRepository;
import com.mecky.attendance.repo.AttendanceRepository;
import com.mecky.attendance.repo.EmployeeRepository;
import com.mecky.attendance.repo.PayrollRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/payroll")
@PreAuthorize("hasRole('ADMIN')")
public class PayrollController {
    private final PayrollRepository payroll;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final AbsenceRepository absences;

    public PayrollController(PayrollRepository payroll, EmployeeRepository employees,
                             AttendanceRepository attendance, AbsenceRepository absences) {
        this.payroll = payroll;
        this.employees = employees;
        this.attendance = attendance;
        this.absences = absences;
    }

    @GetMapping("/run")
    @Transactional(readOnly = true)
    public Object findSavedRun(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        validatePeriod(from, to);
        List<PayrollRecord> rows = payroll.findByPeriodStartAndPeriodEnd(from, to);
        return response(from, to, !rows.isEmpty(), rows.stream().map(PayrollController::view).toList());
    }

    @PostMapping("/run")
    @Transactional
    public Object saveRun(@Valid @RequestBody PayrollPeriodRequest request) {
        validatePeriod(request.from(), request.to());
        List<PayrollRecord> alreadySaved = payroll.findByPeriodStartAndPeriodEnd(request.from(), request.to());
        if (!alreadySaved.isEmpty()) {
            return response(request.from(), request.to(), true,
                    alreadySaved.stream().map(PayrollController::view).toList());
        }

        List<PayrollRecord> records = new ArrayList<>();
        for (Employee employee : employees.findByActiveTrue(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent()) {
            var days = attendance.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                    employee.employeeId, request.from(), request.to()).stream()
                    .filter(row -> row.timeIn != null).toList();
            int present = days.size();
            int late = days.stream().mapToInt(row -> Math.max(0, row.lateMinutes)).sum();
            int recordedAbsences = absences.findByEmployeeEmployeeIdAndAbsenceDateBetweenOrderByAbsenceDateDesc(
                    employee.employeeId, request.from(), request.to()).size();
            BigDecimal gross = employee.dailyRate.multiply(BigDecimal.valueOf(present))
                    .setScale(2, RoundingMode.HALF_UP);
            records.add(new PayrollRecord(employee, request.from(), request.to(), present,
                    employee.dailyRate, recordedAbsences, late, gross));
        }

        List<PayrollRecord> saved = payroll.saveAll(records);
        return response(request.from(), request.to(), true, saved.stream().map(PayrollController::view).toList());
    }

    private static void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a valid payroll date range");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Payroll periods are limited to 367 days");
        }
    }

    private static Map<String, Object> response(LocalDate from, LocalDate to, boolean saved, List<?> rows) {
        return Map.of("from", from, "to", to, "saved", saved, "rows", rows);
    }

    private static Map<String, Object> view(PayrollRecord row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", row.employee.employeeId);
        result.put("employeeName", row.employee.fullName);
        result.put("daysPresent", row.daysPresent);
        result.put("daysAbsent", row.daysAbsent);
        result.put("lateMinutes", row.lateMinutes);
        result.put("dailyRate", row.dailyRateSnapshot);
        result.put("grossPay", row.grossPay);
        result.put("deductions", row.deductions);
        result.put("netPay", row.netPay);
        result.put("createdAt", row.createdAt);
        return result;
    }

    public record PayrollPeriodRequest(@NotNull LocalDate from, @NotNull LocalDate to) { }
}
