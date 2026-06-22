package com.localkart.customer.ui.stores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Order
import com.localkart.common.model.OrderStatus
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class OrderHistoryViewModel : ViewModel() {
    private val repo = FirestoreRepo()
    var orders by mutableStateOf<List<Order>>(emptyList()); private set
    var loading by mutableStateOf(false); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true
            val uid = AuthManager.currentUid
            if (uid != null) runCatching { repo.ordersForCustomer(uid) }.onSuccess { orders = it }
            loading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(onBack: () -> Unit, vm: OrderHistoryViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                vm.orders.isEmpty() -> Text("No orders yet", Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.orders, key = { it.id }) { o -> OrderHistoryRow(o) }
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryRow(o: Order) {
    val date = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(o.createdAt.toDate())
    val items = o.items.joinToString(", ") { "${it.name} ×${it.qty}" }.ifBlank { "—" }
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Receipt, null, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Order ${o.id.take(6).uppercase()}", fontWeight = FontWeight.SemiBold)
                Text(items, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${o.total.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                StatusTag(o.status)
            }
        }
    }
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
