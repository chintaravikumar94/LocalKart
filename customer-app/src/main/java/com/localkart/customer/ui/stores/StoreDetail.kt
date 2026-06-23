package com.localkart.customer.ui.stores

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.model.Product
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import com.localkart.customer.ui.common.ErrorRow
import com.localkart.customer.ui.common.LoadingRow

/**
 * Store detail page: header card + products grid pulled live from Firestore.
 * Back arrow returns to the previous screen via [onBack].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(store: Store, onBack: () -> Unit) {
    var screen by remember { mutableStateOf("detail") }
    if (screen == "reviews") { ReviewsScreen(store) { screen = "detail" }; return }
    if (screen == "chat") { CustomerChatScreen(store) { screen = "detail" }; return }

    val repo = remember { FirestoreRepo() }
    val ctx = LocalContext.current
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store.id) {
        loading = true; error = null
        runCatching { repo.approvedProductsForStore(store.id) }
            .onSuccess { products = it }
            .onFailure { error = it.message ?: "Failed to load products" }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(store.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { screen = "chat" }) { Icon(Icons.Default.ChatBubbleOutline, "Chat") }
                    IconButton(onClick = { screen = "reviews" }) { Icon(Icons.Default.Star, "Reviews") }
                }
            )
        }
    ) { pad ->
        var showAppt by remember { mutableStateOf(false) }
        Column(Modifier.padding(pad)) {
            StoreHeader(store)
            OutlinedButton(
                onClick = { showAppt = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Event, null); Spacer(Modifier.width(8.dp)); Text("Book appointment")
            }
            HorizontalDivider(Modifier.padding(top = 12.dp))
            Text(
                "Products",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            when {
                loading -> LoadingRow()
                error != null -> ErrorRow(error!!) { }
                products.isEmpty() -> Box(
                    Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center
                ) { Text("No products listed yet", style = MaterialTheme.typography.bodyMedium) }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(products) { p ->
                        ProductCard(p) {
                            com.localkart.customer.ui.common.Cart.add(p)
                            Toast.makeText(ctx, "Added ${p.name} to cart", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        if (showAppt) {
            AppointmentDialog(store = store, onDismiss = { showAppt = false })
        }
    }
}

@Composable
private fun StoreHeader(store: Store) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (store.photoUrl.isNotBlank()) {
            coil.compose.AsyncImage(
                store.photoUrl, store.name,
                Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(36.dp))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(store.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(store.category.replace('_', ' '), style = MaterialTheme.typography.bodyMedium)
            if (store.address.isNotBlank())
                Text(store.address, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Text(" ${store.rating} (${store.ratingCount}) · ${if (store.isOpen) "Open now" else "Closed"}",
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ProductCard(p: Product, onAdd: () -> Unit) {
    ElevatedCard(Modifier.padding(6.dp)) {
        Column(Modifier.padding(10.dp)) {
            if (p.imageUrl.isNotBlank()) {
                coil.compose.AsyncImage(
                    p.imageUrl, p.name,
                    Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 3.dp) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(p.name, maxLines = 1, fontWeight = FontWeight.SemiBold)
            if (p.unit.isNotBlank())
                Text(p.unit, style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹${p.price.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (p.mrp > p.price) {
                    Spacer(Modifier.width(6.dp))
                    Text("₹${p.mrp.toInt()}", style = MaterialTheme.typography.labelSmall,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (p.mrp > p.price) {
                val save = (p.mrp - p.price).toInt()
                val pct = ((p.mrp - p.price) / p.mrp * 100).toInt()
                Text("You save ₹$save ($pct% off)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onAdd, Modifier.fillMaxWidth(), enabled = p.inStock) {
                Text(if (p.inStock) "Add" else "Out of stock")
            }
        }
    }
}
