package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 계좌 목록 조회(inquireDemandDepositAccountList) 요청 바디.
 *
 * @param header 공통 Header (추가 필드 없음)
 */
public record SsafyInquireDemandDepositAccountListRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header
) {
}
