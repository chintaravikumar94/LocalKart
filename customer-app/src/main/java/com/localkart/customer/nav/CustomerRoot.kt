package com.localkart.customer.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localkart.customer.ui.services.ServicesMiniApp
import com.localkart.customer.ui.stores.StoresMiniApp
import com.localkart.customer.ui.common.NotificationsScreen
import com.localkart.customer.ui.common.MoreScreen

/**
 * Common header across the whole customer app:
 *   [More]  Local Stores | Local Services  [Notifications]
 * "Local Stores" and "Local Services" are each a full mini-app with its own bottom nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoot() {
    var tab by remember { mutableIntStateOf(1) }      // 1 = Stores, 2 = Services
    var overlay by remember { mutableStateOf<String?>(null) } // "more" | "notifications" | null

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = { overlay = "more" }) { Icon(Icons.Default.MoreHoriz, "More") }
                    TabRow(selectedTabIndex = tab - 1, modifier = Modifier.weight(1f)) {
                        Tab(tab == 1, onClick = { tab = 1; overlay = null },
                            text = { Text("Local Stores") }, icon = { Icon(Icons.Default.Store, null) })
                        Tab(tab == 2, onClick = { tab = 2; overlay = null },
                            text = { Text("Local Services") }, icon = { Icon(Icons.Default.Build, null) })
                    }
                    BadgedBox(badge = { Badge { Text("3") } }, modifier = Modifier.padding(end = 4.dp)) {
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
                "more" -> MoreScreen()
                "notifications" -> NotificationsScreen()
                else -> if (tab == 1) StoresMiniApp() else ServicesMiniApp()
            }
        }
    }
}
