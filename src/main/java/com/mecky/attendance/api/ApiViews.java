package com.mecky.attendance.api;
import com.mecky.attendance.domain.*;
import java.util.*;
public final class ApiViews {
 private ApiViews(){}
 public static Map<String,Object> employee(Employee e){var m=new LinkedHashMap<String,Object>();m.put("employeeId",e.employeeId);m.put("fullName",e.fullName);m.put("position",e.position);m.put("contactNumber",e.contactNumber);m.put("address",e.address);m.put("active",e.active);m.put("sensorSlotId",e.sensorSlotId);m.put("dailyRate",e.dailyRate);return m;}
 public static Map<String,Object> attendance(Attendance a){var m=new LinkedHashMap<String,Object>();m.put("id",a.id);m.put("employee",employee(a.employee));m.put("attendanceDate",a.attendanceDate);m.put("timeIn",a.timeIn);m.put("timeOut",a.timeOut);m.put("status",a.status);m.put("lateMinutes",a.lateMinutes);m.put("hoursWorked",a.hoursWorked);return m;}
 public static Map<String,Object> absence(Absence a){var m=new LinkedHashMap<String,Object>();m.put("id",a.id);m.put("employee",employee(a.employee));m.put("absenceDate",a.absenceDate);m.put("reason",a.reason);m.put("notes",a.notes);m.put("recordedBy",a.recordedBy);return m;}
 public static Map<String,Object> schedule(WorkSchedule s){return Map.of("employeeId",s.employeeId,"workingDays",s.workingDays,"expectedTimeIn",s.expectedTimeIn,"expectedTimeOut",s.expectedTimeOut,"graceMinutes",s.graceMinutes,"minScanGapSeconds",s.minScanGapSeconds);}
 public static Map<String,Object> command(DeviceCommand c){var m=new LinkedHashMap<String,Object>();m.put("id",c.id);m.put("employeeId",c.employeeId);m.put("type",c.type);m.put("status",c.status);m.put("createdAt",c.createdAt);m.put("resultMessage",c.resultMessage);return m;}
}
