package com.localkart.customer.ui.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.model.ServiceProvider
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class ServicesViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var providers by mutableStateOf<List<ServiceProvider>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var category by mutableStateOf("all"); private set

    init { load() }

    fun select(cat: String) { category = cat; load() }
    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            loading = true; error = null
            runCatching { repo.providersByCategory(category, onlyApproved = false) }
                .onSuccess { providers = it }
                .onFailure { error = it.message ?: "Failed to load providers" }
            loading = false
        }
    }
}
