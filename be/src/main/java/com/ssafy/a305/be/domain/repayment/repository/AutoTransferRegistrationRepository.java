package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.AutoTransferRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 자동이체 등록 정보 조회 Repository.
 */
public interface AutoTransferRegistrationRepository extends JpaRepository<AutoTransferRegistration, Integer> {

    Optional<AutoTransferRegistration> findByContract_ContractSeqAndIsActiveTrue(Integer contractSeq);
}
