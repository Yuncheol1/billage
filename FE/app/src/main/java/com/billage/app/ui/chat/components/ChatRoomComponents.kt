package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.chat.models.ChatRoomMode
import com.billage.app.ui.chat.models.ContractSummary
import com.billage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BillageTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "뒤로가기",
                    tint = BillageTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun PinnedContractBanner(
    selectedContract: ContractSummary?,
    onAiProposalClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedContract == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BillagePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = BillagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI와 작성 중",
                            style = MaterialTheme.typography.titleSmall,
                            color = BillageTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedContract.counterpartyName}님과의 계약",
                            style = MaterialTheme.typography.labelSmall,
                            color = BillageTextSecondary
                        )
                    }
                }
                
                // 취소 버튼 (X) - 조율 모드 종료 (제안서 자동 생성 방지)
                IconButton(onClick = onExitClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "취소",
                        tint = BillageTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAiProposalClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BillagePrimary
                )
            ) {
                Text("완성하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChatBottomInputBar(
    mode: ChatRoomMode,
    inputText: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        color = BillageSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "메뉴 열기",
                    tint = if (mode == ChatRoomMode.CONTRACT) BillagePrimary else BillageTextSecondary
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        text = if (mode == ChatRoomMode.CONTRACT) "수정 제안이나 의견을 입력하세요" else "메시지를 입력하세요",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BillageDivider,
                    focusedBorderColor = BillagePrimary
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BillagePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatActionGrid(
    onAiNegotiateClick: () -> Unit,
    onManualEditClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "채팅 작업",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionGridItem(
                label = "AI 협의",
                iconText = "🤖",
                backgroundColor = BillagePrimaryLight,
                onClick = onAiNegotiateClick
            )
            ActionGridItem(
                label = "직접 수정",
                iconText = "✏️",
                backgroundColor = BillageInfo.copy(alpha = 0.1f),
                onClick = onManualEditClick
            )
            ActionGridItem(
                label = "송금하기",
                iconText = "💸",
                backgroundColor = BillageWarningBackground,
                onClick = onTransferClick
            )
        }
    }
}

@Composable
private fun ActionGridItem(
    label: String,
    icon: ImageVector? = null,
    iconText: String? = null,
    backgroundColor: Color = BillageBackground,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(backgroundColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = BillagePrimary,
                    modifier = Modifier.size(28.dp)
                )
            } else if (iconText != null) {
                Text(text = iconText, fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BillageTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AiContractSelectDialog(
    contracts: List<com.billage.app.data.remote.api.ContractListResponse>,
    onDismissRequest: () -> Unit,
    onContractSelect: (com.billage.app.data.remote.api.ContractListResponse) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("AI 협의할 차용증 선택", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "AI와 함께 수정할 차용증을 선택해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BillageTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (contracts.isEmpty()) {
                    Text("진행 중인 차용증이 없습니다.", color = BillageTextSecondary)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(contracts) { contract ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onContractSelect(contract) },
                                shape = RoundedCornerShape(8.dp),
                                color = BillageBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BillageDivider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = BillagePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "계약 #${contract.contractId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "상대방: ${contract.counterpartyName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BillageTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("취소", color = BillageTextSecondary)
            }
        }
    )
}
