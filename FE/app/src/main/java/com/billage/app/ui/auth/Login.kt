package com.billage.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.R
import com.billage.app.ui.theme.BillagePrimary

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF60CBC3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BillagePrimary)
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(176.dp))

            Text(
                text = "Billage",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "쉽게 빌려주고 빌려요",
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "빌리지를 소개합니다.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.kakao_login_medium_wide),
                contentDescription = "Kakao login",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onLoginClick)
            )
            Spacer(modifier = Modifier.height(50.dp))

        }
    }
}

@Composable
private fun SocialLoginButton(
    label: String,
    iconText: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(containerColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 56.dp)
        )
    }
}
