package com.billage.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.common.BottomBarTab
import com.billage.app.ui.common.MenuProfileState
import com.billage.app.ui.common.TopBar
import com.billage.app.ui.common.rememberLogoutUiState
import com.billage.app.ui.home.HomeSideMenu
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarning
import com.billage.app.ui.theme.BillageWarningBackground

@Composable
fun MypageScreen(
    onNavigateHome: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAddContractClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    menuProfileState: MenuProfileState = MenuProfileState()
) {
    val contracts = remember { sampleContracts() }
    val logoutUiState = rememberLogoutUiState(onLoggedOut = onLogoutClick)
    var showMenu by remember { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(ContractFilter.All) }
    val dismissInteraction = remember { MutableInteractionSource() }
    val expandedSections = remember {
        mutableStateMapOf(
            ContractSectionType.InProgress to false,
            ContractSectionType.Repayment to false,
            ContractSectionType.Completed to false
        )
    }

    val sectionStates = remember(contracts) {
        ContractSectionType.entries.map { type ->
            ContractSectionState(
                type = type,
                contracts = contracts.filter { it.section == type }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BillageBackground,
            topBar = {
                TopBar(
                    userName = "빌리지",
                    hasNotification = true,
                    onNotificationClick = onNotificationClick,
                    onMenuClick = { showMenu = true }
                )
            },
            bottomBar = {
                BottomBar(
                    selectedTab = if (showMenu) BottomBarTab.Menu else BottomBarTab.Contract,
                    onAddContractClick = onAddContractClick,
                    onHomeClick = onNavigateHome,
                    onChatClick = onChatClick,
                    onContractClick = {},
                    onMenuClick = { showMenu = true }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "내 계약 전체보기",
                            color = BillageTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        SummaryCards(sectionStates = sectionStates)

                        Spacer(modifier = Modifier.height(16.dp))
                        FilterChips(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it }
                        )
                    }
                }

                items(sectionStates, key = { it.type.name }) { section ->
                    val visibleContracts = section.contracts.filter { contract ->
                        selectedFilter == ContractFilter.All || contract.filter == selectedFilter
                    }
                    ContractSectionCard(
                        state = section,
                        visibleContracts = visibleContracts,
                        expanded = expandedSections[section.type] == true,
                        onToggle = {
                            expandedSections[section.type] = !(expandedSections[section.type] ?: false)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
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
                    userName = menuProfileState.realName.ifBlank { "빌리지" },
                    email = menuProfileState.email,
                    isLoggingOut = logoutUiState.isLoggingOut,
                    onClose = { showMenu = false },
                    onProfileClick = { showMenu = false },
                    onContractMenuClick = { showMenu = false },
                    onReportMenuClick = { showMenu = false },
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
}

@Composable
private fun SummaryCards(sectionStates: List<ContractSectionState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sectionStates.forEach { section ->
            val backgroundColor = when (section.type) {
                ContractSectionType.InProgress -> Color(0xFFFFF6E5)
                ContractSectionType.Repayment -> BillagePrimaryLight
                ContractSectionType.Completed -> Color(0xFFF1F3F6)
            }
            val countColor = when (section.type) {
                ContractSectionType.InProgress -> BillageWarning
                ContractSectionType.Repayment -> BillagePrimary
                ContractSectionType.Completed -> BillageTextSecondary
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = section.contracts.size.toString(),
                        color = countColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = section.type.label,
                        color = countColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: ContractFilter,
    onFilterSelected: (ContractFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContractFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) BillagePrimary else Color(0xFFEBEDF0),
                modifier = Modifier.clickable { onFilterSelected(filter) }
            ) {
                Text(
                    text = filter.label,
                    color = if (selected) Color.White else BillageTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ContractSectionCard(
    state: ContractSectionState,
    visibleContracts: List<ContractCardModel>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BillageSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            color = state.type.iconBackground,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = state.type.icon,
                        contentDescription = state.type.label,
                        tint = state.type.iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.type.title,
                            color = BillageTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "${state.contracts.size}건",
                            color = state.type.countTint,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.type.description(state.contracts.size),
                        color = BillageTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = Color(0xFFB7BEC9)
                )
            }

            AnimatedVisibility(
                visible = expanded && visibleContracts.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    visibleContracts.forEachIndexed { index, contract ->
                        ContractDetailCard(contract = contract)
                        if (index != visibleContracts.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ContractDetailCard(contract: ContractCardModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BillagePrimaryLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillagePrimary)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contract.badge,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = contract.subLabel,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (contract.statusLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = contract.statusLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = contract.counterparty,
                        color = BillageTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = contract.amount,
                        color = BillagePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailMeta(text = "만기일  ${contract.dueDate}")
                    DetailMeta(
                        text = contract.dDayLabel,
                        color = contract.dDayColor
                    )
                    DetailMeta(text = contract.interestLabel)
                }
            }

            HorizontalDivider(color = Color(0xFFD5EEEA), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "채팅방 열기",
                    tint = BillagePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "채팅방 열기",
                    color = BillagePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = BillagePrimary
                )
            }
        }
    }
}

@Composable
private fun DetailMeta(
    text: String,
    color: Color = BillageTextSecondary
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private enum class ContractFilter(val label: String) {
    All("전체"),
    Lend("🤝 빌려줌"),
    Borrow("🤲 빌림")
}

private enum class ContractSectionType(
    val label: String,
    val title: String,
    val iconTint: Color,
    val iconBackground: Color,
    val countTint: Color
) {
    InProgress(
        label = "계약 진행 중",
        title = "계약 진행 중",
        iconTint = BillageWarning,
        iconBackground = BillageWarningBackground,
        countTint = BillageWarning
    ),
    Repayment(
        label = "상환 진행 중",
        title = "상환 진행 중",
        iconTint = BillagePrimary,
        iconBackground = BillagePrimaryLight,
        countTint = BillagePrimary
    ),
    Completed(
        label = "상환 완료",
        title = "상환 완료",
        iconTint = Color(0xFF98A2B3),
        iconBackground = Color(0xFFF1F3F5),
        countTint = Color(0xFF98A2B3)
    );

    val icon
        @Composable get() = when (this) {
            InProgress -> Icons.Default.ChatBubbleOutline
            Repayment -> Icons.AutoMirrored.Filled.TrendingUp
            Completed -> Icons.Default.TaskAlt
        }

    fun description(count: Int): String = when (this) {
        InProgress -> "채팅방에서 조건 협의 및 서명을 완료해주세요"
        Repayment -> "계약 체결 완료 · 상환이 진행 중이에요"
        Completed -> "완료된 계약 ${count}건 보기"
    }
}

private data class ContractSectionState(
    val type: ContractSectionType,
    val contracts: List<ContractCardModel>
)

private data class ContractCardModel(
    val section: ContractSectionType,
    val filter: ContractFilter,
    val badge: String,
    val subLabel: String,
    val statusLabel: String? = null,
    val counterparty: String,
    val amount: String,
    val dueDate: String,
    val dDayLabel: String,
    val dDayColor: Color,
    val interestLabel: String
)

private fun sampleContracts(): List<ContractCardModel> = listOf(
    ContractCardModel(
        section = ContractSectionType.InProgress,
        filter = ContractFilter.Lend,
        badge = "🤝 빌려줌",
        subLabel = "내가 빌려준 돈",
        counterparty = "이영희",
        amount = "1,000,000원",
        dueDate = "2026년 3월 15일",
        dDayLabel = "D-11",
        dDayColor = Color(0xFFFF5A5F),
        interestLabel = "연 4.6%"
    ),
    ContractCardModel(
        section = ContractSectionType.InProgress,
        filter = ContractFilter.Borrow,
        badge = "🤲 빌림",
        subLabel = "내가 빌린 돈",
        statusLabel = "앱 미가입",
        counterparty = "박지민",
        amount = "500,000원",
        dueDate = "2026년 6월 1일",
        dDayLabel = "D-89",
        dDayColor = BillageTextSecondary,
        interestLabel = "무이자"
    ),
    ContractCardModel(
        section = ContractSectionType.Repayment,
        filter = ContractFilter.Lend,
        badge = "🤝 빌려줌",
        subLabel = "매월 자동 이체 예정",
        counterparty = "최서윤",
        amount = "2,400,000원",
        dueDate = "2026년 7월 5일",
        dDayLabel = "잔여 4회",
        dDayColor = BillagePrimary,
        interestLabel = "연 3.2%"
    ),
    ContractCardModel(
        section = ContractSectionType.Repayment,
        filter = ContractFilter.Borrow,
        badge = "🤲 빌림",
        subLabel = "이번 달 납부 완료",
        counterparty = "김도윤",
        amount = "800,000원",
        dueDate = "2026년 8월 20일",
        dDayLabel = "D-169",
        dDayColor = BillageTextSecondary,
        interestLabel = "연 2.9%"
    )
)

@Preview(showBackground = true)
@Composable
private fun MypageScreenPreview() {
    MypageScreen()
}
