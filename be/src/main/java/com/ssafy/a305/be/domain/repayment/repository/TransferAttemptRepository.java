package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.TransferAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 이체 시도 원장 Repository.
 */
public interface TransferAttemptRepository extends JpaRepository<TransferAttempt, Long> {

    /** 결과가 확정되지 않은 시도가 있는지. 있으면 그 회차의 새 이체를 시작하면 안 된다. */
    boolean existsByScheduleSeqAndStatus(Integer scheduleSeq, TransferAttempt.Status status);

    /** 회차의 마지막 시도 번호. 시도가 없으면 0. */
    @Query("select coalesce(max(a.attemptNo), 0) from TransferAttempt a where a.scheduleSeq = :scheduleSeq")
    int findLastAttemptNo(@Param("scheduleSeq") Integer scheduleSeq);
}
