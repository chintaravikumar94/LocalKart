package com.localkart.seller.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.ChatThread
import com.localkart.common.repo.FirestoreRepo
import com.localkart.common.ui.chat.ChatThreadView

/** Seller's "Customer Chats": thread list -> open conversation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerChatsScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<ChatThread?>(null) }
    val sel = selected
    if (sel != null) {
        ChatThreadView(sel.id, sel.customerName.ifBlank { "Customer" }, onBack = { selected = null })
        return
    }

    val repo = remember { FirestoreRepo() }
    var threads by remember { mutableStateOf<List<ChatThread>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val uid = AuthManager.currentUid
        if (uid != null) runCatching { repo.threadsForSeller(uid) }.onSuccess { threads = it }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Chats") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                threads.isEmpty() -> Text("No chats yet", Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(threads, key = { it.id }) { t ->
                        ListItem(
                            headlineContent = { Text(t.customerName.ifBlank { "Customer" }) },
                            supportingContent = { Text(t.lastMessage.ifBlank { "Tap to chat" }, maxLines = 1) },
                            leadingContent = { Icon(Icons.Default.ChatBubbleOutline, null) },
                            modifier = Modifier.clickable { selected = t }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
