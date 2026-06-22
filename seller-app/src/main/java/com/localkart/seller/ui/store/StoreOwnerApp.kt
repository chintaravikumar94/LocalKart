package com.localkart.seller.ui.store

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.localkart.seller.ui.common.*

/** Bottom nav: Home | Grow your business | Orders | Requests | Appointments | My shop */
private enum class OwnerTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    GROW("Grow", Icons.Default.TrendingUp),
    ORDERS("Orders", Icons.Default.Receipt),
    REQUESTS("Requests", Icons.Default.Assignment),
    APPTS("Appts", Icons.Default.Event),
    SHOP("My Shop", Icons.Default.Store)
}

@Composable
fun StoreOwnerApp() {
    var sel by remember { mutableStateOf(OwnerTab.HOME) }
    Scaffold(bottomBar = {
        NavigationBar {
            OwnerTab.values().forEach { t ->
                NavigationBarItem(sel == t, onClick = { sel = t },
                    icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            when (sel) {
                OwnerTab.HOME -> OwnerHome()
                OwnerTab.GROW -> GrowYourBusiness()
                OwnerTab.ORDERS -> OrdersScreen()
                OwnerTab.REQUESTS -> StatListPage("Service Requests",
                    listOf("Total" to "42", "New" to "5", "Active" to "9", "Done" to "28"),
                    listOf("All", "New", "In progress", "Done", "Rejected")) { "Requested service detail" }
                OwnerTab.APPTS -> StatListPage("Appointments",
                    listOf("Total" to "30", "New" to "4", "Confirmed" to "12", "Done" to "14"),
                    listOf("All", "New", "Confirmed", "Done", "Cancelled")) { "Scheduled slot" }
                OwnerTab.SHOP -> ProfileWithIdCard("STR-10293", "Ravikumar Stores", approved = true, rating = 4.6)
            }
        }
    }
}

@Composable
private fun OwnerHome() {
    LazyColumn {
        item { WelcomeHeader("Ravikumar") }
        item { ShopProfileCard("Ravikumar Stores", "Groceries · Service provider available") }
        item { MetricRow("Orders Today" to "14", "Sales" to "₹9,240") }
        item { SellerBanners(listOf("a","b","c")) }
        item { InfoStrip(listOf(
            "New: enable UPI auto-settlement",
            "Tip: add product photos to sell 2x faster",
            "5 customers viewed your store today")) }
        item {
            QuickActionsGrid(listOf(
                QuickAction("Grow", Icons.Default.TrendingUp),
                QuickAction("My Products", Icons.Default.Inventory),
                QuickAction("Orders", Icons.Default.Receipt),
                QuickAction("Customer Chats", Icons.Default.Chat),
                QuickAction("Service Requests", Icons.Default.Assignment),
                QuickAction("My Shop", Icons.Default.Store)
            ))
        }
    }
}
