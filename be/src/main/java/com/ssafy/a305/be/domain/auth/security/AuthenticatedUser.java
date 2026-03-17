package com.ssafy.a305.be.domain.auth.security;

import com.ssafy.a305.be.domain.user.entity.User;

public record AuthenticatedUser(
        Integer userSeq,
        String state
) {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getUserSeq(), user.getState());
    }
}
