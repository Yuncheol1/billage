package com.ssafy.a305.be.domain.contract.repository;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {

    @Query("SELECT DISTINCT c FROM Contract c " +
            "JOIN FETCH c.creditor cr LEFT JOIN FETCH cr.profile " +
            "JOIN FETCH c.debtor db LEFT JOIN FETCH db.profile " +
            "LEFT JOIN FETCH c.repaymentSchedules " +
            "WHERE cr.userSeq = :userSeq OR db.userSeq = :userSeq")
    List<Contract> findAllByUser(@Param("userSeq") Integer userSeq);
}
