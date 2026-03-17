package com.ssafy.a305.be.domain.chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto;
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

    // 더미 테스트를 위해 채팅방이 없으면 자동으로 생성해주는 메서드 포함
    public ChatMessageResponseDto saveMessage(ChatMessageRequestDto requestDto) {
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
            .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 방 정보 업데이트 (마지막 메시지 등)
        chatRoom.updateLastMessage(savedMessage.getContent());
        chatRoomRepository.save(chatRoom);

        String senderName = userProfileRepository.findByUser_UserSeq(requestDto.senderSeq().longValue())
            .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
            .orElse("알 수 없음");

        return ChatMessageResponseDto.of(savedMessage, senderName);
    }

    public List<ChatMessageResponseDto> getMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(msg -> {
                    String senderName = userProfileRepository.findByUser_UserSeq(msg.getSenderSeq().longValue())
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
                        .map(seq -> {
                            String name = userProfileRepository.findByUser_UserSeq(seq.longValue())
                                .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
                                .orElse("알 수 없음");
                            return ParticipantDto.of(seq, name);
                        })
                        .collect(Collectors.toList());
                    return ChatRoomResponseDto.of(room, participantDtos);
                })
                .collect(Collectors.toList());
    }

    public ChatRoomResponseDto createRoomByEmail(Integer myUserSeq, String targetEmail) {
        // 찾으려는 상대방 이메일
        com.ssafy.a305.be.domain.user.entity.User targetUser = userRepository.findByEmail(targetEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + targetEmail));
        
        Integer targetUserSeq = targetUser.getUserSeq().intValue();
        
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
            chatRoom = chatRoomRepository.save(chatRoom);
        }

        // DTO 반환
        List<ParticipantDto> participantDtos = chatRoom.getParticipants().stream()
            .map(seq -> {
                String name = userProfileRepository.findByUser_UserSeq(seq.longValue())
                    .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
                    .orElse("알 수 없음");
                return ParticipantDto.of(seq, name);
            })
            .collect(Collectors.toList());

        return ChatRoomResponseDto.of(chatRoom, participantDtos);
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
}
