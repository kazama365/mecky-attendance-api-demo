package com.mecky.attendance.api;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
public final class ApiDtos {
 private ApiDtos(){}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 public record LoginResponse(String token,String role,String username){}
 public record EmployeeRequest(@NotBlank String employeeId,@NotBlank String fullName,String position,String contactNumber,String address,@jakarta.validation.constraints.DecimalMin("0.00") BigDecimal dailyRate){}
 public record Scan(@NotBlank String scanUuid,@Min(0) int slotId,@NotNull LocalDateTime timestamp){}
 public record ScanBatch(@NotEmpty List<@Valid Scan> scans){}
 public record ScanResult(String scanUuid,String status,String employeeId,String employeeName,String message){}
 public record AttendanceCorrection(@NotNull LocalDateTime timeIn,LocalDateTime timeOut,@Min(0) int lateMinutes,@NotBlank String reason){}
 public record AbsenceRequest(@NotBlank String employeeId,@NotNull LocalDate date,@NotBlank String reason,String notes){}
 public record ScheduleRequest(@NotBlank String employeeId,@NotBlank String workingDays,@NotNull LocalTime expectedTimeIn,@NotNull LocalTime expectedTimeOut,@Min(0) int graceMinutes,@Min(0) int minScanGapSeconds){}
 public record EnrollmentResult(long commandId,@NotNull Boolean success,@NotNull Integer slotId,String message){}
 public record UserRequest(@NotBlank String username,String password,@NotBlank String role,Boolean active,String employeeId){}
 public record WorkerProfile(String employeeId,String fullName,String position){}
 public record WorkerDay(LocalDate date,boolean scheduled,String status,LocalDateTime timeIn,LocalDateTime timeOut,int lateMinutes,BigDecimal hoursWorked,String absenceReason){}
 public record WorkerAttendanceResponse(String employeeId,String fullName,LocalDate from,LocalDate to,List<WorkerDay> days,long daysWorked,long daysAbsent){}
}
