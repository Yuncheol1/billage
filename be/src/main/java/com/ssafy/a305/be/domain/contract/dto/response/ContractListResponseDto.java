package com.ssafy.a305.be.domain.contract.dto.response;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ContractListResponseDto {

    private Integer contractSeq;
    private String contractId;
    private String role;
    private String counterpartyName;
    private BigDecimal principalAmount;
    private String repaymentMethod;
    private String status;
    private LocalDate startDt;
    private LocalDate endDt;
    private Integer progressPercentage;

    public static ContractListResponseDto from(Contract contract, Integer userSeq) {
        // 1. role 판정
        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        String role = isCreditor ? "CREDITOR" : "DEBTOR";

        // 2. counterpartyName 결정
        String counterpartyName;
        if (isCreditor) {
            counterpartyName = contract.getDebtor().getProfile() != null
                    ? contract.getDebtor().getProfile().getRealName()
                    : "알 수 없음";
        } else {
            counterpartyName = contract.getCreditor().getProfile() != null
                    ? contract.getCreditor().getProfile().getRealName()
                    : "알 수 없음";
        }

        // 3. progressPercentage 계산
        List<RepaymentSchedule> schedules = contract.getRepaymentSchedules();
        int total = schedules.size();
        int paidCount = (int) schedules.stream()
                .filter(s -> "PAID".equals(s.getStatus()))
                .count();
        int progressPercentage = total > 0 ? (paidCount * 100) / total : 0;

        return ContractListResponseDto.builder()
                .contractSeq(contract.getContractSeq())
                .contractId(contract.getContractId())
                .role(role)
                .counterpartyName(counterpartyName)
                .principalAmount(contract.getPrincipalAmount())
                .repaymentMethod(contract.getRepaymentMethod())
                .status(contract.getStatus())
                .startDt(contract.getStartDt())
                .endDt(contract.getEndDt())
                .progressPercentage(progressPercentage)
                .build();
    }
}
