package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            join fetch c.creditor
            join fetch c.debtor
            where rs.dueDt in :dueDates
              and rs.status in :scheduleStatuses
              and c.status in :contractStatuses
            """)
    List<RepaymentSchedule> findReminderTargets(
            @Param("dueDates") Collection<LocalDate> dueDates,
            @Param("scheduleStatuses") Collection<String> scheduleStatuses,
            @Param("contractStatuses") Collection<String> contractStatuses
    );

    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            join fetch c.creditor
            join fetch c.debtor
            where rs.dueDt < :today
              and rs.status in :scheduleStatuses
              and c.status in :contractStatuses
            """)
    List<RepaymentSchedule> findOverdueTargets(
            @Param("today") LocalDate today,
            @Param("scheduleStatuses") Collection<String> scheduleStatuses,
            @Param("contractStatuses") Collection<String> contractStatuses
    );
    List<RepaymentSchedule> findAllByContractContractSeqOrderByInstallmentNoAsc(Integer contractSeq);


    @Query("""
            SELECT rs FROM RepaymentSchedule rs 
            JOIN FETCH rs.contract c 
            WHERE (c.creditor.userSeq = :userSeq OR c.debtor.userSeq = :userSeq) 
               AND c.status IN ('SIGNED','ACTIVE', 'OVERDUE') 
              AND rs.status IN (:statuses) 
              AND rs.dueDt >= :today 
            ORDER BY rs.dueDt ASC
            """)
    List<RepaymentSchedule> findUpcomingSchedules(
            @Param("userSeq") Integer userSeq,
            @Param("today") LocalDate today,
            @Param("statuses") List<String> statuses,
            Pageable pageable);

    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            join fetch c.creditor
            join fetch c.debtor
            where rs.scheduleSeq = :scheduleSeq
              and c.contractSeq = :contractSeq
            """)
    Optional<RepaymentSchedule> findByScheduleSeqAndContractSeqWithContractUsers(
            @Param("scheduleSeq") Integer scheduleSeq,
            @Param("contractSeq") Integer contractSeq
    );

    /**
     * 자동이체 대상 회차의 식별자 목록.
     *
     * <p>엔티티가 아닌 식별자만 조회한다. 실제 처리는 건별 트랜잭션 안에서
     * {@link #findByScheduleSeqWithContractUsers(Integer)}로 다시 읽으므로,
     * 목록 조회 시점과 처리 시점 사이에 상태가 바뀌어도 최신 값으로 판단할 수 있다.</p>
     */
    @Query("""
            select rs.scheduleSeq
            from RepaymentSchedule rs
            join rs.contract c
            where rs.dueDt <= :today
              and rs.status = 'NONPAID'
              and c.status in ('SIGNED', 'ACTIVE', 'OVERDUE')
            order by rs.scheduleSeq asc
            """)
    List<Integer> findDueScheduleSeqs(@Param("today") LocalDate today);

    /** 자동이체 처리에 필요한 연관(계약·채무자·채권자)을 함께 읽는다. */
    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            join fetch c.debtor
            join fetch c.creditor
            where rs.scheduleSeq = :scheduleSeq
            """)
    Optional<RepaymentSchedule> findByScheduleSeqWithContractUsers(@Param("scheduleSeq") Integer scheduleSeq);
}
