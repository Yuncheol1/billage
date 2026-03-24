package com.ssafy.a305.be.domain.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ssafy.api")
public class SsafyApiProperties {

    private String baseUrl;
    private String apiKey;
    private String memberSearchPath;
    private String memberCreatePath;
}
