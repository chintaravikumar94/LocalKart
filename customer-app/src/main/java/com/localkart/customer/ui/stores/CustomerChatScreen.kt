package com.localkart.customer.ui.stores

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import com.localkart.common.ui.chat.ChatThreadView

/** Resolves (or creates) the chat thread with a store's owner, then shows the thread. */
@Composable
fun CustomerChatScreen(store: Store, onBack: () -> Unit) {
    var threadId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(store.id) {
        val uid = AuthManager.currentUid ?: return@LaunchedEffect
        threadId = runCatching {
            FirestoreRepo().openThread(uid, AuthManager.currentName, store.ownerUid, store.name, store.id)
        }.getOrNull()
    }
    val id = threadId
    if (id == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else {
        ChatThreadView(id, store.name, onBack)
    }
}
