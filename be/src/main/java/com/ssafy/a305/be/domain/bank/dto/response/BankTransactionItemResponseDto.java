package com.ssafy.a305.be.domain.bank.dto.response;

/**
 * 연동 계좌 거래 내역 한 건 (목록·단건 공통, SSAFY REC 기반 + 서버 가공).
 * <p>
 * {@code transactionSummary}는 SSAFY 원문 그대로(입금 고정 문구, 이체 시 계약번호 등).
 * {@code transactionBalance}는 화면용 절댓값 문자열.
 *
 * @param transactionUniqueNo   SSAFY 거래고유번호(단건 조회 키)
 * @param transactionDate       거래일자({@code yyyyMMdd})
 * @param transactionTime       거래시각({@code HHmmss})
 * @param transactionType       거래 구분 코드(SSAFY, 예: {@code 1} 입금 / {@code 2} 출금 / {@code A} 전체 조회용)
 * @param transactionTypeName   거래 구분 표시명(예: 입금, 출금)
 * @param transactionAccountNo  상대 계좌번호(SSAFY 제공, 없으면 빈 문자열)
 * @param transactionBalance    거래 금액(부호 제거한 절댓값 문자열)
 * @param transactionAfterBalance 이 거래 반영 후 잔액
 * @param transactionSummary    거래 적요·요약(SSAFY 문자열 그대로) => 계약 번호(C-...)
 * @param transactionMemo       거래 메모(SSAFY)
 * @param counterpartName       {@code transactionAccountNo}로 우리 DB에서 찾은 사용자 실명, 없으면 {@code null}
 */
public record BankTransactionItemResponseDto(
        String transactionUniqueNo,
        String transactionDate,
        String transactionTime,
        String transactionType,
        String transactionTypeName,
        String transactionAccountNo,
        String transactionBalance,
        String transactionAfterBalance,
        String transactionSummary,
        String transactionMemo,
        String counterpartName
) {
}
