package com.cryptosafe.app.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.components.InputCard
import com.cryptosafe.app.components.OutputCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DecryptScreen(
    password: CharArray,
    onPasswordChange: (CharArray) -> Unit,
    inputText: String,
    onInputChange: (String) -> Unit,
    outputText: String,
    onOutputChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    isLoading: Boolean,
    onStartLoading: () -> Unit,
    onFinishLoading: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val formContent: @Composable () -> Unit = {
        OutlinedTextField(
            value = String(password),
            onValueChange = { newValue ->
                if (newValue.length <= 999) {
                    onPasswordChange(newValue.toCharArray())
                }
            },
            label = { Text(LocalizationManager.getString("password")) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputCard(
            value = inputText,
            onValueChange = onInputChange,
            label = LocalizationManager.getString("input_label"),
            placeholder = LocalizationManager.getString("output_text"),
            focusedColor = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (inputText.isBlank()) {
                    Toast.makeText(context, LocalizationManager.getString("input_text"), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.isEmpty()) {
                    Toast.makeText(context, LocalizationManager.getString("password_required"), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                onStartLoading()
                scope.launch(Dispatchers.IO) {
                    val passChars = password.clone()
                    try {
                        val result = CryptoEngine.decrypt(inputText, passChars)
                        onOutputChange(result)
                        onPasswordChange(charArrayOf())
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("success"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        onOutputChange("")
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, LocalizationManager.getString("decrypt_error"), Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        passChars.fill('\u0000')
                        onFinishLoading()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(LocalizationManager.getString("decrypt_button"), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    val outputContent: @Composable () -> Unit = {
        if (outputText.isNotEmpty()) {
            OutputCard(
                outputText = outputText,
                onCopy = {
                    clipboard.setText(AnnotatedString(outputText))
                    Toast.makeText(context, LocalizationManager.getString("copied"), Toast.LENGTH_SHORT).show()
                },
                onClear = onClear
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    formContent()
                }
                if (outputText.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        outputContent()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                formContent()
                if (outputText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                outputContent()
            }
        }
    }
}
