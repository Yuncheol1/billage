package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회차 상태 변경 이력 Repository.
 */
public interface RepaymentScheduleHistoryRepository extends JpaRepository<RepaymentScheduleHistory, Long> {
}
