package com.ssafy.a305.be.domain.chat.dto;

public record ChatMessageRequestDto(
    String roomId,
    Integer senderSeq,
    String content,
    Boolean isContractMsg
) {}
