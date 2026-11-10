package com.mecky.attendance.api;

import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.repo.EmployeeRepository;
import com.mecky.attendance.repo.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class SignupController {
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwords;

    public SignupController(UserRepository users, EmployeeRepository employees, PasswordEncoder passwords) {
        this.users = users;
        this.employees = employees;
        this.passwords = passwords;
    }

    @PostMapping("/signup")
    public Object signup(@Valid @RequestBody SignupRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String employeeId = request.employeeId().trim();
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The passwords do not match.");
        }
        if (users.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use.");
        }
        var employee = employees.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID not found. Ask the Admin to add your employee record first."));
        if (!employee.active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This employee record is inactive. Contact the Admin.");
        }
        if (users.findByEmployeeId(employeeId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This employee already has a login account. Contact the Admin.");
        }

        AppUser account = new AppUser(username, passwords.encode(request.password()), "EMPLOYEE");
        account.employeeId = employeeId;
        account.active = false;
        users.save(account);
        return Map.of("status", "PENDING", "message", "Signup received. The Admin must approve your account before you can sign in.");
    }

    public record SignupRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9._-]{3,60}", message = "Use 3-60 letters, numbers, dots, underscores, or hyphens for your username.") String username,
            @NotBlank @Size(max = 30) String employeeId,
            @NotBlank @Size(min = 10, max = 72, message = "Password must be 10-72 characters.") String password,
            @NotBlank @Size(min = 10, max = 72) String confirmPassword) { }
}
