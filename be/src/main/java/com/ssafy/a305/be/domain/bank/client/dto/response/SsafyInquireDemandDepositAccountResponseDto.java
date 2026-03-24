package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/** 수시입출금 계좌 단건 조회 응답 DTO */
public record SsafyInquireDemandDepositAccountResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        SsafyDemandDepositAccountRecDto rec
) {
}
