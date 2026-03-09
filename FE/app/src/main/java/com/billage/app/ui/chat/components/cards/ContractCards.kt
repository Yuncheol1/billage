package com.billage.app.ui.chat.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.ActionStatus
import com.billage.app.ui.chat.models.ConditionProposalMessage
import com.billage.app.ui.chat.models.ContractCompleteMessage
import com.billage.app.ui.chat.models.DraftRequestMessage
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.ProposalRequestMessage
import com.billage.app.ui.chat.models.SignatureRequestMessage
import com.billage.app.ui.chat.models.SystemNoticeMessage
import androidx.compose.material3.Surface
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

@Composable
fun DraftRequestCard(
    message: DraftRequestMessage,
    onAcceptClick: () -> Unit,
    onProposeClick: () -> Unit,
    onViewDetailsClick: () -> Unit = {} // 상세 문서 보기 클릭
) {

    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val titleText = if (message.sender == MessageSender.ME) "내가 돈을 빌려줬어요!" else "${message.senderName}님이 돈을 빌려줬어요!"
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 내용 영역
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "계약명: ${message.contractTitle}", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.summary, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 차용증 전문 보기 (네비게이션 액션)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onViewDetailsClick() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "차용증 원본 보기",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "차용증 전문 보기",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            if (message.sender == MessageSender.OTHER) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // 액션 버튼 영역 (상태에 따른 분기)
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onProposeClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("수정 제안", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("동의", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("초안에 동의했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("수정을 제안했습니다.")
                }
            }
        }
    }
}

@Composable
fun ConditionProposalCard(
    message: ConditionProposalMessage,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "계약 조건 변경 제안",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 내용 영역 (변경된 특약리스트 하이라이트)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "수정된 항목:", 
                        fontWeight = FontWeight.SemiBold, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.modifications.forEachIndexed { index, mod ->
                        Text(
                            text = "${index + 1}. $mod", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (index < message.modifications.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 액션 영역
            if (message.sender == MessageSender.OTHER) {
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRejectClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("거절", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("수락하기", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("제안을 수락했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("제안을 거절했습니다.")
                }
            }
        }
    }
}

@Composable
fun SignatureRequestCard(
    message: SignatureRequestMessage,
    onSignClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "최종 서명 대기 중",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 진행 상태 표시 (나/상대방)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                // 내 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("나의 서명", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (message.isMySignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 상대방 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("상대방 서명", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (message.isOtherSignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // '나'의 서명이 안 된 상태면 버튼 노출
            if (!message.isMySignatureDone) {
                Button(
                    onClick = onSignClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("전자 서명 진행하기", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                ActionCompletedView("내 서명 완료 (상대방 대기 중)")
            }
        }
    }
}

@Composable
fun ContractCompleteCard(
    message: ContractCompleteMessage,
    onDashboardClick: (String) -> Unit,
    onDownloadPdfClick: (String) -> Unit
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = Modifier.width(260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "계약이 성사되었습니다!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "양측 모두 전자서명을 완료하여 상환 타임라인이 시작되었습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onDashboardClick(message.contractId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("계약 대시보드로 이동", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            
            OutlinedButton(
                onClick = { onDownloadPdfClick(message.contractId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("계약서 사본(PDF) 저장", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 버튼 대신 보여줄 공통 완료 뷰
 */
@Composable
private fun ActionCompletedView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SystemNoticeCard(message: SystemNoticeMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProposalRequestCard(
    message: ProposalRequestMessage,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "차용증 수정 제안",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "대상: ${message.contractTitle}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "상대방이 위 계약건에 대해 조건 수정을 제안했습니다. 수락하시면 협의 모드가 켜집니다.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (message.sender == MessageSender.OTHER) {
                Spacer(modifier = Modifier.height(12.dp))
                
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRejectClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("거절", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("수락", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("제안을 수락하여 협의를 시작했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("제안을 거절했습니다.")
                }
            }
        }
    }
}


// ==========================================
// 도메인 맞춤 카드 프리뷰
// ==========================================
@Preview(showBackground = true, name = "1. 초안 카드 (작업 대기)")
@Composable
fun PreviewDraftRequest_Pending() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.PENDING
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. 초안 카드 (작업 완료 - 거절)")
@Composable
fun PreviewDraftRequest_Rejected() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.REJECTED
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "3. 초안 카드 (작업 완료 - 수락)")
@Composable
fun PreviewDraftRequest_Accepted() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.COMPLETED
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "4. 브릿지 카드 (수정 제안 수락 대기)")
@Composable
fun PreviewProposalRequest_Pending() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            ProposalRequestCard(
                message = ProposalRequestMessage(
                    id = "2",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.PENDING
                ),
                onAcceptClick = {},
                onRejectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "5. 최종 조건 제안 카드")
@Composable
fun PreviewConditionProposal() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            ConditionProposalCard(
                message = ConditionProposalMessage(
                    id = "3",
                    sender = MessageSender.SYSTEM,
                    timestamp = LocalDateTime.now(),
                    modifications = listOf("상환 기한을 1개월 연장", "이자율을 3%로 변경"),
                    actionStatus = ActionStatus.COMPLETED
                ),
                onAcceptClick = {},
                onRejectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "6. 시스템 중앙 알림 카드")
@Composable
fun PreviewSystemNotice() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            SystemNoticeCard(
                message = SystemNoticeMessage(
                    id = "notice1",
                    timestamp = LocalDateTime.now(),
                    text = "김철수님이 [100만원 소액 대출 건] 초안 수정을 제안했습니다."
                )
            )
        }
    }
}
