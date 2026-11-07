package com.ssafy.a305.be.domain.notification.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.repayment.service.RepaymentScheduleHistoryRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 연체 전환 테스트. 이체 중(REQUESTED)인 회차를 연체로 덮어쓰지 않는지 확인
 */
@ExtendWith(MockitoExtension.class)
class RepaymentNotificationSchedulerTest {

    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private RepaymentScheduleHistoryRecorder historyRecorder;

    @InjectMocks
    private RepaymentNotificationScheduler scheduler;

    @Test
    @DisplayName("미납 회차는 조건부 UPDATE로 연체가 되고 연체 알림이 나간다")
    void nonpaid_shouldBecomeOverdue() {
        RepaymentSchedule schedule = schedule(1);
        given(repaymentScheduleRepository.findOverdueTargets(any(), anyCollection(), anyCollection()))
                .willReturn(List.of(schedule));
        given(repaymentScheduleRepository.markOverdueIfNonpaid(1)).willReturn(1);

        scheduler.sendOverdueNotifications();

        assertThat(schedule.getStatus()).isEqualTo("OVERDUE");
        then(notificationCommandService).should().sendOverdueNotifications(any(), any(LocalDate.class));
        then(historyRecorder).should().record(1, "NONPAID", "OVERDUE",
                RepaymentScheduleChangeReason.OVERDUE, null, null);
    }

    @Test
    @DisplayName("조회 뒤 이체가 선점한 회차는 연체로 덮어쓰지 않고 알림도 보내지 않는다")
    void claimedAfterRead_shouldNotBeOverwritten() {
        RepaymentSchedule schedule = schedule(1);
        given(repaymentScheduleRepository.findOverdueTargets(any(), anyCollection(), anyCollection()))
                .willReturn(List.of(schedule));
        given(repaymentScheduleRepository.markOverdueIfNonpaid(1)).willReturn(0);

        scheduler.sendOverdueNotifications();

        assertThat(schedule.getStatus())
                .as("엔티티를 바꾸면 커밋 시 REQUESTED를 덮어쓴다")
                .isEqualTo("NONPAID");
        then(notificationCommandService).should(never()).sendOverdueNotifications(any(), any());
        then(historyRecorder).shouldHaveNoInteractions();
    }

    private RepaymentSchedule schedule(int seq) {
        return RepaymentSchedule.builder()
                .scheduleSeq(seq)
                .dueDt(LocalDate.now().minusDays(1))
                .status("NONPAID")
                .build();
    }
}
