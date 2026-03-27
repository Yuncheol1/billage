package com.ssafy.a305.be.domain.contract.service;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.account.repository.BankAccountRepository;
import com.ssafy.a305.be.domain.chat.repository.ChatMessageRepository;
import com.ssafy.a305.be.domain.chat.service.ChatService;
import com.ssafy.a305.be.domain.contract.dto.request.AiClauseChatRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.AiChatMessageDto;
import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractDraftRequestDto;
import com.ssafy.a305.be.domain.contract.dto.request.ContractUpdateRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.*;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.document.entity.Document;
import com.ssafy.a305.be.domain.document.repository.DocumentRepository;
import com.ssafy.a305.be.domain.document.repository.SignatureRepository;
import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.AutoTransferRegistrationRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private static final String CONTRACT_ORIGINAL = "CONTRACT_ORIGINAL";

    private final ContractRepository contractRepository;
    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final BankAccountRepository bankAccountRepository;
    private final AutoTransferRegistrationRepository autoTransferRegistrationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EntityManager entityManager;

    // AI 연동 서비스 (FastAPI 연동)
    private final AIService aiService;

    // AI 분석 결과 캐시 (Key: contractSeq, Value: {lastUpdated: LocalDateTime, result: String})
    private final Map<Integer, AnalysisCacheEntry> analysisCache = new ConcurrentHashMap<>();

    private static class AnalysisCacheEntry {
        LocalDateTime lastUpdated;
        String result;
        AnalysisCacheEntry(LocalDateTime lastUpdated, String result) {
            this.lastUpdated = lastUpdated;
            this.result = result;
        }
    }

    @Transactional
    public ContractDraftResponseDto createDraft(Integer myUserSeq, ContractDraftRequestDto requestDto) {

        // 1. 로그인한 사용자가 채권자인지 채무자인지 판별
        boolean iAmCreditor = myUserSeq.equals(requestDto.getCreditorUserSeq());
        boolean iAmDebtor   = myUserSeq.equals(requestDto.getDebtorUserSeq());

        if (!iAmCreditor && !iAmDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED,
                    "계약 생성자는 채권자 또는 채무자 중 한 명이어야 합니다.");
        }

        // 2. 도메인 유효성 및 엣지 케이스 점검
        validateBusinessRules(requestDto);

        // 3. 채권자/채무자 회원 엔티티 조회
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

        // 9. 자동이체 등록 (채무자/채권자 주 계좌 기반)
        registerAutoTransfer(savedContract, creditor, debtor, requestDto);

        // 10. DTO 매핑 후 리턴
        return ContractDraftResponseDto.builder()
                .contractSeq(savedContract.getContractSeq())
                .contractId(savedContract.getContractId())
                .build();
    }
    @Transactional
    public ContractClause addClauses(Integer contractSeq, String clauseContent, Integer userSeq) {
        // 1. 계약 조회 (동시성 방지를 위한 비관적 락 사용)
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 2. 상태 검증
        if (!"DRAFT".equals(contract.getStatus()) && !"NEGOTIATING".equals(contract.getStatus())) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "초안(DRAFT) 또는 협상(NEGOTIATING) 상태에서만 특약 추가가 가능합니다.");
        }

        // 3. 권한 검증
        if (!contract.getCreditor().getUserSeq().equals(userSeq)
                && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약의 당사자가 아닌 사용자가 요청");
        }

        // 4. AI 없이 바로 특약 생성
        int nextOrderNo = contract.getClauses().size() + 1;

        ContractClause clause = ContractClause.builder()
                .contract(contract)
                .clauseId("SPECIAL_" + nextOrderNo)
                .title("특약사항 " + nextOrderNo)
                .content(clauseContent)
                .orderNo(nextOrderNo)
                .createdDttm(LocalDateTime.now())
                .build();

        contract.addClause(clause);
        contractRepository.flush();
        return clause;
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

        // AI가 유효한 법률 특약을 생성하지 못하고 거절한 경우
        if (aiResponse.getRejected() ||
            aiResponse.getLegalClause() == null || 
            aiResponse.getLegalClause().trim().isEmpty()) {
            
            String reason = (aiResponse.getRejectionReason() != null && !aiResponse.getRejectionReason().isBlank())
                    ? aiResponse.getRejectionReason()
                    : ((aiResponse.getExplanation() != null && !aiResponse.getExplanation().isBlank())
                        ? aiResponse.getExplanation()
                        : "입력하신 내용으로 유효한 법률 특약을 생성할 수 없습니다.");
                        
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "특약 생성 거절: " + reason);
        }

        // 5. ContractClause 엔티티 생성 및 추가
        int nextOrderNo = contract.getClauses().size() + 1;
        String newTitle = (aiResponse.getTitle() != null && !aiResponse.getTitle().isBlank())
                ? aiResponse.getTitle() : ("특약사항 " + nextOrderNo);
        String newContent = aiResponse.getLegalClause();
        String newClauseId = (aiResponse.getClauseId() != null && !aiResponse.getClauseId().isBlank())
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
    public void updateContract(Integer contractSeq, ContractUpdateRequestDto requestDto, Integer userSeq, String roomId) {
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 권한 및 상태 체크
        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);
        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약을 수정할 권한이 없습니다.");
        }

        String status = contract.getStatus();
        if (!"DRAFT".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "수정 가능한 상태가 아닙니다.");
        }


        // 브로드캐스트를 위한 이전 데이터 보관 및 변경 내역 계산
        BigDecimal corePrincipal = contract.getPrincipalAmount();
        BigDecimal coreInterest = contract.getInterestRate();
        List<String> dynamicModifications = new java.util.ArrayList<>();

        // 1. 원금 및 이자율 직접 업데이트
        if (requestDto.getPrincipalAmount() != null) {
            BigDecimal newPrincipal = requestDto.getPrincipalAmount();
            if (corePrincipal == null || corePrincipal.compareTo(newPrincipal) != 0) {
                dynamicModifications.add(String.format("원금 수정: %,d원 → %,d원", 
                    corePrincipal != null ? corePrincipal.longValue() : 0, 
                    newPrincipal.longValue()));
                contract.updateContractTerms(newPrincipal, contract.getInterestRate() != null ? contract.getInterestRate() : BigDecimal.ZERO);
            }
        }

        if (requestDto.getInterestRate() != null) {
            BigDecimal newInterest = requestDto.getInterestRate();
            if (coreInterest == null || coreInterest.compareTo(newInterest) != 0) {
                dynamicModifications.add(String.format("이자율 수정: %.2f%% → %.2f%%", 
                    coreInterest != null ? coreInterest.doubleValue() * 100 : 0, 
                    newInterest.doubleValue() * 100));
                contract.updateContractTerms(contract.getPrincipalAmount(), newInterest);
            }
        }

        // 2. 특약 사항 직접 업데이트
        if (requestDto.getClauses() != null) {
            Set<Integer> touchedIds = new java.util.HashSet<>();
            List<ContractClause> currentClauses = new java.util.ArrayList<>(contract.getClauses());
            
            for (ContractUpdateRequestDto.ClauseUpdateDto req : requestDto.getClauses()) {
                if (req.getClauseSeq() != null && req.getClauseSeq() > 0) {
                    // 기존 특약 수정 혹은 유지
                    touchedIds.add(req.getClauseSeq());
                    currentClauses.stream()
                        .filter(c -> c.getClauseSeq().equals(req.getClauseSeq()))
                        .findFirst()
                        .ifPresent(c -> {
                            if (!c.getContent().equals(req.getContent())) {
                                // 내용이 변경된 경우 Diff 효과 (삭제 후 추가로 표시)
                                dynamicModifications.add("DEL: " + c.getContent());
                                dynamicModifications.add("ADD: " + req.getContent());
                                c.updateContent(req.getContent());
                            }
                        });
                } else {
                    // 신규 특약 추가
                    int nextOrder = contract.getClauses().size() + 1;
                    ContractClause newClause = ContractClause.builder()
                        .contract(contract)
                        .clauseId("SPECIAL_" + UUID.randomUUID().toString().substring(0, 8))
                        .title("특약사항")
                        .content(req.getContent())
                        .orderNo(nextOrder)
                        .createdDttm(LocalDateTime.now())
                        .build();
                    contract.addClause(newClause);
                    dynamicModifications.add("ADD: " + req.getContent());
                }
            }

            // 삭제된 항목 감지 (기존에 있었으나 요청에 없는 것)
            for (ContractClause c : currentClauses) {
                if (!touchedIds.contains(c.getClauseSeq())) {
                    dynamicModifications.add("DEL: " + c.getContent());
                    contract.removeClause(c);
                }
            }
        }

        // 상태 초기화
        contract.setAiDraftContent(null); // 즉시 반영이므로 제안서 필드 비움
        contract.resetAgreement(); // 수락 상태 초기화



        // 아무런 변경이 없었다면 브로드캐스트 생략 가능
        if (dynamicModifications.isEmpty() && (roomId == null || roomId.isBlank())) {
            return;
        }

        // 채팅방에 수정 완료 알림 발송 (roomId가 있을 때만)
        if (roomId != null && !roomId.isBlank()) {
            try {
                // 발신자 역할 및 이름 조회
                String senderRole = "UNKNOWN";
                if (contract.getCreditor().getUserSeq().equals(userSeq)) {
                    senderRole = "CREDITOR";
                } else if (contract.getDebtor().getUserSeq().equals(userSeq)) {
                    senderRole = "DEBTOR";
                }

                String senderName = userProfileRepository.findByUserUserSeq(userSeq)
                    .map(com.ssafy.a305.be.domain.user.entity.UserProfile::getRealName)
                    .orElse("알 수 없음");

                // 알림 페이로드 구성
                Map<String, Object> payload = Map.of(
                    "contract_title", "빌리지 거래 수정됨",
                    "contract_seq", contractSeq,
                    "sender_role", senderRole,
                    "sender_name", senderName,
                    "original_principal", corePrincipal != null ? corePrincipal.doubleValue() : 0.0,
                    "proposed_principal", contract.getPrincipalAmount() != null ? contract.getPrincipalAmount().doubleValue() : 0.0,
                    "original_interest_rate", coreInterest != null ? coreInterest.doubleValue() : 0.0,
                    "proposed_interest_rate", contract.getInterestRate() != null ? contract.getInterestRate().doubleValue() : 0.0,
                    "modifications", dynamicModifications.isEmpty() ? List.of("기타 약정 변경") : dynamicModifications,
                    "is_initial", false
                );

                ObjectMapper mapper = new ObjectMapper();
                String contentJson = mapper.writeValueAsString(payload);

                com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto msgRequest = 
                    new com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto(
                        roomId, 
                        userSeq, 
                        contentJson, 
                        true, 
                        "AI_PROPOSAL" // 카카오 카드 UI 유지를 위해 타입 유지
                    );

                com.ssafy.a305.be.domain.chat.dto.ChatMessageResponseDto response = chatService.saveMessage(msgRequest);
                messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, response);

            } catch (Exception e) {
                log.error("채팅방 수정 알림 발송 실패", e);
            }
        }
    }

    @Transactional
    public void createAiProposalMock(Integer contractSeq, Integer userSeq) {
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 권한 체크
        if (!contract.getCreditor().getUserSeq().equals(userSeq) && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약에 접근할 권한이 없습니다.");
        }

        // Mock AI 제안: 이자율 2% 삭감하여 즉시 반영
        BigDecimal currentRate = contract.getInterestRate();
        BigDecimal proposedRate = currentRate != null ? currentRate.subtract(new BigDecimal("0.02")) : new BigDecimal("0.05");
        if (proposedRate.compareTo(BigDecimal.ZERO) < 0) proposedRate = BigDecimal.ZERO;

        ContractUpdateRequestDto updateDto = new ContractUpdateRequestDto();
        updateDto.setPrincipalAmount(contract.getPrincipalAmount());
        updateDto.setInterestRate(proposedRate);
        
        List<ContractUpdateRequestDto.ClauseUpdateDto> clauses = new ArrayList<>();
        if (contract.getClauses() != null) {
            for (ContractClause c : contract.getClauses()) {
                ContractUpdateRequestDto.ClauseUpdateDto cd = new ContractUpdateRequestDto.ClauseUpdateDto();
                cd.setClauseSeq(c.getClauseSeq());
                cd.setContent(c.getContent());
                clauses.add(cd);
            }
        }
        updateDto.setClauses(clauses);
        
        // 즉시 반영
        updateContract(contractSeq, updateDto, userSeq, null);
    }
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

            return new ContractListResponseDto(
                    contract.getContractSeq(),
                    contract.getContractId(),
                    mappedRole,
                    realName,
                    contract.getPrincipalAmount(),
                    contract.getInterestRate(),
                    contract.getRepaymentMethod(),
                    contract.getStatus(),
                    contract.getStartDt(),
                    contract.getEndDt()
                    );
        }).collect(Collectors.toList());
    }

    private void validateBusinessRules(ContractDraftRequestDto request) {
        // 본인 거래 방지
        if (request.getCreditorUserSeq().equals(request.getDebtorUserSeq())) {
            throw new CustomBusinessException(ErrorCode.SELF_CONTRACT_NOT_ALLOWED);
        }

        // 날짜 유효성 검증
        if (!request.getEndDt().isAfter(request.getStartDt())) {
            throw new CustomBusinessException(ErrorCode.INVALID_DATE, "종료일은 시작일보다 미래여야 합니다.");
        }

        // 상환 방식 정합성 검증 (모든 방식은 상환 횟수 1 이상, 상환일 지정 필수)
        if (request.getTotalInstallments() == null || request.getTotalInstallments() < 1) {
            throw new CustomBusinessException(ErrorCode.INVALID_REPAYMENT_LOGIC, "총 상환 횟수가 1 이상이어야 합니다.");
        }
        if (request.getRepaymentDay() == null || request.getRepaymentDay() < 1 || request.getRepaymentDay() > 31) {
            throw new CustomBusinessException(ErrorCode.INVALID_REPAYMENT_LOGIC, "매월 상환일(1~31) 설정이 누락되거나 잘못되었습니다.");
        }
    }
    //(LUMP_SUM =일시상환/ MONTHLY =1달마다/ QUARTERLY=3개월마다 / SEMI_ANNUAL=6개월마다 / ANNUAL=1년마다)
    private void generateRepaymentSchedules(Contract contract, ContractDraftRequestDto request) {
        String method = request.getRepaymentMethod();
        BigDecimal interestRating=request.getInterestRate();
        if ("LUMP_SUM".equals(method)&&interestRating.compareTo(BigDecimal.ZERO) != 0) {
            int installments = request.getTotalInstallments();
            
            BigDecimal interestPerMth = request.getPrincipalAmount()
                    .multiply(request.getInterestRate())
                    .divide(new BigDecimal("12"), 2, RoundingMode.DOWN);

            for (int i = 1; i <= installments; i++) {
                boolean isLastMonth = (i == installments);
                
                BigDecimal currentPrincipalDue = isLastMonth ? request.getPrincipalAmount() : BigDecimal.ZERO;
                BigDecimal currentInterestDue = interestPerMth;
                
                LocalDate targetMonth = request.getStartDt().plusMonths(i);
                int maxDaysInMonth = targetMonth.lengthOfMonth();
                int actualDay = Math.min(request.getRepaymentDay(), maxDaysInMonth);
                LocalDate dueDt = targetMonth.withDayOfMonth(actualDay);
                
                if (dueDt.isAfter(request.getEndDt())) {
                    dueDt = request.getEndDt();
                }

                RepaymentSchedule schedule = RepaymentSchedule.builder()
                        .contract(contract)
                        .installmentNo(i)
                        .dueDt(dueDt)
                        .principalDue(currentPrincipalDue)
                        .interestDue(currentInterestDue)
                        .totalDue(currentPrincipalDue.add(currentInterestDue))
                        .remainingBalanceAfter(isLastMonth ? BigDecimal.ZERO : request.getPrincipalAmount())
                        .status("NONPAID")
                        .createdDttm(LocalDateTime.now())
                        .build();

                contract.addRepaymentSchedule(schedule);
            }

        } else if("LUMP_SUM".equals(method)&&interestRating.compareTo(BigDecimal.ZERO) == 0){
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
        }

        else {
            int intervalMonths = switch (method) {
                case "MONTHLY" -> 1;
                case "QUARTERLY" -> 3;
                case "SEMI_ANNUAL" -> 6;
                case "ANNUAL" -> 12;
                default -> throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 상환 방식입니다.");
            };

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
                LocalDate targetMonth = request.getStartDt().plusMonths((long) i * intervalMonths);
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

    /**
     * 자동이체 등록: 채무자/채권자의 주 계좌를 조회하여 AutoTransferRegistration을 생성·저장한다.
     */
    private void registerAutoTransfer(Contract contract, User creditor, User debtor, ContractDraftRequestDto request) {
        BankAccount debtorAccount = bankAccountRepository.findByUserUserSeqAndIsPrimaryTrue(debtor.getUserSeq())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "채무자의 주 계좌가 등록되어 있지 않습니다. 자동이체를 설정할 수 없습니다."));

        BankAccount creditorAccount = bankAccountRepository.findByUserUserSeqAndIsPrimaryTrue(creditor.getUserSeq())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "채권자의 주 계좌가 등록되어 있지 않습니다. 자동이체를 설정할 수 없습니다."));

        // transferDay: 모든 상환 방식은 지정된 상환일을 따름
        int transferDay = request.getRepaymentDay();

        AutoTransferRegistration registration = AutoTransferRegistration.builder()
                .contract(contract)
                .debtorAccount(debtorAccount)
                .creditorAccount(creditorAccount)
                .amount(request.getPrincipalAmount())
                .transferDay(transferDay)
                .isActive(true)
                .registeredDttm(LocalDateTime.now())
                .build();

        autoTransferRegistrationRepository.save(registration);
        log.info("[자동이체 등록] contractSeq={}, 채무자계좌={}, 채권자계좌={}, transferDay={}",
                contract.getContractSeq(), debtorAccount.getAccountNo(), creditorAccount.getAccountNo(), transferDay);
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
        UserProfile counterpartyProfile = userProfileRepository.findByUserUserSeq(counterpartySeq).orElse(null);
        String counterpartyName = counterpartyProfile != null && counterpartyProfile.getRealName() != null
                ? counterpartyProfile.getRealName()
                : (isCreditor ? contract.getDebtor().getEmail() : contract.getCreditor().getEmail());
        String counterpartyProfileImageUrl = counterpartyProfile != null
                ? counterpartyProfile.getProfileImageUrl()
                : null;

        SignatureStatus signatureStatus = resolveSignatureStatus(contract, userSeq);
        return new ContractDetailResponseDto(
                contract,
                userSeq,
                counterpartyName,
                counterpartyProfileImageUrl,
                signatureStatus.isMeSigned(),
                signatureStatus.isOpponentSigned()
        );
    }

    private SignatureStatus resolveSignatureStatus(Contract contract, Integer myUserSeq) {
        Document latestDocument = documentRepository
                .findTopByContractContractSeqAndDocTypeOrderByVersionDesc(contract.getContractSeq(), CONTRACT_ORIGINAL)
                .orElse(null);

        boolean creditorSigned = false;
        boolean debtorSigned = false;

        if (latestDocument != null) {
            Integer documentSeq = latestDocument.getDocumentSeq();
            creditorSigned = signatureRepository.existsByDocumentDocumentSeqAndUserUserSeq(
                    documentSeq,
                    contract.getCreditor().getUserSeq()
            );
            debtorSigned = signatureRepository.existsByDocumentDocumentSeqAndUserUserSeq(
                    documentSeq,
                    contract.getDebtor().getUserSeq()
            );
        }

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(myUserSeq);
        return new SignatureStatus(
                isCreditor ? creditorSigned : debtorSigned,
                isCreditor ? debtorSigned : creditorSigned
        );
    }

    private static final class SignatureStatus {
        private final boolean meSigned;
        private final boolean opponentSigned;

        private SignatureStatus(boolean meSigned, boolean opponentSigned) {
            this.meSigned = meSigned;
            this.opponentSigned = opponentSigned;
        }

        private boolean isMeSigned() {
            return meSigned;
        }

        private boolean isOpponentSigned() {
            return opponentSigned;
        }
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
        if (!"DRAFT".equals(status) && !"NEGOTIATING".equals(status) && !"CLOSED".equals(status)) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "이미 서명이 완료되었거나 진행중인 계약은 파기할 수 없습니다.");
        }

        // 계약에 종속된 하위 엔티티 벌크 삭제 (외래키 제약조건 위배 방지)
        
        // 0. BankTransaction 연결 해제 (repayment_schedule 삭제 시 외래키 제약 방지)
        entityManager.createNativeQuery("UPDATE bank_transaction bt JOIN repayment_schedule rs ON bt.matched_schedule_seq = rs.schedule_seq SET bt.matched_schedule_seq = NULL WHERE rs.contract_seq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();

        // 1. Signature -> Document 삭제 시 자식 삭제 (DocumentSeq 기반)
        entityManager.createNativeQuery("DELETE s FROM signature s JOIN document d ON s.document_seq = d.document_seq WHERE d.contract_seq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();
        
        // 2. Document
        entityManager.createQuery("DELETE FROM Document d WHERE d.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();
        
        // 3. AutoTransferRegistration
        entityManager.createQuery("DELETE FROM AutoTransferRegistration a WHERE a.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();

        // 기타 하위 엔티티가 있다면 bulk delete 필요
        // NotificationLog, RepaymentEvent, TaxCalculation, EarlySettlement
        // 하지만 JPARepository를 즉시 도입하기 어려우므로 EntityManager 활용.
        entityManager.createQuery("DELETE FROM NotificationLog n WHERE n.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();
                
        entityManager.createQuery("DELETE FROM TaxCalculation t WHERE t.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();
                
        entityManager.createQuery("DELETE FROM EarlySettlement e WHERE e.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();
                
        // RepaymentEvent는 contractSeq를 가지고 있음
        entityManager.createQuery("DELETE FROM RepaymentEvent r WHERE r.contract.contractSeq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();

        // ClauseNegotiation (contract_clause의 자식)
        entityManager.createNativeQuery("DELETE cn FROM clause_negotiation cn JOIN contract_clause cc ON cn.clause_seq = cc.clause_seq WHERE cc.contract_seq = :contractSeq")
                .setParameter("contractSeq", contractSeq).executeUpdate();

        // Contract 자체 삭제 (Cascade.ALL 에 의해 contract_clause, repayment_schedule 자동 삭제)
        contractRepository.delete(contract);
    }

    @Transactional
    public ContractDetailResponseDto acceptContract(Integer contractSeq, Integer userSeq, String roomId) {
        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);

        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약을 수락할 권한이 없습니다.");
        }

        // 수락 처리
        contract.accept(userSeq);
        
        // 양측 합의 완료 시 수행할 추가 작업이 있다면 여기에 구현 (현재는 즉시 반영 방식이므로 병합 불필요)
        if (Boolean.TRUE.equals(contract.getCreditorAccepted()) && Boolean.TRUE.equals(contract.getDebtorAccepted())) {
            contract.setAiDraftContent(null); // 혹시 남아있을 제안서 데이터 정리
            log.info("Contract mutual agreement reached for contractSeq={}", contractSeq);
        }
        
        contractRepository.save(contract);

        // 시스템 메시지 발송
        String userName = userProfileRepository.findByUserUserSeq(userSeq)
                .map(UserProfile::getRealName)
                .orElse("알 수 없음");

        String content = String.format("%s님이 계약 조건을 수락했습니다.", userName);
        
        com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto messageRequest = 
            new com.ssafy.a305.be.domain.chat.dto.ChatMessageRequestDto(
                roomId,
                userSeq,
                content,
                true,
                "SYSTEM"
            );
        
        com.ssafy.a305.be.domain.chat.dto.ChatMessageResponseDto response = chatService.saveMessage(messageRequest);
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, response);

        return getContractDetail(contractSeq, userSeq);
    }

    @Transactional(readOnly = true)
    public String getAiAnalysis(Integer contractSeq, Integer userSeq) {
        Contract contract = contractRepository.findById(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        // 권한 체크
        if (!contract.getCreditor().getUserSeq().equals(userSeq) && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "계약 분석을 열람할 권한이 없습니다.");
        }

        // 캐시 확인 (계약 수정 시각 기준)
        LocalDateTime lastUpdate = contract.getUpdatedDttm() != null ? contract.getUpdatedDttm() : contract.getCreatedDttm();
        AnalysisCacheEntry cached = analysisCache.get(contractSeq);
        if (cached != null && (lastUpdate == null || !lastUpdate.isAfter(cached.lastUpdated))) {
            log.info("Returning cached AI analysis for contractSeq={}", contractSeq);
            return cached.result;
        }

        // 분석용 텍스트 생성
        String documentContent = generateDocumentText(contract);
        
        // AI 분석 호출
        String analysisResult = aiService.analyzeContract(contract, documentContent);
        
        if (analysisResult != null) {
            analysisCache.put(contractSeq, new AnalysisCacheEntry(lastUpdate, analysisResult));
        }

        return analysisResult;
    }

    private String generateDocumentText(Contract contract) {
        StringBuilder sb = new StringBuilder();
        sb.append("금전소비대차계약서\n\n");
        sb.append(String.format("채권자: %s\n", contract.getCreditor().getEmail()));
        sb.append(String.format("채무자: %s\n", contract.getDebtor().getEmail()));
        sb.append(String.format("차용원금: 일금 %s원\n", contract.getPrincipalAmount()));
        sb.append(String.format("이자율: 연 %s%%\n", contract.getInterestRate().multiply(new BigDecimal("100"))));
        sb.append(String.format("상환방식: %s\n", contract.getRepaymentMethod()));
        sb.append(String.format("차용기간: %s ~ %s\n\n", contract.getStartDt(), contract.getEndDt()));
        
        sb.append("[특약사항]\n");
        for (int i = 0; i < contract.getClauses().size(); i++) {
            ContractClause clause = contract.getClauses().get(i);
            sb.append(String.format("제%d조 (%s): %s\n", i + 1, clause.getTitle(), clause.getContent()));
        }
        
        return sb.toString();
    }

    @Transactional
    public AiClauseChatResponseDto generateClausesFromChat(Integer contractSeq, Integer userSeq, String roomId) {
        // 1. 계약 조회 및 권한 확인
        Contract contract = contractRepository.findById(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "계약을 찾을 수 없습니다."));

        if (!contract.getCreditor().getUserSeq().equals(userSeq) && 
            !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "계약 당사자만 AI 협의를 요청할 수 있습니다.");
        }

        // 2. 채팅 내역 조회 (MongoDB)
        List<com.ssafy.a305.be.domain.chat.entity.ChatMessage> allHistory = 
                chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);

        // 타임라인 필터링: 가장 최근의 마커를 찾음 (공백/마침표 무시 위해 contains 사용)
        int startIndex = -1;
        for (int i = 0; i < allHistory.size(); i++) {
            com.ssafy.a305.be.domain.chat.entity.ChatMessage msg = allHistory.get(i);
            String content = msg.getContent() != null ? msg.getContent() : "";
            if (content.contains("AI_NEGOTIATION_START") || content.contains("AI 협의를 시작합니다")) {
                startIndex = i;
            }
        }

        List<com.ssafy.a305.be.domain.chat.entity.ChatMessage> relevantHistory = (startIndex == -1) 
                ? allHistory 
                : allHistory.subList(startIndex + 1, allHistory.size());

        // 3. AI 요청 DTO 생성 (시스템 메시지는 분석 대상에서 제외하여 소음 및 오탐 방지)
        List<AiChatMessageDto> aiMessages = relevantHistory.stream()
                .filter(msg -> "TEXT".equalsIgnoreCase(msg.getMsgType()) || "AI_CHAT".equalsIgnoreCase(msg.getMsgType()))
                .map(msg -> {
                    String role = "system";
                    if (msg.getSenderSeq() != null) {
                        if (msg.getSenderSeq().equals(contract.getCreditor().getUserSeq())) {
                            role = "creditor";
                        } else if (msg.getSenderSeq().equals(contract.getDebtor().getUserSeq())) {
                            role = "debtor";
                        }
                    }
                    return AiChatMessageDto.builder()
                            .role(role)
                            .content(msg.getContent())
                            .sender_seq(msg.getSenderSeq())
                            .build();
                })
                .collect(Collectors.toList());

        String contractContext = String.format(
                "원금: %s원, 이자율: %s%%, 상환방식: %s",
                contract.getPrincipalAmount(),
                contract.getInterestRate().multiply(new BigDecimal("100")),
                contract.getRepaymentMethod()
        );

        String templateType = Boolean.TRUE.equals(contract.getFamily()) ? "FAMILY_JEONSE" : "FRIEND_SMALL";

        AiClauseChatRequestDto requestDto = AiClauseChatRequestDto.builder()
                .chat_messages(aiMessages)
                .contract_context(contractContext)
                .template_type(templateType)
                .build();

        // 4. AI 서비스 호출
        AiClauseChatResponseDto response = aiService.generateClausesFromChat(requestDto);

        // 5. 결과가 있으면 aiDraftContent로 저장
        if (response != null && !Boolean.TRUE.equals(response.getRejected()) && response.getClauses() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                
                // 프론트엔드 ContractDocumentFullScreen.kt의 ContractUpdateRequest 파싱 규격에 맞춤
                Map<String, Object> frontendDraft = new HashMap<>();
                frontendDraft.put("principalAmount", contract.getPrincipalAmount());
                frontendDraft.put("interestRate", contract.getInterestRate());
                
                List<Map<String, Object>> clauses = new ArrayList<>();
                
                // 1. 기존 특약들을 먼저 담음 (원본 clauseSeq 유지)
                if (contract.getClauses() != null) {
                    for (ContractClause clause : contract.getClauses()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("clauseSeq", clause.getClauseSeq());
                        map.put("content", clause.getContent());
                        clauses.add(map);
                    }
                }
                
                // 2. AI가 생성한 신규 특약들을 추가 (clauseSeq = -1)
                if (response.getClauses() != null) {
                    for (AiClauseResponseDto c : response.getClauses()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("clauseSeq", -1); // 신규 특약임을 나타냄
                        map.put("content", String.format("[%s] %s", c.getTitle(), c.getLegalClause()));
                        clauses.add(map);
                    }
                }
                
                frontendDraft.put("clauses", clauses);

                String jsonResult = mapper.writeValueAsString(frontendDraft);
                contract.setAiDraftContent(jsonResult);
                contract.resetAgreement(); // AI 협의 결과 저장 시 수락 상태 초기화
                contractRepository.save(contract);
                
                log.info("AI Clauses generated and saved for contract {}: {} clauses", contractSeq, response.getClauses().size());
            } catch (Exception e) {
                log.error("Failed to serialize AI clauses result", e);
            }
        }

        return response;
    }
}
