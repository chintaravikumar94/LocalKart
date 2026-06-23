package com.localkart.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Pro bottom navigation: a white elevated bar with rounded top corners. Every item
 * shows icon + label; the selected item's icon sits in a rounded highlight and is
 * tinted with the brand colour. Supports per-item badges.
 */
@Composable
fun ProBottomBar(
    items: List<Pair<String, ImageVector>>,
    selected: Int,
    badges: Map<Int, Int> = emptyMap(),
    onSelect: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon) ->
                val sel = i == selected
                val accent = MaterialTheme.colorScheme.primary
                val muted = MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.width(58.dp).height(30.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        val badge = badges[i] ?: 0
                        if (badge > 0) {
                            BadgedBox(badge = { Badge { Text("$badge") } }) {
                                Icon(icon, label, Modifier.size(22.dp), tint = if (sel) accent else muted)
                            }
                        } else {
                            Icon(icon, label, Modifier.size(22.dp), tint = if (sel) accent else muted)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sel) accent else muted,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
