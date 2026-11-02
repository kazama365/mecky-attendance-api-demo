package com.mecky.attendance.repo;
import com.mecky.attendance.domain.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.*;
import java.util.*;
public interface AbsenceRepository extends JpaRepository<Absence,Long>{
 boolean existsByEmployeeEmployeeIdAndAbsenceDate(String id,LocalDate date);
 List<Absence> findByAbsenceDateBetweenOrderByAbsenceDateDesc(LocalDate a,LocalDate b);
 List<Absence> findByEmployeeEmployeeIdAndAbsenceDateBetweenOrderByAbsenceDateDesc(String id,LocalDate a,LocalDate b);
 long countByEmployeeEmployeeId(String id);
}