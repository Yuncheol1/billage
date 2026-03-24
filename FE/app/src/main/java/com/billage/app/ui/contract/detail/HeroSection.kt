package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarning

@Composable
internal fun HeroSection(
    status: String?,
    counterparty: String,
    dday: String? = null
) {
    val accentColor = when {
        status == "DRAFT" || status == "NEGOTIATING" -> BillageWarning
        status == "ACTIVE" && dday == "만기 초과"    -> BillageDanger
        status == "ACTIVE" && dday == "오늘 만기"    -> BillageWarning
        status == "ACTIVE"                           -> BillagePrimary
        status == "COMPLETED"                        -> BillageSuccess
        status == "CANCELLED"                        -> BillageDanger
        else                                         -> BillagePrimary
    }

    val badgeLabel = when {
        status == "DRAFT" || status == "NEGOTIATING" -> "서명 대기"
        status == "ACTIVE" && dday == "만기 초과"    -> "만기 초과"
        status == "ACTIVE" && dday == "오늘 만기"    -> "오늘 만기"
        status == "ACTIVE"                           -> "계약 중"
        status == "COMPLETED"                        -> "완료"
        status == "CANCELLED"                        -> "취소됨"
        else                                         -> "계약"
    }

    val mainText = when (status) {
        "DRAFT", "NEGOTIATING" -> "서로의 서명을 기다리고 있어요"
        "ACTIVE" -> when (dday) {
            "만기 초과" -> "만기가 초과된\n계약이에요"
            "오늘 만기" -> "오늘이 만기일이에요"
            null        -> "든든한 계약이\n진행 중이에요"
            else        -> "${dday}일 남았어요"
        }
        "COMPLETED" -> "성공적으로\n마무리된 계약이에요"
        "CANCELLED" -> "취소된 계약이에요"
        else        -> "계약이 진행 중이에요"
    }

    val subText = when (status) {
        "DRAFT", "NEGOTIATING" -> "두 분의 서명이 완료되면 계약이 시작돼요"
        "ACTIVE" -> when (dday) {
            "만기 초과" -> "만기일이 지났어요. 상대방과 확인해 보세요"
            "오늘 만기" -> "오늘이 마지막 상환일이에요"
            else        -> "약속한 날짜에 상환이 이루어집니다"
        }
        "COMPLETED" -> "모든 상환 및 거래가 완료되었습니다"
        "CANCELLED" -> "계약이 취소 또는 거절되었습니다"
        else        -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, CircleShape)
            )
            Text(
                text = badgeLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }

        if (status == "COMPLETED") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BillageSuccess,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = mainText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary,
                    lineHeight = 32.sp
                )
            }
        } else {
            Text(
                text = mainText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary,
                lineHeight = 32.sp
            )
        }

        if (subText.isNotBlank()) {
            Text(
                text = subText,
                fontSize = 13.sp,
                color = BillageTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
