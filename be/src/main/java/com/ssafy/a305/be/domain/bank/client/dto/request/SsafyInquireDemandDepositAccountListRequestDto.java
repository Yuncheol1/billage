package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/** 수시입출금 계좌 목록 조회 요청 DTO */
public record SsafyInquireDemandDepositAccountListRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header
) {
}
