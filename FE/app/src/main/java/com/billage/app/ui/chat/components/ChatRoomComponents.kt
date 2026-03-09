@file:OptIn(ExperimentalMaterial3Api::class)

package com.billage.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.ChatRoomMode
import com.billage.app.ui.chat.models.ContractSummary

@Composable
fun ChatTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PinnedContractBanner(
    selectedContract: ContractSummary?,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedContract != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아이콘 데코레이션
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\uD83D\uDCCC", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 텍스트 영역
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "현재 계약 정보 협의 중",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = selectedContract?.title ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 닫기 버튼
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "협의 모드 종료",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    onAddClick: () -> Unit // + 버튼 클릭 시 노출되는 메뉴가 모드에 따라 다름을 처리
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
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
                    // 계약 모드일 때는 + 버튼의 색상을 다르게 줘서 특수 모드임을 암시
                    tint = if (mode == ChatRoomMode.CONTRACT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = if (mode == ChatRoomMode.CONTRACT) "수정 제안이나 의견을 입력하세요 (AI가 정리합니다)" else "메시지를 입력하세요",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ContractSelectBottomSheet(
    contracts: List<ContractSummary>,
    onDismissRequest: () -> Unit,
    onContractSelect: (ContractSummary) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "어떤 차용증에 대해 논의하시겠습니까?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            Divider()
            LazyColumn {
                items(contracts) { contract ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContractSelect(contract) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = contract.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "대상자: ${contract.targetUserName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
