package com.mecky.attendance.api;

import com.mecky.attendance.domain.PayrollRecord;
import com.mecky.attendance.repo.*;
import com.mecky.attendance.service.PayrollCalculator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/payroll")
@PreAuthorize("hasRole('ADMIN')")
public class PayrollController {
    private final PayrollRepository payroll;
    private final EmployeeRepository employees;
    private final PayrollCalculator calculator;

    public PayrollController(PayrollRepository payroll, EmployeeRepository employees, PayrollCalculator calculator) {
        this.payroll = payroll;
        this.employees = employees;
        this.calculator = calculator;
    }

    @GetMapping("/run")
    @Transactional(readOnly = true)
    public Object findSavedRun(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        validatePeriod(from, to);
        var rows = payroll.findByPeriodStartAndPeriodEnd(from, to);
        return response(from, to, !rows.isEmpty(), rows);
    }

    @GetMapping("/preview")
    @Transactional(readOnly = true)
    public Object preview(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        validatePeriod(from, to);
        var saved = payroll.findByPeriodStartAndPeriodEnd(from, to);
        if (!saved.isEmpty()) return response(from, to, true, saved);
        return response(from, to, false, calculator.calculate(
                employees.findAll().stream().filter(employee -> employee.active).toList(), from, to));
    }

    @PostMapping("/run")
    @Transactional
    public Object saveRun(@Valid @RequestBody PayrollPeriodRequest request) {
        validatePeriod(request.from(), request.to());
        // Lock in a stable order before reading the period, so concurrent saves see the completed run.
        var activeEmployees = employees.lockActiveEmployees();
        var saved = payroll.findByPeriodStartAndPeriodEnd(request.from(), request.to());
        if (!saved.isEmpty()) return response(request.from(), request.to(), true, saved);
        var records = calculator.calculate(activeEmployees, request.from(), request.to());
        if (records.stream().anyMatch(PayrollCalculator::awaitingPolicy)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Configure and confirm a deduction policy covering the late attendance dates before saving this run.");
        }
        return response(request.from(), request.to(), true, payroll.saveAll(records));
    }

    public static void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || from.plusDays(366).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid payroll range of up to 367 days.");
        }
    }

    private static Object response(LocalDate from, LocalDate to, boolean saved, List<PayrollRecord> rows) {
        return Map.of("from", from, "to", to, "saved", saved,
                "rows", rows.stream().map(PayrollCalculator::view).toList());
    }

    public record PayrollPeriodRequest(@NotNull LocalDate from, @NotNull LocalDate to) { }
}
