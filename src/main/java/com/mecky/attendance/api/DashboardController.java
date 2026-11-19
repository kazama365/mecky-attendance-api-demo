package com.mecky.attendance.api;

import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;

    public DashboardController(EmployeeRepository employees, AttendanceRepository attendance) {
        this.employees = employees;
        this.attendance = attendance;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Object summary(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Manila"));
        LocalDate start = from == null ? today.minusDays(6) : from;
        LocalDate end = to == null ? today : to;
        if (end.isBefore(start) || end.isAfter(today) || start.plusDays(92).isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose up to 93 past or current days.");
        }
        var active = employees.findAll().stream().filter(employee -> employee.active).map(employee -> employee.employeeId)
                .collect(java.util.stream.Collectors.toSet());
        var latest = attendance.findByAttendanceDateOrderByTimeInDesc(today).stream()
                .filter(row -> active.contains(row.employee.employeeId)).toList();
        var rows = attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(start, end).stream()
                .filter(row -> active.contains(row.employee.employeeId) && row.timeIn != null)
                .collect(java.util.stream.Collectors.groupingBy(row -> row.attendanceDate));
        List<Object> trend = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            var dayRows = rows.getOrDefault(day, List.of());
            trend.add(Map.of("date", day, "present", dayRows.size(),
                    "late", dayRows.stream().filter(row -> row.lateMinutes > 0).count(),
                    "absent", Math.max(0, active.size() - dayRows.size())));
        }
        long present = latest.stream().filter(row -> row.timeIn != null).count();
        return Map.of("date", today, "employees", active.size(), "present", present,
                "late", latest.stream().filter(row -> row.lateMinutes > 0).count(),
                "absent", Math.max(0, active.size() - present),
                "latest", latest.stream().limit(10).map(ApiViews::attendance).toList(), "trend", trend);
    }
}
