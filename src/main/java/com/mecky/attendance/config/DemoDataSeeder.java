package com.mecky.attendance.config;

import com.mecky.attendance.domain.Absence;
import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.domain.Attendance;
import com.mecky.attendance.domain.Employee;
import com.mecky.attendance.domain.WorkSchedule;
import com.mecky.attendance.repo.AbsenceRepository;
import com.mecky.attendance.repo.AttendanceRepository;
import com.mecky.attendance.repo.EmployeeRepository;
import com.mecky.attendance.repo.ScheduleRepository;
import com.mecky.attendance.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** Creates disposable sample accounts and attendance only when the demo profile is active. */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private final EmployeeRepository employees;
    private final ScheduleRepository schedules;
    private final AttendanceRepository attendance;
    private final AbsenceRepository absences;
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public DemoDataSeeder(EmployeeRepository employees, ScheduleRepository schedules,
                          AttendanceRepository attendance, AbsenceRepository absences,
                          UserRepository users, PasswordEncoder passwords) {
        this.employees = employees;
        this.schedules = schedules;
        this.attendance = attendance;
        this.absences = absences;
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public void run(String... args) {
        Employee ana = employee("DEMO-001", "Ana Santos", "Production Operator", "620.00", "0001");
        Employee marco = employee("DEMO-002", "Marco Reyes", "Warehouse Associate", "650.00", "0002");
        Employee liza = employee("DEMO-003", "Liza Cruz", "Quality Checker", "700.00", "0003");
        employees.saveAll(List.of(ana, marco, liza));

        for (Employee employee : List.of(ana, marco, liza)) {
            WorkSchedule schedule = new WorkSchedule(employee.employeeId);
            schedule.workingDays = "MON,TUE,WED,THU,FRI";
            schedule.expectedTimeIn = LocalTime.of(9, 0);
            schedule.expectedTimeOut = LocalTime.of(18, 0);
            schedule.graceMinutes = 5;
            schedules.save(schedule);
        }

        seedRecentAttendance(ana, marco, liza);
        users.save(new AppUser("admin", passwords.encode("DemoAdmin2026!"), "ADMIN"));
        users.save(worker("demo-worker-one", "DemoWorker2026!", ana.employeeId));
        users.save(worker("demo-worker-two", "DemoWorker22026!", marco.employeeId));
    }

    private Employee employee(String id, String name, String position, String dailyRate, String sampleDigits) {
        Employee employee = new Employee();
        employee.employeeId = id;
        employee.fullName = name;
        employee.position = position;
        employee.contactNumber = "09XX-XXX-" + sampleDigits;
        employee.address = "Demo address";
        employee.dailyRate = new BigDecimal(dailyRate);
        // Deliberately leave sensorSlotId empty: demo rows are not enrolled on a real reader.
        return employee;
    }

    private AppUser worker(String username, String password, String employeeId) {
        AppUser user = new AppUser(username, passwords.encode(password), "EMPLOYEE");
        user.employeeId = employeeId;
        return user;
    }

    private void seedRecentAttendance(Employee ana, Employee marco, Employee liza) {
        LocalDate today = LocalDate.now(MANILA);
        LocalDateTime now = LocalDateTime.now(MANILA);
        for (int daysAgo = 4; daysAgo >= 0; daysAgo--) {
            LocalDate day = today.minusDays(daysAgo);
            if (day.getDayOfWeek().getValue() > 5) continue;

            seedAttendance(ana, day, today, now, 8, 55, 17, 0);
            seedAttendance(marco, day, today, now, 9, 17, 17, 5);

            if (day.equals(today)) {
                absences.save(new Absence(liza, day, "Sick", "Sample absence for the demo", "admin"));
            } else if (day.getDayOfWeek().getValue() == 3 && daysAgo < 4) {
                absences.save(new Absence(liza, day, "Leave", "Sample approved leave", "admin"));
            } else {
                seedAttendance(liza, day, today, now, 8, 58, 17, 2);
            }
        }
    }

    private void seedAttendance(Employee employee, LocalDate day, LocalDate today, LocalDateTime now,
                                int inHour, int inMinute, int outHour, int outMinute) {
        Attendance row = new Attendance(employee, day);
        row.timeIn = day.equals(today) ? now.minusMinutes(employee.employeeId.endsWith("002") ? 35 : 45)
                : day.atTime(inHour, inMinute);
        if (day.equals(today)) {
            row.timeOut = null;
            row.hoursWorked = BigDecimal.ZERO;
        } else {
            row.timeOut = day.atTime(outHour, outMinute);
            row.hoursWorked = BigDecimal.valueOf(Duration.between(row.timeIn, row.timeOut).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        }
        row.lateMinutes = employee.employeeId.endsWith("002") && !day.equals(today) ? 12 : 0;
        row.status = row.lateMinutes > 0 ? "LATE" : (row.timeOut == null ? "PRESENT" : "TIME_OUT");
        attendance.save(row);
    }
}
