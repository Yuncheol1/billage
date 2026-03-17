package com.ssafy.a305.be.domain.chat.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.ssafy.a305.be.domain.chat.entity.ChatRoom;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    // 특정 유저가 포함된 채팅방 목록 조회 (수정일 늦은 순 정렬)
    List<ChatRoom> findByParticipantsContainingOrderByLastModifiedAtDesc(Integer userSeq);
}
