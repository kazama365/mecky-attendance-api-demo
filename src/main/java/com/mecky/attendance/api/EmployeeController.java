package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
@RestController @RequestMapping("/api/employees") public class EmployeeController {
 private final EmployeeRepository employees;private final DeviceCommandRepository commands;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 public EmployeeController(EmployeeRepository e,DeviceCommandRepository c){employees=e;commands=c;}
 @GetMapping public Page<?> list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){
  Pageable p=PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by("fullName"));
  var result=q.isBlank()?employees.findByActiveTrue(p):employees.findByActiveTrueAndFullNameContainingIgnoreCaseOrActiveTrueAndEmployeeIdContainingIgnoreCaseOrActiveTrueAndPositionContainingIgnoreCase(q,q,q,p);
  return result.map(ApiViews::employee);
 }
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @org.springframework.transaction.annotation.Transactional
 public Object create(@Valid @RequestBody EmployeeRequest r){
  String id=r.employeeId().trim();
  if(!id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,29}"))
   throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Employee ID must use 1–30 letters, numbers, dashes or underscores and start with a letter or number.");
  if(employees.existsById(id))
   throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This Employee ID already exists.");
  Employee e=new Employee();e.employeeId=id;e.fullName=r.fullName().trim();e.position=r.position();e.contactNumber=r.contactNumber();e.address=r.address();e.dailyRate=r.dailyRate()==null?java.math.BigDecimal.ZERO:r.dailyRate();
  entityManager.persist(e);
  return ApiViews.employee(e);
 }
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public Object update(@PathVariable String id,@Valid @RequestBody EmployeeRequest r){
  Employee e=employees.findById(id).orElseThrow();e.fullName=r.fullName();e.position=r.position();e.contactNumber=r.contactNumber();e.address=r.address();e.dailyRate=r.dailyRate()==null?e.dailyRate:r.dailyRate();return ApiViews.employee(employees.save(e));
 }
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void deactivate(@PathVariable String id){Employee e=employees.findById(id).orElseThrow();e.active=false;employees.save(e);}
 @PostMapping("/{id}/enroll") @PreAuthorize("hasRole('ADMIN')") public Object enroll(@PathVariable String id){if(!employees.existsById(id))throw new java.util.NoSuchElementException("Employee not found");return ApiViews.command(commands.save(new DeviceCommand(id)));}
 @GetMapping("/{id}/enrollment-status") @PreAuthorize("hasRole('ADMIN')") public Object enrollmentStatus(@PathVariable String id){return commands.findTopByEmployeeIdOrderByCreatedAtDesc(id).map(ApiViews::command).orElseThrow(()->new java.util.NoSuchElementException("No enrollment command"));}
}
