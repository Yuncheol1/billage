package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

import java.util.List;

/** 수시입출금 계좌 목록 조회 응답 DTO */
public record SsafyInquireDemandDepositAccountListResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        List<SsafyDemandDepositAccountRecDto> rec
) {
}
