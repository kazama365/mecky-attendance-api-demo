package com.mecky.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecky.attendance.config.JwtService;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@org.springframework.test.context.TestExecutionListeners(listeners = {
    org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
    org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
}, mergeMode = org.springframework.test.context.TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@Transactional
class PasswordWorkflowTest {
    private static final String OLD = "TestPassOld123!";
    private static final String NEW = "TestPassNew456!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired EmployeeRepository employees;
    @Autowired PasswordEncoder passwords;

    private AppUser account(String username, String role) {
        AppUser user = new AppUser(username, passwords.encode(OLD), role);
        if ("EMPLOYEE".equals(role)) {
            Employee employee = new Employee();
            employee.employeeId = username;
            employee.fullName = "Password workflow test";
            employees.saveAndFlush(employee);
            user.employeeId = username;
        }
        return users.saveAndFlush(user);
    }

    private String token(AppUser user) { return "Bearer " + jwt.issue(user.username, user.role, user.passwordVersion); }
    private String payload(String current, String next, String confirmation) throws Exception {
        return json.writeValueAsString(Map.of("currentPassword", current, "newPassword", next, "confirmPassword", confirmation));
    }

    @Test void bothRolesCanChangeOwnPasswordAndOldSessionsExpire() throws Exception {
        for (String role : new String[]{"ADMIN", "EMPLOYEE"}) {
            AppUser user = account("self-" + role.toLowerCase(), role);
            String originalToken = token(user);
            mvc.perform(post("/api/auth/change-password").header("Authorization", originalToken)
                    .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isOk());
            assertTrue(passwords.matches(NEW, users.findById(user.id).orElseThrow().passwordHash));
            assertEquals(1, users.findById(user.id).orElseThrow().passwordVersion);
            String path = "ADMIN".equals(role) ? "/api/dashboard" : "/api/employee-portal/me";
            mvc.perform(get(path).header("Authorization", originalToken)).andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(json.writeValueAsString(Map.of("username", user.username, "password", OLD))))
                    .andExpect(status().isUnauthorized());
            String login = mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(json.writeValueAsString(Map.of("username", user.username, "password", NEW))))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String freshToken = "Bearer " + json.readTree(login).get("token").asText();
            mvc.perform(get(path).header("Authorization", freshToken)).andExpect(status().isOk());
        }
    }

    @Test void wrongPasswordAndMismatchLeaveCredentialsUnchanged() throws Exception {
        AppUser user = account("wrong-password", "EMPLOYEE");
        String authorization = token(user);
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload("Incorrect123!", NEW, NEW))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload(OLD, NEW, "Mismatch123!"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload(OLD, OLD, OLD))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload(OLD, "short", "short"))).andExpect(status().isBadRequest());
        assertTrue(passwords.matches(OLD, users.findById(user.id).orElseThrow().passwordHash));
        assertEquals(0, users.findById(user.id).orElseThrow().passwordVersion);
    }

    @Test void adminResetPreservesWorkerIdentityAndRevokesOnlyWorkerSessions() throws Exception {
        AppUser admin = account("reset-admin", "ADMIN");
        AppUser worker = account("reset-worker", "EMPLOYEE");
        String adminToken = token(admin), workerToken = token(worker), employeeId = worker.employeeId;
        mvc.perform(post("/api/users/" + worker.id + "/reset-password").header("Authorization", adminToken)
                .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isOk());
        AppUser saved = users.findById(worker.id).orElseThrow();
        assertEquals(employeeId, saved.employeeId);
        assertTrue(saved.active);
        assertEquals("EMPLOYEE", saved.role);
        assertTrue(passwords.matches(NEW, saved.passwordHash));
        mvc.perform(get("/api/dashboard").header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/employee-portal/me").header("Authorization", workerToken)).andExpect(status().isForbidden());
    }

    @Test void resetCannotBeUsedByWorkerOrToResetAnAdmin() throws Exception {
        AppUser worker = account("denied-worker", "EMPLOYEE");
        AppUser admin = account("denied-admin", "ADMIN");
        mvc.perform(post("/api/users/" + admin.id + "/reset-password").header("Authorization", token(worker))
                .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isForbidden());
        mvc.perform(post("/api/users/" + admin.id + "/reset-password").header("Authorization", token(admin))
                .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").contentType("application/json")
                .content(payload(OLD, NEW, NEW))).andExpect(status().isForbidden());
    }

    @Test void resetDoesNotApprovePendingAccountsAndOldUpdateCannotBypassVerification() throws Exception {
        AppUser admin = account("pending-admin", "ADMIN");
        AppUser worker = account("pending-worker", "EMPLOYEE");
        worker.active = false;
        users.saveAndFlush(worker);
        mvc.perform(post("/api/users/" + worker.id + "/reset-password").header("Authorization", token(admin))
                .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isOk());
        assertFalse(users.findById(worker.id).orElseThrow().active);
        mvc.perform(put("/api/users/" + worker.id).header("Authorization", token(admin)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("username", worker.username, "role", "EMPLOYEE",
                        "employeeId", worker.employeeId, "active", false, "password", OLD)))).andExpect(status().isBadRequest());
        assertTrue(passwords.matches(NEW, users.findById(worker.id).orElseThrow().passwordHash));
    }

    @Test void unicodeLimitAndFailedVerificationRateLimitAreEnforced() throws Exception {
        AppUser user = account("limited-worker", "EMPLOYEE");
        String authorization = token(user);
        String unicode = "\u20ac".repeat(30);
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload(OLD, unicode, unicode))).andExpect(status().isBadRequest());
        for (int count = 0; count < 5; count++) {
            mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                    .contentType("application/json").content(payload("Incorrect123!", NEW, NEW))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/change-password").header("Authorization", authorization)
                .contentType("application/json").content(payload(OLD, NEW, NEW))).andExpect(status().isTooManyRequests());
        assertTrue(passwords.matches(OLD, users.findById(user.id).orElseThrow().passwordHash));
    }
}
