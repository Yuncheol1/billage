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

    // 자동이체 대상. 처리할 때 건별로 다시 읽으므로 식별자만 가져온다.
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

    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            join fetch c.debtor
            join fetch c.creditor
            where rs.scheduleSeq = :scheduleSeq
            """)
    Optional<RepaymentSchedule> findByScheduleSeqWithContractUsers(@Param("scheduleSeq") Integer scheduleSeq);

    // 이체 전 선점. 1이면 성공, 0이면 다른 요청이 먼저 바꿈
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

    // 돈이 나가지 않은 게 확실할 때만 호출
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

    // 정산 대상
    @Query("""
            select rs.scheduleSeq
            from RepaymentSchedule rs
            where rs.status = 'REQUESTED'
              and rs.transferRequestedDttm <= :requestedBefore
            order by rs.scheduleSeq asc
            """)
    List<Integer> findStaleRequestedScheduleSeqs(@Param("requestedBefore") LocalDateTime requestedBefore);

    // 이체 결과 반영과 정산이 동시에 확정해도 한쪽만 통과
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RepaymentSchedule rs
            set rs.status = 'PAID',
                rs.paidDttm = :paidAt
            where rs.scheduleSeq = :scheduleSeq
              and rs.status = 'REQUESTED'
            """)
    int markPaidIfRequested(@Param("scheduleSeq") Integer scheduleSeq, @Param("paidAt") LocalDateTime paidAt);

    // 채권자 수동 완료. 이체 중(REQUESTED)인 회차를 덮어쓰지 않게 조건부로 바꾼다.
    @Modifying(flushAutomatically = true)
    @Query("""
            update RepaymentSchedule rs
            set rs.status = 'MANUAL_PAID',
                rs.paidDttm = :paidAt
            where rs.scheduleSeq = :scheduleSeq
              and rs.status in ('NONPAID', 'OVERDUE')
            """)
    int markManualPaidIfUnpaid(@Param("scheduleSeq") Integer scheduleSeq, @Param("paidAt") LocalDateTime paidAt);

    // 연체 전환. 이체 중(REQUESTED)인 회차를 OVERDUE로 덮어쓰지 않게 조건부로 바꾼다.
    @Modifying(flushAutomatically = true)
    @Query("""
            update RepaymentSchedule rs
            set rs.status = 'OVERDUE'
            where rs.scheduleSeq = :scheduleSeq
              and rs.status = 'NONPAID'
            """)
    int markOverdueIfNonpaid(@Param("scheduleSeq") Integer scheduleSeq);

    // 운영자 확인 목록용
    @Query("""
            select rs
            from RepaymentSchedule rs
            join fetch rs.contract c
            where rs.status = 'REQUESTED'
              and rs.transferRequestedDttm <= :requestedBefore
            order by rs.transferRequestedDttm asc
            """)
    List<RepaymentSchedule> findStaleRequestedWithContract(@Param("requestedBefore") LocalDateTime requestedBefore);

    // 운영 지표용
    long countByStatusAndTransferRequestedDttmLessThanEqual(String status, LocalDateTime requestedBefore);
}
