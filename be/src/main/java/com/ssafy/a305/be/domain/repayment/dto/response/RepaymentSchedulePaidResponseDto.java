package com.ssafy.a305.be.domain.repayment.dto.response;

/**
 * 회차 상환 처리(이체 없음) 응답 DTO.
 *
 * @param contractSeq 계약 식별자
 * @param scheduleSeq 상환 회차 식별자
 * @param contractId  계약번호 (C-...)
 * @param status      처리 후 회차 상태 (MANUAL_PAID)
 */
public record RepaymentSchedulePaidResponseDto(
        Integer contractSeq,
        Integer scheduleSeq,
        String contractId,
        String status
) {
}

