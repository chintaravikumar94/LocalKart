package com.localkart.common.repo

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.localkart.common.model.*
import kotlinx.coroutines.tasks.await

/**
 * Thin data layer over Firestore. ViewModels call these suspend functions.
 * Geo-filtering by radius is illustrative — for 50L users use GeoFirestore /
 * geohash range queries instead of client-side filtering.
 */
class FirestoreRepo {
    private val db = Firebase.firestore

    // NOTE: onlyApproved defaults to true (production behaviour). During development we
    // pass false so seeded listings (which clients can only create as approved=false)
    // are visible. In production, approval happens via the admin console / a Cloud Function.
    suspend fun storesByCategory(category: String?, onlyApproved: Boolean = true, limit: Long = 50): List<Store> {
        var q: Query = db.collection("stores")
        if (onlyApproved) q = q.whereEqualTo("approved", true)
        if (!category.isNullOrBlank() && category != "all") q = q.whereEqualTo("category", category)
        return q.limit(limit).get().await().toObjects()
    }

    suspend fun providersByCategory(category: String?, onlyApproved: Boolean = true, limit: Long = 50): List<ServiceProvider> {
        var q: Query = db.collection("services")
        if (onlyApproved) q = q.whereEqualTo("approved", true)
        if (!category.isNullOrBlank() && category != "all") q = q.whereEqualTo("category", category)
        return q.limit(limit).get().await().toObjects()
    }

    suspend fun productsForStore(storeId: String): List<Product> =
        db.collection("products").whereEqualTo("storeId", storeId).get().await().toObjects()

    suspend fun activeBanners(): List<Banner> =
        db.collection("banners").whereEqualTo("active", true).orderBy("order").get().await().toObjects()

    suspend fun categories(type: String): List<Category> =
        db.collection("categories").whereEqualTo("type", type).get().await().toObjects()

    suspend fun growItems(role: String): List<GrowItem> =
        db.collection("growItems").get().await().toObjects<GrowItem>()
            .filter { it.targetRole == "all" || it.targetRole == role }

    // ---- Orders ----
    /** Creates an order doc and returns its id. */
    suspend fun placeOrder(customerUid: String, storeId: String, items: List<CartItem>, total: Double): String {
        val order = Order(
            customerUid = customerUid,
            storeId = storeId,
            items = items,
            total = total,
            status = OrderStatus.PENDING
        )
        return db.collection("orders").add(order).await().id
    }

    suspend fun ordersForCustomer(customerUid: String): List<Order> =
        db.collection("orders").whereEqualTo("customerUid", customerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await().toObjects()

    // ---- Seller side ----
    suspend fun ordersForStore(storeId: String): List<Order> =
        db.collection("orders").whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await().toObjects()

    suspend fun requestsForProvider(providerId: String): List<ServiceRequest> =
        db.collection("serviceRequests").whereEqualTo("providerId", providerId)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await().toObjects()

    suspend fun bookingsForProvider(providerId: String): List<Booking> =
        db.collection("bookings").whereEqualTo("providerId", providerId)
            .orderBy("scheduledAt", Query.Direction.DESCENDING).get().await().toObjects()

    suspend fun appointmentsFor(id: String): List<Appointment> =
        db.collection("appointments").whereEqualTo("storeOrProviderId", id)
            .orderBy("scheduledAt", Query.Direction.DESCENDING).get().await().toObjects()

    suspend fun setStoreOpen(storeId: String, open: Boolean) =
        db.collection("stores").document(storeId).update("isOpen", open).await()

    suspend fun setProviderAvailable(providerId: String, available: Boolean) =
        db.collection("services").document(providerId).update("available", available).await()
}
