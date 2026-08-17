package com.nba.plus.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * تأثير الوميض (Shimmer) لأماكن التحميل — بدل المؤشرات الدوارة.
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            ShimmerBase,
            ShimmerHighlight,
            ShimmerBase,
        ),
        start = Offset(x = 800f * progress - 400f, y = 0f),
        end = Offset(x = 800f * progress, y = 220f),
    )

    background(brush)
}

/** لون شفاف شائع للاستخدام داخل المكوّنات. */
val Transparent = Color.Transparent
