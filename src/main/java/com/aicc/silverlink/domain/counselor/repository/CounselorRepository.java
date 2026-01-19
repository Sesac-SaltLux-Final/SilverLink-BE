package com.aicc.silverlink.domain.counselor.repository;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorRepository extends JpaRepository<Counselor, Long> {

    @Query("SELECT c FROM Counselor c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Counselor> findByIdWithUser(@Param("id")Long id);

    @Query("SELECT c FROM Counselor c JOIN FETCH c.user")
    List<Counselor> findAllWithUser();
}
