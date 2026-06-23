package com.localkart.customer.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/** Location bar with a radius selector (5/10/15/20/25 km). Common across pages. */
@Composable
fun LocationBar(
    location: String,
    radiusKm: Int,
    onChangeLocation: () -> Unit,
    onRadiusSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(location, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("Tap to change location",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable { onChangeLocation() })
            }
            Box {
                AssistChip(onClick = { expanded = true }, label = { Text("$radiusKm km") })
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    listOf(5, 10, 15, 20, 25).forEach { km ->
                        DropdownMenuItem(text = { Text("$km km") }, onClick = {
                            onRadiusSelected(km); expanded = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(hint: String, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Editable search field with live filtering + clear button. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, hint: String) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).padding(14.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                Icon(Icons.Default.Close, "Clear",
                    Modifier.clickable { onQueryChange("") }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Horizontal category chips: All | groceries | mobile repairing | ... */
@Composable
fun CategoryChips(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { c ->
            FilterChip(
                selected = c == selected,
                onClick = { onSelect(c) },
                label = { Text(c.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

/**
 * Pro-level auto-sliding banner pager with a smooth progress bar that fills
 * over the dwell time, then advances to the next banner.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSlider(imageUrls: List<String>, dwellMillis: Int = 4000) {
    if (imageUrls.isEmpty()) return
    val pager = rememberPagerState { imageUrls.size }
    var progress by remember { mutableFloatStateOf(0f) }

    // drive progress + auto-advance
    LaunchedEffect(pager.currentPage) {
        progress = 0f
        val steps = 60
        repeat(steps) {
            delay((dwellMillis / steps).toLong())
            progress = (it + 1) / steps.toFloat()
        }
        val next = (pager.currentPage + 1) % imageUrls.size
        pager.animateScrollToPage(next, animationSpec = tween(600))
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 12.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // worm-style dots: active page is an elongated pill
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(imageUrls.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .height(6.dp).width(if (active) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(3.dp).clip(RoundedCornerShape(50)),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (action != null) Text(action, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onAction() })
    }
}
