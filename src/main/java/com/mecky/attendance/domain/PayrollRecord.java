package com.mecky.attendance.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_records", uniqueConstraints =
        @UniqueConstraint(name = "uq_payroll_period", columnNames = {"employee_id", "period_start", "period_end"}))
public class PayrollRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    public Employee employee;

    @Column(name = "period_start", nullable = false)
    public LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    public LocalDate periodEnd;

    @Column(name = "days_present", nullable = false)
    public int daysPresent;

    @Column(name = "daily_rate_snapshot", nullable = false, precision = 10, scale = 2)
    public BigDecimal dailyRateSnapshot;

    @Column(name = "days_absent", nullable = false)
    public int daysAbsent;

    @Column(name = "late_minutes", nullable = false)
    public int lateMinutes;

    @Column(name = "undertime_minutes", nullable = false)
    public int undertimeMinutes;

    @Column(name = "gross_pay", nullable = false, precision = 10, scale = 2)
    public BigDecimal grossPay;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal deductions;

    @Column(name = "net_pay", nullable = false, precision = 10, scale = 2)
    public BigDecimal netPay;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    protected PayrollRecord() { }

    public PayrollRecord(Employee employee, LocalDate from, LocalDate to, int daysPresent,
                         BigDecimal dailyRateSnapshot, int daysAbsent, int lateMinutes, BigDecimal grossPay) {
        this.employee = employee;
        this.periodStart = from;
        this.periodEnd = to;
        this.daysPresent = daysPresent;
        this.dailyRateSnapshot = dailyRateSnapshot;
        this.daysAbsent = daysAbsent;
        this.lateMinutes = lateMinutes;
        this.undertimeMinutes = 0;
        this.grossPay = grossPay;
        this.deductions = BigDecimal.ZERO.setScale(2);
        this.netPay = grossPay;
    }
}
