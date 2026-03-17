package com.billage.app.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.BuildConfig
import com.billage.app.R
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary

@Composable
fun LoginScreen(
    onDevBypassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                text = "Borrow and lend with less friction",
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
                    text = "Start your Billage login",
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
                    .clickable {
                        startKakaoLogin(context)
                    }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDevBypassClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BillageDanger
                )
            ) {
                Text(
                    text = "Go home (dev)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}

private fun startKakaoLogin(context: Context) {
    val authorizeUri = Uri.parse("${BuildConfig.BACKEND_BASE_URL}/api/v1/auth/kakao/authorize")
    val intent = Intent(Intent.ACTION_VIEW, authorizeUri)

    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        Log.e("KakaoLogin", "Browser launch failed", error)
        Toast.makeText(context, "Cannot open browser.", Toast.LENGTH_SHORT).show()
    }
}
