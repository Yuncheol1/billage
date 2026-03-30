package com.billage.app.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary

@Composable
fun SignatureBanner(
    isVisible: Boolean,
    onSignClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE6F7F4)) // 아주 연한 민트 배경
                .clickable { onSignClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BillagePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✍️", fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "양측 모두 수락했습니다",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                    Text(
                        text = "이제 안전하게 전자서명을 진행해 주세요.",
                        fontSize = 12.sp,
                        color = BillageTextPrimary.copy(alpha = 0.7f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BillagePrimary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "서명하기",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
