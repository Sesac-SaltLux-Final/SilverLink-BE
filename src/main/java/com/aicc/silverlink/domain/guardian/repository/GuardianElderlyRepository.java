package com.aicc.silverlink.domain.guardian.repository;

import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GuardianElderlyRepository {
    Optional<GuardianElderly> findByGuardianId(Long guardianId);
}
