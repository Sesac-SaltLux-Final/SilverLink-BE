package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthInfoRepository extends JpaRepository<ElderlyHealthInfo,Long> {

}
