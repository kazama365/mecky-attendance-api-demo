package com.mecky.attendance.repo;
import com.mecky.attendance.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DeviceRepository extends JpaRepository<Device,Long>{Optional<Device> findByName(String name);}
