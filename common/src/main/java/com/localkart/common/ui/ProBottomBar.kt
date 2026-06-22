package com.localkart.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp

/**
 * Pro bottom navigation: the selected item expands into a coloured pill showing
 * icon + label; unselected items are icon-only. Supports per-item badges.
 */
@Composable
fun ProBottomBar(
    items: List<Pair<String, ImageVector>>,
    selected: Int,
    badges: Map<Int, Int> = emptyMap(),
    onSelect: (Int) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 14.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon) ->
                val sel = i == selected
                val tint = if (sel) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = if (sel) 14.dp else 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badge = badges[i] ?: 0
                    if (badge > 0) {
                        BadgedBox(badge = { Badge { Text("$badge") } }) { Icon(icon, label, tint = tint) }
                    } else {
                        Icon(icon, label, tint = tint)
                    }
                    if (sel) {
                        Spacer(Modifier.width(7.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
                    }
                }
            }
        }
    }
}
