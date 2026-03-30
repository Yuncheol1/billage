package com.billage.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.home.ActivityItem
import com.billage.app.ui.theme.*

@Composable
fun ActivityItemRow(activity: ActivityItem) {
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
