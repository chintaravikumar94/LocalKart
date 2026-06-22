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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.common.model.ServiceProvider
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
    var bookingProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    var requestProvider by remember { mutableStateOf<ServiceProvider?>(null) }
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
                SvcTab.HOME -> ServicesHome(onBook = { bookingProvider = it }, onRequest = { requestProvider = it })
                SvcTab.CATEGORY -> ServicesCategory()
                SvcTab.NEARBY -> NearbyProviders(onBook = { bookingProvider = it }, onRequest = { requestProvider = it })
                SvcTab.ACTIVITY -> MyActivity()
            }
        }
    }

    bookingProvider?.let { provider ->
        BookingDialog(provider = provider, onDismiss = { bookingProvider = null })
    }
    requestProvider?.let { provider ->
        ServiceRequestDialog(provider = provider, onDismiss = { requestProvider = null })
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
private fun ServicesHome(
    vm: ServicesViewModel = viewModel(),
    onBook: (ServiceProvider) -> Unit,
    onRequest: (ServiceProvider) -> Unit
) {
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
            else -> items(vm.providers) { p -> ProviderCard(p, onBook = { onBook(p) }, onRequest = { onRequest(p) }) }
        }
    }
}

@Composable
private fun ProviderCard(provider: ServiceProvider, onBook: () -> Unit, onRequest: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (provider.photoUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    provider.photoUrl, provider.name,
                    Modifier.size(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Engineering, null) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.SemiBold)
                Text(provider.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" ${provider.rating} · ${if (provider.pricePerVisit > 0) "₹${provider.pricePerVisit.toInt()}/visit · " else ""}${if (provider.available) "Available" else "Busy"}",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onBook, enabled = provider.available) { Text("Book") }
                TextButton(onClick = onRequest) { Text("Request") }
            }
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
private fun NearbyProviders(
    vm: ServicesViewModel = viewModel(),
    onBook: (ServiceProvider) -> Unit,
    onRequest: (ServiceProvider) -> Unit
) {
    var radius by remember { mutableIntStateOf(15) }
    LazyColumn {
        item { LocationBar("Hyderabad, Madhapur", radius, {}, { radius = it }) }
        item { SearchBar("Search service provider") }
        when {
            vm.loading -> item { LoadingRow() }
            vm.error != null -> item { ErrorRow(vm.error!!) { vm.reload() } }
            vm.providers.isEmpty() -> item { EmptyWithSeed("No providers nearby") { vm.reload() } }
            else -> items(vm.providers) { p -> ProviderCard(p, onBook = { onBook(p) }, onRequest = { onRequest(p) }) }
        }
    }
}

/** My Activity: Service Requests | Bookings | Appointments. Bookings tab is live. */
@Composable
private fun MyActivity(vm: ActivityViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(1) }
    val tabs = listOf("Service Requests", "Bookings", "Appointments")
    LaunchedEffect(tab) { vm.load() }
    Column {
        TabRow(tab) {
            tabs.forEachIndexed { i, t -> Tab(tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> when {
                vm.loading -> LoadingRow()
                vm.requests.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Text("No service requests yet. Tap “Request” on a provider.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn {
                    items(vm.requests) { r ->
                        ListRow(r.title.ifBlank { "Request" },
                            "${r.status.name.lowercase().replaceFirstChar { it.uppercase() }}" +
                                if (r.details.isNotBlank()) " · ${r.details}" else "")
                    }
                }
            }
            1 -> when {
                vm.loading -> LoadingRow()
                vm.bookings.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Text("No bookings yet. Book a service from Home.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn {
                    items(vm.bookings) { b ->
                        ListRow(b.service.ifBlank { "Service" },
                            "${b.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatSlot(b.scheduledAt)}")
                    }
                }
            }
            else -> when {
                vm.loading -> LoadingRow()
                vm.appointments.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Text("No appointments yet. Book one from a store page.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn {
                    items(vm.appointments) { a ->
                        ListRow(a.purpose.ifBlank { "Appointment" },
                            "${a.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatSlot(a.scheduledAt)}")
                    }
                }
            }
        }
    }
}
