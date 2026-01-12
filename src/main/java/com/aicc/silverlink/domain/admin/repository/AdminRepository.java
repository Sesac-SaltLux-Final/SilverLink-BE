package com.aicc.silverlink.domain.admin.repository;

import com.aicc.silverlink.domain.admin.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<AdminProfile, Long> {
}
