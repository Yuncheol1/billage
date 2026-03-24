package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.contract.dto.request.ClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractUpdateRequestDto;
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
import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final AIService aiService;

    @Transactional
    public ContractDraftResponseDto createDraft(Integer creatorUserSeq, ContractDraftRequestDto requestDto) {

        // 1. 도메인 유효성 및 엣지 케이스 점검
        validateBusinessRules(creatorUserSeq, requestDto);

        // 2. 채권자/채무자 회원 엔티티 조회
        User creditor = userRepository.findById(requestDto.getCreditorUserSeq())
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
    public ContractClause addAiClauses(Integer contractSeq, String clauseContent, Integer userSeq) {
        // 1. 계약 조회 (동시성 방지를 위한 비관적 락 사용)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 2. 상태 검증
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안(DRAFT) 상태에서만 특약 추가가 가능합니다.");
        }

        // 3. 권한 검증
        if (!contract.getCreditor().getUserSeq().equals(userSeq)
                && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약의 당사자가 아닌 사용자가 요청");
        }

        // 4. AI 서비스 연동 호출
        String contractContext = String.format("원금: %s, 이자율: %s, 상환방식: %s",
                contract.getPrincipalAmount(), contract.getInterestRate(), contract.getRepaymentMethod());

        AiClauseRequestDto aiRequest = AiClauseRequestDto.builder()
                .contractSeq(contractSeq)
                .userIntent(clauseContent)
                .contractContext(contractContext)
                .templateType("custom")
                .build();

        AiClauseResponseDto aiResponse = aiService.generateClause(aiRequest);

        // 5. ContractClause 엔티티 생성 및 추가
        int nextOrderNo = contract.getClauses().size() + 1;
        String newTitle = (aiResponse.getTitle() != null && !aiResponse.getTitle().isEmpty())
                ? aiResponse.getTitle() : ("특약사항 " + nextOrderNo);
        String newContent = (aiResponse.getLegalClause() != null && !aiResponse.getLegalClause().isEmpty())
                ? aiResponse.getLegalClause() : clauseContent;
        String newClauseId = (aiResponse.getClauseId() != null && !aiResponse.getClauseId().isEmpty())
                ? aiResponse.getClauseId() : ("SPECIAL_" + nextOrderNo);

        ContractClause clause = ContractClause.builder()
                .contract(contract)
                .clauseId(newClauseId)
                .title(newTitle)
                .content(newContent)
                .orderNo(nextOrderNo)
                .createdDttm(LocalDateTime.now())
                .build();

        contract.addClause(clause);
        contractRepository.flush();
        return clause;
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
    public void updateContract(Integer contractSeq, ContractUpdateRequestDto requestDto, Integer userSeq) {
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약을 수정할 권한이 없습니다.");
        }

        String status = contract.getStatus();
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안 작성 중이거나 협상 중인 계약만 수정할 수 있습니다.");
        }

        contract.updateContractTerms(requestDto.getPrincipalAmount(), requestDto.getInterestRate());

        if (requestDto.getClauses() != null) {
            for (ContractUpdateRequestDto.ClauseUpdateDto reqClause : requestDto.getClauses()) {
                contract.getClauses().stream()
                        .filter(c -> c.getClauseSeq().equals(reqClause.getClauseSeq()))
                        .findFirst()
                        .ifPresent(clause -> clause.updateContent(reqClause.getContent()));
            }
        }
    }

    @Transactional
    public void createAiProposalMock(Integer contractSeq, Integer userSeq) {
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약에 접근할 권한이 없습니다.");
        }

        String status = contract.getStatus();
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안 작성 중이거나 협상 중인 계약만 제안서를 만들 수 있습니다.");
        }

        // Mock AI 제안 생성 로직: 이자율 2% 삭감
        BigDecimal proposedInterestRate = contract.getInterestRate() != null
            ? contract.getInterestRate().subtract(new BigDecimal("2.0000"))
            : new BigDecimal("5.0000");

        if (proposedInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            proposedInterestRate = BigDecimal.ZERO;
        }

        List<ContractUpdateRequestDto.ClauseUpdateDto> clauseDtos = contract.getClauses().stream()
                .map(c -> {
                    ContractUpdateRequestDto.ClauseUpdateDto dto = new ContractUpdateRequestDto.ClauseUpdateDto();
                    dto.setClauseSeq(c.getClauseSeq());
                    dto.setContent(c.getContent());
                    return dto;
                })
                .collect(Collectors.toList());

        // 첫 번째 특약에 AI 코멘트 추가
        if (!clauseDtos.isEmpty()) {
            ContractUpdateRequestDto.ClauseUpdateDto firstClause = clauseDtos.get(0);
            firstClause.setContent(firstClause.getContent() + " (AI 권장: 이자율 인하 및 원만한 상환 협의 권장)");
        }

        ContractUpdateRequestDto proposalDto = new ContractUpdateRequestDto();
        proposalDto.setPrincipalAmount(contract.getPrincipalAmount());
        proposalDto.setInterestRate(proposedInterestRate);
        proposalDto.setClauses(clauseDtos);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonStr = mapper.writeValueAsString(proposalDto);
            contract.setAiDraftContent(jsonStr); // AI 초안 내용으로 저장
        } catch (Exception e) {
            throw new RuntimeException("AI 제안 생성 실패", e);
        }
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

    private void validateBusinessRules(Integer creatorUserSeq, ContractDraftRequestDto request) {
        // 계약 생성자 본인 포함 여부 검증
        if (!creatorUserSeq.equals(request.getCreditorUserSeq()) && !creatorUserSeq.equals(request.getDebtorUserSeq())) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "본인이 포함된 계약만 생성할 수 있습니다.");
        }

        // 본인 거래 방지
        if (request.getCreditorUserSeq().equals(request.getDebtorUserSeq())) {
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
