package com.aicc.silverlink.domain.complaint.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "민원", description = "민원 접수/조회 API")
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
}
