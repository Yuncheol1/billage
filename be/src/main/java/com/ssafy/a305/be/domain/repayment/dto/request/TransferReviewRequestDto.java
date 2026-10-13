package com.ssafy.a305.be.domain.repayment.dto.request;

/**
 * 운영자의 이체 결과 확인 요청.
 *
 * @param withdrawn 거래내역에서 이 회차의 출금을 확인했는지
 * @param note      확인 근거 (예: "거래내역 2026-10-13 10:00:12 출금 1건 확인")
 */
public record TransferReviewRequestDto(
        Boolean withdrawn,
        String note
) {
}
