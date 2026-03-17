package com.ssafy.a305.be.domain.chat.dto;

public record ParticipantDto(
    Integer userSeq,
    String name
) {
    public static ParticipantDto of(Integer userSeq, String name) {
        return new ParticipantDto(userSeq, name);
    }
}
