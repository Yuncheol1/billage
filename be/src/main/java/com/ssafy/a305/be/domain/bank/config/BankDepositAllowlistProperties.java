package com.ssafy.a305.be.domain.bank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bank")
public class BankDepositAllowlistProperties {

    /**
     * allowlist accountNo 목록 (comma-separated)
     * 예: 001617...,001621...
     */
    private String depositAllowlistAccountNos;
}

