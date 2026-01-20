package com.aicc.silverlink.domain.inquiry.repository;

import com.aicc.silverlink.domain.inquiry.entity.Faq;
import com.aicc.silverlink.domain.inquiry.entity.Faq.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FaqCategory category);

    List<Faq> findAllByIsActiveTrueOrderByDisplayOrderAsc();
}
