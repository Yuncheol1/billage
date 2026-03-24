package com.billage.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import kotlinx.coroutines.delay

@Composable
fun TipSlider(
    items: List<InsightItem>,
    onItemClick: (Int) -> Unit = {}
) {
    if (items.isEmpty()) return

    // 무한 스크롤: 시작 위치를 items.size의 배수 중앙으로 설정
    val initialPage = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    // 자동 슬라이드 (3초 간격)
    LaunchedEffect(pagerState.settledPage) {
        delay(3000)
        pagerState.animateScrollToPage(pagerState.settledPage + 1)
    }

    Column {
        // ── 슬라이더 카드 ──────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BillagePrimaryLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = items[page % items.size]
                val itemIndex = page % items.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(itemIndex) }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.emoji,
                        fontSize = 26.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = BillageTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.summary,
                            color = BillageTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "자세히 보기",
                        tint = BillageTextSecondary
                    )
                }
            }
        }

        // ── 페이지 인디케이터 도트 ─────────────────────────────
        val currentIndex = pagerState.currentPage % items.size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, _ ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BillagePrimary else BillageCardBorder)
                )
            }
        }
    }
}
