package com.aicc.silverlink.domain.welfare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class WelfareService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
