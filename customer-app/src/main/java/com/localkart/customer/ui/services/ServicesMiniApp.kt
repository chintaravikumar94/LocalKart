package com.localkart.customer.ui.services

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.customer.ui.common.*

/** Bottom nav: Home | Category | Nearby Service Providers | My Activity */
private enum class SvcTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CATEGORY("Category", Icons.Default.GridView),
    NEARBY("Nearby", Icons.Default.NearMe),
    ACTIVITY("My Activity", Icons.Default.EventNote)
}

@Composable
fun ServicesMiniApp() {
    var sel by remember { mutableStateOf(SvcTab.HOME) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                SvcTab.values().forEach { t ->
                    NavigationBarItem(sel == t, onClick = { sel = t },
                        icon = { Icon(t.icon, t.label) }, label = { Text(t.label) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (sel) {
                SvcTab.HOME -> ServicesHome()
                SvcTab.CATEGORY -> ServicesCategory()
                SvcTab.NEARBY -> NearbyProviders()
                SvcTab.ACTIVITY -> MyActivity()
            }
        }
    }
}

private val serviceCategories = listOf(
    "all", "plumber", "electrician", "carpenter", "gardener", "mechanic", "housekeeping", "cook"
)
private val demoBanners = listOf(
    "https://picsum.photos/seed/svc1/800/400",
    "https://picsum.photos/seed/svc2/800/400",
    "https://picsum.photos/seed/svc3/800/400"
)

@Composable
private fun ServicesHome(vm: ServicesViewModel = viewModel()) {
    var radius by remember { mutableIntStateOf(10) }
    LazyColumn {
        item { LocationBar("Hyderabad, Madhapur", radius, {}, { radius = it }) }
        item { SearchBar("Search service") }
        item { CategoryChips(serviceCategories, vm.category) { vm.select(it) } }
        item { BannerSlider(demoBanners) }
        item { SectionHeader("Nearest ${if (vm.category=="all") "Providers" else vm.category.replaceFirstChar { it.uppercase() }}") }
        when {
            vm.loading -> item { LoadingRow() }
            vm.error != null -> item { ErrorRow(vm.error!!) { vm.reload() } }
            vm.providers.isEmpty() -> item { EmptyWithSeed("No service providers nearby") { vm.reload() } }
            else -> items(vm.providers) { p -> ProviderCard(p.name, p.category, p.rating) }
        }
    }
}

@Composable
private fun ProviderCard(name: String, category: String, rating: Double = 4.5) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Engineering, null) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" $rating · 2.1 km · Available", style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(onClick = {}) { Text("Book") }
        }
    }
}

/** Flipkart-style: left rail of service categories, right pane of services. */
@Composable
private fun ServicesCategory() {
    var selected by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(96.dp).fillMaxHeight()) {
            LazyColumn {
                items(serviceCategories) { c ->
                    val idx = serviceCategories.indexOf(c)
                    val active = idx == selected
                    Column(
                        Modifier.fillMaxWidth().clickablePad { selected = idx }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Engineering, null) } }
                        Text(c.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            item { SectionHeader("${serviceCategories[selected].replaceFirstChar { it.uppercase() }} Services") }
            items((1..6).toList()) { ListRow("Service $it", "Tap to view details & book") }
        }
    }
}

@Composable
private fun NearbyProviders() {
    var radius by remember { mutableIntStateOf(15) }
    LazyColumn {
        item { LocationBar("Hyderabad, Madhapur", radius, {}, { radius = it }) }
        item { SearchBar("Search service provider") }
        items((1..10).toList()) { i -> ProviderCard("Nearby Provider $i", "electrician") }
    }
}

/** My Activity: Service Requests | Bookings | Appointments */
@Composable
private fun MyActivity() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Service Requests", "Bookings", "Appointments")
    Column {
        TabRow(tab) {
            tabs.forEachIndexed { i, t -> Tab(tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        LazyColumn {
            items((1..6).toList()) { i ->
                ListRow("${tabs[tab].dropLast(1)} #$i", when (tab) {
                    0 -> "Status: In progress"
                    1 -> "Confirmed · Tomorrow 10 AM"
                    else -> "Confirmed · 24 Jun, 4 PM"
                })
            }
        }
    }
}
