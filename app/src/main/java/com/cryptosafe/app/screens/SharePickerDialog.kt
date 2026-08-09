package com.cryptosafe.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.data.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SharePickerDialog(
    database: AppDatabase,
    sharedText: String,
    onDismiss: () -> Unit,
    onQuickEncrypt: () -> Unit,
    onQuickDecrypt: () -> Unit,
    onBoxSelected: (Box) -> Unit,
    onRequestHide: () -> Unit = onDismiss
) {
    val boxes by database.boxDao().getAllBoxes().collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalizationManager.getString("share_action_title")) },
        text = {
            Column {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            LocalizationManager.getString("share_box_note"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQuickEncrypt()
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            LocalizationManager.getString("quick_encrypt"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQuickDecrypt()
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            LocalizationManager.getString("quick_decrypt"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (boxes.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(boxes, key = { it.id }) { box ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onBoxSelected(box)
                                        onRequestHide()
                                    },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(com.cryptosafe.app.R.drawable.ic_box),
                                        contentDescription = null,
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        box.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        LocalizationManager.getString("no_boxes_yet"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(LocalizationManager.getString("cancel"))
            }
        }
    )
}

suspend fun saveSharedTextToBox(
    database: AppDatabase,
    box: Box,
    password: String,
    sharedText: String
): Boolean {
    return withContext(Dispatchers.IO) {
        database.boxDao().insertMessage(
            Message(
                boxId = box.id,
                encryptedText = sharedText,
                isFromMe = true,
                isPreEncrypted = true
            )
        )
        true
    }
}
