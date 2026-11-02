package com.mecky.attendance.repo;
import com.mecky.attendance.domain.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand,Long>{Optional<DeviceCommand> findFirstByStatusOrderByCreatedAtAsc(String status);Optional<DeviceCommand> findTopByEmployeeIdOrderByCreatedAtDesc(String employeeId);}
