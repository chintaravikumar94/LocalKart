package com.localkart.seller.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.model.UserRole
import com.localkart.common.ui.PillTabRow
import com.localkart.seller.ui.common.SellerMore
import com.localkart.seller.ui.common.SellerNotifications
import com.localkart.seller.ui.provider.ServiceProviderApp
import com.localkart.seller.ui.store.StoreOwnerApp

/**
 * Premium pill-card header for the seller app. Tabs shown depend on the signup role:
 * [More] [Store Owner] / [Service Provider], plus a notification bell.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRoot(role: UserRole) {
    val showStore = role == UserRole.STORE_OWNER || role == UserRole.STORE_AND_PROVIDER
    val showProvider = role == UserRole.SERVICE_PROVIDER || role == UserRole.STORE_AND_PROVIDER

    // Build the pill set dynamically based on role.
    val pills = buildList<Triple<String, String, ImageVector>> {
        add(Triple("more", "More", Icons.Default.MoreHoriz))
        if (showStore) add(Triple("store", "Store Owner", Icons.Default.Storefront))
        if (showProvider) add(Triple("provider", "Service Provider", Icons.Default.Engineering))
    }
    val keys = pills.map { it.first }

    var current by remember { mutableStateOf(if (showStore) "store" else "provider") }
    var overlay by remember { mutableStateOf<String?>(null) } // "more" | "notifications" | null

    val selectedKey = if (overlay == "more") "more" else current
    val selectedPill = keys.indexOf(selectedKey).coerceAtLeast(0)

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LocalKart Seller", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        BadgedBox(badge = { Badge { Text("2") } }) {
                            IconButton(onClick = { overlay = "notifications" }) {
                                Icon(Icons.Default.Notifications, "Notifications")
                            }
                        }
                    }
                    PillTabRow(tabs = pills.map { it.second to it.third }, selected = selectedPill) { i ->
                        val key = keys[i]
                        if (key == "more") overlay = "more" else { current = key; overlay = null }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (overlay) {
                "more" -> SellerMore()
                "notifications" -> SellerNotifications()
                else -> if (current == "store") StoreOwnerApp() else ServiceProviderApp()
            }
        }
    }
}
