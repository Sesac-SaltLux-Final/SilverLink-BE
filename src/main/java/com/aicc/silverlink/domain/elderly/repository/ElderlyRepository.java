package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElderlyRepository extends JpaRepository<Elderly,Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<Elderly> findWithUserById(Long id);

    boolean existsBy(Long id);
}
