package com.billage.app.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.FileMessage
import com.billage.app.ui.chat.models.ImageMessage
import com.billage.app.ui.chat.models.LinkMessage
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

@Composable
fun FileChatCard(
    message: FileMessage,
    onDownloadClick: (String) -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Row(
            modifier = Modifier
                .width(240.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 파일 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "File Type",
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 파일 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${message.fileSizeKb} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            // 다운로드 버튼
            IconButton(
                onClick = { onDownloadClick(message.fileUrl) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download File",
                    tint = contentColor
                )
            }
        }
    }
}

@Composable
fun ImageChatCard(
    message: ImageMessage,
    onImageClick: (String) -> Unit
) {
    BaseChatCard(sender = message.sender) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onImageClick(message.imageUrl) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        ratio = if (message.thumbnailHeight > 0)
                            message.thumbnailWidth.toFloat() / message.thumbnailHeight.toFloat()
                        else 1f
                    )
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Image Placeholder", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun LinkChatCard(
    message: LinkMessage,
    onLinkClick: (String) -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(
            modifier = Modifier
                .width(240.dp)
                .clickable { onLinkClick(message.url) }
        ) {
            // OG Image 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Link Preview",
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // 타이틀
            Text(
                text = message.ogTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 설명
            Text(
                text = message.ogDescription,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // 실제 URL
            Text(
                text = message.url,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMediaCards() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            FileChatCard(
                message = FileMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    fileName = "project_proposal_v2.pdf",
                    fileSizeKb = 2048,
                    fileExtension = "pdf",
                    fileUrl = "https://example.com/file"
                ),
                onDownloadClick = {}
            )
            
            ImageChatCard(
                message = ImageMessage(
                    id = "2",
                    sender = MessageSender.ME,
                    timestamp = LocalDateTime.now(),
                    imageUrl = "https://example.com/image.jpg",
                    thumbnailWidth = 800,
                    thumbnailHeight = 600
                ),
                onImageClick = {}
            )
            
            LinkChatCard(
                message = LinkMessage(
                    id = "3",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    url = "https://github.com/ssafy",
                    ogTitle = "SSAFY Repository",
                    ogDescription = "삼성 청년 SW 아카데미 깃허브입니다. 수많은 프로젝트를 확인하세요.",
                    ogImageUrl = null
                ),
                onLinkClick = {}
            )
        }
    }
}
