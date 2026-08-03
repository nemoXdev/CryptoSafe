package com.cryptosafe.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cryptosafe.app.LocalizationManager

/**
 * منبثقة بشكل مباشر وبسيط (مستوحاة من بنية نافذة كلمة المرور بتطبيق SSE المرجعي):
 * عنوان + محتوى + زرّين متساويي العرض ملاصقين لبعض بالأسفل، بدل الأزرار
 * الافتراضية الصغيرة المحصورة على جهة وحدة بنافذة AlertDialog القياسية.
 */
@Composable
fun FlatDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String = LocalizationManager.getString("cancel"),
    confirmColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.widthIn(min = 320.dp, max = 480.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                content()

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .padding(end = 4.dp)
                    ) {
                        Text(dismissText)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor ?: MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}
