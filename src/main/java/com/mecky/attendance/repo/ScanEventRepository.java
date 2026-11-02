package com.mecky.attendance.repo;
import com.mecky.attendance.domain.ScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ScanEventRepository extends JpaRepository<ScanEvent,String>{}
