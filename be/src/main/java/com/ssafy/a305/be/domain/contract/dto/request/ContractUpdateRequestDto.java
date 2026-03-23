package com.ssafy.a305.be.domain.contract.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractUpdateRequestDto {
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private List<ClauseUpdateDto> clauses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClauseUpdateDto {
        private Integer clauseSeq;
        private String content;
    }
}
