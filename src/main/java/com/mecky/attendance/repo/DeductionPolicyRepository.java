package com.mecky.attendance.repo;

import com.mecky.attendance.domain.DeductionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeductionPolicyRepository extends JpaRepository<DeductionPolicy, Long> {
    List<DeductionPolicy> findAllByOrderByEffectiveDateAsc();
}
