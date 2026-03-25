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
public class AiClauseChatRequestDto {
    private List<AiChatMessageDto> chat_messages;
    private String contract_context;
    private String template_type;
}
