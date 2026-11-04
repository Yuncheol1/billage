package com.ssafy.a305.be.domain.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.a305.be.domain.contract.dto.request.AiClauseRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.AiClauseResponseDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 특약 추가. AI 응답을 기다리는 동안 계약 행 락과 트랜잭션을 잡지 않는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceAiClauseTest {

    private static final int CONTRACT_SEQ = 10;
    private static final int CREDITOR = 1;
    private static final int DEBTOR = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActiveFlagTransactionManager txManager = new ActiveFlagTransactionManager();

    @Spy
    private TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ContractService contractService;

    @Test
    @DisplayName("AI는 트랜잭션 밖에서 호출하고, 락은 AI 응답을 받은 뒤에 잡는다")
    void callsAiOutsideTransactionAndLocksAfter() {
        Contract contract = contract("DRAFT");
        when(contractRepository.findById(CONTRACT_SEQ)).thenReturn(Optional.of(contract));
        when(contractRepository.findByIdWithPessimisticLock(CONTRACT_SEQ)).thenReturn(Optional.of(contract));
        when(aiService.generateClause(any(AiClauseRequestDto.class))).thenAnswer(inv -> {
            assertThat(txManager.active).as("AI 호출 중 트랜잭션").isFalse();
            return aiResponse("{\"rejected\":false,\"title\":\"지연 이자\",\"legal_clause\":\"연체 시 연 5%를 가산한다.\"}");
        });

        ContractClause clause = contractService.addAiClauses(CONTRACT_SEQ, "늦으면 이자 더 받기", CREDITOR);

        InOrder order = inOrder(contractRepository, aiService);
        order.verify(contractRepository).findById(CONTRACT_SEQ);
        order.verify(aiService).generateClause(any(AiClauseRequestDto.class));
        order.verify(contractRepository).findByIdWithPessimisticLock(CONTRACT_SEQ);

        assertThat(clause.getOrderNo()).isEqualTo(1);
        assertThat(clause.getTitle()).isEqualTo("지연 이자");
        assertThat(contract.getClauses()).containsExactly(clause);
    }

    @Test
    @DisplayName("AI가 거절하면 락을 잡지 않고 저장하지 않는다")
    void rejectedByAi_doesNotLockOrSave() {
        Contract contract = contract("DRAFT");
        when(contractRepository.findById(CONTRACT_SEQ)).thenReturn(Optional.of(contract));
        when(aiService.generateClause(any(AiClauseRequestDto.class)))
                .thenReturn(aiResponse("{\"rejected\":true,\"rejection_reason\":\"법률 특약으로 만들 수 없는 내용\"}"));

        assertThatThrownBy(() -> contractService.addAiClauses(CONTRACT_SEQ, "아무 말", DEBTOR))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessageContaining("법률 특약으로 만들 수 없는 내용");

        verify(contractRepository, never()).findByIdWithPessimisticLock(any());
        assertThat(contract.getClauses()).isEmpty();
    }

    @Test
    @DisplayName("AI를 기다리는 사이 계약이 초안을 벗어나면 저장하지 않는다")
    void statusChangedDuringAiCall_doesNotSave() {
        Contract beforeAi = contract("DRAFT");
        Contract afterAi = contract("NEGOTIATING");
        when(contractRepository.findById(CONTRACT_SEQ)).thenReturn(Optional.of(beforeAi));
        when(contractRepository.findByIdWithPessimisticLock(CONTRACT_SEQ)).thenReturn(Optional.of(afterAi));
        when(aiService.generateClause(any(AiClauseRequestDto.class)))
                .thenReturn(aiResponse("{\"rejected\":false,\"legal_clause\":\"특약 문구\"}"));

        assertThatThrownBy(() -> contractService.addAiClauses(CONTRACT_SEQ, "특약", CREDITOR))
                .isInstanceOf(CustomBusinessException.class);

        assertThat(afterAi.getClauses()).isEmpty();
    }

    @Test
    @DisplayName("계약 당사자가 아니면 AI를 호출하지 않는다")
    void notParty_doesNotCallAi() {
        when(contractRepository.findById(CONTRACT_SEQ)).thenReturn(Optional.of(contract("DRAFT")));

        assertThatThrownBy(() -> contractService.addAiClauses(CONTRACT_SEQ, "특약", 99))
                .isInstanceOf(CustomBusinessException.class);

        verify(aiService, never()).generateClause(any());
    }

    private Contract contract(String status) {
        return Contract.builder()
                .contractSeq(CONTRACT_SEQ)
                .creditor(User.builder().userSeq(CREDITOR).build())
                .debtor(User.builder().userSeq(DEBTOR).build())
                .principalAmount(new BigDecimal("1000000"))
                .interestRate(new BigDecimal("0.05"))
                .repaymentMethod("MONTHLY")
                .status(status)
                .build();
    }

    private AiClauseResponseDto aiResponse(String json) {
        try {
            return objectMapper.readValue(json, AiClauseResponseDto.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 트랜잭션이 열려 있는지만 기록하는 가짜 트랜잭션 매니저
    static class ActiveFlagTransactionManager extends AbstractPlatformTransactionManager {
        boolean active;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            active = false;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            active = false;
        }
    }
}
