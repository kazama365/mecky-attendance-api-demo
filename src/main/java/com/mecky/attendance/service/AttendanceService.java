package com.mecky.attendance.service;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import com.mecky.attendance.config.WebSocketConfig.EventSocket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.math.*;
@Service public class AttendanceService {
 private final EmployeeRepository employees; private final AttendanceRepository attendance; private final ScheduleRepository schedules; private final ScanEventRepository events; private final AbsenceRepository absences; private final EventSocket ws;
 public AttendanceService(EmployeeRepository e,AttendanceRepository a,ScheduleRepository s,ScanEventRepository ev,AbsenceRepository ab,EventSocket ws){employees=e;attendance=a;schedules=s;events=ev;absences=ab;this.ws=ws;}
 @Transactional public ScanResult process(Scan scan){
  if(events.existsById(scan.scanUuid()))return new ScanResult(scan.scanUuid(),"DUPLICATE",null,null,"Scan UUID already received");
  LocalDateTime at=scan.timestamp();LocalDate day=at.toLocalDate();var emp=employees.findBySensorSlotId(scan.slotId()).filter(e->e.active);
  if(emp.isEmpty()){events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"UNREGISTERED",null));ws.publish("{\"type\":\"scan.unregistered\",\"slotId\":"+scan.slotId()+"}");return new ScanResult(scan.scanUuid(),"UNREGISTERED",null,null,"Fingerprint slot is not registered");}
  Employee e=emp.get();
  if(absences.existsByEmployeeEmployeeIdAndAbsenceDate(e.employeeId,day)){events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"ABSENCE_CONFLICT",e.employeeId));return new ScanResult(scan.scanUuid(),"REJECTED",e.employeeId,e.fullName,"Employee has an absence recorded for this date");}
  Attendance a=attendance.findByEmployeeEmployeeIdAndAttendanceDate(e.employeeId,day).orElseGet(()->new Attendance(e,day));
  if(a.timeIn==null){
   a.timeIn=at;a.scanUuid=scan.scanUuid();WorkSchedule s=schedules.findById(e.employeeId).orElse(null);int grace=s==null?5:s.graceMinutes;LocalTime expected=s==null?LocalTime.of(9,0):s.expectedTimeIn;
   a.lateMinutes=Math.max(0,(int)Duration.between(expected.plusMinutes(grace),at.toLocalTime()).toMinutes());a.status=a.lateMinutes>0?"LATE":"PRESENT";events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"TIME_IN",e.employeeId));attendance.save(a);
   publishAttendanceAfterCommit(e.employeeId,"{\"type\":\"attendance.new\",\"employeeId\":\""+e.employeeId+"\",\"employeeName\":\""+safe(e.fullName)+"\",\"status\":\""+a.status+"\"}");
   return new ScanResult(scan.scanUuid(),"TIME_IN",e.employeeId,e.fullName,a.status);
  }
  WorkSchedule s=schedules.findById(e.employeeId).orElse(null);int gap=s==null?60:s.minScanGapSeconds;
  if(a.timeOut!=null||Duration.between(a.timeIn,at).getSeconds()<gap){events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"DUPLICATE",e.employeeId));return new ScanResult(scan.scanUuid(),"DUPLICATE",e.employeeId,e.fullName,a.timeOut!=null?"Time-out already recorded":"Minimum scan gap has not elapsed");}
  if(at.isBefore(a.timeIn)){events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"REJECTED",e.employeeId));return new ScanResult(scan.scanUuid(),"REJECTED",e.employeeId,e.fullName,"Scan timestamp precedes time-in");}
  a.timeOut=at;a.hoursWorked=BigDecimal.valueOf(Duration.between(a.timeIn,at).toMinutes()).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);a.status="TIME_OUT";
  events.save(new ScanEvent(scan.scanUuid(),scan.slotId(),at,"TIME_OUT",e.employeeId));attendance.save(a);
  publishAttendanceAfterCommit(e.employeeId,"{\"type\":\"attendance.new\",\"employeeId\":\""+e.employeeId+"\",\"employeeName\":\""+safe(e.fullName)+"\",\"status\":\"TIME_OUT\"}");
  return new ScanResult(scan.scanUuid(),"TIME_OUT",e.employeeId,e.fullName,"Time-out recorded");
 }
 private void publishAttendanceAfterCommit(String employeeId,String json){
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCommit(){ws.publishAttendance(employeeId,json);}
  });
 }
 private static String safe(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
}
