package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/schedules") public class ScheduleController {
 private final ScheduleRepository schedules;private final EmployeeRepository employees;
 public ScheduleController(ScheduleRepository s,EmployeeRepository e){schedules=s;employees=e;}
 @GetMapping public Object list(){return schedules.findAll().stream().map(ApiViews::schedule).toList();}
 @PutMapping @PreAuthorize("hasRole('ADMIN')") public Object save(@Valid @RequestBody ScheduleRequest r){
  employees.findById(r.employeeId()).orElseThrow();WorkSchedule s=schedules.findById(r.employeeId()).orElseGet(()->new WorkSchedule(r.employeeId()));
  s.workingDays=r.workingDays();s.expectedTimeIn=r.expectedTimeIn();s.expectedTimeOut=r.expectedTimeOut();s.graceMinutes=r.graceMinutes();s.minScanGapSeconds=r.minScanGapSeconds();return ApiViews.schedule(schedules.save(s));
 }
}
