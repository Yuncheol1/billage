package com.ssafy.a305.be.domain.contract.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ContractListResponseDto {

    private final Integer contractSeq;
    private final String contractId;
    private final String role;
    private final String counterpartyName;
    private final BigDecimal principalAmount;
    private final BigDecimal interestRate;
    private final String repaymentMethod;
    private final String status;
    private final LocalDate startDt;
    private final LocalDate endDt;

    @QueryProjection
    public ContractListResponseDto(Integer contractSeq, String contractId, String role, String counterpartyName,
                                   BigDecimal principalAmount, BigDecimal interestRate, String repaymentMethod, String status,
                                   LocalDate startDt, LocalDate endDt) {
        this.contractSeq = contractSeq;
        this.contractId = contractId;
        this.role = role;
        this.counterpartyName = counterpartyName;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.repaymentMethod = repaymentMethod;
        this.status = status;
        this.startDt = startDt;
        this.endDt = endDt;
    }
}
