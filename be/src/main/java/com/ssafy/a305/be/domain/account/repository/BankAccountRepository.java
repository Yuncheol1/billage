package com.ssafy.a305.be.domain.account.repository;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.enums.BankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {
    boolean existsByUserUserSeqAndStatus(Integer userSeq, BankAccountStatus status);
    boolean existsByAccountNo(String accountNo);

    List<BankAccount> findByUserUserSeq(Integer userSeq);

    Optional<BankAccount> findFirstByUserUserSeqAndStatusOrderByAccountSeqAsc(
            Integer userSeq,
            BankAccountStatus status
    );

    Optional<BankAccount> findByAccountNo(String accountNo);

    boolean existsByUserUserSeqAndAccountTypeUniqueNoAndStatus(
            Integer userSeq,
            String accountTypeUniqueNo,
            BankAccountStatus status
    );
}
