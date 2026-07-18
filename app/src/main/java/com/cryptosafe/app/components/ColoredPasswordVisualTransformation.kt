package com.cryptosafe.app.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

class ColoredPasswordVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val colored = buildAnnotatedString {
            for (char in text) {
                val color = when {
                    char.isDigit() -> Color(0xFF2196F3)
                    !char.isLetterOrDigit() -> Color(0xFFEF4444)
                    else -> Color.Unspecified
                }
                if (color == Color.Unspecified) {
                    append(char)
                } else {
                    withStyle(SpanStyle(color = color)) {
                        append(char)
                    }
                }
            }
        }
        return TransformedText(colored, OffsetMapping.Identity)
    }
}
