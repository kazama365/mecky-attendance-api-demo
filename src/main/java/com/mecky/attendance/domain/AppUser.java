package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true,length=60) public String username;
 @Column(name="password_hash",nullable=false,length=100) public String passwordHash;
 @Column(nullable=false,length=10) public String role="ADMIN";
 @Column(name="employee_id",unique=true,length=30) public String employeeId;
 @Column(nullable=false) public boolean active=true;
 @Column(name="created_at",nullable=false) public LocalDateTime createdAt=LocalDateTime.now();
 protected AppUser() {}
 public AppUser(String username,String passwordHash,String role){this.username=username;this.passwordHash=passwordHash;this.role=role;}
}