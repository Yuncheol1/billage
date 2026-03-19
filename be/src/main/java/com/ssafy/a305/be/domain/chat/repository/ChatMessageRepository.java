package com.ssafy.a305.be.domain.chat.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.ssafy.a305.be.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);
    long countByRoomIdAndCreatedAtAfterAndSenderSeqNot(String roomId, java.time.LocalDateTime createdAt, Integer senderSeq);
}
