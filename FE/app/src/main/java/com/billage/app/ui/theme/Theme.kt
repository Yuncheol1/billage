package com.billage.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BillageColorScheme = lightColorScheme(
    primary = BillagePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    background = BillageBackground,
    surface = BillageSurface,
    onBackground = BillageTextPrimary,
    onSurface = BillageTextPrimary,
    error = BillageDanger
)

@Composable
fun BillageTheme(
    // 빌리지는 라이트 모드 고정이라 다크모드/동적색상이 필요 없음
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BillageColorScheme,
        typography = Typography,
        content = content
    )
}