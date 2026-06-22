package com.localkart.customer.ui.common

import androidx.compose.runtime.mutableStateListOf
import com.localkart.common.model.CartItem
import com.localkart.common.model.Product

/**
 * Simple in-memory cart shared across the customer app (single-store at a time).
 * Backed by a Compose snapshot list so any screen observing it recomposes on change.
 */
object Cart {
    val items = mutableStateListOf<CartItem>()
    var storeId: String = ""
        private set

    val count: Int get() = items.sumOf { it.qty }
    val total: Double get() = items.sumOf { it.price * it.qty }

    fun add(p: Product) {
        // Adding from a different store resets the cart (one store per order).
        if (storeId.isNotBlank() && storeId != p.storeId) { items.clear() }
        storeId = p.storeId
        val idx = items.indexOfFirst { it.productId == p.id }
        if (idx >= 0) {
            items[idx] = items[idx].copy(qty = items[idx].qty + 1)
        } else {
            items.add(CartItem(productId = p.id, name = p.name, imageUrl = p.imageUrl, price = p.price, qty = 1))
        }
    }

    fun setQty(productId: String, qty: Int) {
        val idx = items.indexOfFirst { it.productId == productId }
        if (idx < 0) return
        if (qty <= 0) items.removeAt(idx) else items[idx] = items[idx].copy(qty = qty)
        if (items.isEmpty()) storeId = ""
    }

    fun clear() { items.clear(); storeId = "" }
}
