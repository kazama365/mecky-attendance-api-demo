package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
@RestController @RequestMapping("/api/employee-portal") @PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeePortalController {
 private final UserRepository users;private final EmployeeRepository employees;private final AttendanceRepository attendance;private final AbsenceRepository absences;private final ScheduleRepository schedules;
 public EmployeePortalController(UserRepository u,EmployeeRepository e,AttendanceRepository a,AbsenceRepository b,ScheduleRepository s){users=u;employees=e;attendance=a;absences=b;schedules=s;}
 @GetMapping("/me") public WorkerProfile me(Authentication auth){Employee e=employeeFor(auth);return new WorkerProfile(e.employeeId,e.fullName,e.position);}
 @GetMapping("/attendance") public WorkerAttendanceResponse attendanceRange(@RequestParam LocalDate from,@RequestParam LocalDate to,Authentication auth){
  LocalDate today=LocalDate.now(ZoneId.of("Asia/Manila"));
  if(from.isAfter(to)||ChronoUnit.DAYS.between(from,to)>=5)throw new IllegalArgumentException("Choose a date range from one to five days");
  if(to.isAfter(today))throw new IllegalArgumentException("Future attendance dates are not available");
  Employee e=employeeFor(auth);
  var attendanceByDay=attendance.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(e.employeeId,from,to).stream().collect(Collectors.toMap(a->a.attendanceDate,a->a,(a,b)->a));
  var absenceByDay=absences.findByEmployeeEmployeeIdAndAbsenceDateBetweenOrderByAbsenceDateDesc(e.employeeId,from,to).stream().collect(Collectors.toMap(a->a.absenceDate,a->a,(a,b)->a));
  WorkSchedule schedule=schedules.findById(e.employeeId).orElse(null);
  List<WorkerDay> days=new ArrayList<>();long worked=0,absent=0;
  for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1)){
   String weekday=d.getDayOfWeek().name().substring(0,3);
   boolean scheduled=schedule!=null&&Arrays.stream(schedule.workingDays.split(",")).map(String::trim).anyMatch(weekday::equalsIgnoreCase);
   Attendance a=attendanceByDay.get(d);Absence absence=absenceByDay.get(d);
   String status;LocalDateTime timeIn=null,timeOut=null;int late=0;BigDecimal hours=BigDecimal.ZERO;String reason=null;
   if(a!=null){status=a.status;timeIn=a.timeIn;timeOut=a.timeOut;late=a.lateMinutes;hours=a.hoursWorked;if(timeIn!=null)worked++;}
   else if(absence!=null){status="ABSENT";reason=absence.reason;absent++;}
   else if(!scheduled)status=schedule==null?"NO_SCHEDULE":"NOT_SCHEDULED";
   else status=d.equals(today)?"NOT_STARTED":"NO_RECORD";
   days.add(new WorkerDay(d,scheduled,status,timeIn,timeOut,late,hours,reason));
  }
  return new WorkerAttendanceResponse(e.employeeId,e.fullName,from,to,days,worked,absent);
 }
 private Employee employeeFor(Authentication auth){
  AppUser user=users.findByUsername(auth.getName()).filter(u->u.active&&"EMPLOYEE".equals(u.role)&&u.employeeId!=null).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Employee account is inactive or not linked"));
  return employees.findById(user.employeeId).filter(e->e.active).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Employee record is inactive"));
 }
}