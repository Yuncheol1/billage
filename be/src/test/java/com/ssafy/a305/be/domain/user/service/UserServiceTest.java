package com.ssafy.a305.be.domain.user.service;

import com.ssafy.a305.be.domain.user.dto.response.UserMeResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserPhoneLookupResponseDto;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    void getMyInfoThrowsWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> userService.getMyInfo(99));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
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
