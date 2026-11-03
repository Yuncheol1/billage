package com.ssafy.a305.be.global.external.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * 운영 알림 웹훅 (Slack·Mattermost). URL이 없으면 로그만 남긴다.
 * 전송 실패로 이체·정산이 멈추지 않도록 예외를 던지지 않는다.
 */
@Slf4j
@Component
public class OpsAlertNotifier {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String webhookUrl;
    private final RestClient restClient;

    public OpsAlertNotifier(@Value("${alert.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TIMEOUT.toMillis());
        factory.setReadTimeout((int) TIMEOUT.toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[운영 알림] 웹훅 미설정 - {}", message);
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.error("[운영 알림] 전송 실패 - {}. 사유={}", message, e.getMessage());
        }
    }
}
