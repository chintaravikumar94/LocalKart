package com.localkart.seller.ui.common

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.ui.QrImage
import kotlinx.coroutines.delay

@Composable
fun WelcomeHeader(name: String) {
    Column(Modifier.padding(16.dp)) {
        Text("Welcome back,", style = MaterialTheme.typography.bodyMedium)
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

/** Shop profile card with an Available on/off toggle. */
@Composable
fun ShopProfileCard(shopName: String, category: String) {
    var available by remember { mutableStateOf(true) }
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Storefront, null) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shopName, fontWeight = FontWeight.Bold)
                Text(category, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (available) "Available" else "Offline", style = MaterialTheme.typography.labelSmall)
                Switch(checked = available, onCheckedChange = { available = it })
            }
        }
    }
}

@Composable
fun MetricRow(left: Pair<String, String>, right: Pair<String, String>) {
    Row(Modifier.fillMaxWidth().padding(12.dp)) {
        MetricCard(left.first, left.second, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        MetricCard(right.first, right.second, Modifier.weight(1f))
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** PhonePe-style auto-sliding info strip with built-in progress bar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoStrip(messages: List<String>, dwellMillis: Int = 3500) {
    if (messages.isEmpty()) return
    val pager = rememberPagerState { messages.size }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(pager.currentPage) {
        progress = 0f
        val steps = 50
        repeat(steps) { delay((dwellMillis / steps).toLong()); progress = (it + 1) / steps.toFloat() }
        pager.animateScrollToPage((pager.currentPage + 1) % messages.size, animationSpec = tween(500))
    }
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp))
    ) {
        Column {
            HorizontalPager(pager, modifier = Modifier.fillMaxWidth().height(44.dp)) { p ->
                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(messages[p], maxLines = 1)
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

/** Banner pager identical in spirit to the customer one. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SellerBanners(urls: List<String>, dwellMillis: Int = 4000) {
    if (urls.isEmpty()) return
    val pager = rememberPagerState { urls.size }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(pager.currentPage) {
        progress = 0f
        val steps = 60
        repeat(steps) { delay((dwellMillis / steps).toLong()); progress = (it + 1) / steps.toFloat() }
        pager.animateScrollToPage((pager.currentPage + 1) % urls.size, animationSpec = tween(600))
    }
    Column(Modifier.padding(vertical = 8.dp)) {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth().height(140.dp)) { _ ->
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))))
        }
        LinearProgressIndicator(progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(3.dp))
    }
}

data class QuickAction(val label: String, val icon: ImageVector)

@Composable
fun QuickActionsGrid(actions: List<QuickAction>, onClick: (String) -> Unit = {}) {
    Text("Quick Actions", Modifier.padding(start = 16.dp, top = 8.dp),
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.heightIn(max = 220.dp),
        contentPadding = PaddingValues(8.dp)) {
        items(actions) { a ->
            Column(Modifier.padding(8.dp).clickable { onClick(a.label) },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 3.dp) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(a.icon, a.label) }
                }
                Spacer(Modifier.height(4.dp))
                Text(a.label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }
    }
}

/** "Grow your business" page: admin-added promo cards. */
@Composable
fun GrowYourBusiness() {
    LazyColumn {
        item {
            Text("Grow Your Business", Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        items((1..6).toList()) { i ->
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.TrendingUp, null) } }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Growth program $i", fontWeight = FontWeight.SemiBold)
                        Text("Boost visibility, get more customers. Easy steps.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = {}) { Text("Start") }
                }
            }
        }
    }
}

/**
 * Generic stat-tabs + search + filter-chip list page. Used for Orders, Requests,
 * Appointments, Job Requests and Bookings (they share the same shape).
 */
@Composable
fun StatListPage(
    title: String,
    metrics: List<Pair<String, String>>,
    filters: List<String>,
    rowSubtitle: (Int) -> String
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(filters.first()) }
    Column(Modifier.fillMaxSize()) {
        Text(title, Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(metrics) { (l, v) -> MetricChip(l, v) }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f -> FilterChip(f == filter, onClick = { filter = f }, label = { Text(f) }) }
        }
        LazyColumn(Modifier.weight(1f)) {
            items((1..10).toList()) { i ->
                ListItem(
                    headlineContent = { Text("$title #$i") },
                    supportingContent = { Text(rowSubtitle(i)) },
                    leadingContent = { Icon(Icons.Default.Receipt, null) },
                    trailingContent = { AssistChip(onClick = {}, label = { Text(filter) }) },
                    modifier = Modifier.clickable { }
                )
                HorizontalDivider()
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

/** My Shop / My Profile page with editable card + dynamic scannable QR ID card. */
@Composable
fun ProfileWithIdCard(sellerId: String, name: String, approved: Boolean, rating: Double) {
    LazyColumn(Modifier.padding(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null) } }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (approved) Icons.Default.Verified else Icons.Default.HourglassEmpty,
                                null, Modifier.size(16.dp),
                                tint = if (approved) Color(0xFF16A34A) else MaterialTheme.colorScheme.secondary)
                            Text(if (approved) " Approved" else " Pending approval", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(" $rating", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(Icons.Default.Edit, null)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            // Pro-level ID card with dynamic QR a customer can scan.
            Surface(shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LocalKart Partner ID", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    QrImage("localkart://partner/$sellerId", Modifier.size(160.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(name, fontWeight = FontWeight.SemiBold)
                    Text("ID: $sellerId", style = MaterialTheme.typography.labelSmall)
                    Text("Scan to view profile & book", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item { ListItem(headlineContent = { Text("Support") }, leadingContent = { Icon(Icons.Default.Help, null) }) }
        item { ListItem(headlineContent = { Text("Contact") }, leadingContent = { Icon(Icons.Default.Call, null) }) }
        item { SellerLogoutItem() }
    }
}

/** Logout row with a confirm dialog; signs out via AuthManager (MainActivity returns to login). */
@Composable
fun SellerLogoutItem() {
    var confirm by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
        leadingContent = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
        modifier = Modifier.clickable { confirm = true }
    )
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to continue.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    com.localkart.common.auth.AuthManager.signOut()
                }) { Text("Log out") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SellerNotifications() {
    val repo = remember { com.localkart.common.repo.FirestoreRepo() }
    var items by remember { mutableStateOf<List<com.localkart.common.model.AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val uid = com.localkart.common.auth.AuthManager.currentUid
        if (uid != null) runCatching { repo.notificationsFor(uid) }.onSuccess { items = it }
        loading = false
    }
    LazyColumn {
        item { Text("Notifications", Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        when {
            loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() } }
            items.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No notifications yet", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            else -> items(items) { n ->
                ListItem(headlineContent = { Text(n.title.ifBlank { "Notification" }) },
                    supportingContent = { if (n.body.isNotBlank()) Text(n.body) },
                    leadingContent = { Icon(Icons.Default.Notifications, null) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SellerMore() {
    val items = listOf("Account", "Payouts", "Help & Support", "Terms", "Logout")
    LazyColumn {
        item { Text("More", Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(items) { label ->
            if (label == "Logout") {
                SellerLogoutItem()
            } else {
                ListItem(headlineContent = { Text(label) }, modifier = Modifier.clickable { })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SellerAuthScreen(onGoogleSignIn: (com.localkart.common.model.UserRole) -> Unit) {
    var role by remember { mutableStateOf("store") }
    val selectedRole = when (role) {
        "provider" -> com.localkart.common.model.UserRole.SERVICE_PROVIDER
        "both" -> com.localkart.common.model.UserRole.STORE_AND_PROVIDER
        else -> com.localkart.common.model.UserRole.STORE_OWNER
    }
    Box(
        Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
            )
        )
    ) {
        Column(Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 10.dp) {
                Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("LocalKart Seller", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text("Sell products & offer services to your neighbourhood",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sign up as", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    listOf("store" to "Store Owner", "provider" to "Service Provider", "both" to "Both")
                        .forEach { (k, label) ->
                            Row(Modifier.fillMaxWidth().clickable { role = k },
                                verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(role == k, onClick = { role = k }); Text(label)
                            }
                        }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onGoogleSignIn(selectedRole) },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp))
                Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
