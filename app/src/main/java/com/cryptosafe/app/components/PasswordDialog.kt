package com.cryptosafe.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cryptosafe.app.LocalizationManager

private val Teal = Color(0xFF6DA397)
private val TealFocused = Color(0xFF8BC9BD)
private val Gold = Color(0xFFE6B65C)
private val BoxBlack = Color(0xFF080808)
private val BoxBorder = Color(0xFF333333)
private val BottomBar = Color(0xFF293D3F)

/**
 * زر على شكل "حبة" بألوان التطبيق (زيتي/ذهبي) — نفس زر شاشة القفل.
 */
@Composable
fun PillButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(50.dp)
            .alpha(if (enabled) (if (pressed) 0.85f else 1f) else 0.4f)
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * حقل إدخال كلمة المرور بنفس تصميم شاشة القفل:
 * خلفية بيضاء، حدّ زيتي 3dp يتوهج عند التركيز، زوايا 8dp، ارتفاع 50dp.
 */
@Composable
fun SafePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Password
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(
                width = 3.dp,
                color = if (focused) TealFocused else Teal,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.Black, fontSize = 18.sp),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(Color(0xFF000000)),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .then(textModifier),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFF888888), fontSize = 18.sp)
                    }
                    inner()
                }
            }
        )
    }
}

/**
 * منبثقة بنفس تصميم شاشة القفل: صندوق أسود بحد رمادي وزوايا 12dp،
 * عنوان أبيض، زرّين على شكل حبة (زيتي للتأكيد، ذهبي للإلغاء)،
 * وشريط سفلي تزييني بلون التطبيق.
 */
@Composable
fun PasswordDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String = LocalizationManager.getString("cancel"),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties()) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 420.dp)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(BoxBlack)
                .border(1.dp, BoxBorder, RoundedCornerShape(12.dp))
                .padding(start = 30.dp, end = 30.dp, top = 20.dp, bottom = 30.dp)
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 25.dp)
            )

            content()

            Spacer(modifier = Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                PillButton(
                    text = confirmText,
                    backgroundColor = Teal,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f)
                )
                PillButton(
                    text = dismissText,
                    backgroundColor = Gold,
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BottomBar)
                    .alpha(0.5f)
            )
        }
    }
}
