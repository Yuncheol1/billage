package com.ssafy.a305.be.domain.repayment.dto;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class RepaymentScheduleResDto {
    private Integer scheduleSeq;
    private Integer installmentNo;
    private LocalDate dueDt;
    private BigDecimal principalDue;
    private BigDecimal interestDue;
    private BigDecimal totalDue;
    private BigDecimal remainingBalanceAfter;
    private String status;
    private LocalDateTime paidDttm;
    public static RepaymentScheduleResDto fromEntity(RepaymentSchedule schedule) {
        return RepaymentScheduleResDto.builder()
                .scheduleSeq(schedule.getScheduleSeq())
                .installmentNo(schedule.getInstallmentNo())
                .dueDt(schedule.getDueDt())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .remainingBalanceAfter(schedule.getRemainingBalanceAfter())
                .status(schedule.getStatus())
                .paidDttm(schedule.getPaidDttm())
                .build();
    }
}