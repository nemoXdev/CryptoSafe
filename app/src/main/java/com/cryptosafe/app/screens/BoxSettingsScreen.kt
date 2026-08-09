package com.cryptosafe.app.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.cryptosafe.app.components.FlashButton
import com.cryptosafe.app.components.FlatDialog
import com.cryptosafe.app.components.PasswordDialog
import com.cryptosafe.app.components.RandomNameButton
import com.cryptosafe.app.components.SafePasswordField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import com.cryptosafe.app.data.Message
import com.cryptosafe.app.security.SecurePasswordStorage
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BoxSettingsScreen(
    box: Box,
    database: AppDatabase,
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit = {},
    boxSessionCache: Map<Long, Pair<String, Long>> = emptyMap()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var boxName by remember { mutableStateOf(box.name) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var autoDeleteHours by remember { mutableIntStateOf(box.autoDeleteHours ?: 0) }
    var lockMode by remember { mutableStateOf(box.lockMode) }
    var lockTimeoutMinutes by remember { mutableIntStateOf(box.lockTimeoutMinutes ?: 5) }
    val msgCount by database.boxDao().getMessageCount(box.id).collectAsState(initial = 0)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val autoDeleteOptions = listOf(
        0 to LocalizationManager.getString("auto_delete_never"),
        1 to LocalizationManager.getString("auto_delete_1h"),
        6 to LocalizationManager.getString("auto_delete_6h"),
        12 to LocalizationManager.getString("auto_delete_12h"),
        24 to LocalizationManager.getString("auto_delete_1d"),
        168 to LocalizationManager.getString("auto_delete_7"),
        720 to LocalizationManager.getString("auto_delete_30"),
        2160 to LocalizationManager.getString("auto_delete_90")
    )

    val lockModeOptions = listOf(
        "always" to LocalizationManager.getString("lock_mode_always"),
        "timed" to LocalizationManager.getString("lock_mode_timed"),
        "never" to LocalizationManager.getString("lock_mode_never"),
        "permanent" to LocalizationManager.getString("lock_mode_permanent")
    )

    val lockTimeoutOptions = listOf(
        1 to LocalizationManager.getString("lock_timeout_1m"),
        5 to LocalizationManager.getString("lock_timeout_5m"),
        15 to LocalizationManager.getString("lock_timeout_15m"),
        60 to LocalizationManager.getString("lock_timeout_60m")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = boxName,
                    onValueChange = { boxName = it },
                    label = { Text(LocalizationManager.getString("box_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    trailingIcon = {
                        RandomNameButton(onClick = { boxName = randomBoxName() })
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlashButton(
                    onClick = {
                        if (boxName.isNotBlank() && boxName != box.name) {
                            scope.launch(Dispatchers.IO) {
                                database.boxDao().updateBox(box.copy(name = boxName))
                                scope.launch(Dispatchers.Main) {
                                    Toast.makeText(context, LocalizationManager.getString("box_renamed"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    cornerRadius = 8.dp,
                    enabled = boxName.isNotBlank() && boxName != box.name
                ) {
                    Text(LocalizationManager.getString("rename_box"))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    LocalizationManager.getString("password"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlashButton(
                    onClick = { showPasswordChangeDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    cornerRadius = 8.dp
                ) {
                    Text(LocalizationManager.getString("change_box_password"))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    LocalizationManager.getString("auto_delete"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                autoDeleteOptions.forEach { (hours, label) ->
                    val onSelect: () -> Unit = {
                        autoDeleteHours = hours
                        scope.launch(Dispatchers.IO) {
                            database.boxDao().updateBox(
                                box.copy(autoDeleteHours = if (hours > 0) hours else null)
                            )
                        }
                    }
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSelect)
                    ) {
                        RadioButton(
                            selected = autoDeleteHours == hours,
                            onClick = onSelect,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    LocalizationManager.getString("lock_mode_section"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                fun persistLockMode(mode: String, minutes: Int?) {
                    if (mode != "permanent" && box.lockMode == "permanent") {
                        SecurePasswordStorage.removeBoxPassword(box.id)
                    }
                    
                    
                    
                    if (mode == "permanent") {
                        val cachedPw = boxSessionCache[box.id]?.first
                        if (cachedPw != null) {
                            SecurePasswordStorage.saveBoxPassword(box.id, cachedPw)
                        }
                    }
                    scope.launch(Dispatchers.IO) {
                        database.boxDao().updateBox(box.copy(lockMode = mode, lockTimeoutMinutes = minutes))
                    }
                }

                lockModeOptions.forEach { (value, label) ->
                    val onSelect = {
                        lockMode = value
                        persistLockMode(value, if (value == "timed") lockTimeoutMinutes else null)
                    }
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSelect)
                    ) {
                        RadioButton(
                            selected = lockMode == value,
                            onClick = onSelect,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (lockMode == "timed") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(modifier = Modifier.padding(start = 40.dp)) {
                        lockTimeoutOptions.forEach { (minutes, label) ->
                            val onSelect = {
                                lockTimeoutMinutes = minutes
                                persistLockMode("timed", minutes)
                            }
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = onSelect)
                            ) {
                                RadioButton(
                                    selected = lockTimeoutMinutes == minutes,
                                    onClick = onSelect,
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    LocalizationManager.getString("stats"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "${LocalizationManager.getString("total_messages")}: $msgCount",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "${LocalizationManager.getString("created_at")}: ${dateFormat.format(Date(box.createdAt))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        FlashButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            containerColor = MaterialTheme.colorScheme.error,
            cornerRadius = 12.dp
        ) {
            Text(LocalizationManager.getString("delete_box"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }

    if (showDeleteDialog) {
        FlatDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = LocalizationManager.getString("delete_box"),
            confirmText = LocalizationManager.getString("delete_box"),
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                SecurePasswordStorage.removeBoxPassword(box.id)
                scope.launch(Dispatchers.IO) {
                    database.boxDao().deleteBoxById(box.id)
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, LocalizationManager.getString("box_deleted"), Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            }
        ) {
            Text(LocalizationManager.getString("delete_box_confirm"))
        }
    }

    if (showPasswordChangeDialog) {
        ChangeBoxPasswordDialog(
            box = box,
            database = database,
            onDismiss = { showPasswordChangeDialog = false },
            onSuccess = {
                showPasswordChangeDialog = false
                SecurePasswordStorage.removeBoxPassword(box.id)
                onPasswordChanged()
                Toast.makeText(context, LocalizationManager.getString("password_changed"), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ChangeBoxPasswordDialog(
    box: Box,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var reencryptProgress by remember { mutableIntStateOf(0) }
    var reencryptTotal by remember { mutableIntStateOf(0) }
    var showPasswords by remember { mutableStateOf(false) }
    val visualTransform = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation()
    val currentPasswordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        currentPasswordFocusRequester.requestFocus()
    }

    PasswordDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = LocalizationManager.getString("change_box_password"),
        confirmText = LocalizationManager.getString("ok"),
        confirmEnabled = !isWorking && currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
        onConfirm = {
            when {
                !CryptoEngine.verifyPasswordForStorage(currentPassword, box.passwordHash) ->
                    Toast.makeText(context, LocalizationManager.getString("wrong_password"), Toast.LENGTH_SHORT).show()
                newPassword.length < 4 ->
                    Toast.makeText(context, LocalizationManager.getString("password_too_short"), Toast.LENGTH_SHORT).show()
                newPassword != confirmPassword ->
                    Toast.makeText(context, LocalizationManager.getString("passwords_do_not_match"), Toast.LENGTH_SHORT).show()
                else -> {
                    isWorking = true
                    reencryptProgress = 0
                    reencryptTotal = 0
                    scope.launch(Dispatchers.IO) {
                        try {
                            val oldChars = currentPassword.toCharArray()
                            val newChars = newPassword.toCharArray()
                            try {
                                
                                
                                val newSalt = CryptoEngine.generateSalt()
                                val newKey = CryptoEngine.deriveBoxKey(newChars, newSalt)
                                var successCount = 0
                                var failCount = 0
                                try {
                                    database.withTransaction {
                                        val messages = database.boxDao().getMessagesByBoxIdSync(box.id)
                                        val total = messages.count { !it.isPreEncrypted }
                                        reencryptTotal = total
                                        var done = 0
                                        for (msg in messages) {
                                            if (!msg.isPreEncrypted) {
                                                done++
                                                reencryptProgress = done
                                                try {
                                                    val plain = CryptoEngine.decrypt(msg.encryptedText, oldChars)
                                                    val reEncrypted = CryptoEngine.encryptWithKey(plain, newKey, newSalt)
                                                    database.boxDao().updateMessage(msg.copy(encryptedText = reEncrypted))
                                                    successCount++
                                                } catch (e: Exception) {
                                                    failCount++
                                                }
                                            }
                                        }
                                        database.boxDao().updateBox(
                                            box.copy(
                                                passwordHash = CryptoEngine.hashPasswordForStorage(newPassword),
                                                encryptionSalt = android.util.Base64.encodeToString(
                                                    newSalt,
                                                    android.util.Base64.NO_WRAP
                                                )
                                            )
                                        )
                                    }
                                } finally {
                                    newKey.fill(0)
                                }
                                scope.launch(Dispatchers.Main) {
                                    isWorking = false
                                    if (failCount > 0) {
                                        Toast.makeText(
                                            context,
                                            LocalizationManager.getString("password_changed_with_errors")
                                                .replace("{success}", successCount.toString())
                                                .replace("{failed}", failCount.toString()),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    onSuccess()
                                }
                            } finally {
                                oldChars.fill('\u0000')
                                newChars.fill('\u0000')
                            }
                        } catch (e: Exception) {
                            scope.launch(Dispatchers.Main) {
                                isWorking = false
                                Toast.makeText(
                                    context,
                                    LocalizationManager.getString("error"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    ) {
            Column {
                Text(
                    LocalizationManager.getString("change_password_warning"),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isWorking) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (reencryptTotal > 0) reencryptProgress.toFloat() / reencryptTotal else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF6DA397),
                        trackColor = Color(0xFF6DA397).copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        LocalizationManager.getString("reencrypt_progress")
                            .replace("{current}", reencryptProgress.toString())
                            .replace("{total}", reencryptTotal.toString()),
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                SafePasswordField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    placeholder = LocalizationManager.getString("current_password"),
                    visualTransformation = visualTransform,
                    keyboardType = KeyboardType.Password,
                    textModifier = Modifier.focusRequester(currentPasswordFocusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SafePasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = LocalizationManager.getString("new_password"),
                    visualTransformation = visualTransform,
                    keyboardType = KeyboardType.Password
                )

                Spacer(modifier = Modifier.height(12.dp))

                SafePasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = LocalizationManager.getString("confirm_new_password"),
                    visualTransformation = visualTransform,
                    keyboardType = KeyboardType.Password
                )

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPasswords = !showPasswords }
                ) {
                    Text(
                        LocalizationManager.getString("show_password"),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = showPasswords,
                        onCheckedChange = { showPasswords = it },
                        modifier = Modifier.requiredSize(24.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6DA397),
                            checkmarkColor = Color.White,
                            uncheckedColor = Color(0xFF555555)
                        )
                    )
                }
            }
        }
    }
