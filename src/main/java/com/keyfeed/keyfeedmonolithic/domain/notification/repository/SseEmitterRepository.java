package com.keyfeed.keyfeedmonolithic.domain.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class SseEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Emitter 저장
    public SseEmitter save(String emitterId, SseEmitter emitter) {
        emitters.put(emitterId, emitter);
        return emitter;
    }

    // 특정 Emitter 삭제
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    // 특정 사용자의 모든 활성 Emitter 조회
    public Map<String, SseEmitter> findAllEmitterStartWithByUserId(String userId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId + "_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}