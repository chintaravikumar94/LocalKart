package com.localkart.common.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.ChatMessage
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

/** Reusable realtime chat thread UI shared by both apps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadView(threadId: String, title: String, onBack: () -> Unit) {
    val repo = remember { FirestoreRepo() }
    val flow = remember(threadId) { repo.messagesFlow(threadId) }
    val messages by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val myUid = AuthManager.currentUid
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        input, { input = it }, Modifier.weight(1f),
                        placeholder = { Text("Message") }, maxLines = 4
                    )
                    IconButton(onClick = {
                        val t = input.trim()
                        if (t.isNotEmpty() && myUid != null) {
                            input = ""
                            scope.launch { runCatching { repo.sendMessage(threadId, myUid, t) } }
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    ) { pad ->
        if (messages.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) {
                Text("Say hello 👋", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(pad).fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { m -> MessageBubble(m, mine = m.senderUid == myUid) }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, mine: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                m.text,
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
