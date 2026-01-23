package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
}
