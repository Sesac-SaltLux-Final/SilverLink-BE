package com.aicc.silverlink.domain.medication.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "복약", description = "복약 정보 관리 API")
@RestController
@RequestMapping("/api/medications")
public class MedicationController {
}
