package com.ssafy.a305.be.global.seed;

import com.ssafy.a305.be.domain.enums.TermsType;
import com.ssafy.a305.be.domain.user.entity.TermsMaster;
import com.ssafy.a305.be.domain.user.repository.TermsMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 약관 마스터 초기 데이터, 기동 시 (terms_key, terms_version) 없을 때만 삽입 (멱등성 보장).
 * 수동 실행용 SQL: db/manual/20260317_seed_terms_master.sql
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TermsMasterDataSeeder implements ApplicationRunner, Ordered {

    private static final String TERMS_VERSION = "v1.0";

    private static final List<TermDefinition> DEFINITIONS = List.of(
            new TermDefinition(
                    "service_terms",
                    TermsType.mandatory,
                    "빌리지 서비스 이용을 위해 서비스 이용약관에 동의합니다. 본 약관에는 서비스 이용 조건, 회원의 권리와 의무, 운영 정책이 포함됩니다."),
            new TermDefinition(
                    "privacy_policy",
                    TermsType.mandatory,
                    "회원가입 및 서비스 제공을 위해 이메일, 휴대폰 번호 등 필요한 개인정보를 수집하고 이용하는 것에 동의합니다."),
            new TermDefinition(
                    "marketing_notifications",
                    TermsType.optional,
                    "이벤트, 혜택, 프로모션 등 마케팅 정보 수신에 동의합니다. 동의하지 않아도 기본 서비스 이용은 가능합니다."),
            new TermDefinition(
                    "personalized_recommendations",
                    TermsType.optional,
                    "서비스 이용 기록을 바탕으로 맞춤형 추천 및 개인화된 안내를 제공받는 것에 동의합니다."));

    private final TermsMasterRepository termsMasterRepository;

    @Override
    public void run(ApplicationArguments args) {
        int inserted = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TermDefinition def : DEFINITIONS) {
            if (termsMasterRepository.existsByTermsKeyAndTermsVersion(def.key(), TERMS_VERSION)) {
                continue;
            }
            termsMasterRepository.save(
                    TermsMaster.builder()
                            .termsKey(def.key())
                            .termsType(def.type())
                            .termsContent(def.content())
                            .termsVersion(TERMS_VERSION)
                            .createdDttm(now)
                            .build());
            inserted++;
            log.info("Terms master seeded: {} ({})", def.key(), TERMS_VERSION);
        }
        if (inserted == 0) {
            log.debug("Terms master seed skipped (all rows already present).");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private record TermDefinition(String key, TermsType type, String content) {}
}
