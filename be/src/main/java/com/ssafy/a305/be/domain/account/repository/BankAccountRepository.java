package com.ssafy.a305.be.domain.account.repository;

import com.ssafy.a305.be.domain.account.entity.BankAccount;
import com.ssafy.a305.be.domain.enums.BankAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {
    boolean existsByUserUserSeqAndStatus(Integer userSeq, BankAccountStatus status);
    boolean existsByAccountNo(String accountNo);
}
