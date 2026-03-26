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
              AND c.status = 'ACTIVE' 
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
}
