package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 계좌 단건 조회(inquireDemandDepositAccount) 요청 바디.
 *
 * @param header    공통 Header
 * @param accountNo 조회할 계좌번호
 */
public record SsafyInquireDemandDepositAccountRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountNo
) {
}
