package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.common.ProfileAvatar
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarning

@Composable
internal fun ContractPartiesCard(
    creditorName: String,
    debtorName: String,
    creditorProfileImageUrl: String?,
    debtorProfileImageUrl: String?,
    isFamily: Boolean?,
    startDt: String?,
    endDt: String?,
    dday: String?,
    repaymentMethod: String?,
    totalInstallments: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BillageSurface, RoundedCornerShape(16.dp))
    ) {
        // ── 당사자 섹션 ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 채권자 (빌려준 사람)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileAvatar(
                    name = creditorName,
                    imageUrl = creditorProfileImageUrl,
                    modifier = Modifier
                        .size(52.dp)
                        .background(BillagePrimaryLight, CircleShape)
                        .border(2.dp, BillagePrimary, CircleShape),
                    textSize = 20.sp
                )
                Text(
                    text = creditorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
                Text(text = "빌려준 사람", fontSize = 11.sp, color = BillageTextSecondary)
            }

            // 화살표 + 관계 뱃지
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isFamily != null) {
                    Box(
                        modifier = Modifier
                            .background(BillagePrimaryLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFamily) "가족" else "지인",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BillagePrimary
                        )
                    }
                }
                Text(text = "→", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BillagePrimary)
            }

            // 채무자 (빌린 사람)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileAvatar(
                    name = debtorName,
                    imageUrl = debtorProfileImageUrl,
                    modifier = Modifier
                        .size(52.dp)
                        .background(BillageBackground, CircleShape)
                        .border(2.dp, BillageCardBorder, CircleShape),
                    fallbackBackgroundColor = BillageBackground,
                    fallbackContentColor = BillageTextSecondary,
                    textSize = 20.sp
                )
                Text(
                    text = debtorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
                Text(text = "빌린 사람", fontSize = 11.sp, color = BillageTextSecondary)
            }
        }

        RowDivider()

        // ── 날짜 섹션 ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "빌린 날", fontSize = 12.sp, color = BillageTextSecondary)
                Text(
                    text = startDt?.replace("-", ".") ?: "-",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (dday != null) {
                    Text(
                        text = dday,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (dday) {
                            "만기 초과" -> BillageDanger
                            "오늘 만기" -> BillageWarning
                            else        -> BillagePrimary
                        }
                    )
                }
                Text(text = "→", fontSize = 18.sp, color = BillageTextSecondary)
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "갚는 날", fontSize = 12.sp, color = BillageTextSecondary)
                Text(
                    text = endDt?.replace("-", ".") ?: "-",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BillageTextPrimary
                )
            }
        }

        // ── 상환 방법 ──────────────────────────
        RowDivider()
        InfoRow(
            label = "갚는 방법",
            value = formatRepaymentMethodFriendly(repaymentMethod, totalInstallments)
        )
    }
}
