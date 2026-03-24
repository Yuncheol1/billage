package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@Composable
internal fun KeyMetricsSection(principalAmount: Double?, interestRate: Double?) {
    val hasInterest = interestRate != null && interestRate > 0.0
    val interestText = if (!hasInterest) {
        "없음"
    } else {
        val pct = interestRate!! * 100
        "연 ${"%.2f".format(pct).trimEnd('0').trimEnd('.')}%"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(BillageSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "빌린 금액", fontSize = 13.sp, color = BillageTextSecondary)
            Text(
                text = principalAmount?.let { formatAmount(it) } ?: "-",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BillageTextPrimary
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(BillageDivider)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "이자", fontSize = 13.sp, color = BillageTextSecondary)
            Text(
                text = interestText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (!hasInterest) BillageSuccess else BillageTextPrimary
            )
        }
    }
}
