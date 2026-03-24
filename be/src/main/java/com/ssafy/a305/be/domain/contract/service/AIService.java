package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Slf4j
@Service

public class AIService {

    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.internal.api.key}")
    private String internalApiKey;
    public AIService(){
        this.restTemplate=new RestTemplate();
    }


    public AiClauseResponseDto generateClause(AiClauseRequestDto requestDto) {
        // 빈 등록 없이도 바로 생성하여 쓸 수 있게 구성함 (RestTemplate 안전망)
        RestTemplate restTemplate = new RestTemplate();
        String aiServerUrl = "http://3.35.206.231:8000/internal/v1/clauses/generate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-API-Key", internalApiKey);

        HttpEntity<AiClauseRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            log.info("Requesting AI for clause generation: contractSeq={}", requestDto.getContractSeq());
            ResponseEntity<AiClauseResponseDto> response = restTemplate.exchange(
                    aiServerUrl,
                    HttpMethod.POST,
                    entity,
                    AiClauseResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("AI Server returned error HTTP Code: {}", response.getStatusCode());
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 서버 연동 실패");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("AI Server Client Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 403) {
                throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "AI 서버 인증 실패(403 Forbidden)");
            } else if (e.getStatusCode().value() == 422) {
                throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 서버 검증 실패(422 Unprocessable Entity)");
            }
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "AI 서버 요청 실패: " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("AI Server Server Error (5xx): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 서버 장애가 발생했습니다.");
        } catch (Exception e) {
            log.error("AI Server 통신 에러 발생: ", e);
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 특약 생성 서버 요청 중 오류가 발생했습니다.");
        }
    }
}
