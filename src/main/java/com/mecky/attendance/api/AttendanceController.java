package com.mecky.attendance.api;
import com.mecky.attendance.repo.AttendanceRepository;
import com.mecky.attendance.api.ApiDtos.AttendanceCorrection;
import com.mecky.attendance.domain.Attendance;
import com.mecky.attendance.service.AuditService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.time.*;
@RestController @RequestMapping("/api/attendance") public class AttendanceController {
 private final AttendanceRepository attendance;private final AuditService audit;
 public AttendanceController(AttendanceRepository a,AuditService audit){attendance=a;this.audit=audit;}
 @GetMapping public Object list(@RequestParam(defaultValue="") String date,@RequestParam(defaultValue="") String q,@RequestParam(required=false) String from,@RequestParam(required=false) String to){
  if(!date.isBlank())return attendance.searchDay(LocalDate.parse(date),q).stream().map(ApiViews::attendance).toList();
  if(from!=null&&to!=null)return attendance.findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate.parse(from),LocalDate.parse(to)).stream().map(ApiViews::attendance).toList();
  return attendance.findAll().stream().map(ApiViews::attendance).toList();
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public Object correct(@PathVariable Long id,@Valid @RequestBody AttendanceCorrection request,org.springframework.security.core.Authentication actor){
  if(request.timeOut()!=null&&request.timeOut().isBefore(request.timeIn()))throw new IllegalArgumentException("Time-out must be at or after time-in");
  Attendance a=attendance.findById(id).orElseThrow();java.util.Map<String,Object> before=new java.util.LinkedHashMap<>(ApiViews.attendance(a));before.put("reason",request.reason());
  a.timeIn=request.timeIn();a.timeOut=request.timeOut();a.lateMinutes=request.lateMinutes();
  a.status=a.timeOut!=null?"TIME_OUT":a.lateMinutes>0?"LATE":"PRESENT";
  a.hoursWorked=a.timeOut==null?java.math.BigDecimal.ZERO:java.math.BigDecimal.valueOf(Duration.between(a.timeIn,a.timeOut).toMinutes()).divide(java.math.BigDecimal.valueOf(60),2,java.math.RoundingMode.HALF_UP);
  attendance.save(a);java.util.Map<String,Object> after=new java.util.LinkedHashMap<>(ApiViews.attendance(a));after.put("reason",request.reason());audit.record(actor.getName(),"CORRECT_ATTENDANCE","attendance",id.toString(),before,after);return after;
 }
}
