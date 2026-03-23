package com.billage.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import kotlinx.coroutines.delay

data class TipItem(
    val text: String,
    val icon: ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color = BillagePrimary
)

@Composable
fun TipSlider() {
    val tips = listOf(
        TipItem("AI가 숨은 독소조항을 찾아줘요", Icons.Default.AutoAwesome),
        TipItem("계약 전, 상대방 평판을 확인하세요", Icons.Default.ThumbUp),
        TipItem("안전한 거래, 전자서명을 이용하세요", Icons.Default.Lock)
    )

    // 무한 스크롤(Infinite Carousel) 세팅: 시작점을 넉넉한 중앙(요소 갯수의 배수)으로 잡아 좌우 이동 무제한
    val initialPage = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % tips.size)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    // 자동 슬라이드 (3초 간격)
    LaunchedEffect(pagerState.settledPage) {
        delay(3000)
        // 무조건 우측(다음 페이지)으로만 전진 (뒤로 감기 방지)
        val nextPage = pagerState.settledPage + 1
        pagerState.animateScrollToPage(nextPage)
    }

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
            val tip = tips[page % tips.size]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tip.icon,
                    contentDescription = null,
                    tint = tip.iconTint,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = tip.text,
                    color = BillageTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BillageTextSecondary
                )
            }
        }
    }
}
