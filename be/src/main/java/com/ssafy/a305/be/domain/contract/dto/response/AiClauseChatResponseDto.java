package com.ssafy.a305.be.domain.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiClauseChatResponseDto {
    private Boolean rejected;
    private String rejection_reason;
    private List<AiClauseResponseDto> clauses;
    private String summary;
}
