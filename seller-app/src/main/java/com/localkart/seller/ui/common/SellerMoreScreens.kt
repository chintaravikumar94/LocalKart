package com.localkart.seller.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Order
import com.localkart.common.model.OrderStatus
import com.localkart.common.repo.FirestoreRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerSubScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { pad -> content(pad) }
}

@Composable
fun PayoutsScreen(onBack: () -> Unit) {
    val repo = remember { FirestoreRepo() }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val uid = AuthManager.currentUid
        if (uid != null) runCatching { repo.ordersForOwner(uid) }.onSuccess { orders = it }
        loading = false
    }
    val earned = orders.filter { it.status == OrderStatus.COMPLETED }.sumOf { it.total }
    val pending = orders.filter { it.status == OrderStatus.PENDING || it.status == OrderStatus.ACTIVE }.sumOf { it.total }
    val completedCount = orders.count { it.status == OrderStatus.COMPLETED }

    SellerSubScreen("Payouts", onBack) { pad ->
        if (loading) {
            Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@SellerSubScreen
        }
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Total earnings", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium)
                        Text("₹${earned.toInt()}", color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                        Text("From $completedCount completed orders", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Row(Modifier.fillMaxWidth()) {
                    PayoutStat("Pending", "₹${pending.toInt()}", Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    PayoutStat("Orders", "${orders.size}", Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                ListItem(headlineContent = { Text("Bank account") },
                    supportingContent = { Text("Add a bank account to receive payouts") },
                    leadingContent = { Icon(Icons.Default.AccountBalance, null) },
                    trailingContent = { TextButton(onClick = {}) { Text("Add") } })
                HorizontalDivider()
                ListItem(headlineContent = { Text("Payout schedule") },
                    supportingContent = { Text("Weekly · every Monday") },
                    leadingContent = { Icon(Icons.Default.Schedule, null) })
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Payouts are settled to your linked bank account after order completion.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PayoutStat(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SellerSupportScreen(onBack: () -> Unit) {
    SellerSubScreen("Help & Support", onBack) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Seller support", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text("partners@localkart.app")
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text("1800-456-7890 (9am–9pm)")
                        }
                    }
                }
            }
            items(
                listOf(
                    "How do I get approved?" to "Register your store/service; our admin reviews and approves it.",
                    "How do I add products?" to "Store Owner → My Products → Add product (with photo).",
                    "When do I get paid?" to "Payouts settle weekly after orders are completed.",
                    "How do I accept orders?" to "Open Orders → Accept, then mark Complete when done."
                )
            ) { (q, a) ->
                var open by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(q) },
                    supportingContent = { if (open) Text(a) },
                    trailingContent = { Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) },
                    modifier = Modifier.clickable { open = !open }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit) {
    SellerSubScreen("Terms & Policies", onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Seller Terms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "By selling on LocalKart you agree to provide accurate listings, honour orders and " +
                    "bookings you accept, maintain quality and timely service, and comply with local laws. " +
                    "LocalKart may suspend accounts that violate these terms. Commission and payout terms " +
                    "apply as communicated in the app. Customer data must be used only to fulfil orders.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Full terms available at localkart.app/terms", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
