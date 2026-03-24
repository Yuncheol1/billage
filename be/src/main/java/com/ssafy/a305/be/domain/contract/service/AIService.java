package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.internal.api.key}")
    private String internalApiKey;

    public AiClauseResponseDto generateClause(AiClauseRequestDto requestDto) {
        log.info("Calling AI Server for clause generation: {}", aiServerUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-API-Key", internalApiKey);

        HttpEntity<AiClauseRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            return restTemplate.postForObject(aiServerUrl, entity, AiClauseResponseDto.class);
        } catch (HttpClientErrorException e) {
            log.error("AI Server Client Error (4xx): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 422) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 서버: 입력값이 유효하지 않습니다.");
            }
            if (e.getStatusCode().value() == 403) {
                throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "AI 서버: API Key 혹은 접근 권한이 없습니다.");
            }
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "AI 서버 요청 실패: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("AI Server Server Error (5xx): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 서버 장애가 발생했습니다.");
        } catch (Exception e) {
            log.error("AI Server Communication Error: {}", e.getMessage());
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}
