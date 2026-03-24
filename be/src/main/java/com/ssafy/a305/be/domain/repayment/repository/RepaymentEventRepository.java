package com.ssafy.a305.be.domain.repayment.repository;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상환 이벤트 저장 Repository.
 */
public interface RepaymentEventRepository extends JpaRepository<RepaymentEvent, Long> {
}
