package com.aicc.silverlink.domain.session.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "세션", description = "사용자 세션 관리 API")
@RestController
@RequestMapping("/api/sessions")
public class SessionController {
}
