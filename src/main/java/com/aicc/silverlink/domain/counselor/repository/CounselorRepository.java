package com.aicc.silverlink.domain.counselor.repository;

import com.aicc.silverlink.domain.counselor.entity.CounselorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CounselorRepository extends JpaRepository<CounselorProfile, Long> {
}
