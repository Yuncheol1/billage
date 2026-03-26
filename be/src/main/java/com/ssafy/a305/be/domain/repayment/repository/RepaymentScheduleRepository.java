package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Integer> {

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
}
