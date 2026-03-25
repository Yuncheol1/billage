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

    /** 유저의 주 계좌(Primary Account) 조회 */
    Optional<BankAccount> findByUserUserSeqAndIsPrimaryTrue(Integer userSeq);

    List<BankAccount> findByUserUserSeq(Integer userSeq);

    boolean existsByUserUserSeqAndAccountTypeUniqueNoAndStatus(
            Integer userSeq,
            String accountTypeUniqueNo,
            BankAccountStatus status
    );

}
