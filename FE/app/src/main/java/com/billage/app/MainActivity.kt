package com.billage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.billage.app.ui.navigation.BillageNavGraph
import com.billage.app.ui.theme.BillageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillageTheme {
                // 앱의 모든 화면 이동은 BillageNavGraph가 담당한다.
                // 앱을 켜면 BillageNavGraph → startDestination → HomeScreen 순서로 실행
                BillageNavGraph()
            }
        }
    }
}