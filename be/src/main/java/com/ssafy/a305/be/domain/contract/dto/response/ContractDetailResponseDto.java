package com.ssafy.a305.be.domain.contract.dto.response;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ContractDetailResponseDto {
    private final Integer contractSeq;
    private final String contractId;
    private final Boolean family;
    private final String myRole;
    private final String myRealName;
    private final String counterpartyName;
    private final String counterpartyProfileImageUrl;
    private final BigDecimal principalAmount;
    private final BigDecimal interestRate;
    private final String repaymentMethod;
    private final Integer totalInstallments;
    private final String status;
    private final LocalDate startDt;
    private final LocalDate endDt;
    private final String aiDraftContent;
    private final LocalDateTime signedDttm;
    private final List<ClauseDetailDto> clauses;
    private final Boolean isMeAccepted;
    private final Boolean isOpponentAccepted;
    private final Boolean isMeSigned;
    private final Boolean isOpponentSigned;
    private final String roomId;
    private final Integer counterpartyUserSeq;

    public ContractDetailResponseDto(
            Contract contract,
            Integer myUserSeq,
            String myRealName,
            String counterpartyName,
            String counterpartyProfileImageUrl,
            Boolean isMeSigned,
            Boolean isOpponentSigned,
            String roomId,
            Integer counterpartyUserSeq
    ) {
        this.contractSeq = contract.getContractSeq();
        this.contractId = contract.getContractId();
        this.family = contract.getFamily();
        this.myRole = contract.getCreditor().getUserSeq().equals(myUserSeq) ? "CREDITOR" : "DEBTOR";
        this.myRealName = myRealName;
        this.counterpartyName = counterpartyName;
        this.counterpartyProfileImageUrl = counterpartyProfileImageUrl;
        this.principalAmount = contract.getPrincipalAmount();
        this.interestRate = contract.getInterestRate();
        this.repaymentMethod = contract.getRepaymentMethod();
        this.totalInstallments = contract.getTotalInstallments();
        this.status = contract.getStatus();
        this.startDt = contract.getStartDt();
        this.endDt = contract.getEndDt();
        this.aiDraftContent = contract.getAiDraftContent();
        this.signedDttm = contract.getSignedDttm();

        if ("CREDITOR".equals(this.myRole)) {
            this.isMeAccepted = contract.getCreditorAccepted();
            this.isOpponentAccepted = contract.getDebtorAccepted();
        } else {
            this.isMeAccepted = contract.getDebtorAccepted();
            this.isOpponentAccepted = contract.getCreditorAccepted();
        }
        this.isMeSigned = isMeSigned;
        this.isOpponentSigned = isOpponentSigned;
        this.roomId = roomId;
        this.counterpartyUserSeq = counterpartyUserSeq;

        this.clauses = contract.getClauses().stream()
                .map(ClauseDetailDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class ClauseDetailDto {
        private final Integer clauseSeq;
        private final String title;
        private final String content;
        private final Integer orderNo;

        public ClauseDetailDto(ContractClause clause) {
            this.clauseSeq = clause.getClauseSeq();
            this.title = clause.getTitle();
            this.content = clause.getContent();
            this.orderNo = clause.getOrderNo();
        }
    }
}
