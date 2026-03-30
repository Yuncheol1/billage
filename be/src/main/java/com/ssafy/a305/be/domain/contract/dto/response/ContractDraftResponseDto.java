package com.ssafy.a305.be.domain.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ContractDraftResponseDto {

    private Long contractSeq;
    private String contractId;
    private String status;
    private AiDraftContent aiDraftContent;
    private List<ClauseResponse> clauses;

    @Getter
    @Builder
    public static class AiDraftContent {
        private String summary;
        private List<String> warningFlags;
    }

    @Getter
    @Builder
    public static class ClauseResponse {
        private Long clauseSeq;
        private String clauseId;
        private String title;
        private String content;
        private Integer orderNo;
    }
}
