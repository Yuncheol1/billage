package com.ssafy.a305.be.domain.repayment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 운영자 확인용 미해결 이체 회차. transferMemo로 거래내역에서 찾는다.
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
