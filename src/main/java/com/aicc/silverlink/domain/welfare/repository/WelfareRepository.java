package com.aicc.silverlink.domain.welfare.repository;

import com.aicc.silverlink.domain.welfare.entity.Welfare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WelfareRepository extends JpaRepository<Welfare, Long> {

    // servId로 중복 체크 및 업데이트를 위해 조회
    Optional<Welfare> findByServId(String servId);

    // 저장 전 존재 여부만 빠르게 확인
    boolean existsByServId(String servId);

    // 어르신 지역 코드에 맞춘 서비스 목록 조회
    List<Welfare> findAllByDistrictCode(String districtCode);

    // 활성화된 서비스만 필터링 (필요 시)
    List<Welfare> findAllByIsActiveTrue();
}