package com.localkart.seller.ui.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localkart.common.model.Booking
import com.localkart.common.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProviderBookingsScreen(vm: ProviderBookingsViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "New", "Confirmed", "Done", "Cancelled")

    val all = vm.bookings
    val shown = all.filter { b ->
        (filter == "All" || b.status.name.equals(filter, true)) &&
        (query.isBlank() || b.service.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        Text("Bookings", Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                "Total" to all.size.toString(),
                "New" to all.count { it.status == BookingStatus.NEW }.toString(),
                "Confirmed" to all.count { it.status == BookingStatus.CONFIRMED }.toString(),
                "Done" to all.count { it.status == BookingStatus.DONE }.toString()
            )) { (l, v) -> MetricChip(l, v) }
        }

        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search service") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f -> FilterChip(f == filter, onClick = { filter = f }, label = { Text(f) }) }
        }

        when {
            vm.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() }
            vm.error != null -> Column(Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load bookings"); Text(vm.error!!, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp)); Button(onClick = { vm.load() }) { Text("Retry") }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No bookings yet", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { b -> BookingRow(b, onAdvance = { vm.advance(b) }, onCancel = { vm.cancel(b) }) }
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
private fun BookingRow(b: Booking, onAdvance: () -> Unit, onCancel: () -> Unit) {
    val time = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(b.scheduledAt.toDate())
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventAvailable, null, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.service.ifBlank { "Service" }, fontWeight = FontWeight.SemiBold)
                Text(time, style = MaterialTheme.typography.bodySmall)
                StatusTag(b.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (b.status == BookingStatus.NEW || b.status == BookingStatus.CONFIRMED) {
                    TextButton(onClick = onAdvance) {
                        Text(if (b.status == BookingStatus.NEW) "Confirm" else "Done")
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: BookingStatus) {
    val (label, color) = when (status) {
        BookingStatus.NEW -> "New" to MaterialTheme.colorScheme.secondary
        BookingStatus.CONFIRMED -> "Confirmed" to MaterialTheme.colorScheme.primary
        BookingStatus.DONE -> "Done" to MaterialTheme.colorScheme.tertiary
        BookingStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
