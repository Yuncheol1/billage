package com.billage.app.ui.contract.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSuccess
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarning
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailScreen(
    contractSeq: String,
    viewModel: ContractDetailViewModel,
    onBackClick: () -> Unit
) {
    val detail by viewModel.contractDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val myName by viewModel.myName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "계약 상세",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = BillageTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface)
            )
        },
        bottomBar = {
            val d = detail
            if (d != null) {
                val btnText = when (d.status) {
                    "DRAFT", "NEGOTIATING" -> "채팅방에서 협의하기"
                    "ACTIVE"               -> "상환 내역 보기"
                    else                   -> null
                }
                if (btnText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BillageSurface)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text(
                                text = btnText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = BillageBackground
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BillagePrimary)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = errorMessage!!, color = BillageDanger, fontSize = 14.sp)
                        Button(
                            onClick = {
                                contractSeq.toIntOrNull()?.let { viewModel.fetchContractDetail(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                        ) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            detail != null -> {
                val d = detail!!
                val iAmCreditor = d.myRole == "CREDITOR"
                val creditorName = if (iAmCreditor) myName else (d.counterpartyName ?: "상대방")
                val debtorName = if (iAmCreditor) (d.counterpartyName ?: "상대방") else myName
                val counterparty = d.counterpartyName ?: "상대방"
                val sortedClauses = d.clauses.sortedBy { it.orderNo }
                val dday = if (d.endDt != null && d.status !in listOf("COMPLETED", "CANCELLED")) {
                    calculateDday(d.endDt)
                } else null

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    // ① 상단 히어로
                    item {
                        HeroSection(status = d.status, counterparty = counterparty, dday = dday)
                    }

                    // ② 핵심 지표 (빌린 금액 / 이자)
                    item {
                        KeyMetricsSection(
                            principalAmount = d.principalAmount,
                            interestRate = d.interestRate
                        )
                    }

                    // ③ 당사자 · 날짜 · 상환방법 — 시각적 통합 카드
                    item {
                        ContractPartiesCard(
                            creditorName = creditorName,
                            debtorName = debtorName,
                            isFamily = d.family,
                            startDt = d.startDt,
                            endDt = d.endDt,
                            dday = dday,
                            repaymentMethod = d.repaymentMethod,
                            totalInstallments = d.totalInstallments
                        )
                    }

                    // ④ 특약사항 — 흰색 카드로 묶기 (있을 때만)
                    if (sortedClauses.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BillageSurface, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "특약사항",
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp,
                                        vertical = 16.dp
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BillageTextSecondary
                                )
                                sortedClauses.forEachIndexed { index, clause ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            text = clause.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BillageTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = clause.content,
                                            fontSize = 13.sp,
                                            color = BillageTextSecondary,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    if (index < sortedClauses.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            thickness = 1.dp,
                                            color = BillageDivider
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 서명 완료 정보
                    if (d.signedDttm != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "양측 서명 완료 · ${
                                        d.signedDttm.substringBefore("T").replace("-", ".")
                                    }",
                                    fontSize = 12.sp,
                                    color = BillageTextSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // 차용증 전문 링크
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "차용증 전문 보기",
                                fontSize = 13.sp,
                                color = BillagePrimary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { /* TODO */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
// 섹션 컴포저블
// ─────────────────────────────────────────

@Composable
private fun HeroSection(
    status: String?,
    counterparty: String,
    dday: String? = null
) {
    // 상태별 포인트 컬러
    val accentColor = when {
        status == "DRAFT" || status == "NEGOTIATING" -> BillageWarning
        status == "ACTIVE" && dday == "만기 초과"    -> BillageDanger
        status == "ACTIVE" && dday == "오늘 만기"    -> BillageWarning
        status == "ACTIVE"                           -> BillagePrimary
        status == "COMPLETED"                        -> BillageSuccess
        status == "CANCELLED"                        -> BillageDanger
        else                                         -> BillagePrimary
    }

    // 뱃지 라벨
    val badgeLabel = when {
        status == "DRAFT" || status == "NEGOTIATING" -> "서명 대기"
        status == "ACTIVE" && dday == "만기 초과"    -> "만기 초과"
        status == "ACTIVE" && dday == "오늘 만기"    -> "오늘 만기"
        status == "ACTIVE"                           -> "계약 중"
        status == "COMPLETED"                        -> "완료"
        status == "CANCELLED"                        -> "취소됨"
        else                                         -> "계약"
    }

    // 메인 타이틀
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

    // 서브 텍스트
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
        // 미니멀 상태 뱃지 (도트 + 텍스트)
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

        // 메인 타이틀 — COMPLETED는 체크 아이콘 동반
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

        // 서브 텍스트
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

@Composable
private fun KeyMetricsSection(principalAmount: Double?, interestRate: Double?) {
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
        // 빌린 금액
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "빌린 금액",
                fontSize = 13.sp,
                color = BillageTextSecondary
            )
            Text(
                text = principalAmount?.let { formatAmount(it) } ?: "-",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BillageTextPrimary
            )
        }

        // 수직 구분선 — 카드 내부 전체 높이
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(BillageDivider)
        )

        // 이자
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "이자",
                fontSize = 13.sp,
                color = BillageTextSecondary
            )
            Text(
                text = interestText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (!hasInterest) BillageSuccess else BillageTextPrimary
            )
        }
    }
}

@Composable
private fun ContractPartiesCard(
    creditorName: String,
    debtorName: String,
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
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(BillagePrimaryLight, CircleShape)
                        .border(2.dp, BillagePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = creditorName.firstOrNull()?.toString() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillagePrimary
                    )
                }
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
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(BillageBackground, CircleShape)
                        .border(2.dp, BillageCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = debtorName.firstOrNull()?.toString() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextSecondary
                    )
                }
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
            // 빌린 날
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

            // 가운데 D-day + 화살표
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

            // 갚는 날
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

// ─────────────────────────────────────────
// 공통 컴포저블
// ─────────────────────────────────────────

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = BillageTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = BillageTextSecondary)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 1.dp,
        color = BillageDivider
    )
}

// ─────────────────────────────────────────
// 포맷 헬퍼
// ─────────────────────────────────────────

private fun formatAmount(amount: Double): String =
    "%,.0f".format(amount)

private fun formatRepaymentMethodFriendly(method: String?, totalInstallments: Int?): String {
    val n = totalInstallments?.let { " · 총 ${it}회" } ?: ""
    return when (method) {
        "LUMP_SUM"    -> "만기일에 한 번에"
        "MONTHLY"     -> "매달$n"
        "QUARTERLY"   -> "3개월마다$n"
        "SEMI_ANNUAL" -> "6개월마다$n"
        "ANNUAL"      -> "1년마다$n"
        else          -> "-"
    }
}

private fun calculateDday(endDt: String): String {
    return try {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(endDt))
        when {
            days > 0   -> "D-$days"
            days == 0L -> "오늘 만기"
            else       -> "만기 초과"
        }
    } catch (e: Exception) {
        "-"
    }
}
