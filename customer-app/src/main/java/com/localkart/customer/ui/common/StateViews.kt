package com.localkart.customer.ui.common

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localkart.common.repo.Seeder
import kotlinx.coroutines.launch

@Composable
fun LoadingRow() {
    // Skeleton shimmer instead of a plain spinner (pro UX).
    ShimmerList(rows = 6)
}

@Composable
fun ErrorRow(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Couldn't load data", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

/**
 * Empty state with a one-tap "Add sample data" button (dev seeder).
 * Calls onDone() after seeding so the caller can reload its data.
 */
@Composable
fun EmptyWithSeed(message: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Inbox, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("No data yet. Add some sample stores & services to try it out.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true
                    val r = Seeder.seed()
                    busy = false
                    if (r.isSuccess) {
                        Toast.makeText(ctx, "Added ${r.getOrNull()} sample items", Toast.LENGTH_SHORT).show()
                        onDone()
                    } else {
                        Toast.makeText(ctx, "Seed failed: ${r.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        ) { Text(if (busy) "Adding…" else "Add sample data") }
    }
}
