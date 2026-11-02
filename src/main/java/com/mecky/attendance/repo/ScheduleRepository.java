package com.mecky.attendance.repo;
import com.mecky.attendance.domain.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScheduleRepository extends JpaRepository<WorkSchedule,String>{}
