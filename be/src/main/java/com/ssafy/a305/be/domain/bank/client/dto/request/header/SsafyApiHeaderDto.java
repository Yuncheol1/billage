package com.ssafy.a305.be.domain.bank.client.dto.request.header;

import com.fasterxml.jackson.annotation.JsonProperty;

/** SSAFY 요청 공통 Header DTO */
public record SsafyApiHeaderDto(
        @JsonProperty("apiName")
        String apiName,
        @JsonProperty("transmissionDate")
        String transmissionDate,
        @JsonProperty("transmissionTime")
        String transmissionTime,
        @JsonProperty("institutionCode")
        String institutionCode,
        @JsonProperty("fintechAppNo")
        String fintechAppNo,
        @JsonProperty("apiServiceCode")
        String apiServiceCode,
        @JsonProperty("institutionTransactionUniqueNo")
        String institutionTransactionUniqueNo,
        @JsonProperty("apiKey")
        String apiKey,
        @JsonProperty("userKey")
        String userKey
) {
}
