package com.ssafy.a305.be.domain.auth.service;

import com.ssafy.a305.be.domain.auth.client.KakaoOAuthClient;
import com.ssafy.a305.be.domain.auth.client.dto.KakaoTokenResponse;
import com.ssafy.a305.be.domain.auth.client.dto.KakaoUserInfoResponse;
import com.ssafy.a305.be.domain.auth.config.JwtProperties;
import com.ssafy.a305.be.domain.auth.dto.request.ConsentCompletionRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.DevTokenIssueRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.KakaoLoginRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.LogoutRequestDto;
import com.ssafy.a305.be.domain.auth.dto.request.TokenReissueRequestDto;
import com.ssafy.a305.be.domain.auth.dto.response.ConsentCompletionResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.ConsentListResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.DevTokenIssueResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.KakaoLoginResponseDto;
import com.ssafy.a305.be.domain.auth.dto.response.TokenReissueResponseDto;
import com.ssafy.a305.be.domain.enums.TermsType;
import com.ssafy.a305.be.domain.auth.util.JwtTokenProvider;
import com.ssafy.a305.be.domain.user.entity.SocialAuth;
import com.ssafy.a305.be.domain.user.entity.TermsMaster;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserConsent;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.domain.user.repository.SocialAuthRepository;
import com.ssafy.a305.be.domain.user.repository.TermsMasterRepository;
import com.ssafy.a305.be.domain.user.repository.UserConsentRepository;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String KAKAO_PROVIDER = "KAKAO";
    private static final String USER_STATE_PENDING = "PENDING";
    private static final String USER_STATE_DELETED = "DELETED";
    private static final String USER_STATE_BLOCKED = "BLOCKED";
    private static final String DEFAULT_LOCK_TYPE = "NONE";
    private static final String DEFAULT_BADGE_LEVEL = "BRONZE";
    private static final String EMPTY_RRN_PLACEHOLDER = "";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final DeviceSessionService deviceSessionService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final TermsMasterRepository termsMasterRepository;
    private final UserConsentRepository userConsentRepository;

    @Transactional
    public KakaoLoginResponseDto loginWithKakao(KakaoLoginRequestDto requestDto) {
        KakaoTokenResponse kakaoToken = kakaoOAuthClient.getToken(
                requestDto.authorizationCode(),
                requestDto.redirectUri()
        );
        KakaoUserInfoResponse userInfo = kakaoOAuthClient.getUserInfo(kakaoToken.accessToken());
        String providerUserId = String.valueOf(userInfo.id());

        SocialLoginResult loginResult = socialAuthRepository.findByProviderAndProviderUserId(KAKAO_PROVIDER, providerUserId)
                .map(socialAuth -> SocialLoginResult.existingUser(syncExistingUser(socialAuth.getUser(), userInfo)))
                .orElseGet(() -> registerNewKakaoUser(providerUserId, userInfo));

        validateLoginAllowed(loginResult.user());

        String accessToken = jwtTokenProvider.createAccessToken(loginResult.user().getUserSeq());
        String refreshToken = jwtTokenProvider.createRefreshToken(loginResult.user().getUserSeq());
        refreshTokenService.save(
                loginResult.user().getUserSeq(),
                refreshToken,
                jwtProperties.getRefreshTokenExpirationSeconds()
        );

        return KakaoLoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtProperties.getAccessTokenExpirationSeconds())
                .refreshTokenExpiresIn(jwtProperties.getRefreshTokenExpirationSeconds())
                .isNewUser(loginResult.isNewUser())
                .userSeq(loginResult.user().getUserSeq())
                .userState(loginResult.user().getState())
                .build();
    }

    @Transactional
    public TokenReissueResponseDto reissue(TokenReissueRequestDto requestDto) {
        String refreshToken = requestDto.refreshToken();
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Integer userSeq = jwtTokenProvider.extractUserSeqFromRefreshToken(refreshToken);
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        validateLoginAllowed(user);
        refreshTokenService.validate(userSeq, refreshToken);

        String newAccessToken = jwtTokenProvider.createAccessToken(userSeq);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userSeq);
        refreshTokenService.rotate(userSeq, newRefreshToken, jwtProperties.getRefreshTokenExpirationSeconds());

        return TokenReissueResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(jwtProperties.getAccessTokenExpirationSeconds())
                .refreshTokenExpiresIn(jwtProperties.getRefreshTokenExpirationSeconds())
                .userState(user.getState())
                .build();
    }

    @Transactional
    public DevTokenIssueResponseDto issueDevToken(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        validateLoginAllowed(user);

        String accessToken = jwtTokenProvider.createAccessToken(userSeq);
        String refreshToken = jwtTokenProvider.createRefreshToken(userSeq);
        refreshTokenService.save(userSeq, refreshToken, jwtProperties.getRefreshTokenExpirationSeconds());

        return DevTokenIssueResponseDto.builder()
                .userSeq(user.getUserSeq())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtProperties.getAccessTokenExpirationSeconds())
                .refreshTokenExpiresIn(jwtProperties.getRefreshTokenExpirationSeconds())
                .userState(user.getState())
                .build();
    }

    @Transactional
    public void logout(LogoutRequestDto requestDto) {
        String refreshToken = requestDto.refreshToken();
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Integer userSeq = jwtTokenProvider.extractUserSeqFromRefreshToken(refreshToken);
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        validateLoginAllowed(user);
        refreshTokenService.validate(userSeq, refreshToken);
        refreshTokenService.delete(userSeq);
        deviceSessionService.deactivateFcmToken(userSeq, requestDto.fcmToken());
    }

    @Transactional(readOnly = true)
    public ConsentListResponseDto getConsents(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "사용자를 찾을 수 없습니다."));

        Map<Integer, Boolean> userConsentMap = userConsentRepository.findByUserUserSeq(userSeq).stream()
                .collect(Collectors.toMap(
                        userConsent -> userConsent.getTermsMaster().getTermsSeq(),
                        UserConsent::getIsAgreed,
                        (existing, replacement) -> replacement
                ));

        List<ConsentListResponseDto.ConsentItem> consentItems = termsMasterRepository.findAllByOrderByTermsSeqAsc().stream()
                .map(termsMaster -> ConsentListResponseDto.ConsentItem.builder()
                        .termsSeq(termsMaster.getTermsSeq())
                        .termsKey(termsMaster.getTermsKey())
                        .termsType(termsMaster.getTermsType().name())
                        .termsVersion(termsMaster.getTermsVersion())
                        .termsContent(termsMaster.getTermsContent())
                        .agreed(userConsentMap.getOrDefault(termsMaster.getTermsSeq(), false))
                        .build())
                .toList();

        return ConsentListResponseDto.builder()
                .userSeq(user.getUserSeq())
                .userState(user.getState())
                .consents(consentItems)
                .build();
    }

    @Transactional
    public ConsentCompletionResponseDto completeConsents(Integer userSeq, ConsentCompletionRequestDto requestDto) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "사용자를 찾을 수 없습니다."));

        Map<Integer, Boolean> requestAgreementMap = requestDto.consents().stream()
                .collect(Collectors.toMap(
                        ConsentCompletionRequestDto.ConsentItem::termsSeq,
                        ConsentCompletionRequestDto.ConsentItem::agreed,
                        (existing, replacement) -> replacement,
                        HashMap::new
                ));

        List<TermsMaster> termsList = termsMasterRepository.findByTermsSeqIn(requestAgreementMap.keySet());
        if (termsList.size() != requestAgreementMap.size()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "존재하지 않는 약관이 포함되어 있습니다.");
        }

        validateMandatoryConsents(requestAgreementMap);

        for (TermsMaster termsMaster : termsList) {
            boolean agreed = Boolean.TRUE.equals(requestAgreementMap.get(termsMaster.getTermsSeq()));
            saveOrUpdateConsent(user, termsMaster, agreed);
        }

        user.changeState("ACTIVE");

        return ConsentCompletionResponseDto.builder()
                .userSeq(user.getUserSeq())
                .userState(user.getState())
                .build();
    }

    private SocialLoginResult registerNewKakaoUser(String providerUserId, KakaoUserInfoResponse userInfo) {
        LocalDateTime now = LocalDateTime.now();
        User newUser = userRepository.saveAndFlush(User.builder() // fix) save -> saveAndFlush
                .email(resolveAvailableEmail(extractVerifiedEmail(userInfo)))
                .phone(resolveAvailablePhone(null, userInfo))
                .state(USER_STATE_PENDING)
                .createdDttm(now)
                .updatedDttm(now)
                .build());

        syncUserProfile(newUser, userInfo, now);

        socialAuthRepository.save(SocialAuth.builder()
                .user(newUser)
                .provider(KAKAO_PROVIDER)
                .providerUserId(providerUserId)
                .connectedDttm(now)
                .build());

        return SocialLoginResult.newUser(newUser);
    }

    private User syncExistingUser(User user, KakaoUserInfoResponse userInfo) {
        String verifiedEmail = extractVerifiedEmail(userInfo);
        if ((user.getEmail() == null || user.getEmail().isBlank()) && verifiedEmail != null && !userRepository.existsByEmail(verifiedEmail)) {
            user.updateEmail(verifiedEmail);
        }

        String resolvedPhone = resolveAvailablePhone(user, userInfo);
        if (resolvedPhone != null && !resolvedPhone.equals(user.getPhone())) {
            user.updatePhone(resolvedPhone);
        }

        syncUserProfile(user, userInfo, LocalDateTime.now());
        return user;
    }

    private void validateLoginAllowed(User user) {
        if (USER_STATE_DELETED.equals(user.getState()) || USER_STATE_BLOCKED.equals(user.getState())) {
            throw new CustomBusinessException(ErrorCode.LOGIN_NOT_ALLOWED);
        }
    }

    private String extractVerifiedEmail(KakaoUserInfoResponse userInfo) {
        if (userInfo.kakaoAccount() == null) {
            return null;
        }

        KakaoUserInfoResponse.KakaoAccount kakaoAccount = userInfo.kakaoAccount();
        if (!Boolean.TRUE.equals(kakaoAccount.hasEmail())
                || !Boolean.TRUE.equals(kakaoAccount.isEmailValid())
                || !Boolean.TRUE.equals(kakaoAccount.isEmailVerified())) {
            return null;
        }

        return kakaoAccount.email();
    }

    private String resolveAvailableEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return userRepository.existsByEmail(email) ? null : email;
    }

    private String resolveAvailablePhone(User user, KakaoUserInfoResponse userInfo) {
        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(extractPhoneNumber(userInfo));
        if (normalizedPhoneNumber == null || normalizedPhoneNumber.isBlank()) {
            return user == null ? null : user.getPhone();
        }

        Integer currentUserSeq = user == null ? -1 : user.getUserSeq();
        if (userRepository.existsByPhoneAndUserSeqNot(normalizedPhoneNumber, currentUserSeq)) {
            return user == null ? null : user.getPhone();
        }

        return normalizedPhoneNumber;
    }

    private void syncUserProfile(User user, KakaoUserInfoResponse userInfo, LocalDateTime now) {
        String realName = extractRealName(userInfo);
        Boolean phoneVerified = extractPhoneVerified(userInfo);

        userProfileRepository.findByUserUserSeq(user.getUserSeq())
                .ifPresentOrElse(
                        userProfile -> userProfile.syncFromKakao(realName, phoneVerified),
                        () -> userProfileRepository.save(UserProfile.builder()
                                .user(user)
                                // Placeholder until resident registration number is collected separately.
                                .rrn(EMPTY_RRN_PLACEHOLDER)
                                .realName(realName)
                                .phoneVerified(phoneVerified)
                                .profileCompletedDt(null)
                                .lockEnabled(false)
                                .lockType(DEFAULT_LOCK_TYPE)
                                .patternHash(null)
                                .onTimeCount(0)
                                .overdueCount(0)
                                .resolvedCount(0)
                                .avgDelayDays(BigDecimal.ZERO)
                                .badgeLevel(DEFAULT_BADGE_LEVEL)
                                .badgeIsPublic(false)
                                .updatedDttm(now)
                                .build())
                );
    }

    private String extractRealName(KakaoUserInfoResponse userInfo) {
        if (userInfo.kakaoAccount() == null) {
            return null;
        }
        return userInfo.kakaoAccount().name();
    }

    private String extractPhoneNumber(KakaoUserInfoResponse userInfo) {
        if (userInfo.kakaoAccount() == null) {
            return null;
        }
        return userInfo.kakaoAccount().phoneNumber();
    }

    private Boolean extractPhoneVerified(KakaoUserInfoResponse userInfo) {
        if (userInfo.kakaoAccount() == null) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE.equals(userInfo.kakaoAccount().phoneNumberVerified());
    }

    private void validateMandatoryConsents(Map<Integer, Boolean> requestAgreementMap) {
        Set<Integer> agreedMandatoryTermsSeqs = requestAgreementMap.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<TermsMaster> mandatoryTerms = termsMasterRepository.findByTermsType(TermsType.mandatory);
        boolean allMandatoryAgreed = mandatoryTerms.stream()
                .map(TermsMaster::getTermsSeq)
                .allMatch(agreedMandatoryTermsSeqs::contains);

        if (!allMandatoryAgreed) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "필수 약관에 모두 동의해야 합니다.");
        }
    }

    private void saveOrUpdateConsent(User user, TermsMaster termsMaster, boolean agreed) {
        UserConsent userConsent = userConsentRepository.findByUserUserSeqAndTermsMasterTermsSeq(
                        user.getUserSeq(),
                        termsMaster.getTermsSeq()
                )
                .orElseGet(() -> UserConsent.builder()
                        .user(user)
                        .termsMaster(termsMaster)
                        .isAgreed(agreed)
                        .agreedDttm(LocalDateTime.now())
                        .build());

        userConsent.changeAgreement(agreed);
        userConsentRepository.save(userConsent);
    }

    private record SocialLoginResult(User user, boolean isNewUser) {
        private static SocialLoginResult newUser(User user) {
            return new SocialLoginResult(user, true);
        }

        private static SocialLoginResult existingUser(User user) {
            return new SocialLoginResult(user, false);
        }
    }
}
