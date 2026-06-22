package com.localkart.customer.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.ui.PillTabRow
import com.localkart.customer.ui.common.MoreScreen
import com.localkart.customer.ui.common.NotificationsScreen
import com.localkart.customer.ui.services.ServicesMiniApp
import com.localkart.customer.ui.stores.StoresMiniApp

/**
 * Premium pill-card header: [More] [Local Stores] [Local Services] with a
 * notification bell in a slim top bar. Stores & Services are full mini-apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoot() {
    var tab by remember { mutableIntStateOf(1) }              // 1 = Stores, 2 = Services
    var overlay by remember { mutableStateOf<String?>(null) } // "more" | "notifications" | null

    val pills = listOf(
        "More" to Icons.Default.MoreHoriz,
        "Local Stores" to Icons.Default.Store,
        "Local Services" to Icons.Default.Build
    )
    val selectedPill = when {
        overlay == "more" -> 0
        tab == 1 -> 1
        else -> 2
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
                Column {
                    // slim brand row + notification bell
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LocalKart", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        BadgedBox(badge = { Badge { Text("3") } }) {
                            IconButton(onClick = { overlay = "notifications" }) {
                                Icon(Icons.Default.Notifications, "Notifications")
                            }
                        }
                    }
                    PillTabRow(tabs = pills, selected = selectedPill) { i ->
                        when (i) {
                            0 -> overlay = "more"
                            1 -> { tab = 1; overlay = null }
                            2 -> { tab = 2; overlay = null }
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
