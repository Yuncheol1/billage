package com.ssafy.a305.be.domain.repayment.batch;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentEventRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.TransferFailException;
import com.ssafy.a305.be.global.external.ssafy.SsafyTransferApiClient;
import com.ssafy.a305.be.global.external.ssafy.dto.SsafyTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 자동이체 배치 로직 단위 테스트.
 *
 * <p>DB/외부 API 의존성을 Mockito로 격리하여,
 * Processor의 비즈니스 로직과 도메인 메서드를 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AutoTransferBatchLogicTest {

    @Mock
    private AutoTransferRegistrationRepository registrationRepository;

    @Mock
    private RepaymentEventRepository repaymentEventRepository;

    @Mock
    private SsafyTransferApiClient ssafyTransferApiClient;

    private ItemProcessor<RepaymentSchedule, RepaymentSchedule> processor;

    // ──── 테스트 픽스쳐 ────
    private User debtor;
    private User creditor;
    private Contract contract;
    private BankAccount debtorAccount;
    private BankAccount creditorAccount;

    @BeforeEach
    void setUp() {
        // 테스트용 Processor를 직접 빌드 (AutoTransferBatchConfig의 로직 복제)
        processor = schedule -> {
            Integer contractSeq = schedule.getContract().getContractSeq();

            AutoTransferRegistration registration = registrationRepository
                    .findActiveWithAccounts(contractSeq)
                    .orElse(null);

            if (registration == null) {
                return null;
            }

            String withdrawalAccountNo = registration.getDebtorAccount().getAccountNo();
            String depositAccountNo = registration.getCreditorAccount().getAccountNo();
            BigDecimal amount = schedule.getTotalDue();

            String debtorUserKey = schedule.getContract().getDebtor().getSsafyUserKey();
            ssafyTransferApiClient.transfer(withdrawalAccountNo, depositAccountNo, amount, "빌려줘 자동이체", debtorUserKey);

            LocalDateTime now = LocalDateTime.now();
            schedule.markPaid(now);

            RepaymentEvent event = RepaymentEvent.builder()
                    .repaymentSchedule(schedule)
                    .contract(schedule.getContract())
                    .evidenceGrade("GOLD")
                    .amountPaid(amount)
                    .reportedByUser(schedule.getContract().getDebtor())
                    .confirmedByUser(schedule.getContract().getCreditor())
                    .status("AUTO_CONFIRMED")
                    .note("자동이체 처리")
                    .requestedDttm(now)
                    .confirmedDttm(now)
                    .build();
            repaymentEventRepository.save(event);

            return schedule;
        };

        // 픽스쳐 생성
        debtor = User.builder()
                .userSeq(1)
                .email("debtor@test.com")
                .ssafyUserKey("debtor-ssafy-key-001")
                .state("ACTIVE")
                .build();

        creditor = User.builder()
                .userSeq(2)
                .email("creditor@test.com")
                .ssafyUserKey("creditor-ssafy-key-002")
                .state("ACTIVE")
                .build();

        contract = Contract.builder()
                .contractSeq(100)
                .contractId("C-20260325-TEST01")
                .creditor(creditor)
                .debtor(debtor)
                .principalAmount(new BigDecimal("5000000"))
                .status("ACTIVE")
                .startDt(LocalDate.of(2026, 1, 1))
                .endDt(LocalDate.of(2027, 1, 1))
                .build();

        debtorAccount = BankAccount.builder()
                .accountSeq(10)
                .accountNo("0012345678901234")
                .isPrimary(true)
                .build();

        creditorAccount = BankAccount.builder()
                .accountSeq(20)
                .accountNo("0098765432109876")
                .isPrimary(true)
                .build();
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("도메인 메서드 테스트")
    class DomainMethodTests {

        @Test
        @DisplayName("markPaid(): 상태가 PAID로 변경되고 paidDttm이 설정된다")
        void markPaid_shouldChangeToPaid() {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            LocalDateTime paidAt = LocalDateTime.of(2026, 3, 25, 10, 0, 0);
            schedule.markPaid(paidAt);

            assertThat(schedule.getStatus()).isEqualTo("PAID");
            assertThat(schedule.getPaidDttm()).isEqualTo(paidAt);
        }

        @Test
        @DisplayName("markOverdue(): 상태가 OVERDUE로 변경된다")
        void markOverdue_shouldChangeToOverdue() {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            schedule.markOverdue();

            assertThat(schedule.getStatus()).isEqualTo("OVERDUE");
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Processor 성공 케이스")
    class ProcessorSuccessTests {

        @Test
        @DisplayName("자동이체 등록이 있고 API 성공 시 → PAID + RepaymentEvent 저장")
        void process_success_shouldMarkPaidAndSaveEvent() throws Exception {
            // given
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            AutoTransferRegistration registration = AutoTransferRegistration.builder()
                    .registrationSeq(1)
                    .contract(contract)
                    .debtorAccount(debtorAccount)
                    .creditorAccount(creditorAccount)
                    .amount(new BigDecimal("500000"))
                    .transferDay(15)
                    .isActive(true)
                    .build();

            given(registrationRepository.findActiveWithAccounts(100))
                    .willReturn(Optional.of(registration));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willReturn(new SsafyTransferResponse()); // Mock 성공 응답

            // when
            RepaymentSchedule result = processor.process(schedule);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PAID");
            assertThat(result.getPaidDttm()).isNotNull();

            // SSAFY API가 올바른 파라미터로 호출되었는지 검증
            then(ssafyTransferApiClient).should().transfer(
                    eq("0012345678901234"),   // 채무자 출금 계좌
                    eq("0098765432109876"),   // 채권자 입금 계좌
                    eq(new BigDecimal("500000")),
                    eq("빌려줘 자동이체"),
                    eq("debtor-ssafy-key-001")
            );

            // RepaymentEvent가 저장되었는지 검증
            ArgumentCaptor<RepaymentEvent> eventCaptor = ArgumentCaptor.forClass(RepaymentEvent.class);
            then(repaymentEventRepository).should().save(eventCaptor.capture());

            RepaymentEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getEvidenceGrade()).isEqualTo("GOLD");
            assertThat(savedEvent.getStatus()).isEqualTo("AUTO_CONFIRMED");
            assertThat(savedEvent.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(savedEvent.getReportedByUser()).isEqualTo(debtor);
            assertThat(savedEvent.getConfirmedByUser()).isEqualTo(creditor);
        }

        @Test
        @DisplayName("이체 금액은 Registration이 아니라 Schedule의 totalDue를 사용해야 한다")
        void process_shouldUseScheduleTotalDue_notRegistrationAmount() throws Exception {
            // given: Registration 금액(500만)과 Schedule totalDue(50만)가 다른 경우
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("416667"))  // 할부 1회분
                    .status("NONPAID")
                    .build();

            AutoTransferRegistration registration = AutoTransferRegistration.builder()
                    .registrationSeq(1)
                    .contract(contract)
                    .debtorAccount(debtorAccount)
                    .creditorAccount(creditorAccount)
                    .amount(new BigDecimal("5000000"))  // 메타 데이터 (원금 전체)
                    .transferDay(15)
                    .isActive(true)
                    .build();

            given(registrationRepository.findActiveWithAccounts(100))
                    .willReturn(Optional.of(registration));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willReturn(new SsafyTransferResponse());

            // when
            processor.process(schedule);

            // then: 416,667원이 이체되어야 함 (5,000,000원이 아님)
            then(ssafyTransferApiClient).should().transfer(
                    anyString(), anyString(),
                    eq(new BigDecimal("416667")),
                    anyString(), anyString()
            );
        }
    }

    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("Processor 실패/스킵 케이스")
    class ProcessorFailTests {

        @Test
        @DisplayName("자동이체 등록이 없으면 → null 반환 (Skip)")
        void process_noRegistration_shouldReturnNull() throws Exception {
            // given
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            given(registrationRepository.findActiveWithAccounts(100))
                    .willReturn(Optional.empty());

            // when
            RepaymentSchedule result = processor.process(schedule);

            // then
            assertThat(result).isNull();
            then(ssafyTransferApiClient).shouldHaveNoInteractions();
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SSAFY API 실패 시 → TransferFailException 발생")
        void process_apiFailure_shouldThrowException() {
            // given
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            AutoTransferRegistration registration = AutoTransferRegistration.builder()
                    .registrationSeq(1)
                    .contract(contract)
                    .debtorAccount(debtorAccount)
                    .creditorAccount(creditorAccount)
                    .amount(new BigDecimal("500000"))
                    .transferDay(15)
                    .isActive(true)
                    .build();

            given(registrationRepository.findActiveWithAccounts(100))
                    .willReturn(Optional.of(registration));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willThrow(new TransferFailException("잔액 부족"));

            // when & then
            assertThatThrownBy(() -> processor.process(schedule))
                    .isInstanceOf(TransferFailException.class)
                    .hasMessageContaining("잔액 부족");

            // 실패 시 상태 변경 안 됨
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            // 실패 시 RepaymentEvent 저장 안 됨
            then(repaymentEventRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("API 실패 후에도 원본 Schedule의 status가 NONPAID로 유지된다 (멱등성)")
        void process_apiFailure_shouldNotMutateSchedule() {
            // given
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .scheduleSeq(1)
                    .contract(contract)
                    .installmentNo(1)
                    .totalDue(new BigDecimal("500000"))
                    .status("NONPAID")
                    .build();

            AutoTransferRegistration registration = AutoTransferRegistration.builder()
                    .registrationSeq(1)
                    .contract(contract)
                    .debtorAccount(debtorAccount)
                    .creditorAccount(creditorAccount)
                    .amount(new BigDecimal("500000"))
                    .transferDay(15)
                    .isActive(true)
                    .build();

            given(registrationRepository.findActiveWithAccounts(100))
                    .willReturn(Optional.of(registration));
            given(ssafyTransferApiClient.transfer(anyString(), anyString(), any(), anyString(), anyString()))
                    .willThrow(new TransferFailException("네트워크 오류"));

            // when
            try {
                processor.process(schedule);
            } catch (Exception ignored) {
            }

            // then: Schedule은 변하지 않아야 함
            assertThat(schedule.getStatus()).isEqualTo("NONPAID");
            assertThat(schedule.getPaidDttm()).isNull();
        }
    }
}
