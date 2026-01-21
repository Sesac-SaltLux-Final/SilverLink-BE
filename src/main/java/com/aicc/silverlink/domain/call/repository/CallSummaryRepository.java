package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSummaryRepository extends JpaRepository<CallSummary, Long> {
}
