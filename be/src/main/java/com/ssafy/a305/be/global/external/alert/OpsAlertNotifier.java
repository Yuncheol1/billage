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
 * 운영 알림을 웹훅으로 보낸다.
 *
 * <p>Slack·Mattermost의 Incoming Webhook 형식({@code {"text": ...}})을 따른다.
 * URL이 설정되지 않은 환경(로컬·테스트)에서는 보내지 않고 로그만 남긴다.</p>
 *
 * <p>알림은 부가 기능이다. 전송에 실패해도 예외를 던지지 않아, 알림 때문에 이체·정산이 멈추지 않는다.</p>
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
