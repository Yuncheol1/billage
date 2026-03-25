package com.ssafy.a305.be.domain.bank.client.dto.request.header;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSAFY Open API 요청 시 JSON 바디의 {@code Header} 객체.
 *
 * @param apiName                         호출 API 이름 (명세의 apiName과 동일)
 * @param transmissionDate                전송일자 (yyyyMMdd)
 * @param transmissionTime                전송시각 (HHmmss)
 * @param institutionCode                 기관코드
 * @param fintechAppNo                    핀테크 앱 번호
 * @param apiServiceCode                  API 서비스 코드 (보통 apiName과 동일)
 * @param institutionTransactionUniqueNo  기관 거래고유번호 (요청마다 유일하게 생성)
 * @param apiKey                          SSAFY API 키
 * @param userKey                         SSAFY 회원 userKey
 */
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
