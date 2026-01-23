package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.call.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallAnalysisService {
    private final CallRecordRepository callRecordRepository;
    private final LlmModelRepository llmModelRepository;
    private final ElderlyResponseRepository elderlyResponseRepository;
    private final CallSummaryRepository callSummaryRepository;
    private final CallEmotionRepository callEmotionRepository;
    private final EssentialQuestionRepository essentialQuestionRepository;

    // TODO: Implement analysis logic
}
