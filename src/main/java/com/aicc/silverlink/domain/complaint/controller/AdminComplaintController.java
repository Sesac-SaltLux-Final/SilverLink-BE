package com.aicc.silverlink.domain.complaint.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "민원 관리 (관리자)", description = "민원 처리 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/complaints")
public class AdminComplaintController {
}
