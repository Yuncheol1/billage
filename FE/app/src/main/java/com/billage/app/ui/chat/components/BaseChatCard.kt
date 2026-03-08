package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.theme.BillageTheme

/**
 * 모든 채팅 카드를 감싸는 공통 컨테이너 BaseChatCard
 *
 * @param sender 발신자 (말풍선 좌우 정렬 및 색상 결정)
 * @param modifier 추가 Modifier 적용
 * @param content 내부 콘텐츠 슬롯 (여기에 카드별 디자인이 들어감)
 */
@Composable
fun BaseChatCard(
    sender: MessageSender,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 발신자에 따라 좌/우 정렬 분기
    val alignment = when (sender) {
        MessageSender.ME -> Alignment.CenterEnd
        MessageSender.OTHER -> Alignment.CenterStart
        MessageSender.SYSTEM -> Alignment.Center
    }

    // 발신자에 따른 말풍선 색상 분기 (테마 구조 연동)
    val backgroundColor = when (sender) {
        MessageSender.ME -> MaterialTheme.colorScheme.primaryContainer
        MessageSender.OTHER -> MaterialTheme.colorScheme.surfaceVariant
        MessageSender.SYSTEM -> Color.Transparent
    }

    // 발신자에 따른 말풍선 꼬리 둥글기 분기
    val shape = when (sender) {
        MessageSender.ME -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 0.dp // 우측 하단 꼬리
        )
        MessageSender.OTHER -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 0.dp, // 좌측 하단 꼬리
            bottomEnd = 16.dp
        )
        MessageSender.SYSTEM -> RoundedCornerShape(16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation = if (sender == MessageSender.SYSTEM) 0.dp else 2.dp,
                    shape = shape
                )
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            content()
        }
    }
}

// ==========================================
// BaseChatCard 컴포넌트 프리뷰 (테스트 영역)
// ==========================================
@Preview(showBackground = true)
@Composable
fun PreviewBaseChatCardMe() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            BaseChatCard(sender = MessageSender.OTHER) {
                Text(
                    text = "이 카드는 상대방이 보낸 카드의 틀입니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BaseChatCard(sender = MessageSender.ME) {
                Text(
                    text = "이 카드는 내가 보낸 카드(오른쪽 정렬)의 기본 틀입니다.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            BaseChatCard(sender = MessageSender.SYSTEM) {
                Text(
                    text = "System: 오늘의 채팅이 시작되었습니다.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
