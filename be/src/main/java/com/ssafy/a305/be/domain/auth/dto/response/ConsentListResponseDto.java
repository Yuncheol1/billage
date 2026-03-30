package com.ssafy.a305.be.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConsentListResponseDto {

    private Integer userSeq;
    private String userState;
    private List<ConsentItem> consents;

    @Getter
    @Builder
    public static class ConsentItem {
        private Integer termsSeq;
        private String termsKey;
        private String termsType;
        private String termsVersion;
        private String termsContent;
        private Boolean agreed;
    }
}
