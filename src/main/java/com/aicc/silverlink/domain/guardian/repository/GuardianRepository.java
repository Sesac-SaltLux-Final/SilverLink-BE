package com.aicc.silverlink.domain.guardian.repository;

import com.aicc.silverlink.domain.guardian.entity.GuardianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuardianRepository extends JpaRepository<GuardianProfile, Long> {
}
