package com.mecky.attendance.repo;
import com.mecky.attendance.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
public interface EmployeeRepository extends JpaRepository<Employee,String>{
 Page<Employee> findByActiveTrue(Pageable p);
 Page<Employee> findByActiveTrueAndFullNameContainingIgnoreCaseOrActiveTrueAndEmployeeIdContainingIgnoreCaseOrActiveTrueAndPositionContainingIgnoreCase(String name,String id,String pos,Pageable p);
 Optional<Employee> findBySensorSlotId(Integer slot);
 long countByActiveTrue();
}
