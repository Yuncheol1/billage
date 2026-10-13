package com.ssafy.a305.be.domain.repayment.controller;

import com.ssafy.a305.be.domain.repayment.dto.request.TransferReviewRequestDto;
import com.ssafy.a305.be.domain.repayment.service.TransferReconciliationService;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 운영자 이체 확인 API의 인증 테스트.
 *
 * <p>회차를 PAID나 미납으로 바꿀 수 있는 API이므로, 키가 없거나 틀리면 서비스까지 가지 않아야 한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class TransferReviewControllerTest {

    private static final String KEY = "ops-test-key";

    @Mock
    private TransferReconciliationService transferReconciliationService;

    @Test
    @DisplayName("키가 맞으면 서비스에 위임한다")
    void validKey_shouldDelegate() {
        TransferReviewController controller = new TransferReviewController(transferReconciliationService, KEY);
        given(transferReconciliationService.resolveByReview(anyInt(), anyBoolean(), anyString(), any(LocalDateTime.class)))
                .willReturn(TransferReconciliationService.Result.RELEASED);

        assertThat(controller.review(KEY, 12, new TransferReviewRequestDto(false, "출금 없음"))
                .getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("키가 틀리거나 없으면 거절하고 서비스를 호출하지 않는다")
    void wrongOrMissingKey_shouldReject() {
        TransferReviewController controller = new TransferReviewController(transferReconciliationService, KEY);

        assertThatThrownBy(() -> controller.review("wrong", 12, new TransferReviewRequestDto(true, "확인")))
                .isInstanceOf(CustomBusinessException.class);
        assertThatThrownBy(() -> controller.findUnresolved(null))
                .isInstanceOf(CustomBusinessException.class);
        then(transferReconciliationService).should(never()).resolveByReview(anyInt(), anyBoolean(), anyString(), any());
        then(transferReconciliationService).should(never()).findUnresolved(any());
    }

    @Test
    @DisplayName("서버에 키가 설정되지 않았으면 빈 키를 보내도 거절한다")
    void unconfiguredKey_shouldRejectEverything() {
        TransferReviewController controller = new TransferReviewController(transferReconciliationService, "");

        assertThatThrownBy(() -> controller.findUnresolved(""))
                .isInstanceOf(CustomBusinessException.class);
        then(transferReconciliationService).should(never()).findUnresolved(any());
    }

    @Test
    @DisplayName("키가 맞으면 미확정 목록을 돌려준다")
    void validKey_shouldListUnresolved() {
        TransferReviewController controller = new TransferReviewController(transferReconciliationService, KEY);
        given(transferReconciliationService.findUnresolved(any())).willReturn(List.of());

        assertThat(controller.findUnresolved(KEY).getStatusCode().is2xxSuccessful()).isTrue();
    }
}
