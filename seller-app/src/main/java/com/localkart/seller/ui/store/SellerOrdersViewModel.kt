package com.localkart.seller.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Order
import com.localkart.common.model.OrderStatus
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class SellerOrdersViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var orders by mutableStateOf<List<Order>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            val uid = AuthManager.currentUid
            if (uid == null) { error = "Not signed in"; loading = false; return@launch }
            runCatching { repo.ordersForOwner(uid) }
                .onSuccess { orders = it }
                .onFailure { error = it.message ?: "Failed to load orders" }
            loading = false
        }
    }

    /** Advance an order to the next stage (Pending → Active → Completed). */
    fun advance(order: Order) {
        val next = when (order.status) {
            OrderStatus.PENDING -> OrderStatus.ACTIVE
            OrderStatus.ACTIVE -> OrderStatus.COMPLETED
            else -> return
        }
        viewModelScope.launch {
            runCatching { repo.updateOrderStatus(order.id, next) }.onSuccess { load() }
        }
    }
}
