package com.ssafy.a305.be.domain.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private long accessTokenExpirationSeconds;
    private long refreshTokenExpirationSeconds;
}
