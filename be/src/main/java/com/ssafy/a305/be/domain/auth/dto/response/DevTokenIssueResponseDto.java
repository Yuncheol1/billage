package com.ssafy.a305.be.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DevTokenIssueResponseDto {
    private Integer userSeq;
    private String email;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
    private String userState;
}
