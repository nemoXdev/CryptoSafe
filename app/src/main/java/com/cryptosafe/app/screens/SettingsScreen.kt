package com.cryptosafe.app.screens

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.components.FilledField
import com.cryptosafe.app.security.BiometricHelper
import com.cryptosafe.app.security.SecurePasswordStorage

// ---- عناصر مساعدة لشكل "قائمة مباشرة" بدون بطاقات (بس بألوان خزنة ونحاس) ----

@Composable
private fun SectionHeader(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onHelp: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(SecurePasswordStorage.isBiometricEnabled()) }
    var screenshotEnabled by remember { mutableStateOf(SecurePasswordStorage.isScreenshotProtectionEnabled()) }
    var showPinFields by remember { mutableStateOf(false) }
    val pinShowCheckboxRow: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(LocalizationManager.getString("show_password"), style = MaterialTheme.typography.bodyMedium)
            Checkbox(
                checked = showPinFields,
                onCheckedChange = { showPinFields = it },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
    val hasPin = SecurePasswordStorage.hasPin()
    var autoLockTimer by remember { mutableIntStateOf(SecurePasswordStorage.getAutoLockTimer()) }
    val canUseBiometric = BiometricHelper.isAvailable(context)
    val act = context as? Activity

    DisposableEffect(screenshotEnabled) {
        if (act != null) {
            if (screenshotEnabled) {
                act.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {
            if (act != null) {
                if (SecurePasswordStorage.isScreenshotProtectionEnabled()) {
                    act.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ---- الأمان: رمز القفل (PIN) ----
        SectionHeader(LocalizationManager.getString("security_settings"))

        if (!hasPin) {
            Text(LocalizationManager.getString("set_pin_description"), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            FilledField(
                value = newPin,
                onValueChange = { if (it.length <= 32) newPin = it },
                label = LocalizationManager.getString("new_pin"),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilledField(
                value = confirmPin,
                onValueChange = { if (it.length <= 32) confirmPin = it },
                label = LocalizationManager.getString("confirm_pin"),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            pinShowCheckboxRow()

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (newPin.length < 4) {
                        Toast.makeText(context, LocalizationManager.getString("pin_min_length"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        Toast.makeText(context, LocalizationManager.getString("pins_do_not_match"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    SecurePasswordStorage.savePinHash(CryptoEngine.hashPin(newPin))
                    SecurePasswordStorage.setPinAttempts(0)
                    Toast.makeText(context, LocalizationManager.getString("pin_set_success"), Toast.LENGTH_SHORT).show()
                    newPin = ""
                    confirmPin = ""
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                enabled = newPin.length >= 4 && confirmPin.length >= 4
            ) {
                Text(LocalizationManager.getString("set_pin"), fontWeight = FontWeight.Medium)
            }
        } else {
            Text(LocalizationManager.getString("change_pin_description"), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            FilledField(
                value = currentPin,
                onValueChange = { if (it.length <= 32) currentPin = it },
                label = LocalizationManager.getString("current_pin"),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilledField(
                value = newPin,
                onValueChange = { if (it.length <= 32) newPin = it },
                label = LocalizationManager.getString("new_pin"),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilledField(
                value = confirmPin,
                onValueChange = { if (it.length <= 32) confirmPin = it },
                label = LocalizationManager.getString("confirm_pin"),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )

            pinShowCheckboxRow()

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (currentPin.length < 4) {
                        Toast.makeText(context, LocalizationManager.getString("enter_current_pin"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!CryptoEngine.verifyPin(currentPin, SecurePasswordStorage.getPinHash() ?: "")) {
                        Toast.makeText(context, LocalizationManager.getString("wrong_pin"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPin.length < 4) {
                        Toast.makeText(context, LocalizationManager.getString("pin_min_length"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        Toast.makeText(context, LocalizationManager.getString("pins_do_not_match"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    SecurePasswordStorage.savePinHash(CryptoEngine.hashPin(newPin))
                    SecurePasswordStorage.setPinAttempts(0)
                    Toast.makeText(context, LocalizationManager.getString("pin_changed_success"), Toast.LENGTH_SHORT).show()
                    currentPin = ""
                    newPin = ""
                    confirmPin = ""
                },
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text(LocalizationManager.getString("change_pin"), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (currentPin.length < 4) {
                        Toast.makeText(context, LocalizationManager.getString("enter_current_pin"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!CryptoEngine.verifyPin(currentPin, SecurePasswordStorage.getPinHash() ?: "")) {
                        Toast.makeText(context, LocalizationManager.getString("wrong_pin"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    SecurePasswordStorage.removePinHash()
                    SecurePasswordStorage.setPinAttempts(0)
                    SecurePasswordStorage.setPinLockoutTime(0)
                    Toast.makeText(context, LocalizationManager.getString("pin_removed_success"), Toast.LENGTH_SHORT).show()
                    currentPin = ""
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(LocalizationManager.getString("remove_pin"), fontWeight = FontWeight.Medium)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // ---- مؤقت القفل التلقائي ----
        if (hasPin) {
            SectionHeader(LocalizationManager.getString("auto_lock_timer"))
            val timerOptions = listOf(
                0 to LocalizationManager.getString("auto_lock_immediate"),
                30 to LocalizationManager.getString("auto_lock_30s"),
                60 to LocalizationManager.getString("auto_lock_1m"),
                300 to LocalizationManager.getString("auto_lock_5m"),
                600 to LocalizationManager.getString("auto_lock_10m"),
                1800 to LocalizationManager.getString("auto_lock_30m")
            )
            timerOptions.forEach { (seconds, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            autoLockTimer = seconds
                            SecurePasswordStorage.setAutoLockTimer(seconds)
                        }
                ) {
                    RadioButton(
                        selected = autoLockTimer == seconds,
                        onClick = {
                            autoLockTimer = seconds
                            SecurePasswordStorage.setAutoLockTimer(seconds)
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---- البصمة ----
        if (canUseBiometric) {
            SectionHeader(LocalizationManager.getString("biometric_settings"))
            SettingsToggleRow(
                title = LocalizationManager.getString("biometric_unlock"),
                description = LocalizationManager.getString("biometric_unlock_desc"),
                checked = biometricEnabled,
                onCheckedChange = {
                    biometricEnabled = it
                    SecurePasswordStorage.setBiometricEnabled(it)
                }
            )
        }

        // ---- الخصوصية ----
        SectionHeader(LocalizationManager.getString("privacy_settings"))
        SettingsToggleRow(
            title = LocalizationManager.getString("screenshot_protection"),
            description = LocalizationManager.getString("screenshot_protection_desc"),
            checked = screenshotEnabled,
            onCheckedChange = {
                screenshotEnabled = it
                SecurePasswordStorage.setScreenshotProtectionEnabled(it)
                if (act != null) {
                    if (it) {
                        act.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        )

        // ---- المساعدة ----
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Help, null, modifier = Modifier.padding(end = 8.dp))
            Text(LocalizationManager.getString("help"), fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
