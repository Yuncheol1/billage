package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleChangeReason;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentScheduleHistory;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleHistoryRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RepaymentScheduleHistoryRecorderTest {

    @Mock
    private RepaymentScheduleHistoryRepository historyRepository;
    @Mock
    private RepaymentScheduleRepository repaymentScheduleRepository;
    @Mock
    private UserRepository userRepository;

    private RepaymentScheduleHistoryRecorder recorder;
    private RepaymentSchedule schedule;

    @BeforeEach
    void setUp() {
        recorder = new RepaymentScheduleHistoryRecorder(historyRepository, repaymentScheduleRepository, userRepository);
        schedule = RepaymentSchedule.builder().scheduleSeq(1).status("REQUESTED").build();
        given(repaymentScheduleRepository.getReferenceById(1)).willReturn(schedule);
    }

    private RepaymentScheduleHistory saved() {
        ArgumentCaptor<RepaymentScheduleHistory> captor = ArgumentCaptor.forClass(RepaymentScheduleHistory.class);
        then(historyRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("이전·이후 상태, 이유, 바꾼 사용자, 상세를 한 줄로 남긴다")
    void record_shouldSaveAllFields() {
        User debtor = User.builder().userSeq(7).build();
        given(userRepository.getReferenceById(7)).willReturn(debtor);

        recorder.record(1, "REQUESTED", "NONPAID", RepaymentScheduleChangeReason.TRANSFER_REJECTED, 7,
                "SSAFY_TRANSFER_FAILED|A1014|잔액 부족");

        RepaymentScheduleHistory history = saved();
        assertThat(history.getRepaymentSchedule()).isSameAs(schedule);
        assertThat(history.getFromStatus()).isEqualTo("REQUESTED");
        assertThat(history.getToStatus()).isEqualTo("NONPAID");
        assertThat(history.getReason()).isEqualTo(RepaymentScheduleChangeReason.TRANSFER_REJECTED);
        assertThat(history.getChangedByUser()).isSameAs(debtor);
        assertThat(history.getDetail()).isEqualTo("SSAFY_TRANSFER_FAILED|A1014|잔액 부족");
        assertThat(history.getCreatedDttm()).isNotNull();
    }

    @Test
    @DisplayName("스케줄러·정산처럼 사용자가 없는 경로면 바꾼 사용자를 비워 둔다")
    void record_withoutUser_shouldLeaveUserEmpty() {
        recorder.record(1, "NONPAID", "OVERDUE", RepaymentScheduleChangeReason.OVERDUE, null, null);

        assertThat(saved().getChangedByUser()).isNull();
        then(userRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상세가 칸 길이를 넘으면 잘라서 남긴다")
    void record_longDetail_shouldBeTruncated() {
        String longDetail = "x".repeat(RepaymentScheduleHistoryRecorder.DETAIL_MAX_LENGTH + 100);

        recorder.record(1, "REQUESTED", "REQUESTED", RepaymentScheduleChangeReason.TRANSFER_OUTCOME_UNKNOWN,
                null, longDetail);

        assertThat(saved().getDetail()).hasSize(RepaymentScheduleHistoryRecorder.DETAIL_MAX_LENGTH);
    }
}
