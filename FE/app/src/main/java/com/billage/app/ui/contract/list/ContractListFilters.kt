package com.billage.app.ui.contract.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageInfo
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

// ─────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────

enum class ContractRoleFilter(val label: String, val apiValue: String?) {
    All("전체", null),
    Lend("빌려준 계약", "CREDITOR"),
    Borrow("빌린 계약", "DEBTOR")
}

/**
 * 백엔드 status enum 7종을 UI 표현으로 매핑.
 * UI 필터와 API 값은 분리 — 상태 필터링은 클라이언트에서만 처리.
 */
enum class ContractStatusFilter(val label: String, val apiValues: List<String>) {
    All("전체", emptyList()),
    /** 서명 및 협의 단계 (DRAFT + NEGOTIATING) */
    Signing("서명 전", listOf("DRAFT", "NEGOTIATING")),
    /** 계약 체결 후 상환 단계 (SIGNED + ACTIVE + OVERDUE + DISPUTED) */
    Repaying("계약 진행 중", listOf("SIGNED", "ACTIVE", "OVERDUE", "DISPUTED")),
    /** 상환 완료 */
    Closed("계약 완료", listOf("CLOSED"))
}

/** API status 문자열 → ContractStatusFilter 변환 헬퍼 */
fun String.toContractStatusFilter(): ContractStatusFilter =
    ContractStatusFilter.entries.firstOrNull { this in it.apiValues }
        ?: ContractStatusFilter.All

/** API role 문자열 → ContractRoleFilter 변환 헬퍼 */
fun String.toContractRoleFilter(): ContractRoleFilter =
    ContractRoleFilter.entries.firstOrNull { it.apiValue == this }
        ?: ContractRoleFilter.All


// ─────────────────────────────────────────────────────────
// Filter UI Components
// ─────────────────────────────────────────────────────────

/** 역할 선택 탭 (전체 / 빌려준 계약 / 빌린 계약) — 세그먼트 탭 스타일 */
@Composable
fun RoleFilterTabs(
    selectedRole: ContractRoleFilter,
    onRoleSelected: (ContractRoleFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BillageSurface, shape = RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ContractRoleFilter.entries.forEach { role ->
            val isSelected = role == selectedRole
            val activeColor = when (role) {
                ContractRoleFilter.All    -> BillageTextPrimary
                ContractRoleFilter.Lend   -> BillagePrimary
                ContractRoleFilter.Borrow -> BillageDanger
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRoleSelected(role) }
                    .background(
                        color = if (isSelected) activeColor else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = role.label,
                    color = if (isSelected) Color.White else BillageTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/** 상태 선택 탭 (전체 / 서명 전 / 계약 진행 중 / 계약 완료) — 세그먼트 탭 스타일 */
@Composable
fun StatusFilterChips(
    selectedStatus: ContractStatusFilter,
    onStatusSelected: (ContractStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BillageSurface, shape = RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ContractStatusFilter.entries.forEach { status ->
            val isSelected = status == selectedStatus
            val activeColor = when (status) {
                ContractStatusFilter.All      -> BillageTextPrimary
                ContractStatusFilter.Signing  -> BillageInfo
                ContractStatusFilter.Repaying -> BillagePrimary
                ContractStatusFilter.Closed   -> BillageTextSecondary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStatusSelected(status) }
                    .background(
                        color = if (isSelected) activeColor else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status.label,
                    color = if (isSelected) Color.White else BillageTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
