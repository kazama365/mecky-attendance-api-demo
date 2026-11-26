package com.mecky.attendance.api;

import com.mecky.attendance.service.PasswordChangeService;
import com.mecky.attendance.service.PasswordChangeService.PasswordRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
public class PasswordController {
    private final PasswordChangeService passwords;

    public PasswordController(PasswordChangeService passwords) { this.passwords = passwords; }

    @PostMapping("/api/auth/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public Object changeOwn(Principal principal, @Valid @RequestBody PasswordRequest request) {
        passwords.changeOwn(principal.getName(), request);
        return Map.of("message", "Password updated. Sign in again with your new password.");
    }

    @PostMapping("/api/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Object resetWorker(@PathVariable Long id, Principal principal, @Valid @RequestBody PasswordRequest request) {
        String username = passwords.resetWorker(principal.getName(), id, request);
        return Map.of("message", "Password reset for " + username + ". The worker must sign in again using the new password.");
    }
}
