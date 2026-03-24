package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/** 수시입출금 계좌 생성 요청 DTO */
public record SsafyCreateDemandDepositAccountRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountTypeUniqueNo
) {
}
