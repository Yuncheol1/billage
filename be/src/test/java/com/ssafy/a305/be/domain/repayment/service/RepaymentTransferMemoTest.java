package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이체 적요 형식과 판정 규칙 테스트.
 *
 * <p>이 형식은 정산이 거래내역에서 이체를 되찾는 유일한 근거이며, 한번 은행에 기록되면
 * 소급 수정이 불가능하다. 그래서 생성({@code of})과 판정({@code matches})이 항상 왕복
 * 가능한지를 중심으로 검증한다.</p>
 */
class RepaymentTransferMemoTest {

    private static final String CONTRACT_ID = "C-20260918-3F9A0C";

    private RepaymentSchedule newSchedule(Integer scheduleSeq, Integer installmentNo, String contractId) {
        Contract contract = Contract.builder()
                .contractSeq(100)
                .contractId(contractId)
                .principalAmount(new BigDecimal("5000000"))
                .status("ACTIVE")
                .build();

        return RepaymentSchedule.builder()
                .scheduleSeq(scheduleSeq)
                .contract(contract)
                .installmentNo(installmentNo)
                .totalDue(new BigDecimal("500000"))
                .status("NONPAID")
                .build();
    }

    @Nested
    @DisplayName("적요 생성")
    class Generation {

        @Test
        @DisplayName("마커·회차·계약번호를 이 순서로 조합한다")
        void of_shouldComposeMarkerInstallmentAndContractId() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.of(schedule))
                    .isEqualTo("BL12345-1 3회차 C-20260918-3F9A0C");
        }

        @Test
        @DisplayName("마커가 항상 맨 앞에 온다 (은행이 뒤를 잘라도 정산 키는 살아남아야 한다)")
        void of_shouldStartWithMarker() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.of(schedule)).startsWith("BL12345-1");
        }

        @Test
        @DisplayName("시도 번호가 적요에 반영된다")
        void of_shouldReflectAttemptNo() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.of(schedule, 2)).startsWith("BL12345-2");
        }

        @Test
        @DisplayName("installmentNo가 null이어도 예외 없이 회차 부분만 생략한다")
        void of_nullInstallmentNo_shouldOmitInstallmentPart() {
            RepaymentSchedule schedule = newSchedule(12345, null, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.of(schedule))
                    .isEqualTo("BL12345-1 C-20260918-3F9A0C");
        }

        @Test
        @DisplayName("contractId가 없어도 마커는 남는다")
        void of_blankContractId_shouldKeepMarker() {
            RepaymentSchedule schedule = newSchedule(12345, 3, null);

            assertThat(RepaymentTransferMemo.of(schedule)).isEqualTo("BL12345-1 3회차");
        }

        @Test
        @DisplayName("정상 형식은 방어적 상한(60자)에 걸리지 않는다")
        void of_normalCase_shouldBeWellUnderLimit() {
            RepaymentSchedule schedule = newSchedule(999999, 99, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.of(schedule).length()).isLessThanOrEqualTo(60);
        }
    }

    @Nested
    @DisplayName("거래내역 판정")
    class Matching {

        @Test
        @DisplayName("생성한 적요는 같은 회차로 다시 찾아진다 (왕복)")
        void matches_shouldRoundTrip() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);
            String memo = RepaymentTransferMemo.of(schedule);

            assertThat(RepaymentTransferMemo.matches(memo, 12345)).isTrue();
        }

        @Test
        @DisplayName("시도 번호가 달라도 같은 회차로 찾아진다")
        void matches_shouldIgnoreAttemptNo() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.matches(RepaymentTransferMemo.of(schedule, 7), 12345)).isTrue();
        }

        @Test
        @DisplayName("다른 회차의 이체는 찾아지지 않는다")
        void matches_otherSchedule_shouldBeFalse() {
            RepaymentSchedule schedule = newSchedule(12345, 3, CONTRACT_ID);

            assertThat(RepaymentTransferMemo.matches(RepaymentTransferMemo.of(schedule), 54321)).isFalse();
        }

        @Test
        @DisplayName("접두사가 겹치는 회차를 오인하지 않는다 (BL123 vs BL1234)")
        void matches_shouldNotConfusePrefixOverlap() {
            RepaymentSchedule longer = newSchedule(1234, 1, CONTRACT_ID);

            // "BL1234-1 ..." 안에 "BL123"이 들어 있지만, 구분자 '-' 덕분에 123으로는 매칭되면 안 된다
            assertThat(RepaymentTransferMemo.matches(RepaymentTransferMemo.of(longer), 123)).isFalse();
            assertThat(RepaymentTransferMemo.matches(RepaymentTransferMemo.of(longer), 1234)).isTrue();
        }

        @Test
        @DisplayName("은행이 적요 앞에 문구를 덧붙여도 찾아진다")
        void matches_withBankPrefix_shouldStillMatch() {
            assertThat(RepaymentTransferMemo.matches("(수시입출금) : BL12345-1 3회차", 12345)).isTrue();
        }

        @Test
        @DisplayName("마커가 없는 거래는 찾아지지 않는다")
        void matches_unrelatedTransaction_shouldBeFalse() {
            assertThat(RepaymentTransferMemo.matches("(수시입출금) : 입금", 12345)).isFalse();
            assertThat(RepaymentTransferMemo.matches("C-20260918-3F9A0C", 12345)).isFalse();
        }

        @Test
        @DisplayName("null 입력은 false")
        void matches_null_shouldBeFalse() {
            assertThat(RepaymentTransferMemo.matches(null, 12345)).isFalse();
            assertThat(RepaymentTransferMemo.matches("BL12345-1", null)).isFalse();
        }
    }
}
