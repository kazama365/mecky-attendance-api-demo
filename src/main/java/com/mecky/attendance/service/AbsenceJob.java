package com.mecky.attendance.service;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Component public class AbsenceJob {
 private final EmployeeRepository employees;private final ScheduleRepository schedules;private final AttendanceRepository attendance;private final AbsenceRepository absences;
 public AbsenceJob(EmployeeRepository e,ScheduleRepository s,AttendanceRepository a,AbsenceRepository b){employees=e;schedules=s;attendance=a;absences=b;}
 @Scheduled(cron="0 55 23 * * *",zone="Asia/Manila") @Transactional public void markAbsent(){
  LocalDate d=LocalDate.now(ZoneId.of("Asia/Manila"));String day=d.getDayOfWeek().name().substring(0,3);
  for(Employee e:employees.findAll())if(e.active){WorkSchedule s=schedules.findById(e.employeeId).orElse(null);if(s!=null&&s.workingDays.contains(day)&&attendance.findByEmployeeEmployeeIdAndAttendanceDate(e.employeeId,d).isEmpty()&&!absences.existsByEmployeeEmployeeIdAndAbsenceDate(e.employeeId,d))absences.save(new Absence(e,d,"Unexcused","Automatically generated at end of day","system"));}
 }
}
