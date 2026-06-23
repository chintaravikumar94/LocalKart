package com.localkart.seller.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.CatalogItem
import com.localkart.common.model.Product
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var storeId by mutableStateOf<String?>(null); private set
    var storeName by mutableStateOf(""); private set
    var products by mutableStateOf<List<Product>>(emptyList()); private set
    var catalog by mutableStateOf<List<CatalogItem>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load(); loadCatalog() }

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

    private fun loadCatalog() {
        viewModelScope.launch { runCatching { repo.catalogItems("product") }.onSuccess { catalog = it } }
    }

    /** Pick a catalog item and list it with the seller's price; pending admin approval. */
    fun addFromCatalog(item: CatalogItem, mrp: Double, price: Double) {
        val sid = storeId ?: return
        viewModelScope.launch {
            saving = true
            runCatching {
                repo.addProduct(
                    Product(
                        storeId = sid, catalogId = item.id, name = item.name,
                        category = item.category, imageUrl = item.imageUrl,
                        price = price, mrp = mrp, unit = item.unit, inStock = true, approved = false
                    )
                )
            }.onSuccess { load() }.onFailure { error = it.message }
            saving = false
        }
    }

    fun toggleStock(p: Product) {
        viewModelScope.launch { runCatching { repo.setProductStock(p.id, !p.inStock) }.onSuccess { load() } }
    }

    fun delete(p: Product) {
        viewModelScope.launch { runCatching { repo.deleteProduct(p.id) }.onSuccess { load() } }
    }
}
