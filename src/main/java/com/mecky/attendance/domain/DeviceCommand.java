package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="device_commands")
public class DeviceCommand {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=20) public String type="ENROLL";
 @Column(name="employee_id",nullable=false,length=30) public String employeeId;
 @Column(nullable=false,length=20) public String status="PENDING";
 @Column(name="created_at",nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
 @Column(name="result_message",length=300) public String resultMessage;
 protected DeviceCommand() {}
 public DeviceCommand(String employeeId){this.employeeId=employeeId;}
}
