package com.localkart.common.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp

/**
 * Premium pill-card tab bar: each tab is a rounded card with an icon badge above
 * its label; the selected tab is filled with the primary colour.
 */
@Composable
fun PillTabRow(
    tabs: List<Pair<String, ImageVector>>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEachIndexed { i, (label, icon) ->
            val sel = i == selected
            Surface(
                onClick = { onSelect(i) },
                shape = RoundedCornerShape(18.dp),
                color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shadowElevation = if (sel) 6.dp else 0.dp,
                border = if (sel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).height(74.dp)
            ) {
                Column(
                    Modifier.fillMaxSize().padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(
                            if (sel) Color.White.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, label, Modifier.size(18.dp),
                            tint = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        textAlign = TextAlign.Center,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
