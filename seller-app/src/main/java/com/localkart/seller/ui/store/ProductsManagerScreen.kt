package com.localkart.seller.ui.store

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.localkart.common.model.CatalogItem
import com.localkart.common.model.Product

/** Store owner's products: pick from the company catalog and set your price (pending approval). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsManagerScreen(onBack: () -> Unit, vm: ProductsViewModel = viewModel()) {
    var pickerOpen by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf<CatalogItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Products")
                        if (vm.storeName.isNotBlank()) Text(vm.storeName, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (vm.storeId != null)
                ExtendedFloatingActionButton(onClick = { pickerOpen = true },
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add from catalog") })
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                vm.storeId == null -> Text("You don't own a store yet.", Modifier.align(Alignment.Center).padding(32.dp))
                vm.products.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("No products yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap “Add from catalog” to list company-approved items.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.products, key = { it.id }) { p -> ProductManageRow(p, vm) }
                }
            }
        }
    }

    if (pickerOpen) {
        CatalogPicker(vm.catalog, onDismiss = { pickerOpen = false }) { item -> pickerOpen = false; pricing = item }
    }
    pricing?.let { item ->
        PricingDialog(item, saving = vm.saving, onDismiss = { pricing = null }) { mrp, price ->
            vm.addFromCatalog(item, mrp, price); pricing = null
        }
    }
}

@Composable
private fun ProductManageRow(p: Product, vm: ProductsViewModel) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p.imageUrl.isNotBlank())
                AsyncImage(p.imageUrl, p.name, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.Image, null, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${p.price.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (p.mrp > p.price) {
                        Spacer(Modifier.width(6.dp))
                        Text("₹${p.mrp.toInt()}", style = MaterialTheme.typography.labelSmall,
                            textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (p.approved) Text("Live", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                else Text("Pending approval", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(checked = p.inStock, onCheckedChange = { vm.toggleStock(p) })
            IconButton(onClick = { vm.delete(p) }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogPicker(catalog: List<CatalogItem>, onDismiss: () -> Unit, onPick: (CatalogItem) -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = catalog.filter { q.isBlank() || it.name.contains(q, true) || it.category.contains(q, true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text("Pick from catalog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search items") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
                    Text("No catalog items. Ask admin to add some.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(shown, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text(item.category + (if (item.suggestedMrp > 0) " · ~₹${item.suggestedMrp.toInt()}" else "")) },
                        leadingContent = {
                            if (item.imageUrl.isNotBlank())
                                AsyncImage(item.imageUrl, item.name, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.Image, null)
                        },
                        trailingContent = { Icon(Icons.Default.Add, null) },
                        modifier = Modifier.clickable { onPick(item) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PricingDialog(item: CatalogItem, saving: Boolean, onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    var mrp by remember { mutableStateOf(if (item.suggestedMrp > 0) item.suggestedMrp.toInt().toString() else "") }
    var price by remember { mutableStateOf("") }
    val mrpV = mrp.toDoubleOrNull(); val priceV = price.toDoubleOrNull()
    val valid = priceV != null && priceV > 0 && (mrpV == null || mrpV >= priceV)
    val save = if (mrpV != null && priceV != null && mrpV > priceV) (mrpV - priceV) else 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set price · ${item.name}") },
        text = {
            Column {
                OutlinedTextField(mrp, { mrp = it }, label = { Text("Original price (MRP) ₹") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(price, { price = it }, label = { Text("Your selling price ₹") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                if (save > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Customer saves ₹${save.toInt()} (${(save / mrpV!! * 100).toInt()}% off)",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                Text("Goes live after admin approval.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !saving, onClick = { onSave(mrpV ?: priceV!!, priceV!!) }) {
                Text(if (saving) "Saving…" else "List item")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
