package com.cryptosafe.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.data.BoxWithCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoxesScreen(
    database: AppDatabase,
    onBoxClick: (Box) -> Unit,
    onCreateBox: () -> Unit,
    onBoxSettings: (Box) -> Unit
) {
    val boxes by database.boxDao().getAllBoxesWithCount().collectAsState(initial = emptyList())

    BoxWithContent(
        boxes = boxes,
        onCreateBox = onCreateBox,
        onBoxClick = onBoxClick,
        onBoxSettings = onBoxSettings
    )
}

@Composable
private fun BoxWithContent(
    boxes: List<BoxWithCount>,
    onCreateBox: () -> Unit,
    onBoxClick: (Box) -> Unit,
    onBoxSettings: (Box) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (boxes.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    LocalizationManager.getString("no_boxes"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    LocalizationManager.getString("create_first_box"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    LocalizationManager.getString("boxes_explanation"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(boxes, key = { it.box.id }) { item ->
                    BoxCard(
                        box = item.box,
                        msgCount = item.messageCount,
                        dateFormat = dateFormat,
                        onClick = { onBoxClick(item.box) },
                        onSettings = { onBoxSettings(item.box) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateBox,
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, LocalizationManager.getString("create_box"))
        }
    }
}

@Composable
private fun BoxCard(
    box: Box,
    msgCount: Int,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    box.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${msgCount} ${LocalizationManager.getString("messages").lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    dateFormat.format(Date(box.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                painter = painterResource(com.cryptosafe.app.R.drawable.ic_settings),
                contentDescription = LocalizationManager.getString("box_settings"),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onSettings),
                tint = Color.Unspecified
            )
        }
    }
}
