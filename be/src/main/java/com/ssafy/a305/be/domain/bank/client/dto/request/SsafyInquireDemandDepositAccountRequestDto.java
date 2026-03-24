package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/** 수시입출금 계좌 단건 조회 요청 DTO */
public record SsafyInquireDemandDepositAccountRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountNo
) {
}
