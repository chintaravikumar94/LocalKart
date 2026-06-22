package com.localkart.seller.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.common.model.Order
import com.localkart.common.model.OrderStatus

/** Seller Orders page wired to the live `orders` collection. */
@Composable
fun OrdersScreen(vm: SellerOrdersViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Active", "Completed")

    val all = vm.orders
    val revenue = all.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.total }
    val pending = all.count { it.status == OrderStatus.PENDING }
    val done = all.count { it.status == OrderStatus.COMPLETED }

    val shown = all.filter { o ->
        (filter == "All" || o.status.name.equals(filter, true)) &&
        (query.isBlank() || o.id.contains(query, true) ||
            o.items.any { it.name.contains(query, true) })
    }

    Column(Modifier.fillMaxSize()) {
        Text("Orders", Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                "Orders" to all.size.toString(),
                "Revenue" to "₹${revenue.toInt()}",
                "Pending" to pending.toString(),
                "Done" to done.toString()
            )) { (l, v) -> MetricChip(l, v) }
        }

        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search by item or order id") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f -> FilterChip(f == filter, onClick = { filter = f }, label = { Text(f) }) }
        }

        when {
            vm.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() }
            vm.error != null -> Column(Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load orders"); Text(vm.error!!, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp)); Button(onClick = { vm.load() }) { Text("Retry") }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No orders yet", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { o -> OrderRow(o) { vm.advance(o) } }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    ElevatedCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun OrderRow(o: Order, onAdvance: () -> Unit) {
    val itemsSummary = o.items.joinToString(", ") { "${it.name} ×${it.qty}" }.ifBlank { "—" }
    ListItem(
        headlineContent = { Text("₹${o.total.toInt()} · ${o.items.sumOf { it.qty }} items") },
        supportingContent = { Text(itemsSummary, maxLines = 2) },
        overlineContent = { Text("Order ${o.id.take(6).uppercase()}") },
        leadingContent = { Icon(Icons.Default.Receipt, null) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                StatusTag(o.status)
                if (o.status == OrderStatus.PENDING || o.status == OrderStatus.ACTIVE) {
                    TextButton(onClick = onAdvance) {
                        Text(if (o.status == OrderStatus.PENDING) "Accept" else "Complete")
                    }
                }
            }
        }
    )
    HorizontalDivider()
}

@Composable
private fun StatusTag(status: OrderStatus) {
    val (label, color) = when (status) {
        OrderStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
        OrderStatus.ACTIVE -> "Active" to MaterialTheme.colorScheme.primary
        OrderStatus.COMPLETED -> "Done" to MaterialTheme.colorScheme.tertiary
        OrderStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
