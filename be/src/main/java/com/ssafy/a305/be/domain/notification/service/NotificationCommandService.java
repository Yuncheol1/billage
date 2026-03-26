package com.ssafy.a305.be.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.entity.FcmToken;
import com.ssafy.a305.be.domain.notification.entity.NotificationLog;
import com.ssafy.a305.be.domain.notification.repository.FcmTokenRepository;
import com.ssafy.a305.be.domain.notification.repository.NotificationLogRepository;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private static final List<Integer> REMINDER_OFFSETS = List.of(7, 3, 1, 0);
    private static final List<String> ACTIVE_LIKE_CONTRACT_STATUSES = List.of("SIGNED", "ACTIVE", "OVERDUE");
    private static final List<String> OPEN_SCHEDULE_STATUSES = List.of("NONPAID", "SOMEPAID");

    private final NotificationLogRepository notificationLogRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void sendContractSignedNotifications(Contract contract) {
        RepaymentSchedule representativeSchedule = resolveRepresentativeSchedule(contract);
        String counterpartyNameForCreditor = resolveDisplayName(contract.getDebtor());
        String counterpartyNameForDebtor = resolveDisplayName(contract.getCreditor());

        sendNotification(
                contract.getCreditor(),
                "CONTRACT_SIGNED_CREDITOR",
                "계약이 체결되었어요",
                counterpartyNameForCreditor + "님과의 대여 계약이 체결되었어요.",
                contract,
                representativeSchedule,
                Map.of(
                        "type", "CONTRACT",
                        "eventKey", "CONTRACT_SIGNED_CREDITOR",
                        "contractSeq", String.valueOf(contract.getContractSeq())
                )
        );

        sendNotification(
                contract.getDebtor(),
                "CONTRACT_SIGNED_DEBTOR",
                "계약이 체결되었어요",
                counterpartyNameForDebtor + "님에게 빌린 계약이 체결되었어요.",
                contract,
                representativeSchedule,
                Map.of(
                        "type", "CONTRACT",
                        "eventKey", "CONTRACT_SIGNED_DEBTOR",
                        "contractSeq", String.valueOf(contract.getContractSeq())
                )
        );
    }

    @Transactional
    public void sendRepaymentPaidNotifications(RepaymentSchedule schedule) {
        String amount = formatAmount(schedule.getTotalDue());
        String installmentLabel = buildInstallmentLabel(schedule);
        Contract contract = schedule.getContract();

        sendNotification(
                contract.getDebtor(),
                "REPAYMENT_PAID_DEBTOR",
                "상환이 완료되었어요",
                installmentLabel + " " + amount + " 상환이 완료되었어요.",
                contract,
                schedule,
                Map.of(
                        "type", "REPAYMENT",
                        "eventKey", "REPAYMENT_PAID_DEBTOR",
                        "contractSeq", String.valueOf(contract.getContractSeq()),
                        "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                )
        );

        sendNotification(
                contract.getCreditor(),
                "REPAYMENT_RECEIVED_CREDITOR",
                "입금이 확인되었어요",
                installmentLabel + " " + amount + " 입금이 처리되었어요.",
                contract,
                schedule,
                Map.of(
                        "type", "REPAYMENT",
                        "eventKey", "REPAYMENT_RECEIVED_CREDITOR",
                        "contractSeq", String.valueOf(contract.getContractSeq()),
                        "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                )
        );
    }

    @Transactional
    public void sendContractClosedNotifications(Contract contract) {
        RepaymentSchedule representativeSchedule = resolveRepresentativeSchedule(contract);

        sendNotification(
                contract.getCreditor(),
                "CONTRACT_CLOSED_CREDITOR",
                "계약이 완료되었어요",
                "모든 상환이 완료되어 계약이 종료되었어요.",
                contract,
                representativeSchedule,
                Map.of(
                        "type", "CONTRACT",
                        "eventKey", "CONTRACT_CLOSED_CREDITOR",
                        "contractSeq", String.valueOf(contract.getContractSeq())
                )
        );

        sendNotification(
                contract.getDebtor(),
                "CONTRACT_CLOSED_DEBTOR",
                "계약이 완료되었어요",
                "모든 상환이 완료되어 계약이 종료되었어요.",
                contract,
                representativeSchedule,
                Map.of(
                        "type", "CONTRACT",
                        "eventKey", "CONTRACT_CLOSED_DEBTOR",
                        "contractSeq", String.valueOf(contract.getContractSeq())
                )
        );
    }

    @Transactional
    public void sendDueReminderNotifications(RepaymentSchedule schedule, LocalDate today) {
        if (!ACTIVE_LIKE_CONTRACT_STATUSES.contains(schedule.getContract().getStatus())
                || !OPEN_SCHEDULE_STATUSES.contains(schedule.getStatus())) {
            return;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, schedule.getDueDt());
        if (!REMINDER_OFFSETS.contains((int) daysUntilDue)) {
            return;
        }

        String amount = formatAmount(schedule.getTotalDue());
        String installmentLabel = buildInstallmentLabel(schedule);
        String daysText = daysUntilDue == 0 ? "오늘" : daysUntilDue + "일";
        Contract contract = schedule.getContract();
        String debtorName = resolveDisplayName(contract.getDebtor());

        if (!alreadySentToday(contract.getDebtor().getUserSeq(), "REPAYMENT_DUE_REMINDER_DEBTOR", schedule.getScheduleSeq(), today)) {
            sendNotification(
                    contract.getDebtor(),
                    "REPAYMENT_DUE_REMINDER_DEBTOR",
                    "상환 일정이 다가오고 있어요",
                    daysUntilDue == 0
                            ? installmentLabel + " " + amount + " 상환일이에요. 오늘까지 상환해 주세요."
                            : "상환일까지 " + daysText + " 남았어요. " + installmentLabel + " " + amount + "을 준비해 주세요.",
                    contract,
                    schedule,
                    Map.of(
                            "type", "REPAYMENT",
                            "eventKey", "REPAYMENT_DUE_REMINDER_DEBTOR",
                            "contractSeq", String.valueOf(contract.getContractSeq()),
                            "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                    )
            );
        }

        if (!alreadySentToday(contract.getCreditor().getUserSeq(), "REPAYMENT_DUE_REMINDER_CREDITOR", schedule.getScheduleSeq(), today)) {
            sendNotification(
                    contract.getCreditor(),
                    "REPAYMENT_DUE_REMINDER_CREDITOR",
                    "상환 예정일이 다가오고 있어요",
                    daysUntilDue == 0
                            ? debtorName + "님의 " + installmentLabel + " 상환일이에요."
                            : debtorName + "님의 " + installmentLabel + " 상환 예정일까지 " + daysText + " 남았어요.",
                    contract,
                    schedule,
                    Map.of(
                            "type", "REPAYMENT",
                            "eventKey", "REPAYMENT_DUE_REMINDER_CREDITOR",
                            "contractSeq", String.valueOf(contract.getContractSeq()),
                            "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                    )
            );
        }
    }

    @Transactional
    public void sendOverdueNotifications(RepaymentSchedule schedule, LocalDate today) {
        Contract contract = schedule.getContract();
        if (!ACTIVE_LIKE_CONTRACT_STATUSES.contains(contract.getStatus())) {
            return;
        }

        boolean partiallyUnpaid = "SOMEPAID".equals(schedule.getStatus());
        String debtorEventKey = partiallyUnpaid ? "REPAYMENT_PARTIAL_OVERDUE" : "REPAYMENT_OVERDUE";
        String creditorEventKey = partiallyUnpaid ? "REPAYMENT_PARTIAL_OVERDUE_CREDITOR" : "REPAYMENT_OVERDUE_CREDITOR";
        String debtorMessage = partiallyUnpaid
                ? buildInstallmentLabel(schedule) + "가 일부 미납 상태예요. 남은 금액을 상환해 주세요."
                : buildInstallmentLabel(schedule) + "가 아직 미납 상태예요. 지금 상환해 주세요.";
        String creditorMessage = partiallyUnpaid
                ? resolveDisplayName(contract.getDebtor()) + "님의 " + buildInstallmentLabel(schedule) + "가 일부 미납 상태예요."
                : resolveDisplayName(contract.getDebtor()) + "님의 " + buildInstallmentLabel(schedule) + "가 연체 상태예요.";

        if (alreadySentToday(contract.getDebtor().getUserSeq(), debtorEventKey, schedule.getScheduleSeq(), today)) {
            return;
        }
        sendNotification(
                contract.getDebtor(),
                debtorEventKey,
                partiallyUnpaid ? "일부 미납이 발생했어요" : "연체가 발생했어요",
                debtorMessage,
                contract,
                schedule,
                Map.of(
                        "type", "REPAYMENT",
                        "eventKey", debtorEventKey,
                        "contractSeq", String.valueOf(contract.getContractSeq()),
                        "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                )
        );

        if (alreadySentToday(contract.getCreditor().getUserSeq(), creditorEventKey, schedule.getScheduleSeq(), today)) {
            return;
        }
        sendNotification(
                contract.getCreditor(),
                creditorEventKey,
                partiallyUnpaid ? "일부 미납이 발생했어요" : "연체가 발생했어요",
                creditorMessage,
                contract,
                schedule,
                Map.of(
                        "type", "REPAYMENT",
                        "eventKey", creditorEventKey,
                        "contractSeq", String.valueOf(contract.getContractSeq()),
                        "scheduleSeq", String.valueOf(schedule.getScheduleSeq())
                )
        );
    }

    private boolean alreadySentToday(Integer userSeq, String eventKey, Integer scheduleSeq, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return notificationLogRepository.countByUserUserSeqAndEventKeyAndRepaymentScheduleScheduleSeqAndSentDttmBetween(
                userSeq,
                eventKey,
                scheduleSeq,
                start,
                end
        ) > 0;
    }

    private RepaymentSchedule resolveRepresentativeSchedule(Contract contract) {
        return contract.getRepaymentSchedules().stream()
                .min(Comparator
                        .comparing(RepaymentSchedule::getDueDt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RepaymentSchedule::getInstallmentNo, Comparator.nullsLast(Integer::compareTo)))
                .orElseThrow(() -> new IllegalStateException(
                        "Contract signed notification requires at least one repayment schedule. contractSeq="
                                + contract.getContractSeq()));
    }

    private void sendNotification(
            User user,
            String eventKey,
            String title,
            String message,
            Contract contract,
            RepaymentSchedule schedule,
            Map<String, String> data
    ) {
        NotificationLog notificationLog = notificationLogRepository.save(NotificationLog.builder()
                .user(user)
                .eventKey(eventKey)
                .repaymentSchedule(schedule)
                .contract(contract)
                .title(title)
                .message(message)
                .isRead(false)
                .status("SAVED")
                .sentDttm(LocalDateTime.now())
                .build());

        List<FcmToken> tokens = fcmTokenRepository.findAllByUserUserSeqAndIsActiveTrue(user.getUserSeq());
        if (tokens.isEmpty()) {
            return;
        }

        String firstMessageId = null;
        boolean anySuccess = false;
        for (FcmToken token : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(message)
                                .build());
                data.forEach(builder::putData);
                builder.putData("notificationSeq", String.valueOf(notificationLog.getNotificationSeq()));

                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                if (firstMessageId == null) {
                    firstMessageId = messageId;
                }
                anySuccess = true;
            } catch (Exception e) {
                NotificationCommandService.log.warn("Failed to send notification. userSeq={}, eventKey={}, tokenSeq={}",
                        user.getUserSeq(), eventKey, token.getTokenSeq(), e);
            }
        }

        if (anySuccess) {
            notificationLog.markDispatchResult("SENT", firstMessageId, LocalDateTime.now());
        } else {
            notificationLog.markDispatchResult("FAILED", null, LocalDateTime.now());
        }
    }

    private String resolveDisplayName(User user) {
        return userProfileRepository.findByUserUserSeq(user.getUserSeq())
                .map(profile -> profile.getRealName())
                .orElse("상대방");
    }

    private String buildInstallmentLabel(RepaymentSchedule schedule) {
        Integer installmentNo = schedule.getInstallmentNo();
        if (installmentNo == null) {
            return "상환 일정";
        }
        return installmentNo + "회차";
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0원";
        }
        return String.format("%,d원", amount.longValue());
    }
}
