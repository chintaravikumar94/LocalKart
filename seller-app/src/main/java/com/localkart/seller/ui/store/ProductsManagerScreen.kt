package com.localkart.seller.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.common.model.Product

/** Store owner's product manager: list, add, toggle stock, delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsManagerScreen(onBack: () -> Unit, vm: ProductsViewModel = viewModel()) {
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Products")
                        if (vm.storeName.isNotBlank())
                            Text(vm.storeName, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (vm.storeId != null)
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add product") }
                )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                vm.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                vm.storeId == null -> Text(
                    "You don't own a store yet. Register one first.",
                    Modifier.align(Alignment.Center).padding(32.dp)
                )
                vm.products.isEmpty() -> Text(
                    "No products yet. Tap “Add product”.",
                    Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.products, key = { it.id }) { p -> ProductManageRow(p, vm) }
                }
            }
        }
    }

    if (showAdd) {
        AddProductDialog(
            onDismiss = { showAdd = false },
            onSave = { name, price, mrp, unit -> vm.add(name, price, mrp, unit); showAdd = false }
        )
    }
}

@Composable
private fun ProductManageRow(p: Product, vm: ProductsViewModel) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Image, null, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.SemiBold)
                Text("₹${p.price.toInt()}" + if (p.unit.isNotBlank()) " · ${p.unit}" else "",
                    style = MaterialTheme.typography.bodySmall)
                Text(if (p.inStock) "In stock" else "Out of stock",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (p.inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Switch(checked = p.inStock, onCheckedChange = { vm.toggleStock(p) })
            IconButton(onClick = { vm.delete(p) }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@Composable
private fun AddProductDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && price.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add product") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(price, { price = it }, label = { Text("Price ₹") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(mrp, { mrp = it }, label = { Text("MRP ₹ (optional)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit e.g. 1 kg (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(name.trim(), price.toDouble(),
                        mrp.toDoubleOrNull() ?: price.toDouble(), unit.trim())
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
