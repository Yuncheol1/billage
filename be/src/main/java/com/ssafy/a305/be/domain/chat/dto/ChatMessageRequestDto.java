package com.ssafy.a305.be.domain.chat.dto;

import jakarta.validation.constraints.Size;

public record ChatMessageRequestDto(
    String roomId,
    Integer senderSeq,
    @Size(max = 10000)
    String content,
    Boolean isContractMsg,
    String msgType
) {}
