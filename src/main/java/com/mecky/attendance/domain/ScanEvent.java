package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="scan_events")
public class ScanEvent {
 @Id @Column(name="scan_uuid",length=36) public String scanUuid;
 @Column(name="sensor_slot_id",nullable=false) public int sensorSlotId;
 @Column(name="scanned_at",nullable=false) public LocalDateTime scannedAt;
 @Column(nullable=false,length=20) public String outcome;
 @Column(name="employee_id",length=30) public String employeeId;
 protected ScanEvent() {}
 public ScanEvent(String uuid,int slot,LocalDateTime at,String result,String emp){scanUuid=uuid;sensorSlotId=slot;scannedAt=at;outcome=result;employeeId=emp;}
}
