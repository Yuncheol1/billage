package com.ssafy.a305.be.domain.contract.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractSignatureResponseDto {
    private Integer signatureSeq;
    private Integer userSeq;
    private String role;
    private boolean contractFullySigned;
    private Integer generatedDocumentSeq;
}
