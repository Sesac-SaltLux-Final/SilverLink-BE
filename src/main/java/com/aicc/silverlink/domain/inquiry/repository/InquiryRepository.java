package com.aicc.silverlink.domain.inquiry.repository;

import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByElderlyIdAndIsDeletedFalseOrderByCreatedAtDesc(Long elderlyId);

    List<Inquiry> findAllByElderlyIdInAndIsDeletedFalseOrderByCreatedAtDesc(List<Long> elderlyIds);

    Optional<Inquiry> findByIdAndIsDeletedFalse(Long id);
}
