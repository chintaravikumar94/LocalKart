package com.localkart.customer.ui.stores

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.model.Store
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class StoresViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var stores by mutableStateOf<List<Store>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var category by mutableStateOf("all"); private set

    init { load() }

    fun select(cat: String) { category = cat; load() }
    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            loading = true; error = null
            // onlyApproved = false during development so seeded data shows. Flip to true for prod.
            runCatching { repo.storesByCategory(category, onlyApproved = false) }
                .onSuccess { stores = it }
                .onFailure { error = it.message ?: "Failed to load stores" }
            loading = false
        }
    }
}
