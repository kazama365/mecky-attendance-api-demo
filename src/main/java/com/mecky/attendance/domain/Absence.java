package com.mecky.attendance.domain;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="absences",uniqueConstraints=@UniqueConstraint(columnNames={"employee_id","absence_date"}))
public class Absence {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="employee_id",nullable=false) public Employee employee;
 @Column(name="absence_date",nullable=false) public LocalDate absenceDate;
 @Column(nullable=false,length=20) public String reason;
 @Column(length=500) public String notes;
 @Column(name="recorded_by",nullable=false,length=60) public String recordedBy;
 protected Absence() {}
 public Absence(Employee e,LocalDate d,String r,String n,String by){employee=e;absenceDate=d;reason=r;notes=n;recordedBy=by;}
}
