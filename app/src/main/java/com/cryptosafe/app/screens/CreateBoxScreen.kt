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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.data.AppDatabase
import com.cryptosafe.app.data.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CreateBoxScreen(
    database: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var boxName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var autoDeleteHours by remember { mutableIntStateOf(0) }
    var lockMode by remember { mutableStateOf("always") }
    var lockTimeoutMinutes by remember { mutableIntStateOf(5) }

    val strength = remember(password) {
        CryptoEngine.checkPasswordStrength(password.toCharArray())
    }

    val strengthColor = when (strength.second) {
        "weak" -> Color(0xFFEF4444)
        "medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

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
        OutlinedTextField(
            value = boxName,
            onValueChange = { boxName = it },
            label = { Text(LocalizationManager.getString("box_name")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(LocalizationManager.getString("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(LocalizationManager.getString("confirm_password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )

        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                LocalizationManager.getString("password_strength"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { strength.first / 4f },
                modifier = Modifier.fillMaxWidth(),
                color = strengthColor,
                trackColor = strengthColor.copy(alpha = 0.2f)
            )
            Text(
                LocalizationManager.getString(strength.second),
                style = MaterialTheme.typography.labelSmall,
                color = strengthColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            LocalizationManager.getString("auto_delete"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        autoDeleteOptions.forEach { (hours, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { autoDeleteHours = hours }
            ) {
                RadioButton(
                    selected = autoDeleteHours == hours,
                    onClick = { autoDeleteHours = hours },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            LocalizationManager.getString("lock_mode_section"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        lockModeOptions.forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { lockMode = value }
            ) {
                RadioButton(
                    selected = lockMode == value,
                    onClick = { lockMode = value },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (lockMode == "timed") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
            ) {
                Column {
                    lockTimeoutOptions.forEach { (minutes, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { lockTimeoutMinutes = minutes }
                        ) {
                            RadioButton(
                                selected = lockTimeoutMinutes == minutes,
                                onClick = { lockTimeoutMinutes = minutes },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(label, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (boxName.isBlank()) {
                    Toast.makeText(context, LocalizationManager.getString("enter_box_name"), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.length < 4) {
                    Toast.makeText(context, LocalizationManager.getString("password_too_short"), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, LocalizationManager.getString("passwords_do_not_match"), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch(Dispatchers.IO) {
                    try {
                        database.boxDao().insertBox(
                            Box(
                                name = boxName,
                                passwordHash = CryptoEngine.hashPasswordForStorage(password),
                                autoDeleteHours = if (autoDeleteHours > 0) autoDeleteHours else null,
                                lockMode = lockMode,
                                lockTimeoutMinutes = if (lockMode == "timed") lockTimeoutMinutes else null
                            )
                        )
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("box_created"), Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    } catch (e: Exception) {
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(LocalizationManager.getString("create_box"), fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}
