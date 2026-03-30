package com.ssafy.a305.be.domain.bank.client;

import com.ssafy.a305.be.domain.auth.config.SsafyApiProperties;
import com.ssafy.a305.be.domain.bank.client.dto.request.header.SsafyApiHeaderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class SsafyApiHeaderFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter TX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SsafyApiProperties ssafyApiProperties;

    public SsafyApiHeaderDto create(String apiName, String userKey) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        return new SsafyApiHeaderDto(
                apiName,
                DATE_FORMATTER.format(now),
                TIME_FORMATTER.format(now),
                ssafyApiProperties.getInstitutionCode(),
                ssafyApiProperties.getFintechAppNo(),
                apiName,
                generateInstitutionTransactionUniqueNo(now),
                ssafyApiProperties.getApiKey(),
                userKey
        );
    }

    private String generateInstitutionTransactionUniqueNo(ZonedDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return TX_FORMATTER.format(now) + random;
    }
}
