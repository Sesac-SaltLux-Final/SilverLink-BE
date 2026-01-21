package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallEmotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallEmotionRepository extends JpaRepository<CallEmotion, Long> {
}
