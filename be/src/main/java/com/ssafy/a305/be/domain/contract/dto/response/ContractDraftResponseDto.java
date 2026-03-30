package com.ssafy.a305.be.domain.contract.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ContractDraftResponseDto {

    @NotNull
    private Integer contractSeq;//PK

    @NotNull
    private String contractId;
}
