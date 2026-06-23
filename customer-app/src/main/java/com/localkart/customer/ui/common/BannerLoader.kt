package com.localkart.customer.ui.common

import androidx.compose.runtime.*
import com.localkart.common.model.Banner
import com.localkart.common.model.BannerSettings
import com.localkart.common.repo.FirestoreRepo
import com.localkart.common.ui.BannerCarousel

/**
 * Loads active banners for an audience + the global banner design settings, and
 * renders them with each banner's resolved style. Falls back to [fallback] images.
 */
@Composable
fun LiveBannerSlider(audience: String = "customer", fallback: List<String> = emptyList()) {
    val repo = remember { FirestoreRepo() }
    var banners by remember { mutableStateOf<List<Banner>>(emptyList()) }
    var settings by remember { mutableStateOf(BannerSettings()) }
    LaunchedEffect(audience) {
        runCatching { repo.bannersFor(audience) }.onSuccess { banners = it }
        runCatching { repo.bannerSettings() }.onSuccess { settings = it }
    }
    // Show banners only when the admin has actually added them for this audience.
    if (banners.isNotEmpty()) BannerCarousel(banners, settings)
}
