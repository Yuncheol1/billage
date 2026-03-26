package com.ssafy.a305.be.domain.repayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import lombok.Builder;
import lombok.Getter;
@Getter
@Builder
public class UpcomingScheduleResDto {
    private Integer scheduleSeq;
    private Integer contractSeq;
    private Integer installmentNo;
    private LocalDate dueDt;
    private BigDecimal totalDue;
    private String status;
    public static UpcomingScheduleResDto fromEntity(RepaymentSchedule schedule) {
        return UpcomingScheduleResDto.builder()
                .scheduleSeq(schedule.getScheduleSeq())
                .contractSeq(schedule.getContract().getContractSeq()) // 원본 계약 식별자
                .installmentNo(schedule.getInstallmentNo())
                .dueDt(schedule.getDueDt())
                .totalDue(schedule.getTotalDue())
                .status(schedule.getStatus())
                .build();
    }
}