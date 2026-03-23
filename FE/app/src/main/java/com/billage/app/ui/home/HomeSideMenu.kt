package com.billage.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageInfo
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarning

@Composable
fun HomeSideMenu(
    userName: String,
    email: String,
    isLoggingOut: Boolean,
    onClose: () -> Unit,
    onProfileClick: () -> Unit,
    onContractMenuClick: () -> Unit,
    onReportMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSupportClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.82f),
        color = BillageSurface,
        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
        shadowElevation = 12.dp
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF31C9AF), Color(0xFF18C1B8))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF0A6E69)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = if (userName.isBlank()) "홍길동" else userName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (email.isBlank()) "이메일 정보 없음" else email,
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.clickable(onClick = onProfileClick)
                ) {
                    Text(
                        text = "프로필 설정",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "메뉴",
                    color = BillageTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuCardItem(
                    icon = Icons.Default.Description,
                    iconTint = BillageWarning,
                    iconBackground = Color(0xFFFFF4D5),
                    title = "내 계약",
                    subtitle = "계약서 및 거래 관리",
                    onClick = onContractMenuClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                MenuCardItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = BillageInfo,
                    iconBackground = Color(0xFFE8F7FF),
                    title = "계약 통계",
                    subtitle = "거래 분석 및 리포트",
                    onClick = onReportMenuClick
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF1F3F5))
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "설정 및 지원",
                    color = BillageTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuRowItem(
                    icon = Icons.Default.Settings,
                    title = "설정",
                    onClick = onSettingsClick
                )
                Spacer(modifier = Modifier.height(6.dp))
                MenuRowItem(
                    icon = Icons.Default.Info,
                    title = "고객센터",
                    onClick = onSupportClick
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFF1F3F5))
                Spacer(modifier = Modifier.height(20.dp))

                MenuTextItem(text = "이용약관", onClick = onTermsClick)
                Spacer(modifier = Modifier.height(18.dp))
                MenuTextItem(text = "개인정보처리방침", onClick = onPrivacyClick)

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "버전 1.0.0",
                    color = Color(0xFFA0A8B3),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onLogoutClick,
                enabled = !isLoggingOut,
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = BillageDanger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isLoggingOut) "로그아웃 중..." else "로그아웃",
                    color = BillageDanger,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MenuCardItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = BillageTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = BillageTextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB0B7C3)
        )
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BillageTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            color = BillageTextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB0B7C3)
        )
    }
}

@Composable
private fun MenuTextItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = BillageTextPrimary,
        fontSize = 14.sp,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
