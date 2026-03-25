package com.ssafy.a305.be.domain.auth.service;

import com.ssafy.a305.be.domain.auth.client.KakaoOAuthClient;
import com.ssafy.a305.be.domain.auth.client.dto.KakaoTokenResponse;
import com.ssafy.a305.be.domain.auth.client.dto.KakaoUserInfoResponse;
import com.ssafy.a305.be.domain.auth.dto.request.KakaoLoginRequestDto;
import com.ssafy.a305.be.domain.auth.dto.response.KakaoLoginResponseDto;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.notification.repository.FcmTokenRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.SocialAuthRepository;
import com.ssafy.a305.be.domain.user.repository.TermsMasterRepository;
import com.ssafy.a305.be.domain.user.repository.UserConsentRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = {
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=6379"
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class
})
class AuthServiceRedisIntegrationTest {

    private static final Integer USER_SEQ = 1;
    private static final String REDIS_KEY = "auth:refresh:" + USER_SEQ;

    @Autowired
    private AuthService authService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SocialAuthRepository socialAuthRepository;

    @MockBean
    private ContractRepository contractRepository;

    @MockBean
    private FcmTokenRepository fcmTokenRepository;

    @MockBean
    private TermsMasterRepository termsMasterRepository;

    @MockBean
    private UserConsentRepository userConsentRepository;

    @AfterEach
    void tearDown() {
        stringRedisTemplate.delete(REDIS_KEY);
    }

    @Test
    void loginWithKakao_savesRefreshTokenToRedis() {
        when(kakaoOAuthClient.getToken(anyString(), anyString()))
                .thenReturn(new KakaoTokenResponse("bearer", "kakao-access", 3600L, "kakao-refresh", 7200L, null));
        when(kakaoOAuthClient.getUserInfo(anyString()))
                .thenReturn(new KakaoUserInfoResponse(
                        12345L,
                        null,
                        new KakaoUserInfoResponse.KakaoAccount(
                                "test@example.com",
                                null,
                                null,
                                false,
                                true,
                                true,
                                true,
                                "male",
                                "0216",
                                "1998",
                                null
                        )
                ));
        when(socialAuthRepository.findByProviderAndProviderUserId("KAKAO", "12345"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    return User.builder()
                            .userSeq(USER_SEQ)
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .state(user.getState())
                            .createdDttm(user.getCreatedDttm())
                            .updatedDttm(user.getUpdatedDttm())
                            .deletedDttm(user.getDeletedDttm())
                            .build();
                });

        KakaoLoginResponseDto response = authService.loginWithKakao(
                new KakaoLoginRequestDto("auth-code", "http://localhost/callback")
        );

        String storedRefreshToken = stringRedisTemplate.opsForValue().get(REDIS_KEY);

        assertThat(storedRefreshToken).isEqualTo(response.getRefreshToken());
        assertThat(storedRefreshToken).isNotBlank();
    }
}
