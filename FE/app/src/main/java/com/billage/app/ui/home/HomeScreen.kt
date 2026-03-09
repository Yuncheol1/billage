package com.billage.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billage.app.ui.theme.*

@Composable
fun HomeScreen(
    onContractClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAddContractClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onScheduleAllClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            HomeTopBar(
                userName = uiState.userName,
                hasNotification = uiState.hasNotification,
                onNotificationClick = onNotificationClick
            )
        },
        bottomBar = {
            HomeBottomBar(onAddContractClick = onAddContractClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(
                visible = uiState.hasNotification,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    NotificationBanner(
                        message = uiState.notificationMessage,
                        onDismiss = { viewModel.dismissNotification() }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle(title = "나의 계약 현황")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContractSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "받을 돈",
                    count = uiState.lentCount,
                    amount = uiState.lentAmount,
                    onClick = { onContractClick("lent") }
                )
                ContractSummaryCard(
                    modifier = Modifier.weight(1f),
                    label = "갚을 돈",
                    count = uiState.borrowedCount,
                    amount = uiState.borrowedAmount,
                    onClick = { onContractClick("borrowed") }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SectionTitleWithAction(
                title = "다가오는 일정",
                actionLabel = "전체보기 >",
                onActionClick = onScheduleAllClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    ScheduleTimeline(schedules = uiState.upcomingSchedules)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            AiBannerCard()

            Spacer(modifier = Modifier.height(28.dp))
            SectionTitle(title = "최근 거래활동")
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    uiState.recentActivities.forEach { activity ->
                        ActivityItemRow(activity = activity)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onActivityClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "전체 내역 보기",
                            color = BillagePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = userName.ifEmpty { "빌리지" },
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
                        contentDescription = "알림",
                        tint = BillageTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

@Composable
private fun NotificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BillageWarningBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BillageWarning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = BillageTextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = BillageTextSecondary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = BillageTextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SectionTitleWithAction(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = BillageTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        TextButton(
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = actionLabel,
                color = BillageTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ContractSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    amount: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${count}건",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ScheduleTimeline(schedules: List<ScheduleItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        schedules.forEachIndexed { index, schedule ->
            ScheduleTimelineItem(
                schedule = schedule,
                isFirst = index == 0,
                isLast = index == schedules.lastIndex
            )
        }
    }
}

@Composable
private fun ScheduleTimelineItem(
    schedule: ScheduleItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    val isReceiving = schedule.type == "받을"
    val dotColor = if (isReceiving) BillagePrimary else BillageWarning
    val badgeColor = if (isReceiving) BillagePrimaryLight else BillageWarningBackground
    val badgeTextColor = if (isReceiving) BillagePrimary else BillageWarning
    val badgeLabel = if (isReceiving) "받는 날" else "갚는 날"

    val message = if (isReceiving) {
        "${schedule.name}님께 ${schedule.amount} 받을 날이에요"
    } else {
        "${schedule.name}님께 ${schedule.amount} 드릴 날이에요"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(2.dp)
                    .fillMaxHeight()
                    .then(
                        if (isFirst) Modifier.padding(top = 14.dp)
                        else Modifier
                    )
                    .background(BillagePrimaryLight)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 9.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 10.dp,
                    bottom = if (isLast) 4.dp else 24.dp
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = schedule.date,
                    color = BillageTextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = BillageTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AiBannerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimaryLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = BillagePrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "AI가 독소조항을 찾아줘요",
                color = BillageTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BillageTextSecondary
            )
        }
    }
}

@Composable
private fun ActivityItemRow(activity: ActivityItem) {
    val iconTint = when (activity.type) {
        "transfer" -> BillageSuccess   // 초록 — 돈 받음
        "chat"     -> BillageInfo      // 민트 — 협의 중
        else       -> BillagePrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.message,
                color = BillageTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = activity.timeAgo,
                color = BillageTextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BillageTextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
    HorizontalDivider(color = BillageDivider)
}

@Composable
private fun HomeBottomBar(onAddContractClick: () -> Unit) {
    Box {
        NavigationBar(
            containerColor = BillageSurface,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Home, contentDescription = "홈") },
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
                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "메시지") },
                label = { Text("메시지") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = BillageTextSecondary,
                    unselectedTextColor = BillageTextSecondary
                )
            )
            // FAB(+버튼)이 가운데 자리를 차지하므로 빈 자리를 확보하기 위한 더미 아이템
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,  // 터치 비활성화
                icon = { Spacer(modifier = Modifier.size(48.dp)) },
                label = { Text("") }
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Description, contentDescription = "내 계약") },
                label = { Text("내 계약") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = BillageTextSecondary,
                    unselectedTextColor = BillageTextSecondary
                )
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Menu, contentDescription = "메뉴") },
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
                contentDescription = "계약 추가",
                tint = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
