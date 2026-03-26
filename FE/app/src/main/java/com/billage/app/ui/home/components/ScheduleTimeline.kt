package com.billage.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.home.ScheduleItem
import com.billage.app.ui.theme.*

@Composable
fun ScheduleTimeline(schedules: List<ScheduleItem>) {
    if (schedules.isEmpty()) {
        Text(
            text = "다가오는 일정이 없습니다.",
            color = BillageTextSecondary,
            fontSize = 14.sp
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        schedules.forEachIndexed { index, schedule ->
            ScheduleTimelineItem(
                schedule = schedule,
                isFirst = index == 0,
                isLast = index == schedules.lastIndex
            )
        }
    }
}

@Composable
fun ScheduleTimelineItem(
    schedule: ScheduleItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    val isReceiving = schedule.type == "받을"
    val dotColor = if (isReceiving) BillagePrimary else BillageWarning
    val badgeColor = if (isReceiving) BillagePrimaryLight else BillageWarningBackground
    val badgeTextColor = if (isReceiving) BillagePrimary else BillageWarning
    val badgeLabel = if (isReceiving) "받는 날" else "갚는 날"

    val message = if (isReceiving) {
        "${schedule.name}님께 ${schedule.amount} 받을 날이에요"
    } else {
        "${schedule.name}님께 ${schedule.amount} 드릴 날이에요"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(2.dp)
                    .fillMaxHeight()
                    .then(
                        if (isFirst) Modifier.padding(top = 14.dp)
                        else Modifier
                    )
                    .background(BillagePrimaryLight)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 9.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 10.dp,
                    bottom = if (isLast) 4.dp else 24.dp
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = schedule.date,
                    color = BillageTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = BillageTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
