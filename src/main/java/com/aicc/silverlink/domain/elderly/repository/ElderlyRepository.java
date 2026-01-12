package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.ElderlyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElderlyRepository extends JpaRepository<ElderlyProfile, Long> {
}
