package com.mecky.attendance.repo;
import com.mecky.attendance.domain.Attendance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;
public interface AttendanceRepository extends JpaRepository<Attendance,Long>{
 Optional<Attendance> findByEmployeeEmployeeIdAndAttendanceDate(String id,LocalDate date);
 List<Attendance> findByAttendanceDateOrderByTimeInDesc(LocalDate date);
 List<Attendance> findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate a,LocalDate b);
 List<Attendance> findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(String id,LocalDate a,LocalDate b);
 long countByAttendanceDateAndTimeInIsNotNull(LocalDate d);
 long countByAttendanceDateAndStatus(LocalDate d,String s);
 @Query("select a from Attendance a where a.attendanceDate=:d and (:q='' or lower(a.employee.fullName) like lower(concat('%',:q,'%')) or lower(a.employee.employeeId) like lower(concat('%',:q,'%'))) order by a.timeIn desc")
 List<Attendance> searchDay(@Param("d") LocalDate d,@Param("q") String q);
}