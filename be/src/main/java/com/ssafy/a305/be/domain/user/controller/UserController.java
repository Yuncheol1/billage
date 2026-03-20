package com.ssafy.a305.be.domain.user.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.user.dto.request.UserPhoneLookupRequestDto;
import com.ssafy.a305.be.domain.user.dto.response.UserMeResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserPhoneLookupResponseDto;
import com.ssafy.a305.be.domain.user.dto.response.UserSearchResponseDto;
import com.ssafy.a305.be.domain.user.service.UserService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UserMeResponseDto responseDto = userService.getMyInfo(requireAuthenticatedUser(authenticatedUser).userSeq());
        return ResponseEntity.ok(successResponse("사용자 정보 조회 성공", responseDto));
    }

    @PostMapping("/lookup/phone")
    public ResponseEntity<Map<String, Object>> lookupPhone(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UserPhoneLookupRequestDto requestDto) {
        requireAuthenticatedUser(authenticatedUser);

        UserPhoneLookupResponseDto responseDto = userService.lookupRegisteredPhone(
                requestDto.phone(),
                requestDto.realName());
        return ResponseEntity.ok(successResponse("전화번호 가입 여부 확인 성공", responseDto));
    }

    @GetMapping("/search")
    public ResponseEntity<Integer> searchByEmail(@RequestParam String email) {
        Integer userSeq = userService.searchByEmail(email);
        return ResponseEntity.ok(userSeq);
    }

    @GetMapping("/contain")
    public ResponseEntity<List<UserSearchResponseDto>> searchUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("email") String email) {
        if (email == null || email.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        Integer excludedUserSeq = (authenticatedUser != null) ? authenticatedUser.userSeq() : null;
        return ResponseEntity.ok(userService.searchByEmail(email, excludedUserSeq));

    }

    private AuthenticatedUser requireAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new CustomBusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        return authenticatedUser;
    }

    private Map<String, Object> successResponse(String message, Object data) {
        return Map.of(
                "status", HttpStatus.OK.value(),
                "message", message,
                "data", data
        );
    }
}
