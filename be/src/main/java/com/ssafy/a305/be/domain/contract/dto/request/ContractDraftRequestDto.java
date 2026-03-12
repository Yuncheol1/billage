package com.ssafy.a305.be.domain.contract.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContractDraftRequestDto {

    @NotNull(message = "템플릿 종류는 필수입니다.")
    private String templateType;

    @NotNull(message = "채무자 식별자는 필수입니다.")
    private Integer debtorUserSeq;

    @NotNull(message = "원금은 필수입니다.")
    @Min(value = 1, message = "원금은 0보다 커야 합니다.")
    private BigDecimal principalAmount;

    @DecimalMin(value = "0.0", message = "이자율은 0.0 이상이어야 합니다.")
    @DecimalMax(value = "0.20", message = "법정 최고 이자율(연 20%)을 초과할 수 없습니다.")
    private BigDecimal interestRate;

    @NotNull(message = "상환 방식은 필수입니다.")
    private String repaymentMethod;

    private Integer totalInstallments;
    
    @Min(value = 1, message = "상환일은 1이상 31이하여야 합니다.")
    @Max(value = 31, message = "상환일은 1이상 31이하여야 합니다.")
    private Integer repaymentDay;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDt;

    @NotNull(message = "만기일은 필수입니다.")
    private LocalDate endDt;
}
