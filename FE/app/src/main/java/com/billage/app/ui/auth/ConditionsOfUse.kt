package com.billage.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.billage.app.ui.theme.BillagePrimary

@Composable
fun ConditionsOfUseScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ageConsent by remember { mutableStateOf(false) }
    var serviceConsent by remember { mutableStateOf(false) }
    var privacyConsent by remember { mutableStateOf(false) }
    var marketingConsent by remember { mutableStateOf(false) }

    val allRequiredChecked = ageConsent && serviceConsent && privacyConsent
    val allChecked = allRequiredChecked && marketingConsent

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BillagePrimary
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BillagePrimary)
                .padding(horizontal = 24.dp, vertical = 200.dp)
        ) {
            Text(
                text = "Billage",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)

            )

            Text(
                text = "서비스 이용을 위해 약관 동의가 필요해요",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column {
                    TermsCheckRow(
                        title = "전체 동의",
                        required = false,
                        checked = allChecked,
                        onClick = {
                            val next = !allChecked
                            ageConsent = next
                            serviceConsent = next
                            privacyConsent = next
                            marketingConsent = next
                        },
                        titleFontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    DividerLine()
                    Spacer(modifier = Modifier.height(12.dp))

                    TermsCheckRow(
                        title = "만 14세 이상입니다",
                        required = true,
                        checked = ageConsent,
                        onClick = { ageConsent = !ageConsent }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TermsCheckRow(
                        title = "서비스 이용약관 동의",
                        required = true,
                        checked = serviceConsent,
                        onClick = { serviceConsent = !serviceConsent }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TermsCheckRow(
                        title = "개인정보 수집 및 이용 동의",
                        required = true,
                        checked = privacyConsent,
                        onClick = { privacyConsent = !privacyConsent }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TermsCheckRow(
                        title = "마케팅 정보 수신 동의",
                        required = false,
                        checked = marketingConsent,
                        onClick = { marketingConsent = !marketingConsent }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        color = if (allRequiredChecked) Color(0xFFF3D54E) else Color(0xFFB6DAD6),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = allRequiredChecked, onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "동의하고 계속하기",
                    color = Color(0xFF25312F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TermsCheckRow(
    title: String,
    required: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    titleFontWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (checked) BillagePrimary else Color(0xFFE8EEED),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = title,
                color = Color(0xFF1F2A28),
                fontWeight = titleFontWeight,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Text(
            text = if (required) "(필수)" else "(선택)",
            color = if (required) Color(0xFF3D8C84) else Color(0xFF92A8A4),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE6EEEE))
    )
}
