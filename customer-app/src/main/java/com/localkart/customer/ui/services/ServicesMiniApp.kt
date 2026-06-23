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
import com.localkart.common.ui.ProBottomBar
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
            ProBottomBar(
                items = SvcTab.values().map { it.label to it.icon },
                selected = sel.ordinal
            ) { i -> sel = SvcTab.values()[i] }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (sel) {
                SvcTab.HOME -> ServicesHome(onBook = { bookingProvider = it }, onRequest = { requestProvider = it })
                SvcTab.CATEGORY -> ServicesCategory(onBook = { bookingProvider = it }, onRequest = { requestProvider = it })
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
    var query by remember { mutableStateOf("") }
    val user = rememberUserLocation()

    val results = vm.providers
        .filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) }
        .map { p ->
            val d = if (user.hasLocation && p.location != null)
                com.localkart.common.util.LocationUtil.distanceKm(user.lat!!, user.lng!!, p.location!!.latitude, p.location!!.longitude)
            else null
            p to d
        }
        .filter { (_, d) -> d == null || d <= radius }
        .sortedBy { it.second ?: Double.MAX_VALUE }

    LazyColumn {
        item { LocationBarPro(radius) { radius = it } }
        item { SearchField(query, { query = it }, "Search services & providers") }
        item { CategoryChips(serviceCategories, vm.category) { vm.select(it) } }
        item { LiveBannerSlider("customer", demoBanners) }
        item { SectionHeader(if (query.isNotBlank()) "Results for \"$query\""
            else "Providers within $radius km") }
        when {
            vm.loading -> item { LoadingRow() }
            vm.error != null -> item { ErrorRow(vm.error!!) { vm.reload() } }
            vm.providers.isEmpty() -> item { EmptyWithSeed("No service providers nearby") { vm.reload() } }
            results.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(if (query.isNotBlank()) "No matches for \"$query\"" else "No providers within $radius km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> items(results) { (p, d) -> ProviderCard(p, d, onBook = { onBook(p) }, onRequest = { onRequest(p) }) }
        }
    }
}

@Composable
private fun ProviderCard(provider: ServiceProvider, distanceKm: Double? = null, onBook: () -> Unit, onRequest: () -> Unit) {
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
                    Text(" ${provider.rating}" + (distanceKm?.let { " · ${"%.1f".format(it)} km" } ?: "") +
                        " · ${if (provider.pricePerVisit > 0) "₹${provider.pricePerVisit.toInt()}/visit · " else ""}${if (provider.available) "Available" else "Busy"}",
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

/** Flipkart-style: left rail of categories, right pane of real providers (book/request). */
@Composable
private fun ServicesCategory(onBook: (ServiceProvider) -> Unit, onRequest: (ServiceProvider) -> Unit) {
    val repo = remember { com.localkart.common.repo.FirestoreRepo() }
    var providers by remember { mutableStateOf<List<ServiceProvider>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        runCatching { repo.providersByCategory("all", onlyApproved = false) }
            .onSuccess { providers = it }
        loading = false
    }
    val cat = serviceCategories[selected]
    val filtered = if (cat == "all") providers else providers.filter { it.category == cat }

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
                                Icon(Icons.Default.Engineering, null,
                                    tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) } }
                        Text(c.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            item { SectionHeader("${cat.replaceFirstChar { it.uppercase() }} ${if (cat == "all") "Providers" else "Services"}") }
            when {
                loading -> item { LoadingRow() }
                filtered.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                    Text("No providers here yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else -> items(filtered) { p -> ProviderCard(p, onBook = { onBook(p) }, onRequest = { onRequest(p) }) }
            }
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
