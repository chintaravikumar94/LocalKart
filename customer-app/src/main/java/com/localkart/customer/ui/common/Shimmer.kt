package com.localkart.customer.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Animated shimmer brush used for skeleton loaders. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -300f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "x"
    )
    val c = MaterialTheme.colorScheme.surfaceVariant
    val cols = listOf(c.copy(alpha = 0.95f), c.copy(alpha = 0.45f), c.copy(alpha = 0.95f))
    return Brush.linearGradient(cols, start = Offset(x - 300f, 0f), end = Offset(x, 300f))
}

@Composable
private fun Bar(width: Float, height: Int, brush: Brush) {
    Box(Modifier.fillMaxWidth(width).height(height.dp).clip(RoundedCornerShape(6.dp)).background(brush))
}

/** A list of skeleton cards shown while real data loads. */
@Composable
fun ShimmerList(rows: Int = 5) {
    val brush = shimmerBrush()
    Column {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Bar(0.6f, 14, brush)
                    Bar(0.85f, 11, brush)
                    Bar(0.4f, 11, brush)
                }
            }
        }
    }
}
