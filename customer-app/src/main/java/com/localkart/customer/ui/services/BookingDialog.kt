package com.localkart.customer.ui.services

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Booking
import com.localkart.common.model.ServiceProvider
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val dayLabels = listOf("Today", "Tomorrow", "Day after")
private data class Slot(val hour: Int, val label: String)
private val slots = listOf(Slot(9, "9:00 AM"), Slot(12, "12:00 PM"), Slot(15, "3:00 PM"), Slot(18, "6:00 PM"))

private fun slotToTimestamp(dayOffset: Int, hour: Int): Timestamp {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, dayOffset)
    c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
    return Timestamp(c.time)
}

fun formatSlot(ts: Timestamp): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(ts.toDate())

/** Booking sheet: pick service + day + time slot, then write a Booking to Firestore. */
@Composable
fun BookingDialog(provider: ServiceProvider, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepo() }

    var service by remember { mutableStateOf(provider.category.replaceFirstChar { it.uppercase() }) }
    var dayIdx by remember { mutableIntStateOf(0) }
    var slotIdx by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book ${provider.name}") },
        text = {
            Column {
                OutlinedTextField(service, { service = it }, label = { Text("Service needed") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Day", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dayLabels) { d ->
                        val i = dayLabels.indexOf(d)
                        FilterChip(i == dayIdx, onClick = { dayIdx = i }, label = { Text(d) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Time", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(slots) { s ->
                        val i = slots.indexOf(s)
                        FilterChip(i == slotIdx, onClick = { slotIdx = i }, label = { Text(s.label) })
                    }
                }
                if (provider.pricePerVisit > 0) {
                    Spacer(Modifier.height(12.dp))
                    Text("Visit charge: ₹${provider.pricePerVisit.toInt()}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && service.isNotBlank(),
                onClick = {
                    val uid = AuthManager.currentUid
                    if (uid == null) {
                        Toast.makeText(ctx, "Please sign in again", Toast.LENGTH_SHORT).show(); return@TextButton
                    }
                    scope.launch {
                        saving = true
                        val ts = slotToTimestamp(dayIdx, slots[slotIdx].hour)
                        val r = runCatching { repo.createBooking(uid, provider.id, service.trim(), ts) }
                        saving = false
                        if (r.isSuccess) {
                            Toast.makeText(ctx, "Booking requested for ${formatSlot(ts)}", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(ctx, "Booking failed: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            ) { Text(if (saving) "Booking…" else "Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Raise a service request (no fixed slot): title + details. */
@Composable
fun ServiceRequestDialog(provider: ServiceProvider, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepo() }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request ${provider.name}") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("What do you need?") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(details, { details = it }, label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && title.isNotBlank(),
                onClick = {
                    val uid = AuthManager.currentUid
                    if (uid == null) { Toast.makeText(ctx, "Please sign in again", Toast.LENGTH_SHORT).show(); return@TextButton }
                    scope.launch {
                        saving = true
                        val r = runCatching { repo.createServiceRequest(uid, provider.id, title.trim(), details.trim()) }
                        saving = false
                        if (r.isSuccess) {
                            Toast.makeText(ctx, "Request sent to ${provider.name}", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else Toast.makeText(ctx, "Failed: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            ) { Text(if (saving) "Sending…" else "Send request") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Loads the signed-in customer's bookings + service requests for the My Activity tabs. */
class ActivityViewModel : ViewModel() {
    private val repo = FirestoreRepo()
    var bookings by mutableStateOf<List<Booking>>(emptyList()); private set
    var requests by mutableStateOf<List<com.localkart.common.model.ServiceRequest>>(emptyList()); private set
    var loading by mutableStateOf(false); private set

    fun load() {
        viewModelScope.launch {
            loading = true
            val uid = AuthManager.currentUid
            if (uid != null) {
                runCatching { repo.bookingsForCustomer(uid) }.onSuccess { bookings = it }
                runCatching { repo.serviceRequestsForCustomer(uid) }.onSuccess { requests = it }
            }
            loading = false
        }
    }
}
