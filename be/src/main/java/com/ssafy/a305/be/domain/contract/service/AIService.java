package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.AiClauseChatRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseChatResponseDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIService {

    @Value("${ai.internal.api.key}")
    private String internalApiKey;

    public AiClauseResponseDto generateClause(AiClauseRequestDto requestDto) {
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

    public String analyzeContract(Contract contract, String documentContent) {
        RestTemplate restTemplate = new RestTemplate();
        String aiServerUrl = "http://3.35.206.231:8000/internal/v1/analyze";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-API-Key", internalApiKey);

        // Map contract data to AI schema
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contract_seq", contract.getContractSeq());
        requestBody.put("contract_text", documentContent);
        requestBody.put("mode", "full");
        
        // Entity uses 'family' boolean instead of templateType string
        String templateType = Boolean.TRUE.equals(contract.getFamily()) ? "FAMILY_JEONSE" : "FRIEND_SMALL";
        requestBody.put("template_type", templateType);
        
        requestBody.put("principal_amount", contract.getPrincipalAmount());
        requestBody.put("interest_rate", contract.getInterestRate());
        requestBody.put("repayment_method", contract.getRepaymentMethod());
        requestBody.put("start_dt", contract.getStartDt() != null ? contract.getStartDt().toString() : null);
        requestBody.put("end_dt", contract.getEndDt() != null ? contract.getEndDt().toString() : null);

        // Map clauses
        if (contract.getClauses() != null) {
            List<Map<String, String>> clauses = contract.getClauses().stream()
                .map(clause -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("clause_id", clause.getClauseId() != null ? clause.getClauseId() : "");
                    map.put("title", clause.getTitle() != null ? clause.getTitle() : "");
                    map.put("content", clause.getContent() != null ? clause.getContent() : "");
                    return map;
                })
                .collect(Collectors.toList());
            requestBody.put("clauses", clauses);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Requesting AI for contract analysis: contractSeq={}, templateType={}", contract.getContractSeq(), templateType);
            ResponseEntity<String> response = restTemplate.exchange(
                    aiServerUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("AI Analysis Server returned error: {}", response.getStatusCode());
                return null;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("AI Analysis Client Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("AI Analysis Communication Error: ", e);
            return null;
        }
    }

    public AiClauseChatResponseDto generateClausesFromChat(AiClauseChatRequestDto requestDto) {
        RestTemplate restTemplate = new RestTemplate();
        String aiServerUrl = "http://3.35.206.231:8000/internal/v1/clauses/from-chat";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-API-Key", internalApiKey);

        HttpEntity<AiClauseChatRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            log.info("Requesting AI for clause generation from chat history");
            ResponseEntity<AiClauseChatResponseDto> response = restTemplate.exchange(
                    aiServerUrl,
                    HttpMethod.POST,
                    entity,
                    AiClauseChatResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("AI Server (from-chat) returned error Code: {}", response.getStatusCode());
                throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 협의 내용 생성 실패");
            }
        } catch (Exception e) {
            log.error("AI Server (from-chat) 통신 에러: ", e);
            throw new CustomBusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 서버와 통신 중 오류가 발생했습니다.");
        }
    }
}
