package com.localkart.seller.ui.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
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
import com.localkart.common.model.Appointment
import com.localkart.common.model.AppointmentStatus
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AppointmentsViewModel : ViewModel() {
    private val repo = FirestoreRepo()
    var appointments by mutableStateOf<List<Appointment>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            val uid = AuthManager.currentUid
            if (uid == null) { error = "Not signed in"; loading = false; return@launch }
            runCatching { repo.appointmentsForOwner(uid) }
                .onSuccess { appointments = it }
                .onFailure { error = it.message ?: "Failed to load appointments" }
            loading = false
        }
    }

    fun advance(a: Appointment) {
        val next = when (a.status) {
            AppointmentStatus.NEW -> AppointmentStatus.CONFIRMED
            AppointmentStatus.CONFIRMED -> AppointmentStatus.DONE
            else -> return
        }
        viewModelScope.launch { runCatching { repo.updateAppointmentStatus(a.id, next) }.onSuccess { load() } }
    }

    fun cancel(a: Appointment) {
        viewModelScope.launch {
            runCatching { repo.updateAppointmentStatus(a.id, AppointmentStatus.CANCELLED) }.onSuccess { load() }
        }
    }
}

@Composable
fun AppointmentsScreen(vm: AppointmentsViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "New", "Confirmed", "Done", "Cancelled")

    val all = vm.appointments
    val shown = all.filter { a ->
        (filter == "All" || a.status.name.equals(filter, true)) &&
        (query.isBlank() || a.purpose.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        Text("Appointments", Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                "Total" to all.size.toString(),
                "New" to all.count { it.status == AppointmentStatus.NEW }.toString(),
                "Confirmed" to all.count { it.status == AppointmentStatus.CONFIRMED }.toString(),
                "Done" to all.count { it.status == AppointmentStatus.DONE }.toString()
            )) { (l, v) -> MetricChip(l, v) }
        }

        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search purpose") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f -> FilterChip(f == filter, onClick = { filter = f }, label = { Text(f) }) }
        }

        when {
            vm.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() }
            vm.error != null -> Column(Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load appointments"); Text(vm.error!!, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp)); Button(onClick = { vm.load() }) { Text("Retry") }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No appointments yet", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { a -> AppointmentRow(a, onAdvance = { vm.advance(a) }, onCancel = { vm.cancel(a) }) }
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
private fun AppointmentRow(a: Appointment, onAdvance: () -> Unit, onCancel: () -> Unit) {
    val time = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(a.scheduledAt.toDate())
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, null, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.purpose.ifBlank { "Appointment" }, fontWeight = FontWeight.SemiBold)
                Text(time, style = MaterialTheme.typography.bodySmall)
                StatusTag(a.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (a.status == AppointmentStatus.NEW || a.status == AppointmentStatus.CONFIRMED) {
                    TextButton(onClick = onAdvance) {
                        Text(if (a.status == AppointmentStatus.NEW) "Confirm" else "Done")
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: AppointmentStatus) {
    val (label, color) = when (status) {
        AppointmentStatus.NEW -> "New" to MaterialTheme.colorScheme.secondary
        AppointmentStatus.CONFIRMED -> "Confirmed" to MaterialTheme.colorScheme.primary
        AppointmentStatus.DONE -> "Done" to MaterialTheme.colorScheme.tertiary
        AppointmentStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
