package com.ssafy.a305.be.domain.bank.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;

/**
 * SSAFY 수시입출금 입금(updateDemandDepositAccountDeposit) 요청 바디.
 */
public record SsafyUpdateDemandDepositAccountDepositRequestDto(
        @JsonProperty("Header")
        SsafyApiHeaderDto header,
        String accountNo,
        String transactionBalance,
        String transactionSummary
) {
}
