package com.localkart.customer.ui.stores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Order
import com.localkart.common.model.Product
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import com.localkart.common.ui.ProBottomBar
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
                StoreTab.CATEGORY -> StoresCategory(onOpenStore = { detailStore = it })
                StoreTab.NEARBY -> NearbyStores(onOpenStore = { detailStore = it })
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
        item { LocationBarPro(radius) { radius = it } }
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

/** Flipkart-style category page: left rail of real stores, right pane of their products. */
@Composable
private fun StoresCategory(onOpenStore: (Store) -> Unit = {}) {
    val repo = remember { FirestoreRepo() }
    val ctx = LocalContext.current
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var selected by remember { mutableStateOf<Store?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loadingP by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { repo.storesByCategory("all", onlyApproved = false) }
            .onSuccess { stores = it; if (selected == null) selected = it.firstOrNull() }
    }
    LaunchedEffect(selected?.id) {
        val s = selected ?: return@LaunchedEffect
        loadingP = true
        runCatching { repo.productsForStore(s.id) }.onSuccess { products = it }
        loadingP = false
    }

    Row(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(104.dp).fillMaxHeight()) {
            LazyColumn {
                items(stores, key = { it.id }) { s ->
                    val active = s.id == selected?.id
                    Column(
                        Modifier.fillMaxWidth().clickablePad { selected = s }.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                if (s.photoUrl.isNotBlank())
                                    AsyncImage(s.photoUrl, s.name, Modifier.size(46.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.Storefront, null)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(s.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            val s = selected
            if (s == null) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No stores yet") } }
            } else {
                item {
                    ElevatedCard(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(s.category.replace('_', ' '), style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalButton(onClick = { onOpenStore(s) }) { Text("View") }
                        }
                    }
                }
                item { SectionHeader("Products") }
                when {
                    loadingP -> item { LoadingRow() }
                    products.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                        Text("No products yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else -> items(products.chunked(2)) { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                            row.forEach { p -> Box(Modifier.weight(1f)) { ProductTileLive(p, ctx) } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTileLive(p: Product, ctx: android.content.Context) {
    ElevatedCard(Modifier.padding(6.dp)) {
        Column(Modifier.padding(10.dp)) {
            if (p.imageUrl.isNotBlank())
                AsyncImage(p.imageUrl, p.name, Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            else Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 3.dp) {
                Box(Modifier.fillMaxWidth().height(78.dp), Alignment.Center) { Icon(Icons.Default.Image, null) } }
            Spacer(Modifier.height(6.dp))
            Text(p.name, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text("₹${p.price.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Button(onClick = { Cart.add(p); Toast.makeText(ctx, "Added ${p.name}", Toast.LENGTH_SHORT).show() },
                Modifier.fillMaxWidth(), enabled = p.inStock) { Text(if (p.inStock) "Add" else "Out") }
        }
    }
}

@Composable
private fun NearbyStores(vm: StoresViewModel = viewModel(), onOpenStore: (Store) -> Unit = {}) {
    var radius by remember { mutableIntStateOf(15) }
    val user = rememberUserLocation()
    val results = vm.stores
        .map { s ->
            val d = if (user.hasLocation && s.location != null)
                com.localkart.common.util.LocationUtil.distanceKm(user.lat!!, user.lng!!, s.location!!.latitude, s.location!!.longitude)
            else null
            s to d
        }
        .filter { (_, d) -> d == null || d <= radius }
        .sortedBy { it.second ?: Double.MAX_VALUE }

    LazyColumn {
        item { LocationBarPro(radius) { radius = it } }
        item { LiveBannerSlider("customer", demoBanners) }
        item { SectionHeader("Categories") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(storeCategories) { c ->
                    val active = c == vm.category
                    Column(Modifier.clickablePad { vm.select(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = RoundedCornerShape(50),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Category, null,
                                    tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(c.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
        item { SectionHeader("Stores within $radius km") }
        when {
            vm.loading -> item { LoadingRow() }
            vm.stores.isEmpty() -> item { EmptyWithSeed("No stores nearby") { vm.reload() } }
            results.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No stores within $radius km", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            else -> items(results) { (s, d) -> StoreCard(s.name, s.category, s.rating, s.photoUrl, d) { onOpenStore(s) } }
        }
    }
}

@Composable
private fun AccountScreen() {
    var showOrders by remember { mutableStateOf(false) }
    if (showOrders) {
        OrderHistoryScreen(onBack = { showOrders = false })
        return
    }
    val repo = remember { FirestoreRepo() }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    LaunchedEffect(Unit) {
        val uid = AuthManager.currentUid
        if (uid != null) runCatching { repo.ordersForCustomer(uid) }.onSuccess { orders = it }
    }
    val spent = orders.sumOf { it.total }
    LazyColumn(Modifier.padding(12.dp)) {
        item { ProfileCard() }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                StatCard("Orders", "${orders.size}", Modifier.weight(1f).clickable { showOrders = true })
                Spacer(Modifier.width(8.dp))
                StatCard("Total spent", "₹${spent.toInt()}", Modifier.weight(1f))
            }
        }
        item { SectionHeader("Recent orders", action = if (orders.isNotEmpty()) "View all" else null) { showOrders = true } }
        if (orders.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                Text("No orders yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else items(orders.take(5)) { o ->
            ListRow("Order ${o.id.take(6).uppercase()}",
                "₹${o.total.toInt()} · ${o.status.name.lowercase().replaceFirstChar { it.uppercase() }}")
        }
    }
}
