package com.mecky.attendance.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deduction_policies", uniqueConstraints = @UniqueConstraint(columnNames = "effective_date"))
public class DeductionPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "effective_date", nullable = false)
    public LocalDate effectiveDate;
    @Column(name = "rate_per_minute", nullable = false, precision = 10, scale = 4)
    public BigDecimal ratePerMinute;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "created_by", nullable = false, length = 60)
    public String createdBy;
}
