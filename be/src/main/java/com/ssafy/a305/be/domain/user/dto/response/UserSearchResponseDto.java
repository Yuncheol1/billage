package com.ssafy.a305.be.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponseDto {
    private Integer userSeq;
    private String email;
    private String realName;
    private String profileImageUrl;
}
