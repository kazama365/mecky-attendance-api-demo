package com.mecky.attendance.service;

import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class PayrollCalculator {
    private final AttendanceRepository attendance;
    private final AbsenceRepository absences;
    private final DeductionPolicyRepository policies;

    public PayrollCalculator(AttendanceRepository attendance, AbsenceRepository absences,
                             DeductionPolicyRepository policies) {
        this.attendance = attendance;
        this.absences = absences;
        this.policies = policies;
    }

    public List<PayrollRecord> calculate(List<Employee> employees, LocalDate from, LocalDate to) {
        List<DeductionPolicy> versions = policies.findAllByOrderByEffectiveDateAsc();
        return employees.stream().map(employee -> calculate(employee, from, to, versions)).toList();
    }

    private PayrollRecord calculate(Employee employee, LocalDate from, LocalDate to,
                                    List<DeductionPolicy> versions) {
        var days = attendance.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                employee.employeeId, from, to).stream().filter(day -> day.timeIn != null).toList();
        int late = days.stream().mapToInt(day -> Math.max(0, day.lateMinutes)).sum();
        int absent = absences.findByEmployeeEmployeeIdAndAbsenceDateBetweenOrderByAbsenceDateDesc(
                employee.employeeId, from, to).size();
        BigDecimal gross = employee.dailyRate.multiply(BigDecimal.valueOf(days.size())).setScale(2, RoundingMode.HALF_UP);
        PayrollRecord row = new PayrollRecord(employee, from, to, days.size(), employee.dailyRate, absent, late, gross);
        BigDecimal deduction = BigDecimal.ZERO;
        StringJoiner snapshot = new StringJoiner("; ");
        for (Attendance day : days) {
            if (day.lateMinutes <= 0) continue;
            DeductionPolicy policy = versions.stream().filter(version -> !version.effectiveDate.isAfter(day.attendanceDate))
                    .reduce((previous, next) -> next).orElse(null);
            if (policy == null) {
                snapshot.add(day.attendanceDate + ": AWAITING POLICY (" + day.lateMinutes + " late minutes)");
            } else {
                deduction = deduction.add(policy.ratePerMinute.multiply(BigDecimal.valueOf(day.lateMinutes)));
                snapshot.add(day.attendanceDate + ": " + day.lateMinutes + " min x PHP " + policy.ratePerMinute
                        + "/min, policy #" + policy.id + " effective " + policy.effectiveDate);
            }
        }
        row.lateDeduction = deduction.setScale(2, RoundingMode.HALF_UP).min(gross);
        row.deductions = row.lateDeduction.add(row.otherDeductions);
        row.netPay = gross.subtract(row.deductions);
        row.policySnapshot = snapshot.length() == 0 ? "No late minutes. Late deduction PHP 0.00." : snapshot.toString();
        return row;
    }

    public static boolean awaitingPolicy(PayrollRecord row) {
        return row.policySnapshot != null && row.policySnapshot.contains("AWAITING POLICY");
    }

    public static Map<String, Object> view(PayrollRecord row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", row.employee.employeeId);
        result.put("employeeName", row.employee.fullName);
        result.put("daysPresent", row.daysPresent);
        result.put("daysAbsent", row.daysAbsent);
        result.put("lateMinutes", row.lateMinutes);
        result.put("dailyRate", row.dailyRateSnapshot);
        result.put("grossPay", row.grossPay);
        result.put("lateDeduction", row.lateDeduction);
        result.put("otherDeductions", row.otherDeductions);
        result.put("deductions", row.deductions);
        result.put("netPay", row.netPay);
        result.put("policySnapshot", row.policySnapshot == null ? "Legacy saved run. Original deductions retained." : row.policySnapshot);
        result.put("awaitingPolicy", awaitingPolicy(row));
        result.put("createdAt", row.createdAt);
        return result;
    }
}
