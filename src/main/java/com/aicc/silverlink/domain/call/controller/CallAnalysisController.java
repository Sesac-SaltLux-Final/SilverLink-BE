package com.aicc.silverlink.domain.call.controller;

import com.aicc.silverlink.domain.call.service.CallAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/call-analysis")
@RequiredArgsConstructor
public class CallAnalysisController {
    private final CallAnalysisService callAnalysisService;

    // TODO: Add endpoints
}
