package com.ssafy.a305.be.domain.bank.client.dto.response.header;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SSAFY Open API 응답 JSON 바디의 {@code Header} 객체.
 *
 * @param responseCode                    결과 코드 (정상 예: H0000)
 * @param responseMessage                 결과 메시지
 * @param apiName                         호출된 API 이름
 * @param transmissionDate                전송일자
 * @param transmissionTime                전송시각
 * @param institutionCode                 기관코드
 * @param apiKey                          API 키 (응답에 포함되는 경우)
 * @param apiServiceCode                  API 서비스 코드
 * @param institutionTransactionUniqueNo  요청 시 보낸 기관 거래고유번호
 */
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
