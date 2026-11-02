package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.time.*;
@RestController @RequestMapping("/api/absences") public class AbsenceController {
 private final AbsenceRepository absences;private final EmployeeRepository employees;
 public AbsenceController(AbsenceRepository a,EmployeeRepository e){absences=a;employees=e;}
 @GetMapping public Object list(@RequestParam(required=false) String from,@RequestParam(required=false) String to){var rows=from!=null&&to!=null?absences.findByAbsenceDateBetweenOrderByAbsenceDateDesc(LocalDate.parse(from),LocalDate.parse(to)):absences.findAll();return rows.stream().map(ApiViews::absence).toList();}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public Object create(@Valid @RequestBody AbsenceRequest r,org.springframework.security.core.Authentication auth){Employee e=employees.findById(r.employeeId()).orElseThrow();return ApiViews.absence(absences.save(new Absence(e,r.date(),r.reason(),r.notes(),auth.getName())));}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public Object update(@PathVariable Long id,@Valid @RequestBody AbsenceRequest r){Absence a=absences.findById(id).orElseThrow();a.employee=employees.findById(r.employeeId()).orElseThrow();a.absenceDate=r.date();a.reason=r.reason();a.notes=r.notes();return ApiViews.absence(absences.save(a));}
 @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id){absences.deleteById(id);}
}
