package com.mecky.attendance.service;

import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.repo.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordChangeService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ConcurrentHashMap<String, FailedVerification> failures = new ConcurrentHashMap<>();

    public PasswordChangeService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public void changeOwn(String username, PasswordRequest request) {
        AppUser account = verifiedActor(username, request.currentPassword());
        replacePassword(account, request);
    }

    @Transactional
    public String resetWorker(String adminUsername, Long workerId, PasswordRequest request) {
        AppUser admin = verifiedActor(adminUsername, request.currentPassword());
        if (!"ADMIN".equals(admin.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Admin can reset worker passwords.");
        }
        AppUser worker = users.lockById(workerId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker account not found."));
        if (!"EMPLOYEE".equals(worker.role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a worker account. Use Change password for your own account.");
        }
        replacePassword(worker, request);
        return worker.username;
    }

    private AppUser verifiedActor(String username, String currentPassword) {
        AppUser actor = users.lockByUsername(username).filter(user -> user.active).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Sign in again to change a password."));
        Instant now = Instant.now();
        FailedVerification previous = failures.get(username);
        if (previous != null && previous.until().isAfter(now) && previous.count() >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect passwords. Wait 10 minutes and try again.");
        }
        if (currentPassword.getBytes(StandardCharsets.UTF_8).length > 72 || !passwords.matches(currentPassword, actor.passwordHash)) {
            failures.compute(username, (key, value) -> value == null || !value.until().isAfter(now)
                    ? new FailedVerification(1, now.plusSeconds(600))
                    : new FailedVerification(value.count() + 1, value.until()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is incorrect.");
        }
        failures.remove(username);
        return actor;
    }

    private void replacePassword(AppUser account, PasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The new passwords do not match.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a new password of at most 72 UTF-8 bytes.");
        }
        if (passwords.matches(request.newPassword(), account.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a password different from the existing password.");
        }
        account.passwordHash = passwords.encode(request.newPassword());
        account.passwordVersion++;
        users.save(account);
    }

    private record FailedVerification(int count, Instant until) { }

    public record PasswordRequest(@NotBlank @Size(max = 200) String currentPassword,
            @NotBlank @Size(min = 10, max = 72) String newPassword,
            @NotBlank @Size(min = 10, max = 72) String confirmPassword) { }
}
