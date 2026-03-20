package com.ssafy.a305.be.domain.contract.repository;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer>, ContractRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.contractSeq = :contractSeq")
    Optional<Contract> findByIdWithPessimisticLock(@Param("contractSeq") Integer contractSeq);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Contract c
            WHERE (c.creditor.userSeq = :userSeq OR c.debtor.userSeq = :userSeq)
              AND c.status IN :statuses
            """)
    boolean existsByUserSeqAndStatuses(@Param("userSeq") Integer userSeq, @Param("statuses") Collection<String> statuses);

}
