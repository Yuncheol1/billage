package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 자동이체 등록 정보 조회 Repository.
 */
public interface AutoTransferRegistrationRepository extends JpaRepository<AutoTransferRegistration, Integer> {

    Optional<AutoTransferRegistration> findByContract_ContractSeqAndIsActiveTrue(Integer contractSeq);

    /** 배치용: 계좌 정보까지 한 번에 Fetch Join으로 조회 (N+1 방지) */
    @Query("SELECT r FROM AutoTransferRegistration r " +
           "JOIN FETCH r.debtorAccount " +
           "JOIN FETCH r.creditorAccount " +
           "WHERE r.contract.contractSeq = :contractSeq AND r.isActive = true")
    Optional<AutoTransferRegistration> findActiveWithAccounts(@Param("contractSeq") Integer contractSeq);
}
