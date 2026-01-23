package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {
}
