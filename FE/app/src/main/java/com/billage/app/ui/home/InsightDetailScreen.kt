package com.billage.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.home.components.insightItems
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightDetailScreen(
    insightIndex: Int,
    onBackClick: () -> Unit
) {
    val item = insightItems.getOrNull(insightIndex) ?: return

    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = BillageTextPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = item.emoji, fontSize = 36.sp)
                    Text(
                        text = item.title,
                        color = BillageTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            itemsIndexed(item.sections) { index, section ->
                Text(
                    text = section.heading,
                    color = BillageTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = section.body,
                    color = BillageTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                if (index < item.sections.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BillageCardBorder)
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
