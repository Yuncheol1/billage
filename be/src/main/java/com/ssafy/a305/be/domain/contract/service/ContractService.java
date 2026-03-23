package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.ClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDetailResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractDraftResponseDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractListResponseDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

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

        if (!"ACTIVE".equals(debtor.getState()) || !"ACTIVE".equals(creditor.getState())) {
            throw new CustomBusinessException(ErrorCode.INACTIVE_USER_TARGET);
        }

        // 3. 계약 ID 채번: C-yyyyMMdd-랜덤문자
        String contractId = generateContractId();

        // 4. AI 서비스 연결 (Mock data 가정)
        // AIDraftResult aiResult = aiService.generateDraft(...);
        /*
         * List<String> warningFlags = new ArrayList<>();
         * if (requestDto.getInterestRate().compareTo(new BigDecimal("0.046")) < 0) {
         * warningFlags.add("LOW_INTEREST_RATE_TAX_WARNING");
         * }
         */

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

    @Transactional
    public void addAiClauses(Integer contractSeq, String clauseContent, Integer userSeq) {
        // 1. 계약 조회 (동시성 방지를 위한 비관적 락 사용)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        // 2. 상태 검증
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS);
        }

        // 3. 권한 검증
        if (!contract.getCreditor().getUserSeq().equals(userSeq)
                && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 4. AI 서비스 호출 (Mock 처리)
        String refinedContent = "[AI 변환] " + clauseContent;

        // 5. ContractClause 엔티티 생성 및 추가
        int nextOrderNo = contract.getClauses().size() + 1;

        ContractClause clause = ContractClause.builder()
                .contract(contract)
                .clauseId("SPECIAL_" + nextOrderNo)
                .title("특약사항 " + nextOrderNo)
                .content(refinedContent)
                .orderNo(nextOrderNo)
                .createdDttm(LocalDateTime.now())
                .build();

        contract.addClause(clause);
    }

    @Transactional
    public void updateClause(Integer contractSeq, Integer clauseSeq, ClauseRequestDto requestDto, Integer userSeq) {
        // 1. 계약 조회 (동시성 방지를 위한 비관적 락 사용 권장)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 2. 권한 검증
        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 특약을 수정할 권한이 없습니다.");
        }

        // 3. 상태 검증
        String status = contract.getStatus();
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안 작성 중이거나 협상 중인 계약의 특약만 수정할 수 있습니다.");
        }

        // 4. AI 서비스 호출 (Mock 처리)
        String refinedContent = "[AI 변환] " + requestDto.getClauseContent();

        // 5. 특약 조회 및 수정
        ContractClause targetClause = contract.getClauses().stream()
                .filter(c -> c.getClauseSeq().equals(clauseSeq))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 특약을 찾을 수 없습니다."));

        targetClause.updateContent(refinedContent);
    }

    @Transactional
    public void deleteClause(Integer contractSeq, Integer clauseSeq, Integer userSeq) {
        // 1. 계약 조회 (동시성 방지를 위한 비관적 락 사용 권장)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 2. 권한 검증
        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 특약을 삭제할 권한이 없습니다.");
        }

        // 3. 상태 검증
        String status = contract.getStatus();
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안 작성 중이거나 협상 중인 계약의 특약만 삭제할 수 있습니다.");
        }



        // 5. 특약 조회 및 수정
        ContractClause targetClause = contract.getClauses().stream()
                .filter(c -> c.getClauseSeq().equals(clauseSeq))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 특약을 찾을 수 없습니다."));

        contract.removeClause(targetClause);
    }
    @Transactional(readOnly = true)
    public List<ContractListResponseDto> getContracts(Integer userSeq, String role, String status) {
        List<Contract> contracts = contractRepository.searchContracts(userSeq, role, status);

        // N+1 문제 해결: 조회된 모든 파트너의 userSeq 추출 후 한 번에 프로필 조회
        List<Integer> counterpartySeqs = contracts.stream()
                .map(c -> c.getCreditor().getUserSeq().equals(userSeq) ? c.getDebtor().getUserSeq() : c.getCreditor().getUserSeq())
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> realNameMap = userProfileRepository.findByUserUserSeqIn(counterpartySeqs)
                .stream()
                .collect(Collectors.toMap(
                        up -> up.getUser().getUserSeq(),
                        UserProfile::getRealName,
                        (v1, v2) -> v1
                ));

        return contracts.stream().map(contract -> {
            boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
            String mappedRole = isCreditor ? "CREDITOR" : "DEBTOR";

            User counterparty = isCreditor ? contract.getDebtor() : contract.getCreditor();

            // N+1 해결된 메모리 맵에서 상대 실명 매핑
            String realName = realNameMap.getOrDefault(counterparty.getUserSeq(), counterparty.getEmail());

            int totalSchedules = contract.getRepaymentSchedules().size();
            int paidSchedules = (int) contract.getRepaymentSchedules().stream()
                    .filter(s -> "PAID".equals(s.getStatus()))
                    .count();

            return new ContractListResponseDto(
                    contract.getContractSeq(),
                    contract.getContractId(),
                    mappedRole,
                    realName,
                    contract.getPrincipalAmount(),
                    contract.getRepaymentMethod(),
                    contract.getStatus(),
                    contract.getStartDt(),
                    contract.getEndDt()
                    );
        }).collect(Collectors.toList());
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
                    .interestDue(calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(),
                            request.getStartDt(), request.getEndDt()))
                    .totalDue(request.getPrincipalAmount()
                            .add(calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(),
                                    request.getStartDt(), request.getEndDt())))
                    .status("NONPAID")
                    .createdDttm(LocalDateTime.now())
                    .build();
            contract.addRepaymentSchedule(schedule);

        } else /* if ("INSTALLMENT".equals(request.getRepaymentMethod())) */ {
            int installments = request.getTotalInstallments();
            BigDecimal principalPerMth = request.getPrincipalAmount().divide(new BigDecimal(installments), 2,
                    RoundingMode.DOWN);

            // 단순 월 분할 이자 계산 가정
            BigDecimal totalInterest = calculateTotalInterest(request.getPrincipalAmount(), request.getInterestRate(),
                    request.getStartDt(), request.getEndDt());
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
        return "C-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Transactional(readOnly = true)
    public ContractDetailResponseDto getContractDetail(Integer contractSeq, Integer userSeq) {

        Contract contract = contractRepository.findContractDetailById(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);

        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약을 열람할 권한이 없습니다.");
        }


        Integer counterpartySeq = isCreditor ? contract.getDebtor().getUserSeq() : contract.getCreditor().getUserSeq();
        String counterpartyName = userProfileRepository.findByUserUserSeq(counterpartySeq)
                .map(UserProfile::getRealName)
                .orElse(isCreditor ? contract.getDebtor().getEmail() : contract.getCreditor().getEmail());

        return new ContractDetailResponseDto(contract, userSeq, counterpartyName);
    }
    @Transactional
    public void deleteContract(Integer contractSeq, Integer userSeq) {
        // 계약 조회 (동시 삭제/서명 등의 충돌 방지를 위한 비관적 락 사용)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약을 삭제할 권한이 없습니다.");
        }

        String status = contract.getStatus();
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status)) {

            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "이미 서명이 완료되었거나 활성화된 계약은 파기할 수 없습니다.");
        }

        contractRepository.delete(contract);
    }
}
