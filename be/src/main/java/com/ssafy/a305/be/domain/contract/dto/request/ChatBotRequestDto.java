package com.ssafy.a305.be.domain.contract.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotRequestDto {
    private String message;
    private List<AiChatMessageDto> history;
    private Integer contract_seq;
}
