package com.localkart.seller.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localkart.common.model.UserRole
import com.localkart.seller.ui.store.StoreOwnerApp
import com.localkart.seller.ui.provider.ServiceProviderApp
import com.localkart.seller.ui.common.SellerMore
import com.localkart.seller.ui.common.SellerNotifications

/**
 * Common header for the seller app:
 *   [More]  Store Owner | Service Provider  [Notifications]
 * If the user signed up only as a Store Owner, only that tab shows; if as both,
 * two tabs show on top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRoot(role: UserRole) {
    val showStore = role == UserRole.STORE_OWNER || role == UserRole.STORE_AND_PROVIDER
    val showProvider = role == UserRole.SERVICE_PROVIDER || role == UserRole.STORE_AND_PROVIDER
    var tab by remember { mutableIntStateOf(if (showStore) 0 else 1) }
    var overlay by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { overlay = "more" }) { Icon(Icons.Default.MoreHoriz, "More") }
                    if (showStore && showProvider) {
                        TabRow(selectedTabIndex = tab, modifier = Modifier.weight(1f)) {
                            Tab(tab == 0, onClick = { tab = 0; overlay = null },
                                text = { Text("Store Owner") }, icon = { Icon(Icons.Default.Storefront, null) })
                            Tab(tab == 1, onClick = { tab = 1; overlay = null },
                                text = { Text("Service Provider") }, icon = { Icon(Icons.Default.Engineering, null) })
                        }
                    } else {
                        Text(
                            if (showStore) "Store Owner" else "Service Provider",
                            Modifier.weight(1f).padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    BadgedBox(badge = { Badge { Text("2") } }, modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { overlay = "notifications" }) {
                            Icon(Icons.Default.Notifications, "Notifications")
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (overlay) {
                "more" -> SellerMore()
                "notifications" -> SellerNotifications()
                else -> if (tab == 0 && showStore) StoreOwnerApp() else ServiceProviderApp()
            }
        }
    }
}
