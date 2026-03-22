package com.ssafy.a305.be.global.config;

import jakarta.persistence.EntityManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TestDataInit {

    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;

    public TestDataInit(EntityManager em, PlatformTransactionManager transactionManager) {
        this.em = em;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * ApplicationReadyEvent를 사용하여 모든 스프링 빈과 프록시가 로드된 후 실행되도록 함.     * @PostConstruct 대신 이 방식을 권장.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        transactionTemplate.execute(status -> {
            try {
                log.info("🚀 채팅 테스트용 더미 데이터 초기화를 시작합니다.");

                // 1. 외래키 체크 일시 중단 (강제 삭제 및 재삽입을 위함)
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

                // 2. 기존 데이터 삭제 (채팅방 연관 데이터 등은 몽고DB나 다른 테이블에 있을 수 있음)
                // 여기서는 user와 user_profile만 확실히 초기화
                em.createNativeQuery("DELETE FROM `user_profile` WHERE user_seq IN (100, 101, 102)").executeUpdate();
                em.createNativeQuery("DELETE FROM `users` WHERE user_seq IN (100, 101, 102)").executeUpdate();
                
                log.info("   - 기존 테스트 계정 삭제 완료");

                // 3. User & Profile 삽입
                insertUser(100, "test1@test.com", "01000000100", "test1", "900101-1234567");
                insertUser(101, "test2@test.com", "01000000101", "test2", "900101-2345678");
                insertUser(102, "test3@test.com", "01000000102", "test3", "900101-3456789");

                // 4. 외래키 체크 복구
                em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

                log.info("✅ 채팅 테스트 계정 설정 완료!");

            } catch (Exception e) {
                log.error("❌ 테스트 데이터 초기화 중 심각한 오류 발생: ", e);
                // 실무라면 status.setRollbackOnly() 등을 고려
            }
            return null;
        });
    }

    private void insertUser(int userSeq, String email, String phone, String realName, String rrn) {
        // 중복 가입 방지를 위해 이메일/폰번호 기준 기존 데이터 삭제 (다른 seq로 가입된 경우 대비)
        em.createNativeQuery("DELETE FROM `users` WHERE email = '" + email + "' OR phone = '" + phone + "'").executeUpdate();

        // User 삽입
        em.createNativeQuery("INSERT INTO `users` (user_seq, email, phone, state, created_dttm) VALUES " +
                "(" + userSeq + ", '" + email + "', '" + phone + "', 'ACTIVE', NOW())").executeUpdate();

        // Profile 삽입
        em.createNativeQuery("INSERT INTO `user_profile` (user_seq, rrn, real_name, phone_verified, lock_enabled, lock_type) VALUES " +
                "(" + userSeq + ", '" + rrn + "', '" + realName + "', true, false, 'NONE')").executeUpdate();
        
        log.info("   - 유저 생성 성공: {} ({})", realName, email);
    }
}

