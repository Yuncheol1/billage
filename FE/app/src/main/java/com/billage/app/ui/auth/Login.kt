package com.billage.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.R
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
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
                    .clickable {
                        val activity = context.findActivity()
                        if (activity == null) {
                            Log.e("KakaoLogin", "Login failed: Activity not found from context")
                            return@clickable
                        }
                        startKakaoLogin(activity, onLoginClick)
                    }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BillageDanger
                )
            ) {
                Text(
                    text = "홈 바로가기(개발용)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(38.dp))

        }
    }
}

private fun startKakaoLogin(
    activity: Activity,
    onSuccess: () -> Unit
) {
    val callback: (com.kakao.sdk.auth.model.OAuthToken?, Throwable?) -> Unit = { _, error ->
        if (error != null) {
            Log.e("KakaoLogin", "Login failed", error)
            Toast.makeText(
                activity,
                "Kakao login failed: ${error.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            onSuccess()
        }
    }

    if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
        UserApiClient.instance.loginWithKakaoTalk(activity) { _, error ->
            if (error != null) {
                val clientError = error as? ClientError
                if (clientError?.reason == ClientErrorCause.Cancelled) {
                    Log.i("KakaoLogin", "Login cancelled by user")
                    Toast.makeText(
                        activity,
                        "Kakao login cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@loginWithKakaoTalk
                }
                UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
            } else {
                onSuccess()
            }
        }
    } else {
        UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
