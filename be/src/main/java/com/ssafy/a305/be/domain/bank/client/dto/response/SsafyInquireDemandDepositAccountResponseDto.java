package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

/**
 * SSAFY 수시입출금 계좌 단건 조회 응답 바디.
 *
 * @param header 응답 Header
 * @param rec    단일 계좌 REC
 */
public record SsafyInquireDemandDepositAccountResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        SsafyDemandDepositAccountRecDto rec
) {
}
