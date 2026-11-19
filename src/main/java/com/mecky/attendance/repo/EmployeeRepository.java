package com.mecky.attendance.repo;
import com.mecky.attendance.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface EmployeeRepository extends JpaRepository<Employee,String>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from Employee e where e.active=true order by e.employeeId")
 java.util.List<Employee> lockActiveEmployees();
 @org.springframework.data.jpa.repository.Query("select e from Employee e where e.active=:active and (:q='' or lower(e.fullName) like lower(concat('%',:q,'%')) or lower(e.employeeId) like lower(concat('%',:q,'%')) or lower(e.position) like lower(concat('%',:q,'%')))")
 Page<Employee> searchByActive(@org.springframework.data.repository.query.Param("active") boolean active,
     @org.springframework.data.repository.query.Param("q") String q, Pageable pageable);
 Page<Employee> findByActiveTrue(Pageable p);
 Page<Employee> findByActiveTrueAndFullNameContainingIgnoreCaseOrActiveTrueAndEmployeeIdContainingIgnoreCaseOrActiveTrueAndPositionContainingIgnoreCase(String name,String id,String pos,Pageable p);
 Optional<Employee> findBySensorSlotId(Integer slot);
 long countByActiveTrue();
}
