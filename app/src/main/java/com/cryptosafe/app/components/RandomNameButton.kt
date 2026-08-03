package com.cryptosafe.app.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cryptosafe.app.LocalizationManager
import com.cryptosafe.app.R

@Composable
fun RandomNameButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 1.2f else 1f)
    val glowAlpha by animateFloatAsState(if (isPressed) 1f else 0f)

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawBehind {
                    if (glowAlpha > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f * glowAlpha),
                            radius = size.minDimension / 2f,
                            center = center
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dice),
                contentDescription = LocalizationManager.getString("random_name"),
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
