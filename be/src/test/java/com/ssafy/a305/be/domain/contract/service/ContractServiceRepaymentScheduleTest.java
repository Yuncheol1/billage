package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 상환 스케줄 생성 테스트
 * 공통 조건: 원금 1,200,000원, 연 12%, 2026-01-10 시작, 매월 10일 상환. totalInstallments는 회차 수
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceRepaymentScheduleTest {

    private static final BigDecimal PRINCIPAL = new BigDecimal("1200000");
    private static final BigDecimal RATE = new BigDecimal("0.12");
    private static final LocalDate START = LocalDate.of(2026, 1, 10);

    @InjectMocks
    private ContractService contractService;

    @Test
    @DisplayName("분기 상환 4회차(12개월)는 3개월마다 원금을 나눠 갚고, 회차 이자는 남은 원금의 3개월치")
    void quarterly_shouldSplitPrincipalAndChargeQuarterInterest() {
        List<RepaymentSchedule> schedules = generate("QUARTERLY", 4, START.plusMonths(12));

        assertThat(schedules).extracting(RepaymentSchedule::getInstallmentNo).containsExactly(1, 2, 3, 4);
        assertThat(schedules).extracting(RepaymentSchedule::getDueDt).containsExactly(
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 10, 10), LocalDate.of(2027, 1, 10));
        assertAmounts(schedules, List.of("300000", "300000", "300000", "300000"),
                List.of("36000", "27000", "18000", "9000"));
    }

    @Test
    @DisplayName("반기 상환 2회차(12개월)는 6개월마다, 회차 이자는 6개월치")
    void semiAnnual_shouldChargeHalfYearInterest() {
        List<RepaymentSchedule> schedules = generate("SEMI_ANNUAL", 2, START.plusMonths(12));

        assertThat(schedules).extracting(RepaymentSchedule::getDueDt).containsExactly(
                LocalDate.of(2026, 7, 10), LocalDate.of(2027, 1, 10));
        assertAmounts(schedules, List.of("600000", "600000"), List.of("72000", "36000"));
    }

    @Test
    @DisplayName("연간 상환 2회차(24개월)는 1년마다, 회차 이자는 12개월치")
    void annual_shouldChargeYearInterest() {
        List<RepaymentSchedule> schedules = generate("ANNUAL", 2, START.plusMonths(24));

        assertThat(schedules).extracting(RepaymentSchedule::getDueDt).containsExactly(
                LocalDate.of(2027, 1, 10), LocalDate.of(2028, 1, 10));
        assertAmounts(schedules, List.of("600000", "600000"), List.of("144000", "72000"));
    }

    @Test
    @DisplayName("월 상환은 기존과 같이 매월 1회차, 남은 원금 기준 1개월치 이자")
    void monthly_shouldStayUnchanged() {
        List<RepaymentSchedule> schedules = generate("MONTHLY", 12, START.plusMonths(12));

        assertThat(schedules).hasSize(12);
        assertThat(schedules.get(0).getDueDt()).isEqualTo(LocalDate.of(2026, 2, 10));
        assertThat(schedules.get(11).getDueDt()).isEqualTo(LocalDate.of(2027, 1, 10));
        assertThat(schedules.get(0).getInterestDue()).isEqualByComparingTo("12000");
        assertThat(schedules.get(11).getInterestDue()).isEqualByComparingTo("1000");
        assertThat(sum(schedules, RepaymentSchedule::getInterestDue)).isEqualByComparingTo("78000");
        assertThat(sum(schedules, RepaymentSchedule::getPrincipalDue)).isEqualByComparingTo(PRINCIPAL);
    }

    @Test
    @DisplayName("반기·연간도 계약 기간이 한 주기 이상이면 생성할 수 있다")
    void validate_shouldAcceptWhenPeriodCoversOneCycle() {
        assertThatCode(() -> contractService.validateBusinessRules(request("SEMI_ANNUAL", 2, START.plusMonths(12))))
                .doesNotThrowAnyException();
        assertThatCode(() -> contractService.validateBusinessRules(request("ANNUAL", 2, START.plusMonths(24))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("계약 기간이 한 주기보다 짧으면 거절한다")
    void validate_shouldRejectWhenPeriodShorterThanOneCycle() {
        assertThatThrownBy(() -> contractService.validateBusinessRules(request("QUARTERLY", 1, START.plusMonths(2))))
                .isInstanceOf(CustomBusinessException.class);
    }

    private List<RepaymentSchedule> generate(String method, int installments, LocalDate endDt) {
        Contract contract = Contract.builder().build();

        contractService.generateRepaymentSchedules(contract, request(method, installments, endDt));

        return contract.getRepaymentSchedules();
    }

    private ContractDraftRequestDto request(String method, int installments, LocalDate endDt) {
        return new ContractDraftRequestDto(false, 1, 2, PRINCIPAL, RATE, method, installments, 10, START, endDt);
    }

    private void assertAmounts(List<RepaymentSchedule> schedules, List<String> principals, List<String> interests) {
        assertThat(schedules).hasSize(principals.size());
        for (int i = 0; i < schedules.size(); i++) {
            RepaymentSchedule s = schedules.get(i);
            assertThat(s.getPrincipalDue()).as("%d회차 원금", i + 1).isEqualByComparingTo(principals.get(i));
            assertThat(s.getInterestDue()).as("%d회차 이자", i + 1).isEqualByComparingTo(interests.get(i));
            assertThat(s.getTotalDue()).as("%d회차 합계", i + 1)
                    .isEqualByComparingTo(s.getPrincipalDue().add(s.getInterestDue()));
        }
        assertThat(sum(schedules, RepaymentSchedule::getPrincipalDue)).isEqualByComparingTo(PRINCIPAL);
        assertThat(schedules.get(schedules.size() - 1).getRemainingBalanceAfter()).isEqualByComparingTo("0");
    }

    private BigDecimal sum(List<RepaymentSchedule> schedules, Function<RepaymentSchedule, BigDecimal> field) {
        return schedules.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
