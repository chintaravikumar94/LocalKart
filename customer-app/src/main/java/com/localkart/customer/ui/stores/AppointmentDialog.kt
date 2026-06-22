package com.localkart.customer.ui.stores

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.util.Calendar

private val apptDays = listOf("Today", "Tomorrow", "Day after")
private val apptSlots = listOf(10 to "10:00 AM", 13 to "1:00 PM", 16 to "4:00 PM", 18 to "6:00 PM")

private fun apptTimestamp(dayOffset: Int, hour: Int): Timestamp {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, dayOffset)
    c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
    return Timestamp(c.time)
}

/** Book an appointment with a store (purpose + day + time). */
@Composable
fun AppointmentDialog(store: Store, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepo() }
    var purpose by remember { mutableStateOf("") }
    var dayIdx by remember { mutableIntStateOf(0) }
    var slotIdx by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appointment at ${store.name}") },
        text = {
            Column {
                OutlinedTextField(purpose, { purpose = it }, label = { Text("Purpose") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Day", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apptDays) { d ->
                        val i = apptDays.indexOf(d)
                        FilterChip(i == dayIdx, onClick = { dayIdx = i }, label = { Text(d) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Time", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apptSlots) { s ->
                        val i = apptSlots.indexOf(s)
                        FilterChip(i == slotIdx, onClick = { slotIdx = i }, label = { Text(s.second) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && purpose.isNotBlank(),
                onClick = {
                    val uid = AuthManager.currentUid
                    if (uid == null) { Toast.makeText(ctx, "Please sign in again", Toast.LENGTH_SHORT).show(); return@TextButton }
                    scope.launch {
                        saving = true
                        val ts = apptTimestamp(dayIdx, apptSlots[slotIdx].first)
                        val r = runCatching { repo.createAppointment(uid, store.id, purpose.trim(), ts) }
                        saving = false
                        if (r.isSuccess) {
                            Toast.makeText(ctx, "Appointment requested", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else Toast.makeText(ctx, "Failed: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            ) { Text(if (saving) "Booking…" else "Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
