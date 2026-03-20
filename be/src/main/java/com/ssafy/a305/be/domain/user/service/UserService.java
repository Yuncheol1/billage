package com.ssafy.a305.be.domain.user.service;

import com.ssafy.a305.be.domain.user.dto.response.UserMeResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserPhoneLookupResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserSearchResponseDto;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.global.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class UserService {

    private static final String USER_STATE_ACTIVE = "ACTIVE";

    private final UserRepository userRepository;

    public UserMeResponseDto getMyInfo(Integer userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        return UserMeResponseDto.builder()
                .userSeq(user.getUserSeq())
                .realName(user.getProfile() != null ? user.getProfile().getRealName() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    public UserPhoneLookupResponseDto lookupRegisteredPhone(String phone) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(phone);
        validatePhoneNumber(normalizedPhone);

        boolean registered = userRepository.existsByPhoneAndState(normalizedPhone, USER_STATE_ACTIVE);
        return new UserPhoneLookupResponseDto(registered);
    }

    @Transactional(readOnly = true)
    public Integer searchByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 이메일의 사용자를 찾을 수 없습니다."));

        if (!"ACTIVE".equals(user.getState())) {
            throw new CustomBusinessException(ErrorCode.INACTIVE_USER_TARGET);
        }

        return user.getUserSeq();
    }

    public List<UserSearchResponseDto> searchByEmail(String email, Integer excludedUserSeq) {
        List<User> users = userRepository.findByEmailContaining(email);
        return users.stream()
                .filter(user -> USER_STATE_ACTIVE.equals(user.getState()))
                .filter(user -> !user.getUserSeq().equals(excludedUserSeq))
                .map(user -> UserSearchResponseDto.builder()
                        .userSeq(user.getUserSeq())
                        .email(user.getEmail())
                        .realName(user.getProfile() != null ? user.getProfile().getRealName() : "알 수 없음")
                        .build())
                .collect(Collectors.toList());
    }

    private void validatePhoneNumber(String normalizedPhone) {
        if (normalizedPhone == null
                || normalizedPhone.length() < 10
                || normalizedPhone.length() > 11
                || !normalizedPhone.startsWith("0")) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효한 전화번호를 입력해주세요.");
        }
    }
}
