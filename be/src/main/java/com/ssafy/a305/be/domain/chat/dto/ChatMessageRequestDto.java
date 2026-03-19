package com.ssafy.a305.be.domain.chat.dto;

import jakarta.validation.constraints.Size;

public record ChatMessageRequestDto(
    String roomId,
    Integer senderSeq,
    @Size(max = 255)
    String content,
    Boolean isContractMsg
) {}
