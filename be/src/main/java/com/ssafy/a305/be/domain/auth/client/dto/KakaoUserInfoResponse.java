package com.ssafy.a305.be.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        Long id,
        KakaoProperties properties,
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoProperties(
            @JsonProperty("profile_image")
            String profileImage
    ) {
    }

    public record KakaoAccount(
            String email,
            String name,
            @JsonProperty("phone_number")
            String phoneNumber,
            @JsonProperty("phone_number_verified")
            Boolean phoneNumberVerified,
            @JsonProperty("has_email")
            Boolean hasEmail,
            @JsonProperty("is_email_valid")
            Boolean isEmailValid,
            @JsonProperty("is_email_verified")
            Boolean isEmailVerified,
            String gender,
            String birthday,
            String birthyear,
            Profile profile
    ) {
        public record Profile(
                @JsonProperty("profile_image_url")
                String profileImageUrl,
                @JsonProperty("thumbnail_image_url")
                String thumbnailImageUrl,
                @JsonProperty("is_default_image")
                Boolean isDefaultImage
        ) {
        }
    }
}
