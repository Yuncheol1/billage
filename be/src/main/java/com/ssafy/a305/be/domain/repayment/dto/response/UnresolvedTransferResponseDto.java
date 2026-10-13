package com.ssafy.a305.be.domain.repayment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 결과가 확정되지 않은 이체 회차. 운영자가 거래내역을 직접 확인할 때 쓴다.
 *
 * @param scheduleSeq 상환 회차 식별자
 * @param contractSeq 계약 식별자
 * @param contractId  계약번호 (C-...)
 * @param installmentNo 회차 번호
 * @param amount      이체 금액
 * @param dueDt       납부 기한
 * @param requestedAt 이체 요청 시각
 * @param transferMemo 이체 적요에 실은 회차 식별자. 거래내역에서 이 값으로 찾는다.
 */
public record UnresolvedTransferResponseDto(
        Integer scheduleSeq,
        Integer contractSeq,
        String contractId,
        Integer installmentNo,
        BigDecimal amount,
        LocalDate dueDt,
        LocalDateTime requestedAt,
        String transferMemo
) {
}
