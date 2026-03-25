package com.billage.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight

@Composable
fun ProfileAvatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fallbackBackgroundColor: Color = BillagePrimaryLight,
    fallbackContentColor: Color = BillagePrimary,
    textSize: TextUnit = 18.sp
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fallbackBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            ProfileAvatarFallback(
                name = name,
                contentColor = fallbackContentColor,
                textSize = textSize
            )
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "$name profile image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    ProfileAvatarFallback(
                        name = name,
                        contentColor = fallbackContentColor,
                        textSize = textSize
                    )
                },
                error = {
                    ProfileAvatarFallback(
                        name = name,
                        contentColor = fallbackContentColor,
                        textSize = textSize
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileAvatarFallback(
    name: String,
    contentColor: Color,
    textSize: TextUnit
) {
    Text(
        text = name.firstOrNull()?.uppercase() ?: "?",
        color = contentColor,
        fontSize = textSize,
        fontWeight = FontWeight.Bold
    )
}
