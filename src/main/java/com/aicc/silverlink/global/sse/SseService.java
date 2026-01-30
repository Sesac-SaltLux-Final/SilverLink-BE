package com.aicc.silverlink.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long callId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 30); // 30분 타임아웃
        emitters.put(callId, emitter);

        emitter.onCompletion(() -> emitters.remove(callId));
        emitter.onTimeout(() -> emitters.remove(callId));

        // 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            log.error("SSE connection error", e);
        }

        log.info("SSE Connected: callId={}", callId);
        return emitter;
    }

    public void broadcast(Long callId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(callId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("SSE send error: callId={}", callId, e);
                emitters.remove(callId);
            }
        }
    }
}
