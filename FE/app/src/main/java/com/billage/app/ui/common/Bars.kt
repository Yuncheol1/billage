package com.billage.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    userName: String,
    hasNotification: Boolean,
    onNotificationClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BillagePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = userName.ifEmpty { "Billage" },
                    color = BillageTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        actions = {
            BadgedBox(
                badge = {
                    if (hasNotification) {
                        Badge(containerColor = BillageDanger)
                    }
                }
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = BillageTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
        }
    )
}

@Composable
fun BottomBar(onAddContractClick: () -> Unit) {
    Box {
        NavigationBar(
            containerColor = BillageSurface,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("홈") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BillagePrimary,
                    selectedTextColor = BillagePrimary,
                    indicatorColor = BillagePrimaryLight
                )
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chat") },
                label = { Text("채팅") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = BillageTextSecondary,
                    unselectedTextColor = BillageTextSecondary
                )
            )
            // Spacer for center FAB
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = { Spacer(modifier = Modifier.size(48.dp)) },
                label = { Text("") }
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Description, contentDescription = "Contracts") },
                label = { Text("내 계약") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = BillageTextSecondary,
                    unselectedTextColor = BillageTextSecondary
                )
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                label = { Text("메뉴") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = BillageTextSecondary,
                    unselectedTextColor = BillageTextSecondary
                )
            )
        }
        FloatingActionButton(
            onClick = onAddContractClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp),
            containerColor = BillagePrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add contract",
                tint = Color.White
            )
        }
    }
}
