package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalTime;
@Entity @Table(name="schedules")
public class WorkSchedule {
 @Id @Column(name="employee_id",length=30) public String employeeId;
 @Column(name="working_days",nullable=false,length=40) public String workingDays="MON,TUE,WED,THU,FRI";
 @Column(name="expected_time_in",nullable=false) public LocalTime expectedTimeIn=LocalTime.of(9,0);
 @Column(name="expected_time_out",nullable=false) public LocalTime expectedTimeOut=LocalTime.of(18,0);
 @Column(name="grace_minutes",nullable=false) public int graceMinutes=5;
 @Column(name="min_scan_gap_seconds",nullable=false) public int minScanGapSeconds=60;
 protected WorkSchedule() {}
 public WorkSchedule(String id){employeeId=id;}
}
