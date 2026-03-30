package com.ssafy.a305.be.domain.contract.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiClauseRequestDto {

    @JsonProperty("contract_seq")
    private Integer contractSeq;

    @JsonProperty("user_intent")
    private String userIntent;

    @JsonProperty("contract_context")
    private String contractContext;

    @JsonProperty("template_type")
    private String templateType;
}
