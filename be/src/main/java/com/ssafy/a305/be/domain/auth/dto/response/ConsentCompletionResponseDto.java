package com.ssafy.a305.be.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConsentCompletionResponseDto {

    private Integer userSeq;
    private String userState;
}
