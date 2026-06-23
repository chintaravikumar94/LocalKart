package com.localkart.common.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localkart.common.model.Banner
import com.localkart.common.model.BannerSettings
import kotlinx.coroutines.delay

private fun resolveCorner(b: Banner, g: BannerSettings): Dp {
    val s = if (b.cornerStyle == "default") g.cornerStyle else b.cornerStyle
    return if (s == "square") 4.dp else 18.dp
}
private fun resolveBorder(b: Banner, g: BannerSettings): Boolean =
    if (b.borderStyle == "default") g.border else b.borderStyle == "on"
private fun resolveHeight(b: Banner, g: BannerSettings): Dp {
    val s = if (b.heightStyle == "default") g.heightStyle else b.heightStyle
    return when (s) { "short" -> 120.dp; "tall" -> 188.dp; else -> 150.dp }
}
private fun resolveTemplate(b: Banner, g: BannerSettings): String =
    if (b.template == "default") g.template else b.template

/** Auto-sliding banner carousel that renders each banner with admin-chosen design. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(banners: List<Banner>, settings: BannerSettings, dwellMillis: Int = 4000) {
    if (banners.isEmpty()) return
    val pager = rememberPagerState { banners.size }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pager.currentPage, banners.size) {
        progress = 0f
        val steps = 60
        repeat(steps) { delay((dwellMillis / steps).toLong()); progress = (it + 1) / steps.toFloat() }
        if (banners.size > 1) pager.animateScrollToPage((pager.currentPage + 1) % banners.size, animationSpec = tween(600))
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
            val b = banners[page]
            val corner = resolveCorner(b, settings)
            val height = resolveHeight(b, settings)
            val template = resolveTemplate(b, settings)
            var box = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(height).clip(RoundedCornerShape(corner))
            Box(box.background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (b.imageUrl.startsWith("http")) {
                    AsyncImage(b.imageUrl, b.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))))
                }
                if (resolveBorder(b, settings)) {
                    Box(Modifier.matchParentSize().border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(corner)))
                }
                if (template == "framed") {
                    Box(Modifier.matchParentSize().padding(7.dp).border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(corner - 4.dp)))
                }
                if (template == "overlay" && b.title.isNotBlank()) {
                    Box(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            .padding(12.dp)
                    ) {
                        Text(b.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(banners.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier.padding(horizontal = 3.dp).height(6.dp).width(if (active) 20.dp else 6.dp)
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
