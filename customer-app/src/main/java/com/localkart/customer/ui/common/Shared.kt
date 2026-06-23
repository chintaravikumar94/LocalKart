package com.localkart.customer.ui.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Small helper: clickable with no ripple bleed, used in tight rails. */
fun Modifier.clickablePad(onClick: () -> Unit): Modifier = this.then(Modifier.clickable { onClick() })

@Composable
fun ProfileCard() {
    var name by remember { mutableStateOf(com.localkart.common.auth.AuthManager.currentName) }
    var email by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        com.localkart.common.auth.AuthManager.currentUser()?.let {
            if (it.name.isNotBlank()) name = it.name
            email = it.email; photo = it.photoUrl
        }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (photo.isNotBlank()) {
                coil.compose.AsyncImage(photo, name,
                    Modifier.size(56.dp).clip(RoundedCornerShape(50)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            } else {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name.ifBlank { "LocalKart User" }, fontWeight = FontWeight.Bold)
                if (email.isNotBlank()) Text(email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ListRow(title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Default.Image, null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = Modifier.clickable { /* open detail page */ }
    )
    HorizontalDivider()
}

@Composable
fun ProductTile(name: String, price: String) {
    ElevatedCard(Modifier.padding(6.dp).clickable { /* product detail */ }) {
        Column(Modifier.padding(10.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 3.dp) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null) }
            }
            Spacer(Modifier.height(6.dp))
            Text(name, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(price, color = MaterialTheme.colorScheme.primary)
            Button(onClick = {}, Modifier.fillMaxWidth()) { Text("Add") }
        }
    }
}

/** Flipkart-style cart with live items, qty controls and place-order to Firestore. */
@Composable
fun CartScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repo = remember { com.localkart.common.repo.FirestoreRepo() }
    var placing by remember { mutableStateOf(false) }

    if (Cart.items.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShoppingCart, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                Text("Your cart is empty", style = MaterialTheme.typography.titleMedium)
                Text("Add products from a store to get started", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item { SectionHeader("My Cart (${Cart.count})") }
            items(Cart.items, key = { it.productId }) { line ->
                ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, Modifier.size(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(line.name, fontWeight = FontWeight.SemiBold)
                            Text("₹${line.price.toInt()}", color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = { Cart.setQty(line.productId, line.qty - 1) }) { Text("−") }
                        Text("  ${line.qty}  ")
                        OutlinedButton(onClick = { Cart.setQty(line.productId, line.qty + 1) }) { Text("+") }
                    }
                }
            }
        }
        Surface(tonalElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    Text("₹${Cart.total.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    enabled = !placing,
                    onClick = {
                        val uid = com.localkart.common.auth.AuthManager.currentUid
                        if (uid == null) {
                            Toast.makeText(ctx, "Please sign in again", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            placing = true
                            val r = runCatching {
                                repo.placeOrder(uid, Cart.storeId, Cart.items.toList(), Cart.total)
                            }
                            placing = false
                            if (r.isSuccess) {
                                Cart.clear()
                                Toast.makeText(ctx, "Order placed! ID: ${r.getOrNull()}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(ctx, "Order failed: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text(if (placing) "Placing…" else "Place Order") }
            }
        }
    }
}

@Composable
fun NotificationsScreen() {
    val repo = remember { com.localkart.common.repo.FirestoreRepo() }
    var items by remember { mutableStateOf<List<com.localkart.common.model.AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val uid = com.localkart.common.auth.AuthManager.currentUid
        if (uid != null) runCatching { repo.notificationsFor(uid) }.onSuccess { items = it }
        loading = false
    }
    LazyColumn {
        item { SectionHeader("Notifications") }
        when {
            loading -> item { LoadingRow() }
            items.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No notifications yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> items(items) { n ->
                ListItem(
                    headlineContent = { Text(n.title.ifBlank { "Notification" }) },
                    supportingContent = { if (n.body.isNotBlank()) Text(n.body) },
                    leadingContent = { Icon(Icons.Default.Notifications, null) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun MoreScreen() {
    var confirmLogout by remember { mutableStateOf(false) }
    val items = listOf(
        "My Profile" to Icons.Default.Person,
        "Order History" to Icons.Default.Receipt,
        "Addresses" to Icons.Default.LocationOn,
        "Help & Support" to Icons.Default.Help,
        "About LocalKart" to Icons.Default.Info,
        "Logout" to Icons.Default.Logout
    )
    LazyColumn {
        item { SectionHeader("More") }
        items(items) { (label, icon) ->
            val isLogout = label == "Logout"
            ListItem(
                headlineContent = { Text(label, color = if (isLogout) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified) },
                leadingContent = { Icon(icon, null, tint = if (isLogout) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified) },
                modifier = Modifier.clickable { if (isLogout) confirmLogout = true }
            )
            HorizontalDivider()
        }
    }
    if (confirmLogout) {
        LogoutDialog(onDismiss = { confirmLogout = false }, onConfirm = {
            confirmLogout = false
            com.localkart.common.auth.AuthManager.signOut()
        })
    }
}

@Composable
fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log out?") },
        text = { Text("You'll need to sign in again to continue.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Log out") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Premium Google sign-in entry screen. */
@Composable
fun AuthScreen(onGoogleSignIn: () -> Unit, onPhoneSignIn: () -> Unit = {}) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 10.dp
            ) {
                Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("LocalKart", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(
                "Shop local stores & book trusted services near you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(44.dp))
            Button(onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp))
                Text("Continue with Google", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPhoneSignIn,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Continue with Phone (OTP)")
            }
            Spacer(Modifier.height(22.dp))
            Text("By continuing you agree to our Terms & Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
