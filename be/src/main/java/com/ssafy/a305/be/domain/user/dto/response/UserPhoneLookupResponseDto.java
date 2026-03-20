package com.ssafy.a305.be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPhoneLookupResponseDto {
    private boolean registered;
    private Integer userSeq;
}
