package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.ActionStatus
import com.billage.app.ui.chat.models.ConditionProposalMessage
import com.billage.app.ui.chat.models.ContractCompleteMessage
import com.billage.app.ui.chat.models.DraftRequestMessage
import com.billage.app.ui.chat.models.FileMessage
import com.billage.app.ui.chat.models.ImageMessage
import com.billage.app.ui.chat.models.LinkMessage
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.SignatureRequestMessage
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

/**
 * 모든 채팅 카드 컴포넌트를 모아서 시각적으로 확인할 수 있는 프리뷰 데모 (Storybook 역할)
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatCardsFullPreview() {
    BillageTheme(dynamicColor = false) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // [1] 미디어 카드 데모
            item {
                FileChatCard(
                    message = FileMessage(
                        id = "msg1",
                        sender = MessageSender.OTHER,
                        timestamp = LocalDateTime.now(),
                        fileName = "Billage_Rules.pdf",
                        fileSizeKb = 1024,
                        fileExtension = "pdf",
                        fileUrl = ""
                    ),
                    onDownloadClick = {}
                )
            }
            item {
                ImageChatCard(
                    message = ImageMessage(
                        id = "msg2",
                        sender = MessageSender.ME,
                        timestamp = LocalDateTime.now(),
                        imageUrl = "",
                        thumbnailWidth = 400,
                        thumbnailHeight = 300
                    ),
                    onImageClick = {}
                )
            }
            item {
                LinkChatCard(
                    message = LinkMessage(
                        id = "msg3",
                        sender = MessageSender.OTHER,
                        timestamp = LocalDateTime.now(),
                        url = "https://www.ssafy.com",
                        ogTitle = "SSAFY 공식 홈페이지",
                        ogDescription = "삼성 청년 SW 아카데미(SSAFY)에서 소프트웨어 교육을 받아보세요.",
                        ogImageUrl = null
                    ),
                    onLinkClick = {}
                )
            }

            // [2] 계약 프로세스 핑퐁 데모
            item {
                DraftRequestCard(
                    message = DraftRequestMessage(
                        id = "msg4",
                        sender = MessageSender.OTHER,
                        timestamp = LocalDateTime.now(),
                        contractTitle = "300만원 차용 건",
                        actionStatus = ActionStatus.PENDING
                    ),
                    onAcceptClick = {},
                    onProposeClick = {}
                )
            }
            item {
                ConditionProposalCard(
                    message = ConditionProposalMessage(
                        id = "msg5",
                        sender = MessageSender.ME, // 내가 이전 초안을 수정하여 제안함
                        timestamp = LocalDateTime.now(),
                        modifications = listOf("상환일을 2026.12.31로 변경", "이자율 5% 추가"),
                        actionStatus = ActionStatus.COMPLETED // 이미 상대가 수락한 경우
                    ),
                    onAcceptClick = {},
                    onRejectClick = {}
                )
            }
            item {
                SignatureRequestCard(
                    message = SignatureRequestMessage(
                        id = "msg6",
                        sender = MessageSender.SYSTEM,
                        timestamp = LocalDateTime.now(),
                        isMySignatureDone = false,
                        isOtherSignatureDone = true // 상대는 이미 서명함
                    ),
                    onSignClick = {}
                    )
            }
            item {
                ContractCompleteCard(
                    message = ContractCompleteMessage(
                        id = "msg7",
                        sender = MessageSender.SYSTEM,
                        timestamp = LocalDateTime.now(),
                        contractId = "billage-1002",
                        contractTitle = "300만원 차용 건"
                    ),
                    onDashboardClick = {},
                    onDownloadPdfClick = {}
                )
            }
        }
    }
}
