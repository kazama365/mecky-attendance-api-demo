package com.mecky.attendance.api;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/dashboard") public class DashboardController {
 private final EmployeeRepository employees;private final AttendanceRepository attendance;
 public DashboardController(EmployeeRepository e,AttendanceRepository a){employees=e;attendance=a;}
 @GetMapping public Object summary(){LocalDate d=LocalDate.now(ZoneId.of("Asia/Manila"));long total=employees.countByActiveTrue();var today=attendance.findByAttendanceDateOrderByTimeInDesc(d);long present=today.stream().filter(a->a.timeIn!=null).count();long late=today.stream().filter(a->a.lateMinutes>0).count();return Map.of("date",d,"employees",total,"present",present,"late",late,"absent",Math.max(0,total-present),"latest",today.stream().limit(10).map(ApiViews::attendance).toList());}
}
