package com.cryptosafe.app.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosafe.app.CryptoEngine
import com.cryptosafe.app.LocalizationManager
import kotlin.math.roundToInt

@Composable
fun PasswordStrengthCard(
    password: CharArray,
    showPassword: Boolean,
    onPasswordChange: (CharArray) -> Unit,
    onTogglePassword: () -> Unit,
    strength: Pair<Int, String>
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("password_prefs", android.content.Context.MODE_PRIVATE) }
    var passwordLength by remember { mutableFloatStateOf(prefs.getInt("length", 16).toFloat()) }
    fun savePrefs() { prefs.edit().apply { putInt("length", passwordLength.toInt()); apply() } }

    val visualTransformation = remember(showPassword) {
        if (showPassword) ColoredPasswordVisualTransformation() else PasswordVisualTransformation()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = String(password),
                onValueChange = { newValue ->
                    if (newValue.length <= 999) {
                        onPasswordChange(newValue.toCharArray())
                    }
                },
                label = { Text(LocalizationManager.getString("password")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = visualTransformation,
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            val strengthColor = when (strength.second) {
                "weak" -> Color(0xFFEF4444)
                "medium" -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            LinearProgressIndicator(
                progress = { strength.first / 4f },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = strengthColor,
                trackColor = strengthColor.copy(alpha = 0.2f)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(LocalizationManager.getString("count") + ":", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                val isOverLimit = password.size >= 999
                val countColor = if (isOverLimit) Color(0xFFCF6679) else Color(0xFF2196F3)
                Text("${password.size}", style = MaterialTheme.typography.labelSmall, color = countColor, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(LocalizationManager.getString("password_strength") + ":", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(LocalizationManager.getString(strength.second), style = MaterialTheme.typography.labelSmall, color = strengthColor, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(LocalizationManager.getString("length") + ": ${passwordLength.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))

            val minVal = 8f
            val maxVal = 120f
            val steps = 111
            val trackHeight = 4.dp
            val thumbSize = 22.dp
            var trackWidthPx by remember { mutableFloatStateOf(1f) }
            val density = LocalDensity.current
            val thumbSizePx = with(density) { thumbSize.toPx() }
            val fraction = ((passwordLength - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(thumbSize + 8.dp)
                        .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
                        .pointerInput(isRtl) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val rawRatio = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                    val ratio = if (isRtl) 1f - rawRatio else rawRatio
                                    val range = maxVal - minVal
                                    val stepSize = range / steps
                                    val raw = minVal + ratio * range
                                    passwordLength = (raw / stepSize).roundToInt() * stepSize
                                    passwordLength = passwordLength.coerceIn(minVal, maxVal)
                                    savePrefs()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val sign = if (isRtl) -1f else 1f
                                    val delta = sign * dragAmount.x / trackWidthPx * (maxVal - minVal)
                                    val stepSize = (maxVal - minVal) / steps
                                    val raw = passwordLength + delta
                                    passwordLength = (raw / stepSize).roundToInt() * stepSize
                                    passwordLength = passwordLength.coerceIn(minVal, maxVal)
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight)) {
                        drawRoundRect(color = Color(0xFF2A2A3C), size = size, cornerRadius = CornerRadius(2.dp.toPx()))
                        val dotRadius = 1.5.dp.toPx()
                        val totalSteps = (maxVal - minVal).toInt()
                        for (i in 0..totalSteps) {
                            val r = (i.toFloat() / totalSteps).coerceIn(0f, 1f)
                            val skip = if (isRtl) r >= 1f - fraction else r <= fraction
                            if (skip) continue
                            drawCircle(color = Color(0xFFF59E0B), radius = dotRadius, center = Offset(size.width * r, size.height / 2f))
                        }
                    }

                    if (isRtl) {
                        Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(fraction).height(trackHeight).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    } else {
                        Box(modifier = Modifier.fillMaxWidth(fraction).height(trackHeight).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    }

                    Box(
                        modifier = Modifier.offset {
                            val x = if (isRtl) ((trackWidthPx - thumbSizePx) * (1f - fraction)).roundToInt()
                            else ((trackWidthPx - thumbSizePx) * fraction).roundToInt()
                            IntOffset(x, 0)
                        }.size(thumbSize).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(8, 16, 32, 64, 96, 120).forEach { len ->
                    Text("$len", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.clickable { passwordLength = len.toFloat(); savePrefs() })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onPasswordChange(CryptoEngine.generatePassword(length = passwordLength.toInt()).toCharArray()) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(LocalizationManager.getString("generate_password"), fontSize = 13.sp)
                }
                Button(onClick = { if (password.isNotEmpty()) { clipboard.setText(AnnotatedString(String(password))); Toast.makeText(context, LocalizationManager.getString("copied"), Toast.LENGTH_SHORT).show() } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(LocalizationManager.getString("copy"), fontSize = 13.sp)
                }
            }
        }
    }
}
