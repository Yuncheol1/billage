package com.billage.app.ui.contract.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillageIconBackground
import com.billage.app.ui.theme.BillageInfo
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

// ─────────────────────────────────────────────────────────
// Data Model
// ─────────────────────────────────────────────────────────

/**
 * 계약 목록 단일 카드의 UI 표현 데이터 모델.
 * API ContractListResponse를 ContractListViewModel에서 이 모델로 매핑한다.
 */
data class ContractCardModel(
    val id: Int = 0,
    val status: ContractStatusFilter,
    val role: ContractRoleFilter,
    val counterparty: String,
    val amount: String,
    val dueDate: String,
    val dDayLabel: String,
    val dDayColorHex: Int,   // ARGB Int — Compose Color(hex)로 감쌀 것
    val interestLabel: String
)

// ─────────────────────────────────────────────────────────
// Contract List Card UI
// ─────────────────────────────────────────────────────────

/**
 * 내 계약 목록의 단일 계약 카드 컴포넌트.
 * - 카드 좌측 6dp 액센트 바 색상으로 계약 상태를 직관적으로 구분.
 * - 우상단 뱃지로 현재 진행 상태를 텍스트로 표현.
 * - 하단 '상세 보기' 버튼 클릭 시 [onDetailClick] 호출.
 */
@Composable
fun ContractListCard(
    contract: ContractCardModel,
    onDetailClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("계약 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("이 계약서를 삭제할까요?\n삭제 후에는 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick?.invoke()
                }) {
                    Text("삭제", color = BillageDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = BillageTextSecondary)
                }
            }
        )
    }

    // 역할 레이블
    val isLend = contract.role == ContractRoleFilter.Lend
    val roleLabel = if (isLend) "🤝 빌려준 계약" else "🤲 빌린 계약"
    // BillageDanger는 Color.kt 토큰 중 산호/오렌지 계열에 가장 가까운 값으로 사용
    val roleLabelColor = if (isLend) BillagePrimary else BillageDanger

    // 상태별 액센트 바 색상 + 뱃지 색상 (레이블은 enum.label 사용)
    data class StatusStyle(
        val accentColor: Color,
        val badgeBg: Color,
        val badgeText: Color
    )

    val statusStyle = when (contract.status) {
        ContractStatusFilter.Signing  -> StatusStyle(BillageInfo,          BillageInfo,           Color.White)
        ContractStatusFilter.Repaying -> StatusStyle(BillagePrimary,       BillagePrimaryLight,   BillagePrimary)
        ContractStatusFilter.Closed   -> StatusStyle(BillageTextSecondary, BillageIconBackground, BillageTextSecondary)
        ContractStatusFilter.All      -> StatusStyle(BillageCardBorder,    Color.Transparent,     Color.Transparent)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BillageSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BillageCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        // IntrinsicSize.Min: 액센트 바가 Column 콘텐츠 높이에 맞춰 늘어나도록
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // ── 좌측 액센트 바 ────────────────────────────────────
            // Card의 clip이 좌상단·좌하단을 12dp로 잘라주므로 별도 shape 불필요
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusStyle.accentColor)
            )

            // ── 본문 영역 ─────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // ── 헤더 (역할 레이블 + 상태 뱃지) ─────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = roleLabel,
                        color = roleLabelColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (contract.status != ContractStatusFilter.All) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusStyle.badgeBg
                        ) {
                            Text(
                                text = contract.status.label,
                                color = statusStyle.badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // ── 계약 정보 ────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = contract.counterparty,
                            color = BillageTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = contract.amount,
                            color = BillagePrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        ContractMetaText(text = "만기일  ${contract.dueDate}")
                        ContractMetaText(text = contract.dDayLabel, color = Color(contract.dDayColorHex))
                        ContractMetaText(text = contract.interestLabel)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                HorizontalDivider(color = BillageDivider, thickness = 1.dp)

                // ── 하단 버튼 행 ──────────────────────────────────
                if (onDeleteClick != null) {
                    // DRAFT / NEGOTIATING: 삭제 버튼 + 상세 보기 버튼 분할
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDeleteDialog = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "계약 삭제",
                                tint = BillageDanger,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "삭제",
                                color = BillageDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        VerticalDivider(color = BillageDivider, thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .weight(2f)
                                .clickable(onClick = onDetailClick)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "계약 상세 보기",
                                tint = BillagePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "상세 보기",
                                color = BillagePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = BillagePrimary
                            )
                        }
                    }
                } else {
                    // SIGNED / ACTIVE / CLOSED: 상세 보기만
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDetailClick)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "계약 상세 보기",
                            tint = BillagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "상세 보기",
                            color = BillagePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = BillagePrimary
                        )
                    }
                }
            }
        }
    }
}

/** 계약 카드 하단의 메타 정보 텍스트 (만기일, D-Day, 이자율) */
@Composable
private fun ContractMetaText(
    text: String,
    color: Color = BillageTextSecondary
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}
