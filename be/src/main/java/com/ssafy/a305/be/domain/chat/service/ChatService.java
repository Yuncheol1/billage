package com.ssafy.a305.be.domain.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.ssafy.a305.be.domain.user.entity.UserProfile;
import org.springframework.stereotype.Service;

import org.springframework.context.ApplicationEventPublisher;

import com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto;
import com.ssafy.a305.be.domain.notification.dto.ChatNotificationEvent;
import com.ssafy.a305.be.domain.chat.dto.ChatMessageResponseDto;
import com.ssafy.a305.be.domain.chat.dto.ChatRoomResponseDto;
import com.ssafy.a305.be.domain.chat.dto.ParticipantDto;
import com.ssafy.a305.be.domain.chat.entity.ChatMessage;
import com.ssafy.a305.be.domain.chat.entity.ChatRoom;
import com.ssafy.a305.be.domain.chat.repository.ChatMessageRepository;
import com.ssafy.a305.be.domain.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final com.ssafy.a305.be.domain.user.repository.UserRepository userRepository;
    private final com.ssafy.a305.be.domain.user.repository.UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 더미 테스트를 위해 채팅방이 없으면 자동으로 생성해주는 메서드 포함
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto requestDto) {
        if (requestDto.content() != null && requestDto.content().length() > 255) {
            throw new IllegalArgumentException("메시지는 255자 이하로 입력해주세요.");
        }
        
        log.debug("Saving message to room {}: {}", requestDto.roomId(), requestDto.content());
        // 방 존재 확인 (없으면 생성 - 테스트용)
        ChatRoom chatRoom = chatRoomRepository.findById(requestDto.roomId())
            .orElseGet(() -> {
                // 더미 채팅방 자동 생성 로직 (1번과 2번이 있다고 가정)
                // 실제 서비스에서는 방 생성 API를 먼저 태우는 것이 정석입니다.
                ChatRoom newRoom = ChatRoom.builder()
                    .id(requestDto.roomId())
                    .participants(List.of(1, 2))
                    .build();
                return chatRoomRepository.save(newRoom);
            });

        // 메시지 생성
        ChatMessage message = ChatMessage.builder()
            .roomId(chatRoom.getId())
            .senderSeq(requestDto.senderSeq())
            .content(requestDto.content())
            .isContractMsg(requestDto.isContractMsg())
            .msgType(requestDto.msgType())
            .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 방 정보 업데이트 (마지막 메시지 등 & 보낸 사람 읽음 처리)
        chatRoom.updateLastMessage(savedMessage.getContent(), savedMessage.getCreatedAt());
        chatRoom.getLastReadAt().put(String.valueOf(requestDto.senderSeq()), savedMessage.getCreatedAt());
        chatRoomRepository.save(chatRoom);

        String senderName = userProfileRepository.findByUserUserSeq(requestDto.senderSeq())
            .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
            .orElse("알 수 없음");

        // 푸시 알림 이벤트를 발행 (수신자들에게 전송)
        chatRoom.getParticipants().stream()
            .filter(participantSeq -> !participantSeq.equals(requestDto.senderSeq()))
            .forEach(recipientSeq -> {
                eventPublisher.publishEvent(new ChatNotificationEvent(
                    chatRoom.getId(),
                    requestDto.senderSeq(),
                    recipientSeq,
                    senderName,
                    requestDto.content()
                ));
            });

        return ChatMessageResponseDto.of(savedMessage, senderName);
    }

    public List<ChatMessageResponseDto> getMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(msg -> {
                    String senderName = userProfileRepository.findByUserUserSeq(msg.getSenderSeq())
                        .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
                        .orElse("알 수 없음");
                    return ChatMessageResponseDto.of(msg, senderName);
                })
                .collect(Collectors.toList());
    }

    public List<ChatRoomResponseDto> getRoomsByUserSeq(Integer userSeq) {
        return chatRoomRepository.findByParticipantsContainingOrderByLastModifiedAtDesc(userSeq).stream()
                .map(room -> {
                    List<ParticipantDto> participantDtos = room.getParticipants().stream()
                        .map(this::apply)
                        .collect(Collectors.toList());
                    
                    // 안읽은 메시지 수 계산 (LocalDateTime.MIN은 BSON 변환 시 long overflow 발생하므로 1970년 사용)
                    LocalDateTime lastReadAt = room.getLastReadAt().getOrDefault(String.valueOf(userSeq), LocalDateTime.of(1970, 1, 1, 0, 0));
                    long unreadCount = chatMessageRepository.countByRoomIdAndCreatedAtAfterAndSenderSeqNot(room.getId(), lastReadAt, userSeq);
                    
                    return ChatRoomResponseDto.of(room, participantDtos, unreadCount);
                })
                .collect(Collectors.toList());
    }

    public ChatRoomResponseDto createRoomByEmail(Integer myUserSeq, String targetEmail) {
        // 찾으려는 상대방 이메일
        com.ssafy.a305.be.domain.user.entity.User targetUser = userRepository.findByEmail(targetEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + targetEmail));
        
        Integer targetUserSeq = targetUser.getUserSeq();
        
        if (myUserSeq.equals(targetUserSeq)) {
            throw new IllegalArgumentException("자기 자신과 채팅방을 생성할 수 없습니다.");
        }

        // 이미 생성된 방인지 확인
        List<ChatRoom> myRooms = chatRoomRepository.findByParticipantsContainingOrderByLastModifiedAtDesc(myUserSeq);
        ChatRoom existingRoom = myRooms.stream()
            .filter(room -> room.getParticipants().contains(targetUserSeq))
            .findFirst()
            .orElse(null);

        ChatRoom chatRoom;
        if (existingRoom != null) {
            chatRoom = existingRoom;
        } else {
            // 새로운 방 생성 (id는 몽고DB ObjectId 자동 생성 혹은 무작위 UUID)
            chatRoom = ChatRoom.builder()
                .id(java.util.UUID.randomUUID().toString())
                .participants(List.of(myUserSeq, targetUserSeq))
                .build();
            // 초기 읽음 시각 설정
            chatRoom.getLastReadAt().put(String.valueOf(myUserSeq), LocalDateTime.now());
            chatRoom.getLastReadAt().put(String.valueOf(targetUserSeq), LocalDateTime.now());
            chatRoom = chatRoomRepository.save(chatRoom);
        }

        // DTO 반환
        List<ParticipantDto> participantDtos = chatRoom.getParticipants().stream()
            .map(this::apply2)
            .collect(Collectors.toList());

        // LocalDateTime.MIN 대신 안전한 과거 시각 사용
        LocalDateTime lastReadAt = chatRoom.getLastReadAt().getOrDefault(String.valueOf(myUserSeq), LocalDateTime.of(1970, 1, 1, 0, 0));
        long unreadCount = chatMessageRepository.countByRoomIdAndCreatedAtAfterAndSenderSeqNot(chatRoom.getId(), lastReadAt, myUserSeq);

        return ChatRoomResponseDto.of(chatRoom, participantDtos, unreadCount);
    }

    public void markAsRead(String roomId, Integer userSeq) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        room.getLastReadAt().put(String.valueOf(userSeq), LocalDateTime.now());
        chatRoomRepository.save(room);
    }

    public void leaveRoom(Integer userSeq, String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));
        
        List<Integer> participants = new java.util.ArrayList<>(room.getParticipants());
        if (participants.contains(userSeq)) {
            participants.remove(userSeq);
            // 참여자가 더이상 없으면 방 삭제
            if (participants.isEmpty()) {
                chatRoomRepository.delete(room);
                // 관련된 메시지들도 삭제할 수 있습니다.
                // chatMessageRepository.deleteByRoomId(roomId); 
            } else {
                // 방의 참여자 목록 업데이트
                room.updateParticipants(participants);
                chatRoomRepository.save(room);
            }
        }
    }

    private ParticipantDto apply(Integer seq) {
        String name = userProfileRepository.findByUserUserSeq(seq)
                .map(UserProfile::getRealName)
                .orElse("알 수 없음");
        return ParticipantDto.of(seq, name);
    }

    private ParticipantDto apply2(Integer seq) {
        String name = userProfileRepository.findByUserUserSeq(seq)
                .map(UserProfile::getRealName)
                .orElse("알 수 없음");
        return ParticipantDto.of(seq, name);
    }
}
