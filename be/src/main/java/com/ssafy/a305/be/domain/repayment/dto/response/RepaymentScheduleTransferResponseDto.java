package com.ssafy.a305.be.domain.repayment.dto.response;

/**
 * 회차 수동 이체 처리 응답 DTO.
 *
 * @param contractSeq         계약 식별자
 * @param scheduleSeq         상환 회차 식별자
 * @param contractId          계약번호 (C-...)
 * @param status              처리 후 회차 상태 (PAID)
 * @param transactionDate     SSAFY 거래일자(yyyyMMdd)
 * @param withdrawalAccountNo 출금 계좌번호(채무자)
 * @param depositAccountNo    입금 계좌번호(채권자)
 */
public record RepaymentScheduleTransferResponseDto(
        Integer contractSeq,
        Integer scheduleSeq,
        String contractId,
        String status,
        String transactionDate,
        String withdrawalAccountNo,
        String depositAccountNo
) {
}

