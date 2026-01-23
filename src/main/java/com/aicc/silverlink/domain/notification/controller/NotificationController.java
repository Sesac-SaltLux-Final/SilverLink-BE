package com.aicc.silverlink.domain.notification.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "알림", description = "푸시 알림 API")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
}
