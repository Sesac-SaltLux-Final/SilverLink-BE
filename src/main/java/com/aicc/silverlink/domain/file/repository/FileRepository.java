package com.aicc.silverlink.domain.file.repository;

import com.aicc.silverlink.domain.file.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileAttachment, Long> {
}
