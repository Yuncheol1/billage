package com.ssafy.a305.be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponseDto {
    private Integer userSeq;
    private String realName;
    private String email;
    private String phone;
}
