package com.localkart.seller.ui.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.localkart.seller.ui.common.*

/** Bottom nav: Home | Grow your Business | Job Requests | Bookings | My Profile */
private enum class ProviderTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    GROW("Grow", Icons.Default.TrendingUp),
    JOBS("Jobs", Icons.Default.Work),
    BOOKINGS("Bookings", Icons.Default.EventAvailable),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun ServiceProviderApp() {
    var sel by remember { mutableStateOf(ProviderTab.HOME) }
    Scaffold(bottomBar = {
        NavigationBar {
            ProviderTab.values().forEach { t ->
                NavigationBarItem(sel == t, onClick = { sel = t },
                    icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad)) {
            when (sel) {
                ProviderTab.HOME -> ProviderHome()
                ProviderTab.GROW -> GrowYourBusiness()
                ProviderTab.JOBS -> StatListPage("Job Requests",
                    listOf("Total" to "57", "New" to "6", "Active" to "11", "Done" to "40"),
                    listOf("All", "New", "Active", "Done", "Declined")) { "Job detail & location" }
                ProviderTab.BOOKINGS -> ProviderBookingsScreen()
                ProviderTab.PROFILE -> ProfileWithIdCard("SVP-55821", "Ravikumar (Electrician)", approved = true, rating = 4.8)
            }
        }
    }
}

@Composable
private fun ProviderHome() {
    LazyColumn {
        item { WelcomeHeader("Ravikumar") }
        item { ShopProfileCard("Ravikumar Electrical", "Electrician") }
        item { MetricRow("New Requests" to "6", "Upcoming Bookings" to "3") }
        item { SellerBanners(listOf("a","b","c")) }
        item { InfoStrip(listOf(
            "Respond fast to rank higher in search",
            "Add your skills to get more jobs",
            "3 new job requests near you")) }
        item {
            QuickActionsGrid(listOf(
                QuickAction("Grow", Icons.Default.TrendingUp),
                QuickAction("Job Requests", Icons.Default.Work),
                QuickAction("Bookings", Icons.Default.EventAvailable),
                QuickAction("My Profile", Icons.Default.Person)
            ))
        }
    }
}
