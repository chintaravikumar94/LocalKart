package com.localkart.customer.ui.stores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Review
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(store: Store, onBack: () -> Unit) {
    val repo = remember { FirestoreRepo() }
    val scope = rememberCoroutineScope()
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAdd by remember { mutableStateOf(false) }

    suspend fun reload() {
        loading = true
        runCatching { repo.reviewsFor("store", store.id) }.onSuccess { reviews = it }
        loading = false
    }
    LaunchedEffect(store.id) { reload() }

    val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.RateReview, null) }, text = { Text("Write review") }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%.1f".format(avg), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                StarRow(avg.toInt())
                                Text("${reviews.size} review(s)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                    if (reviews.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            Text("No reviews yet. Be the first!", style = MaterialTheme.typography.bodyMedium) } }
                    } else items(reviews, key = { it.id }) { r -> ReviewRow(r) }
                }
            }
        }
    }

    if (showAdd) {
        AddReviewDialog(
            onDismiss = { showAdd = false },
            onSubmit = { rating, comment ->
                showAdd = false
                val uid = AuthManager.currentUid
                if (uid != null) {
                    scope.launch {
                        runCatching {
                            repo.addReview(
                                Review(targetType = "store", targetId = store.id, customerUid = uid,
                                    customerName = AuthManager.currentName, rating = rating, comment = comment)
                            )
                        }
                        reload()
                    }
                }
            }
        )
    }
}

@Composable
private fun StarRow(filled: Int, size: Int = 18) {
    Row {
        repeat(5) { i ->
            Icon(
                if (i < filled) Icons.Default.Star else Icons.Default.StarBorder,
                null, Modifier.size(size.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ReviewRow(r: Review) {
    val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(r.createdAt.toDate())
    ListItem(
        headlineContent = { Text(r.customerName.ifBlank { "Customer" }, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Column {
                StarRow(r.rating, size = 14)
                if (r.comment.isNotBlank()) Text(r.comment)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
    HorizontalDivider()
}

@Composable
private fun AddReviewDialog(onDismiss: () -> Unit, onSubmit: (Int, String) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write a review") },
        text = {
            Column {
                Row {
                    repeat(5) { i ->
                        IconButton(onClick = { rating = i + 1 }) {
                            Icon(
                                if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                "Star ${i + 1}", tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                OutlinedTextField(comment, { comment = it }, label = { Text("Your experience (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(rating, comment.trim()) }) { Text("Submit") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
