package com.localkart.seller.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Product
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var storeId by mutableStateOf<String?>(null); private set
    var storeName by mutableStateOf(""); private set
    var products by mutableStateOf<List<Product>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            val uid = AuthManager.currentUid
            if (uid == null) { error = "Not signed in"; loading = false; return@launch }
            runCatching {
                val store = repo.storesForOwner(uid).firstOrNull()
                storeId = store?.id
                storeName = store?.name ?: ""
                if (store != null) repo.productsForStore(store.id) else emptyList()
            }.onSuccess { products = it }
                .onFailure { error = it.message ?: "Failed to load products" }
            loading = false
        }
    }

    fun add(name: String, price: Double, mrp: Double, unit: String) {
        val sid = storeId ?: return
        viewModelScope.launch {
            runCatching {
                repo.addProduct(
                    Product(storeId = sid, name = name, category = "", price = price,
                        mrp = mrp, unit = unit, inStock = true)
                )
            }.onSuccess { load() }.onFailure { error = it.message }
        }
    }

    fun toggleStock(p: Product) {
        viewModelScope.launch { runCatching { repo.setProductStock(p.id, !p.inStock) }.onSuccess { load() } }
    }

    fun delete(p: Product) {
        viewModelScope.launch { runCatching { repo.deleteProduct(p.id) }.onSuccess { load() } }
    }
}
