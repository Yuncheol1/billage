package com.ssafy.a305.be.domain.user.controller;

import com.ssafy.a305.be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<Integer> searchByEmail(@RequestParam String email) {
        Integer userSeq = userService.searchByEmail(email);
        return ResponseEntity.ok(userSeq);
    }
}
