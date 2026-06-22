package com.localkart.customer.ui.common

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Small helper: clickable with no ripple bleed, used in tight rails. */
fun Modifier.clickablePad(onClick: () -> Unit): Modifier = this.then(Modifier.clickable { onClick() })

@Composable
fun ProfileCard() {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Chinta Ravikumar", fontWeight = FontWeight.Bold)
                Text("chintaravikumar1994@gmail.com", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Edit, null)
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

/** Flipkart-style cart with price summary. */
@Composable
fun CartScreen() {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item { SectionHeader("My Cart (3)") }
            items((1..3).toList()) { i ->
                ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, Modifier.size(48.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Cart item $i", fontWeight = FontWeight.SemiBold)
                            Text("₹${i * 99}", color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(onClick = {}) { Text("−") }
                        Text("  1  ")
                        OutlinedButton(onClick = {}) { Text("+") }
                    }
                }
            }
        }
        Surface(tonalElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    Text("₹594", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(onClick = {}) { Text("Place Order") }
            }
        }
    }
}

@Composable
fun NotificationsScreen() {
    LazyColumn {
        item { SectionHeader("Notifications") }
        items((1..8).toList()) { i ->
            ListRow("Notification $i", "Tap to view details · ${i}h ago")
        }
    }
}

@Composable
fun MoreScreen() {
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
            ListItem(
                headlineContent = { Text(label) },
                leadingContent = { Icon(icon, null) },
                modifier = Modifier.clickable { }
            )
            HorizontalDivider()
        }
    }
}

/** Google sign-in entry screen (wire to AuthManager.googleSignInIntent). */
@Composable
fun AuthScreen(onGoogleSignIn: () -> Unit, onPhoneSignIn: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Storefront, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("LocalKart", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Shop local stores & book local services", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp)); Text("Continue with Google")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onPhoneSignIn, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Continue with Phone (OTP)")
        }
    }
}
