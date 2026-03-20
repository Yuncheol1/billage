package com.billage.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.common.BottomBarTab
import com.billage.app.ui.common.MenuProfileState
import com.billage.app.ui.common.TopBar
import com.billage.app.ui.contract.ContractRole
import com.billage.app.ui.common.rememberLogoutUiState
import com.billage.app.ui.theme.*

@Composable
fun HomeScreen(
    onContractClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAddContractClick: (ContractRole) -> Unit = {},
    onActivityClick: () -> Unit = {},
    onScheduleAllClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onMypageClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    menuProfileState: MenuProfileState = MenuProfileState(),
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContractTypeSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val logoutUiState = rememberLogoutUiState(onLoggedOut = onLogoutClick)
    val dismissInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BillageBackground,
            topBar = {
                TopBar(
                    userName = uiState.userName,
                    hasNotification = uiState.hasNotification,
                    onNotificationClick = onNotificationClick,
                    onMenuClick = { showMenu = true }
                )
            },
            bottomBar = {
                BottomBar(
                    selectedTab = if (showMenu) BottomBarTab.Menu else BottomBarTab.Home,
                    onChatClick = onChatClick,
                    onHomeClick = {},
                    onAddContractClick = { showContractTypeSheet = true },
                    onContractClick = onMypageClick,
                    onMenuClick = { showMenu = true }
                )
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

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null
                    ) { showMenu = false }
            )
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                HomeSideMenu(
                    userName = menuProfileState.realName.ifBlank { uiState.userName },
                    email = menuProfileState.email,
                    isLoggingOut = logoutUiState.isLoggingOut,
                    onClose = { showMenu = false },
                    onProfileClick = { showMenu = false },
                    onContractMenuClick = {
                        showMenu = false
                        onMypageClick()
                    },
                    onReportMenuClick = {
                        showMenu = false
                        onMypageClick()
                    },
                    onSettingsClick = { showMenu = false },
                    onSupportClick = { showMenu = false },
                    onTermsClick = { showMenu = false },
                    onPrivacyClick = { showMenu = false },
                    onLogoutClick = {
                        showMenu = false
                        logoutUiState.onLogoutClick()
                    }
                )
            }
        }
    }

    // 계약 만들기 타입 선택 바텀시트
    if (showContractTypeSheet) {
        ContractTypeBottomSheet(
            onDismiss = { showContractTypeSheet = false },
            onSelect = { role ->
                showContractTypeSheet = false
                onAddContractClick(role)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractTypeBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (ContractRole) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BillageSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "계약 만들기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            ContractTypeItem(
                emoji = "🤝",
                title = "돈 빌려주기",
                description = "차용증을 생성하고 상환 일정을 관리해요",
                onClick = { onSelect(ContractRole.CREDITOR) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContractTypeItem(
                emoji = "🙏",
                title = "돈 빌리기",
                description = "차용 요청을 보내고 계약을 진행해요",
                onClick = { onSelect(ContractRole.DEBTOR) }
            )
        }
    }
}

@Composable
private fun ContractTypeItem(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BillageBackground,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = BillagePrimaryLight,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = BillageTextSecondary,
                lineHeight = 18.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BillageTextSecondary
        )
    }
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
        "transfer" -> BillageSuccess
        "chat" -> BillageInfo
        else -> BillagePrimary
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
