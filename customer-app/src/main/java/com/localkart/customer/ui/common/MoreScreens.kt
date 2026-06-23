package com.localkart.customer.ui.common

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

/** Reusable back-bar scaffold for More sub-pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
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
fun MoreProfileScreen(onBack: () -> Unit) {
    SubScreen("My Profile", onBack) { pad ->
        var confirmLogout by remember { mutableStateOf(false) }
        LazyColumn(Modifier.padding(pad).padding(12.dp)) {
            item { ProfileCard() }
            item { Spacer(Modifier.height(8.dp)) }
            item { ListItem(headlineContent = { Text("Edit profile") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                supportingContent = { Text("Name, phone & photo") }) }
            item { HorizontalDivider() }
            item { ListItem(headlineContent = { Text("Saved addresses") },
                leadingContent = { Icon(Icons.Default.LocationOn, null) }) }
            item { HorizontalDivider() }
            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out")
                }
            }
        }
        if (confirmLogout) LogoutDialog(onDismiss = { confirmLogout = false }, onConfirm = {
            confirmLogout = false; com.localkart.common.auth.AuthManager.signOut()
        })
    }
}

@Composable
fun AddressScreen(onBack: () -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    SubScreen("My Location", onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Current location", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(LocationStore.area, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.EditLocation, null); Spacer(Modifier.width(8.dp)); Text("Change location")
            }
            Spacer(Modifier.height(8.dp))
            Text("Your location is used to show nearby stores and services within your selected radius.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showSheet) ChangeLocationSheet(onDismiss = { showSheet = false })
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit) {
    SubScreen("Help & Support", onBack) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                ElevatedCard(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Contact us", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text("support@localkart.app")
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp)); Text("1800-123-4567 (9am–9pm)")
                        }
                    }
                }
            }
            item { SectionHeader("FAQs") }
            items(
                listOf(
                    "How do I place an order?" to "Open a store, add products to cart, then tap Place Order.",
                    "How do I book a service?" to "Open Local Services, pick a provider and tap Book.",
                    "How is delivery distance decided?" to "By your selected location and the km radius on the home screen.",
                    "How do I cancel?" to "Contact the store/provider via chat, or reach our support team."
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
fun AboutScreen(onBack: () -> Unit) {
    SubScreen("About LocalKart", onBack) { pad ->
        Column(Modifier.padding(pad).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("LocalKart", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(
                "LocalKart connects you to trusted local stores and service providers near you — shop daily needs and book services in minutes.",
                style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Text("Made with ❤ for local businesses", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
