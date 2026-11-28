package com.mecky.attendance.repo;
import com.mecky.attendance.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser,Long>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.username=:username")
 Optional<AppUser> lockByUsername(@org.springframework.data.repository.query.Param("username") String username);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.id=:id")
 Optional<AppUser> lockById(@org.springframework.data.repository.query.Param("id") Long id);
 Optional<AppUser> findByUsername(String username);
 Optional<AppUser> findByUsernameIgnoreCase(String username);
 Optional<AppUser> findByEmployeeId(String employeeId);
 boolean existsByRoleAndActiveTrue(String role);
}
