package com.ssafy.a305.be.domain.contract.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiClauseResponseDto {

    private Boolean rejected;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    private String title;
    
    @JsonProperty("clause_id")
    private String clauseId;

    @JsonProperty("legal_clause")
    private String legalClause;

    private String explanation;
    

}
