package com.aicc.silverlink.domain.counseling.repository;

import com.aicc.silverlink.domain.counseling.entity.CounselingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CounselingRepository extends JpaRepository<CounselingRecord, Long> {
}
