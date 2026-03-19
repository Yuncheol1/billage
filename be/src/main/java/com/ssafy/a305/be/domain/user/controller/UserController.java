package com.ssafy.a305.be.domain.user.controller;
import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.user.dto.response.UserSearchResponseDto;
import com.ssafy.a305.be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<Integer> searchByEmail(@RequestParam String email) {
        Integer userSeq = userService.searchByEmail(email);
        return ResponseEntity.ok(userSeq);

    public ResponseEntity<List<UserSearchResponseDto>> searchUsers(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("email") String email) {
        if (email == null || email.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        Integer excludedUserSeq = (authenticatedUser != null) ? authenticatedUser.userSeq() : null;
        return ResponseEntity.ok(userService.searchByEmail(email, excludedUserSeq));

    }
}
