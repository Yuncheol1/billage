package com.ssafy.a305.be.domain.chat.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;
    
    private String roomId;
    private Integer senderSeq; // 보낸 사람의 userSeq
    private String content;
    private LocalDateTime createdAt;
    
    // contract 여부 등 추가 데이터
    private Boolean isContractMsg;
    private String msgType; // TEXT, SYSTEM, AI_PROPOSAL
    
    @Builder
    public ChatMessage(String roomId, Integer senderSeq, String content, Boolean isContractMsg, String msgType) {
        this.roomId = roomId;
        this.senderSeq = senderSeq;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.isContractMsg = isContractMsg != null ? isContractMsg : false;
        this.msgType = msgType != null ? msgType : "TEXT";
    }
}
