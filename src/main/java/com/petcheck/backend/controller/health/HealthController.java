package com.petcheck.backend.controller.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 프론트-백 연결 확인용. 프론트에서 "백엔드 상태: ok" 가 보이면 세팅이 끝난 것이다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "time", LocalDateTime.now().toString()
        );
    }
}
