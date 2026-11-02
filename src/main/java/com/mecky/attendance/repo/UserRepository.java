package com.mecky.attendance.repo;
import com.mecky.attendance.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,Long>{
 Optional<AppUser> findByUsername(String username);
 Optional<AppUser> findByEmployeeId(String employeeId);
}