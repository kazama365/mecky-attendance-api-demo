package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="employees")
public class Employee {
 @Id @Column(name="employee_id",length=30) public String employeeId;
 @Column(name="full_name",nullable=false,length=120) public String fullName;
 @Column(length=80) public String position;
 @Column(name="contact_number",length=30) public String contactNumber;
 @Column(length=300) public String address;
 @Column(nullable=false) public boolean active=true;
 @Column(name="sensor_slot_id",unique=true) public Integer sensorSlotId;
 @Column(name="daily_rate",nullable=false,precision=10,scale=2) public BigDecimal dailyRate=BigDecimal.ZERO;
 @Column(name="created_at",nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
 public Employee() {}
}
