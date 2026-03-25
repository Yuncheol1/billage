package com.ssafy.a305.be.domain.notification.repository;

import com.ssafy.a305.be.domain.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserUserSeqOrderBySentDttmDesc(Integer userSeq);
    long countByUserUserSeqAndIsReadFalse(Integer userSeq);
    long countByUserUserSeqAndEventKeyAndRepaymentScheduleScheduleSeqAndSentDttmBetween(
            Integer userSeq,
            String eventKey,
            Integer scheduleSeq,
            LocalDateTime start,
            LocalDateTime end
    );
}
