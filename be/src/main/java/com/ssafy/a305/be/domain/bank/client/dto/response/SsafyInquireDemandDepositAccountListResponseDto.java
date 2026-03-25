package com.ssafy.a305.be.domain.bank.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.response.header.SsafyApiResponseHeaderDto;

import java.util.List;

/**
 * SSAFY 수시입출금 계좌 목록 조회 응답 바디.
 *
 * @param header 응답 Header
 * @param rec    계좌 목록 (배열). 없으면 null 또는 빈 리스트로 매핑될 수 있음
 */
public record SsafyInquireDemandDepositAccountListResponseDto(
        @JsonProperty("Header")
        SsafyApiResponseHeaderDto header,
        @JsonProperty("REC")
        List<SsafyDemandDepositAccountRecDto> rec
) {
}
