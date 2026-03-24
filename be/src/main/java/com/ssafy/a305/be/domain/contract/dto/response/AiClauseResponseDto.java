package com.ssafy.a305.be.domain.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AiClauseResponseDto {

    @JsonProperty("clause_id")
    private String clauseId;

    private String title;

    @JsonProperty("legal_clause")
    private String legalClause;

    private String explanation;
}
