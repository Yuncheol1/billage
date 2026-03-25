package com.ssafy.a305.be.domain.user.service;

import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.auth.service.RefreshTokenService;
import com.ssafy.a305.be.domain.user.dto.response.UserMeResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserPhoneLookupResponseDto;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.domain.user.repository.SocialAuthRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Set<String> BLOCKING_CONTRACT_STATUSES = Set.of("ACTIVE");

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SocialAuthRepository socialAuthRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void getMyInfoReturnsUserData() {
        User user = User.builder()
                .userSeq(1)
                .email("example@example.com")
                .phone("01012345678")
                .state("ACTIVE")
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .realName("Test User")
                .profileImageUrl("https://example.com/me.png")
                .rrn("")
                .build();
        user = User.builder()
                .userSeq(user.getUserSeq())
                .email(user.getEmail())
                .phone(user.getPhone())
                .state(user.getState())
                .profile(profile)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserMeResponseDto result = userService.getMyInfo(1);

        assertEquals(1, result.getUserSeq());
        assertEquals("Test User", result.getRealName());
        assertEquals("example@example.com", result.getEmail());
        assertEquals("01012345678", result.getPhone());
        assertEquals("https://example.com/me.png", result.getProfileImageUrl());
    }

    @Test
    void getMyInfoThrowsWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> userService.getMyInfo(99));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void withdrawMarksUserDeletedWhenNoBlockingContractExists() {
        User user = User.builder()
                .userSeq(1)
                .email("example@example.com")
                .phone("01012345678")
                .state("ACTIVE")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(contractRepository.existsByUserSeqAndStatuses(1, BLOCKING_CONTRACT_STATUSES)).thenReturn(false);

        userService.withdraw(1);

        assertEquals("DELETED", user.getState());
        assertTrue(user.getEmail().matches("example@example\\.com_del\\d{17}"));
        assertTrue(user.getPhone().matches("01012345678_del\\d{17}"));
        assertNotNull(user.getDeletedDttm());
        verify(contractRepository).existsByUserSeqAndStatuses(1, BLOCKING_CONTRACT_STATUSES);
        verify(refreshTokenService).delete(1);
        verify(socialAuthRepository).deleteByUserUserSeq(1);
    }

    @Test
    void withdrawThrowsConflictWhenBlockingContractExists() {
        User user = User.builder()
                .userSeq(1)
                .email("example@example.com")
                .phone("01012345678")
                .state("ACTIVE")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(contractRepository.existsByUserSeqAndStatuses(1, BLOCKING_CONTRACT_STATUSES)).thenReturn(true);

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> userService.withdraw(1));

        assertEquals(ErrorCode.ACTIVE_CONTRACT_EXISTS, exception.getErrorCode());
        assertEquals("ACTIVE", user.getState());
    }

    @Test
    void lookupRegisteredPhoneNormalizesAndChecksActiveUser() {
        User user = User.builder()
                .userSeq(7)
                .phone("01040140142")
                .state("ACTIVE")
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .realName("Hong Gil Dong")
                .profileImageUrl("https://example.com/hong.png")
                .rrn("")
                .build();
        user = User.builder()
                .userSeq(user.getUserSeq())
                .phone(user.getPhone())
                .state(user.getState())
                .profile(profile)
                .build();

        when(userRepository.findByPhoneAndState(eq("01040140142"), eq("ACTIVE")))
                .thenReturn(Optional.of(user));

        UserPhoneLookupResponseDto result = userService.lookupRegisteredPhone("010-4014-0142", " Hong Gil Dong ");

        assertTrue(result.isRegistered());
        assertEquals(7, result.getUserSeq());
        assertEquals("Hong Gil Dong", result.getRealName());
        assertEquals("https://example.com/hong.png", result.getProfileImageUrl());
        verify(userRepository).findByPhoneAndState("01040140142", "ACTIVE");
    }

    @Test
    void lookupRegisteredPhoneRejectsInvalidPhoneNumber() {
        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> userService.lookupRegisteredPhone("abc", "Hong Gil Dong"));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    void lookupRegisteredPhoneReturnsFalseForUnavailableUser() {
        when(userRepository.findByPhoneAndState(eq("01040140142"), eq("ACTIVE")))
                .thenReturn(Optional.empty());

        UserPhoneLookupResponseDto result = userService.lookupRegisteredPhone("+82 10-4014-0142", "Hong Gil Dong");

        assertFalse(result.isRegistered());
        assertNull(result.getUserSeq());
    }

    @Test
    void lookupRegisteredPhoneReturnsFalseWhenRealNameDoesNotMatch() {
        User user = User.builder()
                .userSeq(7)
                .phone("01040140142")
                .state("ACTIVE")
                .build();
        UserProfile profile = UserProfile.builder()
                .user(user)
                .realName("Kim Cheolsu")
                .rrn("")
                .build();
        user = User.builder()
                .userSeq(user.getUserSeq())
                .phone(user.getPhone())
                .state(user.getState())
                .profile(profile)
                .build();

        when(userRepository.findByPhoneAndState(eq("01040140142"), eq("ACTIVE")))
                .thenReturn(Optional.of(user));

        UserPhoneLookupResponseDto result = userService.lookupRegisteredPhone("010-4014-0142", "Hong Gil Dong");

        assertFalse(result.isRegistered());
        assertNull(result.getUserSeq());
    }
}
