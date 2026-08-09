package com.cryptosafe.app.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.components.PillButton
import com.cryptosafe.app.components.SafePasswordField
import com.cryptosafe.app.security.BiometricCrypto
import com.cryptosafe.app.security.BiometricHelper
import com.cryptosafe.app.security.SecurePasswordStorage
import kotlinx.coroutines.delay


@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(SecurePasswordStorage.getPinAttempts()) }
    var lockoutTime by remember { mutableLongStateOf(SecurePasswordStorage.getPinLockoutTime()) }
    var isLockedOut by remember { mutableStateOf(false) }
    var remainingLockout by remember { mutableLongStateOf(0L) }
    val maxAttempts = 5
    val pinFocusRequester = remember { FocusRequester() }

    fun performUnlock() {
        val currentPin = pin
        if (currentPin.length < 4) {
            Toast.makeText(context, LocalizationManager.getString("pin_min_length"), Toast.LENGTH_SHORT).show()
            return
        }
        val storedHash = SecurePasswordStorage.getPinHash()
        if (storedHash != null && CryptoEngine.verifyPin(currentPin, storedHash)) {
            SecurePasswordStorage.setPinAttempts(0)
            SecurePasswordStorage.setPinLockoutTime(0)
            pin = ""
            onUnlock()
        } else {
            attempts++
            SecurePasswordStorage.setPinAttempts(attempts)
            Toast.makeText(context, LocalizationManager.getString("wrong_pin"), Toast.LENGTH_SHORT).show()
            pin = ""
            if (attempts >= maxAttempts) {
                SecurePasswordStorage.setPinLockoutTime(System.currentTimeMillis())
                isLockedOut = true
                remainingLockout = 30000
            }
        }
    }

    fun checkLockout(): Boolean {
        val now = System.currentTimeMillis()
        val storedLockoutTime = SecurePasswordStorage.getPinLockoutTime()
        if (storedLockoutTime > 0) {
            val elapsed = now - storedLockoutTime
            if (elapsed < 30000) {
                remainingLockout = 30000 - elapsed
                lockoutTime = storedLockoutTime
                return true
            } else {
                SecurePasswordStorage.setPinAttempts(0)
                SecurePasswordStorage.setPinLockoutTime(0)
                attempts = 0
                lockoutTime = 0
            }
        }
        return false
    }

    LaunchedEffect(isLockedOut) {
        if (isLockedOut) {
            val startTime = System.currentTimeMillis()
            val totalLockoutDuration = remainingLockout
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                remainingLockout = maxOf(0L, totalLockoutDuration - elapsed)
                if (remainingLockout <= 0) break
                delay(500)
            }
            isLockedOut = false
            SecurePasswordStorage.setPinAttempts(0)
            SecurePasswordStorage.setPinLockoutTime(0)
            attempts = 0
            lockoutTime = 0
        }
    }

    if (checkLockout()) {
        isLockedOut = true
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties()
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 420.dp)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF080808))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                .padding(start = 30.dp, end = 30.dp, top = 20.dp, bottom = 30.dp)
        ) {
            LaunchedEffect(Unit) {
                withFrameNanos { }
                pinFocusRequester.requestFocus()
            }

            Text(
                LocalizationManager.getString("enter_pin"),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 25.dp)
            )

            SafePasswordField(
                value = pin,
                onValueChange = { if (!isLockedOut && it.length <= 32) pin = it },
                placeholder = LocalizationManager.getString("pin"),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                textModifier = Modifier.focusRequester(pinFocusRequester)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLockedOut) { showPassword = !showPassword }
            ) {
                Text(
                    LocalizationManager.getString("show_password"),
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = showPassword,
                    onCheckedChange = { if (!isLockedOut) showPassword = it },
                    modifier = Modifier.requiredSize(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF6DA397),
                        checkmarkColor = Color.White,
                        uncheckedColor = Color(0xFF555555)
                    )
                )
            }

            if (isLockedOut) {
                val secs = (remainingLockout / 1000) + 1
                Text(
                    LocalizationManager.getString("lockout_wait").replace("{seconds}", "$secs"),
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            } else if (attempts > 0) {
                Text(
                    "${LocalizationManager.getString("attempts_remaining")}: ${maxAttempts - attempts}",
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                PillButton(
                    text = LocalizationManager.getString("ok"),
                    backgroundColor = Color(0xFF6DA397),
                    onClick = { performUnlock() },
                    enabled = !isLockedOut && pin.length >= 4,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = LocalizationManager.getString("cancel"),
                    backgroundColor = Color(0xFFE6B65C),
                    onClick = { onExit() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF293D3F))
                    .alpha(0.5f)
            )

            if (BiometricHelper.isAvailable(context) && SecurePasswordStorage.isBiometricEnabled()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity == null) {
                            Toast.makeText(context, LocalizationManager.getString("error"), Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        val cipher = if (BiometricHelper.isStrongAvailable(context)) {
                            BiometricCrypto.createEncryptCipher()
                        } else {
                            null
                        }
                        BiometricHelper.authenticate(
                            activity = activity,
                            title = LocalizationManager.getString("biometric_title"),
                            subtitle = LocalizationManager.getString("biometric_subtitle"),
                            negativeButtonText = LocalizationManager.getString("cancel"),
                            cryptoObject = cipher?.let { androidx.biometric.BiometricPrompt.CryptoObject(it) },
                            onSuccess = {
                                SecurePasswordStorage.setPinAttempts(0)
                                SecurePasswordStorage.setPinLockoutTime(0)
                                onUnlock()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = !isLockedOut,
                    border = BorderStroke(1.dp, Color(0xFF6DA397)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LocalizationManager.getString("biometric_title"), color = Color.White)
                }
            }
        }
    }
}

