package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="devices")
public class Device {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true,length=80) public String name;
 @Column(name="api_key_hash",nullable=false,length=64) public String apiKeyHash;
 @Column(nullable=false,length=20) public String status="OFFLINE";
 @Column(name="last_seen") public LocalDateTime lastSeen;
 protected Device(){}
 public Device(String name,String hash){this.name=name;apiKeyHash=hash;}
}
