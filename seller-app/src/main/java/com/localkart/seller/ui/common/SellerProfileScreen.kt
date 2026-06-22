package com.localkart.seller.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.localkart.common.auth.AuthManager
import com.localkart.common.repo.FirestoreRepo
import com.localkart.common.repo.StorageRepo
import com.localkart.common.ui.QrImage
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repo = FirestoreRepo()
    var name by mutableStateOf(""); private set
    var photoUrl by mutableStateOf(""); private set
    var approved by mutableStateOf(false); private set
    var rating by mutableStateOf(0.0); private set
    var sellerId by mutableStateOf(""); private set
    var uploading by mutableStateOf(false); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            val uid = AuthManager.currentUid ?: return@launch
            val user = AuthManager.currentUser()
            val store = runCatching { repo.storesForOwner(uid).firstOrNull() }.getOrNull()
            val service = runCatching { repo.servicesForOwner(uid).firstOrNull() }.getOrNull()
            name = user?.name?.ifBlank { AuthManager.currentName } ?: AuthManager.currentName
            photoUrl = listOf(store?.photoUrl, service?.photoUrl, user?.photoUrl).firstOrNull { !it.isNullOrBlank() } ?: ""
            approved = store?.approved ?: service?.approved ?: false
            rating = store?.rating ?: service?.rating ?: 0.0
            sellerId = store?.id ?: service?.id ?: uid
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            val uid = AuthManager.currentUid ?: return@launch
            uploading = true
            runCatching {
                val url = StorageRepo.uploadImage(uri, "profiles")
                repo.updateOwnerPhoto(uid, url)
            }
            uploading = false
            load()
        }
    }
}

@Composable
fun SellerProfileScreen(vm: ProfileViewModel = viewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadPhoto(uri)
    }
    LazyColumn(Modifier.padding(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        if (vm.photoUrl.isNotBlank()) {
                            AsyncImage(vm.photoUrl, "Profile",
                                Modifier.size(72.dp).clip(RoundedCornerShape(50)), contentScale = ContentScale.Crop)
                        } else {
                            Surface(shape = RoundedCornerShape(50), tonalElevation = 4.dp) {
                                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null) }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.BottomEnd).size(26.dp).clickable { picker.launch("image/*") }
                        ) { Box(contentAlignment = Alignment.Center) {
                            if (vm.uploading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.Default.PhotoCamera, "Change photo", Modifier.size(16.dp), tint = Color.White)
                        } }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name(vm), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (vm.approved) Icons.Default.Verified else Icons.Default.HourglassEmpty, null,
                                Modifier.size(16.dp),
                                tint = if (vm.approved) Color(0xFF16A34A) else MaterialTheme.colorScheme.secondary)
                            Text(if (vm.approved) " Approved" else " Pending approval", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(" ${"%.1f".format(vm.rating)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LocalKart Partner ID", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    QrImage("localkart://partner/${vm.sellerId}", Modifier.size(160.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(name(vm), fontWeight = FontWeight.SemiBold)
                    Text("ID: ${vm.sellerId.take(8).uppercase()}", style = MaterialTheme.typography.labelSmall)
                    Text("Scan to view profile & book", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item { ListItem(headlineContent = { Text("Support") }, leadingContent = { Icon(Icons.Default.Help, null) }) }
        item { ListItem(headlineContent = { Text("Contact") }, leadingContent = { Icon(Icons.Default.Call, null) }) }
        item { SellerLogoutItem() }
    }
}

private fun name(vm: ProfileViewModel) = vm.name.ifBlank { "My Profile" }
