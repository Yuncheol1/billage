package com.ssafy.a305.be.domain.chat.dto;

public record ParticipantDto(
    Integer userSeq,
    String name,
    String profileImageUrl
) {
    public static ParticipantDto of(Integer userSeq, String name, String profileImageUrl) {
        return new ParticipantDto(userSeq, name, profileImageUrl);
    }
}
