package com.mecky.attendance.api;

import com.mecky.attendance.domain.DeductionPolicy;
import com.mecky.attendance.repo.DeductionPolicyRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/payroll/policies")
@PreAuthorize("hasRole('ADMIN')")
public class DeductionPolicyController {
    private final DeductionPolicyRepository policies;

    public DeductionPolicyController(DeductionPolicyRepository policies) { this.policies = policies; }

    @GetMapping
    public Object list() { return policies.findAllByOrderByEffectiveDateAsc(); }

    @PostMapping
    public Object create(@Valid @RequestBody PolicyRequest request, Principal principal) {
        if (!request.confirmed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm the deduction formula before saving.");
        }
        DeductionPolicy policy = new DeductionPolicy();
        policy.effectiveDate = request.effectiveDate();
        policy.ratePerMinute = request.ratePerMinute();
        policy.createdBy = principal.getName();
        return policies.save(policy);
    }

    public record PolicyRequest(@NotNull LocalDate effectiveDate,
            @NotNull @DecimalMin("0") @DecimalMax("999999.9999") @Digits(integer = 6, fraction = 4)
            BigDecimal ratePerMinute, boolean confirmed) { }
}
