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
import com.localkart.common.ui.HeaderPill
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

    var current by remember { mutableStateOf(if (showStore) "store" else "provider") }
    var overlay by remember { mutableStateOf<String?>(null) } // "more" | "notifications" | null
    var unread by remember { mutableIntStateOf(0) }
    LaunchedEffect(overlay) {
        val uid = com.localkart.common.auth.AuthManager.currentUid
        if (uid != null) runCatching { com.localkart.common.repo.FirestoreRepo().notificationsFor(uid) }
            .onSuccess { list -> unread = list.count { !it.read } }
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderPill("More", Icons.Default.MoreHoriz, overlay == "more", Modifier.width(64.dp)) {
                        overlay = "more"
                    }
                    if (showStore) HeaderPill("Store Owner", Icons.Default.Storefront,
                        current == "store" && overlay == null, Modifier.weight(1f)) { current = "store"; overlay = null }
                    if (showProvider) HeaderPill("Service Provider", Icons.Default.Engineering,
                        current == "provider" && overlay == null, Modifier.weight(1f)) { current = "provider"; overlay = null }
                    HeaderPill("Alerts", Icons.Default.Notifications, overlay == "notifications",
                        Modifier.width(64.dp), badge = unread) { overlay = "notifications" }
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
