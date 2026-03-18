package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractListResponseDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    
    // AI 연동 인터페이스. 실제 구현체는 따로 있다고 가정합니다.
    // private final AIService aiService;

    @Transactional
    public ContractDraftResponseDto createDraft(Integer creditorUserSeq, ContractDraftRequestDto requestDto) {
        
        // 1. 도메인 유효성 및 엣지 케이스 점검
        validateBusinessRules(creditorUserSeq, requestDto);

        // 2. 채권자/채무자 회원 엔티티 조회
        User creditor = userRepository.findById(creditorUserSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "채권자 회원을 찾을 수 없습니다."));
        
        User debtor = userRepository.findById(requestDto.getDebtorUserSeq())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INACTIVE_USER_TARGET));

        if (!"ACTIVE".equals(debtor.getState())||!"ACTIVE".equals(creditor.getState())) {
            throw new CustomBusinessException(ErrorCode.INACTIVE_USER_TARGET);
        }

        // 3. 계약 ID 채번: C-yyyyMMdd-랜덤문자
        String contractId = generateContractId();

        // 4. AI 서비스 연결 (Mock data 가정)
        // AIDraftResult aiResult = aiService.generateDraft(...);
        /*List<String> warningFlags = new ArrayList<>();
        if (requestDto.getInterestRate().compareTo(new BigDecimal("0.046")) < 0) {
            warningFlags.add("LOW_INTEREST_RATE_TAX_WARNING");
        }*/

        // 5. Contract 엔티티 생성
        Contract contract = Contract.builder()
                .contractId(contractId)
                .family(requestDto.getFamily())
                .creditor(creditor)
                .debtor(debtor)
                .principalAmount(requestDto.getPrincipalAmount())
                .interestRate(requestDto.getInterestRate())
                .repaymentMethod(requestDto.getRepaymentMethod())
                .totalInstallments(requestDto.getTotalInstallments())
                .startDt(requestDto.getStartDt())
                .endDt(requestDto.getEndDt())
                .status("DRAFT")
                .createdDttm(LocalDateTime.now())
                .build();



        // 7. 상환 계획 레코드 계산 및 생성
        generateRepaymentSchedules(contract, requestDto);

        // 8. DB 일괄 저장 (Cascade.ALL 이므로 contract만 저장해줌)
        Contract savedContract = contractRepository.save(contract);

        // 9. DTO 매핑 후 리턴
        return ContractDraftResponseDto.builder()
                .contractSeq(savedContract.getContractSeq())
                .contractId(savedContract.getContractId())
                .build();
    }

    private void validateBusinessRules(Integer creditorUserSeq, ContractDraftRequestDto request) {
        // 본인 거래 방지
        if (creditorUserSeq.equals(request.getDebtorUserSeq())) {
            throw new CustomBusinessException(ErrorCode.SELF_CONTRACT_NOT_ALLOWED);
        }

        // 날짜 유효성 검증
        LocalDate today = LocalDate.now();
        if (request.getStartDt().isBefore(today)) {
            throw new CustomBusinessException(ErrorCode.INVALID_DATE, "시작일은 과거일 수 없습니다.");
        }
        if (!request.getEndDt().isAfter(request.getStartDt())) {
            throw new CustomBusinessException(ErrorCode.INVALID_DATE, "종료일은 시작일보다 미래여야 합니다.");
        }

        // 상환 방식 정합성 검증
        if (!"LUMP_SUM".equals(request.getRepaymentMethod())) {
            if (request.getTotalInstallments() == null || request.getTotalInstallments() < 1) {
                throw new CustomBusinessException(ErrorCode.INVALID_REPAYMENT_LOGIC, "할부상환은 총 상환 횟수가 1 이상이어야 합니다.");
            }
            if (request.getRepaymentDay() == null || request.getRepaymentDay() < 1 || request.getRepaymentDay() > 31) {
                throw new CustomBusinessException(ErrorCode.INVALID_REPAYMENT_LOGIC, "매월 상환일(1~31) 설정이 누락되거나 잘못되었습니다.");
            }
        }
    }

    private void generateRepaymentSchedules(Contract contract, ContractDraftRequestDto request) {
        if ("LUMP_SUM".equals(request.getRepaymentMethod())) {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .contract(contract)
                    .installmentNo(1)
                    .dueDt(contract.getEndDt())
                    .principalDue(request.getPrincipalAmount())
                    .interestDue(calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(), request.getStartDt(), request.getEndDt()))
                    .totalDue(request.getPrincipalAmount().add(calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(), request.getStartDt(), request.getEndDt())))
                    .status("NONPAID")
                    .createdDttm(LocalDateTime.now())
                    .build();
            contract.addRepaymentSchedule(schedule);

        } else /*if ("INSTALLMENT".equals(request.getRepaymentMethod()))*/ {
            int installments = request.getTotalInstallments();
            BigDecimal principalPerMth = request.getPrincipalAmount().divide(new BigDecimal(installments), 2, RoundingMode.DOWN);
            
            // 단순 월 분할 이자 계산 가정
            BigDecimal totalInterest = calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(), request.getStartDt(), request.getEndDt());
            BigDecimal interestPerMth = totalInterest.divide(new BigDecimal(installments), 2, RoundingMode.DOWN);

            BigDecimal remainingPrincipal = request.getPrincipalAmount();

            for (int i = 1; i <= installments; i++) {
                // 납부일자 계산 (repaymentDay 기반 엣지 케이스 처리)
                LocalDate targetMonth = request.getStartDt().plusMonths(i);
                int maxDaysInMonth = targetMonth.lengthOfMonth();
                int actualDay = Math.min(request.getRepaymentDay(), maxDaysInMonth);
                
                LocalDate dueDt = targetMonth.withDayOfMonth(actualDay);
                if (dueDt.isAfter(request.getEndDt())) {
                    dueDt = request.getEndDt(); // 만기일 초과 금지
                }

                // 마지막 회차에는 남은 원금을 다 털어넣도록 자투리 처리
                BigDecimal currentPrincipalDue = (i == installments) ? remainingPrincipal : principalPerMth;
                remainingPrincipal = remainingPrincipal.subtract(currentPrincipalDue);

                RepaymentSchedule schedule = RepaymentSchedule.builder()
                        .contract(contract)
                        .installmentNo(i)
                        .dueDt(dueDt)
                        .principalDue(currentPrincipalDue)
                        .interestDue(interestPerMth)
                        .totalDue(currentPrincipalDue.add(interestPerMth))
                        .remainingBalanceAfter(remainingPrincipal)
                        .status("NONPAID")
                        .createdDttm(LocalDateTime.now())
                        .build();

                contract.addRepaymentSchedule(schedule);
            }
        }
    }

    private BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal rate, LocalDate start, LocalDate end) {
        // 실제 금융 로직에서는 일할 계산 등 복잡하지만 예시용 산식입니다.
        return principal.multiply(rate);
    }

    private String generateContractId() {
        return "C-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
