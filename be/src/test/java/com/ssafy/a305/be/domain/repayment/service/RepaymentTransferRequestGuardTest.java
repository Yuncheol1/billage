package com.ssafy.a305.be.domain.repayment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 자동이체 중복 실행 방지용 Redis 분산락 단위 테스트.
 *
 * <p>SETNX + TTL로 락을 잡고, 해제할 때는 Lua 스크립트로 토큰을 비교한 뒤 삭제(CAS)합니다.
 * 이 테스트는 그 계약(키 형식 / TTL / 토큰 검증 스크립트)을 고정합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RepaymentTransferRequestGuardTest {

    private static final Integer SCHEDULE_SEQ = 42;
    private static final String EXPECTED_KEY = "repay:transfer:lock:schedule:42";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RepaymentTransferRequestGuard guard;

    @Nested
    @DisplayName("tryAcquire()")
    class TryAcquireTests {

        @Test
        @DisplayName("SETNX 성공 시 → 발급된 토큰을 반환한다")
        void tryAcquire_whenSetIfAbsentSucceeds_returnsToken() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(true);

            String token = guard.tryAcquire(SCHEDULE_SEQ);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("이미 락이 잡혀 있으면 → null을 반환한다")
        void tryAcquire_whenKeyAlreadyExists_returnsNull() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(false);

            String token = guard.tryAcquire(SCHEDULE_SEQ);

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Redis가 null을 돌려줘도 NPE 없이 null을 반환한다")
        void tryAcquire_whenRedisReturnsNull_returnsNull() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(null);

            String token = guard.tryAcquire(SCHEDULE_SEQ);

            assertThat(token).isNull();
        }

        @Test
        @DisplayName("키는 repay:transfer:lock:schedule:{seq} 형식이고 TTL은 30초다")
        void tryAcquire_usesExpectedKeyAndTtl() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(true);

            String token = guard.tryAcquire(SCHEDULE_SEQ);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            then(valueOperations).should()
                    .setIfAbsent(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
            // 저장된 값이 곧 호출자가 돌려받는 토큰이어야 CAS 해제가 성립한다
            assertThat(valueCaptor.getValue()).isEqualTo(token);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("호출할 때마다 서로 다른 토큰이 발급된다")
        void tryAcquire_issuesUniqueTokenPerCall() {
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .willReturn(true);

            String first = guard.tryAcquire(SCHEDULE_SEQ);
            String second = guard.tryAcquire(SCHEDULE_SEQ);

            assertThat(first).isNotEqualTo(second);
        }
    }

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        @Test
        @DisplayName("토큰이 null이면 Redis를 건드리지 않는다")
        void release_withNullToken_doesNothing() {
            guard.release(SCHEDULE_SEQ, null);

            then(stringRedisTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("토큰이 공백이면 Redis를 건드리지 않는다")
        void release_withBlankToken_doesNothing() {
            guard.release(SCHEDULE_SEQ, "   ");

            then(stringRedisTemplate).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("정상 토큰이면 해당 키에 대해 Lua 스크립트를 실행한다")
        void release_executesScriptWithKeyAndToken() {
            guard.release(SCHEDULE_SEQ, "my-token");

            @SuppressWarnings("rawtypes")
            ArgumentCaptor<RedisScript> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);

            then(stringRedisTemplate).should()
                    .execute(scriptCaptor.capture(), keysCaptor.capture(), eq("my-token"));

            assertThat(keysCaptor.getValue()).containsExactly(EXPECTED_KEY);
        }

        @Test
        @DisplayName("해제 스크립트는 GET으로 토큰을 확인한 뒤에만 DEL 한다 (CAS)")
        void release_scriptComparesTokenBeforeDelete() {
            guard.release(SCHEDULE_SEQ, "my-token");

            @SuppressWarnings("rawtypes")
            ArgumentCaptor<RedisScript> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
            then(stringRedisTemplate).should()
                    .execute(scriptCaptor.capture(), anyList(), any());

            String script = scriptCaptor.getValue().getScriptAsString();

            // 단순 DEL이면 내 락이 TTL로 만료된 뒤 다른 워커가 새로 잡은 락까지 지워버린다.
            // GET으로 토큰이 내 것인지 확인한 뒤 DEL 하는 구조여야 한다.
            assertThat(script)
                    .contains("redis.call('get'")
                    .contains("redis.call('del'")
                    .contains("ARGV[1]");
        }
    }
}
