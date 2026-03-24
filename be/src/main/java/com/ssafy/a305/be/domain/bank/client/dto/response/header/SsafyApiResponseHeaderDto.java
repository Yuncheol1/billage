package com.ssafy.a305.be.domain.bank.client.dto.response.header;

import com.fasterxml.jackson.annotation.JsonProperty;

/** SSAFY 응답 공통 Header DTO */
public record SsafyApiResponseHeaderDto(
        @JsonProperty("responseCode")
        String responseCode,
        @JsonProperty("responseMessage")
        String responseMessage,
        @JsonProperty("apiName")
        String apiName,
        @JsonProperty("transmissionDate")
        String transmissionDate,
        @JsonProperty("transmissionTime")
        String transmissionTime,
        @JsonProperty("institutionCode")
        String institutionCode,
        @JsonProperty("apiKey")
        String apiKey,
        @JsonProperty("apiServiceCode")
        String apiServiceCode,
        @JsonProperty("institutionTransactionUniqueNo")
        String institutionTransactionUniqueNo
) {
}
