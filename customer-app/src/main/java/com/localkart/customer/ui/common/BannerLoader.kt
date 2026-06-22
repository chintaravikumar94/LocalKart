package com.localkart.customer.ui.common

import androidx.compose.runtime.*
import com.localkart.common.repo.FirestoreRepo

/**
 * Loads active banners for an audience from Firestore and shows them in the
 * pro BannerSlider. Falls back to [fallback] images if none are configured yet.
 */
@Composable
fun LiveBannerSlider(audience: String = "customer", fallback: List<String> = emptyList()) {
    val repo = remember { FirestoreRepo() }
    var urls by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(audience) {
        runCatching { repo.bannersFor(audience) }
            .onSuccess { list -> urls = list.mapNotNull { it.imageUrl.ifBlank { null } } }
    }
    val show = if (urls.isNotEmpty()) urls else fallback
    if (show.isNotEmpty()) BannerSlider(show)
}
