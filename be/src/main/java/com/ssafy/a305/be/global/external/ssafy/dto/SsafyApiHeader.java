package com.ssafy.a305.be.global.external.ssafy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * SSAFY 금융 API 공통 Header DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsafyApiHeader {

    @JsonProperty("apiName")
    private String apiName;

    @JsonProperty("transmissionDate")
    private String transmissionDate;

    @JsonProperty("transmissionTime")
    private String transmissionTime;

    @JsonProperty("institutionCode")
    @Builder.Default
    private String institutionCode = "00100";

    @JsonProperty("fintechAppNo")
    @Builder.Default
    private String fintechAppNo = "001";

    @JsonProperty("apiServiceCode")
    private String apiServiceCode;

    @JsonProperty("institutionTransactionUniqueNo")
    private String institutionTransactionUniqueNo;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("userKey")
    private String userKey;

    // 응답 전용 필드
    @JsonProperty("responseCode")
    private String responseCode;

    @JsonProperty("responseMessage")
    private String responseMessage;
}
