package com.cryptosafe.app.screens

import android.content.Intent
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.ClipboardHelper
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.data.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    box: Box,
    boxPassword: String,
    database: AppDatabase,
    locked: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by database.boxDao().getMessagesByBoxId(box.id).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var decryptedMessages by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var showDecryptWarning by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    
    
    LaunchedEffect(locked) {
        if (locked) {
            messageToDelete = null
            decryptedMessages = emptyMap()
        }
    }

    
    
    
    
    var boxKey by remember(box.id) { mutableStateOf<ByteArray?>(null) }
    var boxSalt by remember(box.id) { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(box.id, boxPassword, locked) {
        if (locked) {
            boxKey?.fill(0)
            boxKey = null
            boxSalt = null
            return@LaunchedEffect
        }
        if (boxKey == null) {
            val passChars = boxPassword.toCharArray()
            try {
                val salt = withContext(Dispatchers.IO) {
                    var s = box.encryptionSalt?.let {
                        runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
                    }
                    if (s == null || s.size != CryptoEngine.SALT_LENGTH) {
                        s = CryptoEngine.generateSalt()
                        database.boxDao().updateBox(
                            box.copy(encryptionSalt = Base64.encodeToString(s, Base64.NO_WRAP))
                        )
                    }
                    s
                }
                val key = withContext(Dispatchers.IO) { CryptoEngine.deriveBoxKey(passChars, salt) }
                boxSalt = salt
                boxKey = key
            } catch (e: Exception) {
                
            } finally {
                passChars.fill('\u0000')
            }
        }
    }

    DisposableEffect(box.id) {
        onDispose {
            boxKey?.fill(0)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        if (showDecryptWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        LocalizationManager.getString("decrypt_warning"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showDecryptWarning = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = LocalizationManager.getString("close"),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            state = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
        ) {
            items(messages.asReversed(), key = { it.id }) { message ->
                val isDecrypted = decryptedMessages.containsKey(message.id)
                val displayText = if (isDecrypted) decryptedMessages[message.id]!! else message.encryptedText

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message.isPreEncrypted -> MaterialTheme.colorScheme.secondaryContainer
                            message.isFromMe -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontFamily = if (!isDecrypted) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (message.isPreEncrypted) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        dateFormat.format(Date(message.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                                Text(
                                    if (isDecrypted) {
                                        LocalizationManager.getString("decrypted") + ": " +
                                            displayText.codePointCount(0, displayText.length)
                                    } else {
                                        LocalizationManager.getString("encrypted") + ": " +
                                            message.encryptedText.codePointCount(0, message.encryptedText.length)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDecrypted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (!isDecrypted) {
                                    Button(
                                        onClick = {
                                            showDecryptWarning = true
                                            val passChars = boxPassword.toCharArray()
                                            scope.launch {
                                                try {
                                                    val decrypted = withContext(Dispatchers.IO) {
                                                        val key = boxKey
                                                        val salt = boxSalt
                                                        if (key != null && salt != null) {
                                                            
                                                            
                                                            CryptoEngine.decryptWithKey(message.encryptedText, key, salt)
                                                                ?: CryptoEngine.decrypt(message.encryptedText, passChars)
                                                        } else {
                                                            CryptoEngine.decrypt(message.encryptedText, passChars)
                                                        }
                                                    }
                                                    decryptedMessages = decryptedMessages + (message.id to decrypted)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, LocalizationManager.getString("decrypt_error"), Toast.LENGTH_LONG).show()
                                                } finally {
                                                    passChars.fill('\u0000')
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(28.dp),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(LocalizationManager.getString("decrypt"), fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            decryptedMessages = decryptedMessages - message.id
                                        },
                                        modifier = Modifier.height(28.dp),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(LocalizationManager.getString("hide"), fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, displayText)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, null))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = LocalizationManager.getString("share"),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        
                                        ClipboardHelper.copySensitive(context, displayText) {
                                            Toast.makeText(context, LocalizationManager.getString("copied"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = LocalizationManager.getString("copy"),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                                IconButton(
                                    onClick = { messageToDelete = message },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = LocalizationManager.getString("delete"),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(LocalizationManager.getString("type_message")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (inputText.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${inputText.codePointCount(0, inputText.length)} ${LocalizationManager.getString("chars")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isBlank()) return@IconButton
                    val textToSend = inputText
                    inputText = ""
                    val passChars = boxPassword.toCharArray()
                    scope.launch(Dispatchers.IO) {
                        try {
                            
                            
                            val encrypted = if (boxKey != null && boxSalt != null) {
                                CryptoEngine.encryptWithKey(textToSend, boxKey!!, boxSalt!!)
                            } else {
                                CryptoEngine.encrypt(textToSend, passChars)
                            }
                            database.boxDao().insertMessage(
                                Message(
                                    boxId = box.id,
                                    encryptedText = encrypted,
                                    isFromMe = true
                                )
                            )
                        } catch (e: Exception) {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            passChars.fill('\u0000')
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = LocalizationManager.getString("send"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text(LocalizationManager.getString("delete_message")) },
            text = { Text(LocalizationManager.getString("delete_message_confirm")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToDelete?.let { msg ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    database.boxDao().deleteMessageById(msg.id)
                                }
                                decryptedMessages = decryptedMessages - msg.id
                            }
                        }
                        messageToDelete = null
                    }
                ) {
                    Text(LocalizationManager.getString("delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) {
                    Text(LocalizationManager.getString("cancel"))
                }
            }
        )
    }
}
