package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.data.remote.api.RepaymentScheduleItem
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@Composable
internal fun ContractTimelineSection(
    schedules: List<RepaymentScheduleItem>,
    isLoading: Boolean,
    errorMessage: String?,
    myRole: String?,
    activeActionScheduleSeq: Int?,
    onManualRepaymentClick: (Int) -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BillagePrimary)
            }
        }

        errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage,
                    color = BillageDanger,
                    fontSize = 14.sp
                )
            }
        }

        schedules.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "표시할 상환 일정이 없습니다.",
                    color = BillageTextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "회차별 상환 흐름",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary
                )

                schedules.forEachIndexed { index, schedule ->
                    TimelineScheduleCard(
                        schedule = schedule,
                        myRole = myRole,
                        activeActionScheduleSeq = activeActionScheduleSeq,
                        onManualRepaymentClick = onManualRepaymentClick,
                        isLast = index == schedules.lastIndex
                    )
                    if (index < schedules.lastIndex) {
                        HorizontalDivider(color = BillageDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineScheduleCard(
    schedule: RepaymentScheduleItem,
    myRole: String?,
    activeActionScheduleSeq: Int?,
    onManualRepaymentClick: (Int) -> Unit,
    isLast: Boolean
) {
    val statusColor = repaymentStatusColor(schedule.status)
    val isActionable = schedule.status in setOf("NONPAID", "SOMEPAID", "OVERDUE")
    val isProcessing = activeActionScheduleSeq == schedule.scheduleSeq

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
            if (!isLast) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(100.dp)
                        .background(BillageCardBorder)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${schedule.installmentNo}회차",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                    Text(
                        text = "예정일 ${formatDate(schedule.dueDt)}",
                        fontSize = 13.sp,
                        color = BillageTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(BillagePrimaryLight, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = repaymentStatusLabel(schedule.status),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            ScheduleAmounts(schedule = schedule)

            if (!schedule.paidDttm.isNullOrBlank()) {
                Text(
                    text = "납부일 ${formatDateTime(schedule.paidDttm)}",
                    fontSize = 12.sp,
                    color = BillageTextSecondary
                )
            }

            if (isActionable) {
                RepaymentActionButtons(
                    myRole = myRole,
                    isProcessing = isProcessing,
                    scheduleSeq = schedule.scheduleSeq,
                    onManualRepaymentClick = onManualRepaymentClick
                )
            }
        }
    }
}

@Composable
private fun RepaymentActionButtons(
    myRole: String?,
    isProcessing: Boolean,
    scheduleSeq: Int,
    onManualRepaymentClick: (Int) -> Unit
) {
    when (myRole) {
        "CREDITOR" -> {
            Button(
                onClick = { onManualRepaymentClick(scheduleSeq) },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
            ) {
                Text(if (isProcessing) "처리 중..." else "개별 상환처리")
            }
        }

        "DEBTOR" -> {
            Button(
                onClick = { onManualRepaymentClick(scheduleSeq) },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
            ) {
                Text(if (isProcessing) "처리 중..." else "바로 상환하기")
            }
        }
    }
}

@Composable
private fun ScheduleAmounts(schedule: RepaymentScheduleItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BillagePrimaryLight, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScheduleAmountRow(label = "총 납부액", value = formatCurrency(schedule.totalDue))
        ScheduleAmountRow(label = "원금", value = formatCurrency(schedule.principalDue))
        ScheduleAmountRow(label = "이자", value = formatCurrency(schedule.interestDue))
        ScheduleAmountRow(label = "납부 후 잔액", value = formatCurrency(schedule.remainingBalanceAfter))
    }
}

@Composable
private fun ScheduleAmountRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = BillageTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )
    }
}
