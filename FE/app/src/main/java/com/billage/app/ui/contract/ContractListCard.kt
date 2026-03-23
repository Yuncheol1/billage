package com.billage.app.ui.contract

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
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
 * - 헤더 색상(민트/산호)으로 역할(빌려줌/빌림)을 직관적으로 구분.
 * - 우측 배지로 현재 진행 상태를 표현.
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
    val isLend = contract.role == ContractRoleFilter.Lend
    val badgeLabel = if (isLend) "🤝 내가 빌려준 계약" else "🤲 내가 빌린 계약"
    val headerBgColor = if (isLend) BillagePrimary else Color(0xFFFF7A5F)

    val (statusText, statusBgColor, statusTextColor) = when (contract.status) {
        ContractStatusFilter.Signing -> Triple("서명 및 협의 중", Color.White.copy(alpha = 0.2f), Color.White)
        ContractStatusFilter.Repaying -> Triple("상환 진행 중", Color.White.copy(alpha = 0.2f), Color.White)
        ContractStatusFilter.Closed -> Triple("상환 완료", Color.White.copy(alpha = 0.2f), Color.White)
        ContractStatusFilter.All -> Triple("", Color.Transparent, Color.Transparent)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BillageSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // ── 헤더 (역할 뱃지 + 상태 배지) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.weight(1f))

                if (statusText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusBgColor
                    ) {
                        Text(
                            text = statusText,
                            color = statusTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── 본문 (상대방 이름, 금액, 만기일 등) ────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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
            }

            HorizontalDivider(color = BillageDivider, thickness = 1.dp)

            // ── 하단 버튼 행 ─────────────────────────────────────
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
                // ACTIVE / CLOSED: 상세 보기만
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
