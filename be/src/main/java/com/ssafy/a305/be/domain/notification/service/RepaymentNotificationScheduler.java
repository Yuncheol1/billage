package com.ssafy.a305.be.domain.notification.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentNotificationScheduler {

    private static final List<String> REMINDER_SCHEDULE_STATUSES = List.of("NONPAID");
    private static final List<String> OVERDUE_SCHEDULE_STATUSES = List.of("NONPAID", "SOMEPAID", "OVERDUE");
    private static final List<String> TARGET_CONTRACT_STATUSES = List.of("SIGNED", "ACTIVE", "OVERDUE");

    private final RepaymentScheduleRepository repaymentScheduleRepository;
    private final NotificationCommandService notificationCommandService;

    @Transactional
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDueReminders() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dueDates = List.of(
                today,
                today.plusDays(1),
                today.plusDays(3),
                today.plusDays(7)
        );

        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findReminderTargets(
                dueDates,
                REMINDER_SCHEDULE_STATUSES,
                TARGET_CONTRACT_STATUSES
        );

        schedules.forEach(schedule -> notificationCommandService.sendDueReminderNotifications(schedule, today));
        log.info("[알림 스케줄러] 상환예정 알림 처리 완료 - targetCount={}", schedules.size());
    }

    @Transactional
    @Scheduled(cron = "0 30 9 * * *")
    public void sendOverdueNotifications() {
        LocalDate today = LocalDate.now();
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findOverdueTargets(
                today,
                OVERDUE_SCHEDULE_STATUSES,
                TARGET_CONTRACT_STATUSES
        );

        schedules.forEach(schedule -> {
            if ("NONPAID".equals(schedule.getStatus())) {
                schedule.markOverdue();
            }
            notificationCommandService.sendOverdueNotifications(schedule, today);
        });
        log.info("[알림 스케줄러] 연체 알림 처리 완료 - targetCount={}", schedules.size());
    }
}
