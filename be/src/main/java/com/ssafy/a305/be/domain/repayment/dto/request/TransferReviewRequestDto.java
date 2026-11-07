package com.ssafy.a305.be.domain.repayment.dto.request;

// 운영자 이체 결과 확인. withdrawn: 출금 확인 여부, note: 확인 근거
public record TransferReviewRequestDto(
        Boolean withdrawn,
        String note
) {
}
