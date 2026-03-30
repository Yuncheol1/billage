package com.ssafy.a305.be.global.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
