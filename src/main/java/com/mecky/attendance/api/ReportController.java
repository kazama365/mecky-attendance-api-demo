package com.mecky.attendance.api;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/reports") public class ReportController {
 private final AttendanceRepository attendance;private final AbsenceRepository absences;private final EmployeeRepository employees;
 public ReportController(AttendanceRepository a,AbsenceRepository b,EmployeeRepository e){attendance=a;absences=b;employees=e;}
 @GetMapping("/attendance") public Object attendance(@RequestParam String from,@RequestParam String to){return attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate.parse(from),LocalDate.parse(to)).stream().map(ApiViews::attendance).toList();}
 @GetMapping("/summary") public Object summary(@RequestParam String from,@RequestParam String to){var rows=attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate.parse(from),LocalDate.parse(to));var absenceRows=absences.findByAbsenceDateBetweenOrderByAbsenceDateDesc(LocalDate.parse(from),LocalDate.parse(to));return Map.of("from",from,"to",to,"daysPresent",rows.size(),"lateCount",rows.stream().filter(a->a.lateMinutes>0).count(),"absenceCount",absenceRows.size(),"totalHours",rows.stream().map(a->a.hoursWorked).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add));}
 @GetMapping("/payroll") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')") public Object payroll(@RequestParam String from,@RequestParam String to){LocalDate a=LocalDate.parse(from),b=LocalDate.parse(to);return employees.findAll().stream().filter(e->e.active).map(e->{var rows=attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(a,b).stream().filter(x->x.employee.employeeId.equals(e.employeeId)).toList();var pay=e.dailyRate.multiply(java.math.BigDecimal.valueOf(rows.size())).setScale(2,java.math.RoundingMode.HALF_UP);return Map.of("employeeId",e.employeeId,"employeeName",e.fullName,"daysPresent",rows.size(),"dailyRate",e.dailyRate,"grossPay",pay,"deductions",java.math.BigDecimal.ZERO.setScale(2),"netPay",pay);}).toList();}
}
