package com.mecky.attendance.repo;

import com.mecky.attendance.domain.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);
}
