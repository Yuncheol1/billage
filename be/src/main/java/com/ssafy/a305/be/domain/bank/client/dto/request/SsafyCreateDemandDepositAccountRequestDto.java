package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 계좌 생성(createDemandDepositAccount) 요청 바디.
 *
 * @param header               공통 Header
 * @param accountTypeUniqueNo  개설할 상품의 고유번호 (명세의 accountTypeUniqueNo)
 */
public record SsafyCreateDemandDepositAccountRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountTypeUniqueNo
) {
}
