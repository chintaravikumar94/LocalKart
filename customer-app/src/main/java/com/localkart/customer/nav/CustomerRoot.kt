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
import com.localkart.common.ui.HeaderPill
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
                    HeaderPill("Local Stores", Icons.Default.Store, tab == 1 && overlay == null, Modifier.weight(1f)) {
                        tab = 1; overlay = null
                    }
                    HeaderPill("Local Services", Icons.Default.Build, tab == 2 && overlay == null, Modifier.weight(1f)) {
                        tab = 2; overlay = null
                    }
                    HeaderPill("Alerts", Icons.Default.Notifications, overlay == "notifications", Modifier.width(64.dp), badge = unread) {
                        overlay = "notifications"
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
