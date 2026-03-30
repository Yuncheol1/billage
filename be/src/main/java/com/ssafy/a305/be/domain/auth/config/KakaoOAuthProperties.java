package com.ssafy.a305.be.domain.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String scope;
    private String tokenUri;
    private String userInfoUri;
}
