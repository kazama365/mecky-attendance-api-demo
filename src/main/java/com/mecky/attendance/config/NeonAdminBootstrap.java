package com.mecky.attendance.config;

import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
@Profile("neon")
public class NeonAdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String initialPassword;

    public NeonAdminBootstrap(UserRepository users, PasswordEncoder passwords,
                              @Value("${app.bootstrap.admin-password}") String initialPassword) {
        this.users = users;
        this.passwords = passwords;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRoleAndActiveTrue("ADMIN")) return;
        if (users.count() != 0) {
            throw new IllegalStateException("Restore an active administrator before starting the site with this database.");
        }
        if (initialPassword.isBlank() || initialPassword.length() < 16
                || initialPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || initialPassword.equals("DemoAdmin2026!")) {
            throw new IllegalStateException("Set a private MECKY_INITIAL_ADMIN_PASSWORD of at least 16 characters and at most 72 UTF-8 bytes.");
        }
        users.save(new AppUser("admin", passwords.encode(initialPassword), "ADMIN"));
    }
}
