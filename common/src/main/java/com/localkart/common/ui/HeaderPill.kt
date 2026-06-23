package com.localkart.common.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Single header pill card (icon badge + label). Width is controlled by the caller's
 * [modifier] — small pills use Modifier.width(...), wide ones use Modifier.weight(1f).
 */
@Composable
fun HeaderPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    onClick: () -> Unit
) {
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val iconTint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(
                    if (selected) Color.White.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
                contentAlignment = Alignment.Center
            ) {
                if (badge > 0) {
                    BadgedBox(badge = { Badge { Text("$badge") } }) {
                        Icon(icon, label, Modifier.size(18.dp), tint = iconTint)
                    }
                } else {
                    Icon(icon, label, Modifier.size(18.dp), tint = iconTint)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label, style = MaterialTheme.typography.labelSmall, color = fg,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}
