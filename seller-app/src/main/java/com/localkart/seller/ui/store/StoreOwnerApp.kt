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
    var overlay by remember { mutableStateOf<String?>(null) }
    Scaffold(bottomBar = {
        NavigationBar {
            OwnerTab.values().forEach { t ->
                NavigationBarItem(sel == t, onClick = { sel = t; overlay = null },
                    icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            if (overlay == "products") {
                ProductsManagerScreen(onBack = { overlay = null })
                return@Box
            }
            if (overlay == "chats") {
                SellerChatsScreen(onBack = { overlay = null })
                return@Box
            }
            when (sel) {
                OwnerTab.HOME -> OwnerHome(
                    onAction = { a ->
                        when (a) {
                            "My Products" -> overlay = "products"
                            "Customer Chats" -> overlay = "chats"
                            "Orders" -> sel = OwnerTab.ORDERS
                            "Service Requests" -> sel = OwnerTab.REQUESTS
                            "Grow" -> sel = OwnerTab.GROW
                            "My Shop" -> sel = OwnerTab.SHOP
                        }
                    }
                )
                OwnerTab.GROW -> GrowYourBusiness()
                OwnerTab.ORDERS -> OrdersScreen()
                OwnerTab.REQUESTS -> RequestsScreen("Service Requests")
                OwnerTab.APPTS -> AppointmentsScreen()
                OwnerTab.SHOP -> SellerProfileScreen()
            }
        }
    }
}

@Composable
private fun OwnerHome(onAction: (String) -> Unit = {}) {
    LazyColumn {
        item { WelcomeHeader("Ravikumar") }
        item { ShopProfileCard("Ravikumar Stores", "Groceries · Service provider available") }
        item { MetricRow("Orders Today" to "14", "Sales" to "₹9,240") }
        item { LiveSellerBanners() }
        item { InfoStrip(listOf(
            "New: enable UPI auto-settlement",
            "Tip: add product photos to sell 2x faster",
            "5 customers viewed your store today")) }
        item {
            QuickActionsGrid(
                listOf(
                    QuickAction("Grow", Icons.Default.TrendingUp),
                    QuickAction("My Products", Icons.Default.Inventory),
                    QuickAction("Orders", Icons.Default.Receipt),
                    QuickAction("Customer Chats", Icons.Default.Chat),
                    QuickAction("Service Requests", Icons.Default.Assignment),
                    QuickAction("My Shop", Icons.Default.Store)
                ),
                onClick = onAction
            )
        }
    }
}
