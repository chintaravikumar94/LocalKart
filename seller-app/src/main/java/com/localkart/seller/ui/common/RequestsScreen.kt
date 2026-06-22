package com.localkart.seller.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
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
import com.localkart.common.model.RequestStatus
import com.localkart.common.model.ServiceRequest
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class RequestsViewModel : ViewModel() {
    private val repo = FirestoreRepo()
    var requests by mutableStateOf<List<ServiceRequest>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            val uid = AuthManager.currentUid
            if (uid == null) { error = "Not signed in"; loading = false; return@launch }
            runCatching { repo.serviceRequestsForOwner(uid) }
                .onSuccess { requests = it }
                .onFailure { error = it.message ?: "Failed to load requests" }
            loading = false
        }
    }

    /** NEW → IN_PROGRESS → DONE */
    fun advance(r: ServiceRequest) {
        val next = when (r.status) {
            RequestStatus.NEW -> RequestStatus.IN_PROGRESS
            RequestStatus.IN_PROGRESS -> RequestStatus.DONE
            else -> return
        }
        viewModelScope.launch { runCatching { repo.updateRequestStatus(r.id, next) }.onSuccess { load() } }
    }

    fun reject(r: ServiceRequest) {
        viewModelScope.launch { runCatching { repo.updateRequestStatus(r.id, RequestStatus.REJECTED) }.onSuccess { load() } }
    }
}

@Composable
fun RequestsScreen(title: String, vm: RequestsViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "New", "In progress", "Done", "Rejected")

    val all = vm.requests
    val shown = all.filter { r ->
        val matchFilter = when (filter) {
            "All" -> true
            "New" -> r.status == RequestStatus.NEW
            "In progress" -> r.status == RequestStatus.IN_PROGRESS
            "Done" -> r.status == RequestStatus.DONE
            "Rejected" -> r.status == RequestStatus.REJECTED
            else -> true
        }
        matchFilter && (query.isBlank() || r.title.contains(query, true) || r.details.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        Text(title, Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(
                "Total" to all.size.toString(),
                "New" to all.count { it.status == RequestStatus.NEW }.toString(),
                "Active" to all.count { it.status == RequestStatus.IN_PROGRESS }.toString(),
                "Done" to all.count { it.status == RequestStatus.DONE }.toString()
            )) { (l, v) -> MetricChip(l, v) }
        }

        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("Search requests") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)

        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f -> FilterChip(f == filter, onClick = { filter = f }, label = { Text(f) }) }
        }

        when {
            vm.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator() }
            vm.error != null -> Column(Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load requests"); Text(vm.error!!, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp)); Button(onClick = { vm.load() }) { Text("Retry") }
            }
            shown.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                Text("No requests yet", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { r -> RequestRow(r, onAdvance = { vm.advance(r) }, onReject = { vm.reject(r) }) }
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
private fun RequestRow(r: ServiceRequest, onAdvance: () -> Unit, onReject: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title.ifBlank { "Request" }, fontWeight = FontWeight.SemiBold)
                if (r.details.isNotBlank())
                    Text(r.details, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                StatusTag(r.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (r.status == RequestStatus.NEW || r.status == RequestStatus.IN_PROGRESS) {
                    TextButton(onClick = onAdvance) {
                        Text(if (r.status == RequestStatus.NEW) "Accept" else "Done")
                    }
                    if (r.status == RequestStatus.NEW)
                        TextButton(onClick = onReject) { Text("Reject") }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: RequestStatus) {
    val (label, color) = when (status) {
        RequestStatus.NEW -> "New" to MaterialTheme.colorScheme.secondary
        RequestStatus.IN_PROGRESS -> "In progress" to MaterialTheme.colorScheme.primary
        RequestStatus.DONE -> "Done" to MaterialTheme.colorScheme.tertiary
        RequestStatus.REJECTED -> "Rejected" to MaterialTheme.colorScheme.error
    }
    AssistChip(onClick = {}, label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color))
}
