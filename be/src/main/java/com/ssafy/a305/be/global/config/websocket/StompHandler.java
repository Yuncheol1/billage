package com.ssafy.a305.be.global.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StompHandler implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // 현재는 로그인 미구현 상태이므로 임시로 연결을 그냥 허용합니다.
                log.info("STOMP CONNECT : {}", accessor.getSessionId());
                
                // 프론트에서 임시로 보내주는 userSeq를 세션 속성에 저장 (필요시)
                String userSeq = accessor.getFirstNativeHeader("userSeq");
                if (userSeq != null) {
                    accessor.getSessionAttributes().put("userSeq", userSeq);
                    log.info("Connected userSeq: {}", userSeq);
                }
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                log.info("STOMP SUBSCRIBE : destination={}", accessor.getDestination());
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                log.info("STOMP DISCONNECT : {}", accessor.getSessionId());
            }
        }
        
        return message;
    }
}
