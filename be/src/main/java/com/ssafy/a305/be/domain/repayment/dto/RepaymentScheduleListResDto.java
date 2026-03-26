package com.ssafy.a305.be.domain.repayment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RepaymentScheduleListResDto {
    private Integer contractSeq;
    private List<RepaymentScheduleResDto> schedules;
    public static RepaymentScheduleListResDto of(Integer contractSeq, List<RepaymentScheduleResDto> schedules) {
        return RepaymentScheduleListResDto.builder()
                .contractSeq(contractSeq)
                .schedules(schedules)
                .build();
    }
}