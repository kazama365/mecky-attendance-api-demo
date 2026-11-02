package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="attendance",uniqueConstraints=@UniqueConstraint(columnNames={"employee_id","attendance_date"}))
public class Attendance {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="employee_id",nullable=false) public Employee employee;
 @Column(name="attendance_date",nullable=false) public LocalDate attendanceDate;
 @Column(name="time_in") public LocalDateTime timeIn;
 @Column(name="time_out") public LocalDateTime timeOut;
 @Column(nullable=false,length=20) public String status="PRESENT";
 @Column(name="late_minutes",nullable=false) public int lateMinutes=0;
 @Column(name="hours_worked",nullable=false,precision=6,scale=2) public BigDecimal hoursWorked=BigDecimal.ZERO;
 @Column(name="scan_uuid",unique=true,length=36) public String scanUuid;
 protected Attendance() {}
 public Attendance(Employee e,LocalDate d){employee=e;attendanceDate=d;}
}
