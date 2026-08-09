package com.cryptosafe.app.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.cryptosafe.app.theme.FieldBorderFocused
import com.cryptosafe.app.theme.FieldFill
import com.cryptosafe.app.theme.FieldFillFocused
import com.cryptosafe.app.theme.FieldText

@Composable
fun FilledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldFillFocused,
            unfocusedContainerColor = FieldFill,
            disabledContainerColor = FieldFill,
            focusedIndicatorColor = FieldBorderFocused,
            unfocusedIndicatorColor = FieldBorderFocused.copy(alpha = 0.35f),
            focusedTextColor = FieldText,
            unfocusedTextColor = FieldText,
            focusedLabelColor = FieldBorderFocused,
            unfocusedLabelColor = FieldText.copy(alpha = 0.6f),
            cursorColor = FieldBorderFocused
        )
    )
}
