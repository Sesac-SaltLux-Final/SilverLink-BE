package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {
}
