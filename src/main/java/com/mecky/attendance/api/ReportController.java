package com.mecky.attendance.api;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/reports") public class ReportController {
 private final AttendanceRepository attendance;private final AbsenceRepository absences;private final EmployeeRepository employees; private final com.mecky.attendance.service.PayrollCalculator calculator;
 public ReportController(AttendanceRepository a,AbsenceRepository b,EmployeeRepository e,com.mecky.attendance.service.PayrollCalculator c){attendance=a;absences=b;employees=e;calculator=c;}
 @GetMapping("/attendance") public Object attendance(@RequestParam String from,@RequestParam String to){return attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate.parse(from),LocalDate.parse(to)).stream().map(ApiViews::attendance).toList();}
 @GetMapping("/summary") public Object summary(@RequestParam String from,@RequestParam String to){var rows=attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate.parse(from),LocalDate.parse(to));var absenceRows=absences.findByAbsenceDateBetweenOrderByAbsenceDateDesc(LocalDate.parse(from),LocalDate.parse(to));return Map.of("from",from,"to",to,"daysPresent",rows.size(),"lateCount",rows.stream().filter(a->a.lateMinutes>0).count(),"absenceCount",absenceRows.size(),"totalHours",rows.stream().map(a->a.hoursWorked).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add));}
 @GetMapping("/payroll") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
 @org.springframework.transaction.annotation.Transactional(readOnly=true)
 public Object payroll(@RequestParam String from,@RequestParam String to){
  LocalDate a=LocalDate.parse(from),b=LocalDate.parse(to);
  PayrollController.validatePeriod(a,b);
  return calculator.calculate(employees.findAll().stream().filter(e->e.active).toList(),a,b)
      .stream().map(com.mecky.attendance.service.PayrollCalculator::view).toList();
 }
}