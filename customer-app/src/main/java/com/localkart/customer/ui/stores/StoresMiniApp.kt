package com.localkart.customer.ui.stores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.localkart.common.model.Store
import com.localkart.customer.ui.common.*

/** Bottom nav for the Local Stores mini-app: Home | Category | Nearby Stores | Account | Cart */
private enum class StoreTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CATEGORY("Category", Icons.Default.GridView),
    NEARBY("Nearby", Icons.Default.NearMe),
    ACCOUNT("Account", Icons.Default.Person),
    CART("Cart", Icons.Default.ShoppingCart)
}

@Composable
fun StoresMiniApp() {
    var sel by remember { mutableStateOf(StoreTab.HOME) }
    var detailStore by remember { mutableStateOf<Store?>(null) }
    Scaffold(
        bottomBar = {
            ProBottomBar(
                items = StoreTab.values().map { it.label to it.icon },
                selected = sel.ordinal,
                badges = if (Cart.count > 0) mapOf(StoreTab.CART.ordinal to Cart.count) else emptyMap()
            ) { i -> sel = StoreTab.values()[i]; detailStore = null }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            val store = detailStore
            if (store != null) {
                StoreDetailScreen(store) { detailStore = null }
            } else when (sel) {
                StoreTab.HOME -> StoresHome(onOpenStore = { detailStore = it })
                StoreTab.CATEGORY -> StoresCategory()
                StoreTab.NEARBY -> NearbyStores()
                StoreTab.ACCOUNT -> AccountScreen()
                StoreTab.CART -> CartScreen()
            }
        }
    }
}

private val storeCategories = listOf(
    "all", "groceries", "mobile_repairing", "fancy", "net_center", "meeseva", "household"
)
private val demoBanners = listOf(
    "https://picsum.photos/seed/lk1/800/400",
    "https://picsum.photos/seed/lk2/800/400",
    "https://picsum.photos/seed/lk3/800/400"
)

@Composable
private fun StoresHome(vm: StoresViewModel = viewModel(), onOpenStore: (Store) -> Unit = {}) {
    var radius by remember { mutableIntStateOf(10) }
    var query by remember { mutableStateOf("") }
    val user = rememberUserLocation()

    // search filter -> attach distance -> radius filter -> sort by distance
    val results = vm.stores
        .filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) }
        .map { s ->
            val d = if (user.hasLocation && s.location != null)
                com.localkart.common.util.LocationUtil.distanceKm(user.lat!!, user.lng!!, s.location!!.latitude, s.location!!.longitude)
            else null
            s to d
        }
        .filter { (_, d) -> d == null || d <= radius }
        .sortedBy { it.second ?: Double.MAX_VALUE }

    LazyColumn {
        item { LocationBar(user.area, radius, {}, { radius = it }) }
        item { SearchField(query, { query = it }, "Search shops & categories") }
        item { CategoryChips(storeCategories, vm.category) { vm.select(it) } }
        item { LiveBannerSlider("customer", demoBanners) }
        item { SectionHeader(if (query.isNotBlank()) "Results for \"$query\""
            else if (vm.category == "all") "Shops within $radius km" else vm.category.replace('_',' ')) }
        when {
            vm.loading -> item { LoadingRow() }
            vm.error != null -> item { ErrorRow(vm.error!!) { vm.reload() } }
            vm.stores.isEmpty() -> item { EmptyWithSeed("No stores nearby") { vm.reload() } }
            results.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(if (query.isNotBlank()) "No matches for \"$query\"" else "No stores within $radius km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> items(results) { (s, d) -> StoreCard(s.name, s.category, s.rating, s.photoUrl, d) { onOpenStore(s) } }
        }
    }
}

@Composable
private fun StoreCard(name: String, category: String, rating: Double = 4.5, photoUrl: String = "", distanceKm: Double? = null, onClick: () -> Unit = {}) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(photoUrl, name, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            } else {
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Storefront, null)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(category.replace('_',' '), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" $rating" + (distanceKm?.let { " · ${"%.1f".format(it)} km" } ?: "") + " · Open",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

/** Flipkart-style category page: left rail of stores, right pane of services + products. */
@Composable
private fun StoresCategory() {
    var selectedStore by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxSize()) {
        // Left rail
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(96.dp).fillMaxHeight()) {
            LazyColumn {
                items((0..9).toList()) { i ->
                    val active = i == selectedStore
                    Column(
                        Modifier.fillMaxWidth().clickablePad { selectedStore = i }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                        ) { Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, null) } }
                        Text("Store $i", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        // Right pane
        LazyColumn(Modifier.weight(1f)) {
            item { SectionHeader("Services") }
            items((1..3).toList()) { ListRow("Service $it", "Tap to view details") }
            item { SectionHeader("Products") }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(360.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items((1..6).toList()) { p -> ProductTile("Product $p", "₹${p * 49}") }
                }
            }
        }
    }
}

@Composable
private fun NearbyStores() {
    var radius by remember { mutableIntStateOf(15) }
    LazyColumn {
        item { LocationBar("Hyderabad, Madhapur", radius, {}, { radius = it }) }
        item { LiveBannerSlider("customer", demoBanners) }
        item { SectionHeader("Categories") }
        item {
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(120.dp)) {
                items(listOf("All","Own Store","Grocery","Pharmacy","Fancy","Repair","Meeseva","More")) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(50), tonalElevation = 3.dp) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Category, null) } }
                        Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        item { SearchBar("Search shop") }
        items((1..10).toList()) { i -> StoreCard("Nearby Store $i", "grocery") }
    }
}

@Composable
private fun AccountScreen() {
    var showOrders by remember { mutableStateOf(false) }
    if (showOrders) {
        OrderHistoryScreen(onBack = { showOrders = false })
        return
    }
    LazyColumn(Modifier.padding(12.dp)) {
        item { ProfileCard() }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                StatCard("Orders", "View", Modifier.weight(1f).clickable { showOrders = true })
                Spacer(Modifier.width(8.dp))
                StatCard("Wishlist", "5", Modifier.weight(1f))
            }
        }
        item { SectionHeader("Recently viewed") }
        items((1..3).toList()) { ListRow("Viewed item $it", "2 days ago") }
        item { SectionHeader("Recently purchased") }
        items((1..3).toList()) { ListRow("Purchased item $it", "Delivered") }
    }
}
