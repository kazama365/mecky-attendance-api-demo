package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.UserRequest;
import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.repo.UserRepository;
import com.mecky.attendance.repo.EmployeeRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import java.util.*;
@RestController @RequestMapping("/api/users") @PreAuthorize("hasRole('ADMIN')")
public class UserController {
 private final UserRepository users;private final EmployeeRepository employees;private final PasswordEncoder passwords;
 public UserController(UserRepository u,EmployeeRepository e,PasswordEncoder p){users=u;employees=e;passwords=p;}
 @GetMapping public Object list(){return users.findAll().stream().map(UserController::view).toList();}
 @PostMapping public Object create(@Valid @RequestBody UserRequest r){
  validateAccount(r,null);
  if(r.password()==null||r.password().length()<10)throw new IllegalArgumentException("Password must contain at least 10 characters");
  AppUser u=new AppUser(r.username(),passwords.encode(r.password()),r.role());u.employeeId=r.employeeId();u.active=r.active()==null||r.active();users.save(u);return view(u);
 }
 @PutMapping("/{id}") public Object update(@PathVariable Long id,@Valid @RequestBody UserRequest r){
  AppUser u=users.findById(id).orElseThrow();
  validateAccount(r,id);
  if(!u.role.equals(r.role()))throw new IllegalArgumentException("An account role cannot be changed after creation");
  u.username=r.username();if("EMPLOYEE".equals(u.role))u.employeeId=r.employeeId();
  if(r.active()!=null)u.active=r.active();
  if(r.password()!=null&&!r.password().isBlank()){if(r.password().length()<10)throw new IllegalArgumentException("Password must contain at least 10 characters");u.passwordHash=passwords.encode(r.password());}
  users.save(u);return view(u);
 }
 private void validateAccount(UserRequest r,Long accountId){
  if(!"ADMIN".equals(r.role())&&!"EMPLOYEE".equals(r.role()))throw new IllegalArgumentException("Role must be ADMIN or EMPLOYEE");
  if("ADMIN".equals(r.role())){
   if(r.employeeId()!=null&&!r.employeeId().isBlank())throw new IllegalArgumentException("Admin accounts cannot be linked to an employee");
   return;
  }
  if(r.employeeId()==null||r.employeeId().isBlank())throw new IllegalArgumentException("Employee accounts require an employee ID");
  var employee=employees.findById(r.employeeId()).orElseThrow(()->new NoSuchElementException("Employee not found"));
  if(!employee.active)throw new IllegalArgumentException("A deactivated employee cannot receive an account");
  users.findByEmployeeId(r.employeeId()).filter(existing->!Objects.equals(existing.id,accountId)).ifPresent(existing->{throw new ResponseStatusException(HttpStatus.CONFLICT,"Employee already has an account");});
 }
 private static Map<String,Object> view(AppUser u){var result=new LinkedHashMap<String,Object>();result.put("id",u.id);result.put("username",u.username);result.put("role",u.role);result.put("employeeId",u.employeeId);result.put("active",u.active);return result;}
}