package com.ssafy.a305.be.global.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ws://localhost:8080/ws-chat 로 소켓 연결 허용
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*"); // CORS 전체 허용 (개발용)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 수신(구독)할 경로의 Prefix -> 내장 SimpleBroker 활성화
        registry.enableSimpleBroker("/sub");
        
        // 클라이언트가 송신(발행)할 경로의 Prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 메시지 수신 전에 가로채는 인터셉터 등록 (토큰 검증 목적)
        registration.interceptors(stompHandler);
    }
}
