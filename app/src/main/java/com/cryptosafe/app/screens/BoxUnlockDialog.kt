package com.cryptosafe.app.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.components.PasswordDialog
import com.cryptosafe.app.components.SafePasswordField
import com.cryptosafe.app.data.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * يطلب كلمة مرور الصندوق ويتحقق منها مقابل box.passwordHash قبل السماح بالدخول.
 * لا يخزن كلمة المرور أبداً - فقط يعيدها مؤقتاً للمستدعي عبر onUnlocked.
 * بنفس تصميم شاشة القفل: صندوق أسود، حقل بحدّ زيتي، وزرّين على شكل حبة.
 */
@Composable
fun BoxUnlockDialog(
    box: Box,
    onDismiss: () -> Unit,
    onUnlocked: (password: String) -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        passwordFocusRequester.requestFocus()
    }

    PasswordDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = LocalizationManager.getString("enter_box_password"),
        confirmText = LocalizationManager.getString("ok"),
        confirmEnabled = password.isNotEmpty() && !isVerifying,
        onConfirm = {
            val candidate = password
            isVerifying = true
            // التحقق باشتقاق Argon2 (ثقيل ~1 ثانية) خارج خيط الواجهة حتى لا تتجمد الشاشة
            scope.launch {
                val valid = withContext(Dispatchers.IO) {
                    CryptoEngine.verifyPasswordForStorage(candidate, box.passwordHash)
                }
                isVerifying = false
                password = ""
                if (valid) {
                    onUnlocked(candidate)
                } else {
                    Toast.makeText(context, LocalizationManager.getString("wrong_password"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    ) {
        Column {
            SafePasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = LocalizationManager.getString("password"),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                textModifier = Modifier.focusRequester(passwordFocusRequester)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp)
                    .clickable { showPassword = !showPassword }
            ) {
                Text(
                    LocalizationManager.getString("show_password"),
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Checkbox(
                    checked = showPassword,
                    onCheckedChange = { showPassword = it },
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
