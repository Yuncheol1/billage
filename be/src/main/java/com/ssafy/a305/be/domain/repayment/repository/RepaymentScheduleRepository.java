package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /**
     * 이체를 시작하기 전에 회차를 REQUESTED로 선점한다.
     *
     * <p>조회한 상태가 그대로일 때만 바뀌므로, 같은 회차에 요청이 동시에 들어와도 DB가 한 건만
     * 통과시킨다. 반환값이 1이면 선점에 성공한 것이고 0이면 다른 요청이 먼저 상태를 바꾼 것이다.</p>
     *
     * <p>벌크 UPDATE는 영속성 컨텍스트를 거치지 않으므로, 이미 읽어 둔 엔티티가 옛 상태를 들고
     * 남지 않도록 실행 후 컨텍스트를 비운다.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RepaymentSchedule rs
            set rs.status = 'REQUESTED',
                rs.transferRequestedDttm = :requestedAt
            where rs.scheduleSeq = :scheduleSeq
              and rs.status = :expectedStatus
            """)
    int claimForTransfer(
            @Param("scheduleSeq") Integer scheduleSeq,
            @Param("expectedStatus") String expectedStatus,
            @Param("requestedAt") LocalDateTime requestedAt
    );

    /** REQUESTED였던 회차를 이체 전 상태로 되돌린다. 돈이 나가지 않았음이 확실할 때만 호출한다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RepaymentSchedule rs
            set rs.status = :restoreStatus
            where rs.scheduleSeq = :scheduleSeq
              and rs.status = 'REQUESTED'
            """)
    int releaseTransferClaim(
            @Param("scheduleSeq") Integer scheduleSeq,
            @Param("restoreStatus") String restoreStatus
    );

    /** 요청한 지 충분히 지났는데도 결과가 확정되지 않은 회차. 정산 대상이다. */
    @Query("""
            select rs.scheduleSeq
            from RepaymentSchedule rs
            where rs.status = 'REQUESTED'
              and rs.transferRequestedDttm <= :requestedBefore
            order by rs.scheduleSeq asc
            """)
    List<Integer> findStaleRequestedScheduleSeqs(@Param("requestedBefore") LocalDateTime requestedBefore);
}
