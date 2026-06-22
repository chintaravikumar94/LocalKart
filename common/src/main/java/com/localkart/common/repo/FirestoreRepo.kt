package com.localkart.common.repo

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.localkart.common.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    // Sorted client-side to avoid needing a composite index during development.
    suspend fun ordersForCustomer(customerUid: String): List<Order> =
        db.collection("orders").whereEqualTo("customerUid", customerUid)
            .get().await().toObjects<Order>().sortedByDescending { it.createdAt }

    // ---- Products (store owner) ----
    suspend fun storesForOwner(ownerUid: String): List<Store> =
        db.collection("stores").whereEqualTo("ownerUid", ownerUid).get().await().toObjects()

    suspend fun servicesForOwner(ownerUid: String): List<ServiceProvider> =
        db.collection("services").whereEqualTo("ownerUid", ownerUid).get().await().toObjects()

    suspend fun addProduct(product: Product): String =
        db.collection("products").add(product).await().id

    suspend fun setProductStock(productId: String, inStock: Boolean) =
        db.collection("products").document(productId).update("inStock", inStock).await()

    suspend fun deleteProduct(productId: String) =
        db.collection("products").document(productId).delete().await()

    // ---- Bookings ----
    suspend fun createBooking(customerUid: String, providerId: String, service: String, scheduledAt: com.google.firebase.Timestamp): String {
        val booking = Booking(
            customerUid = customerUid, providerId = providerId,
            service = service, scheduledAt = scheduledAt, status = BookingStatus.NEW
        )
        return db.collection("bookings").add(booking).await().id
    }

    suspend fun bookingsForCustomer(customerUid: String): List<Booking> =
        db.collection("bookings").whereEqualTo("customerUid", customerUid)
            .get().await().toObjects<Booking>().sortedByDescending { it.scheduledAt }

    /** All bookings across every service this seller owns (sorted newest first). */
    suspend fun bookingsForOwner(ownerUid: String): List<Booking> {
        val providerIds = db.collection("services").whereEqualTo("ownerUid", ownerUid)
            .get().await().documents.map { it.id }
        if (providerIds.isEmpty()) return emptyList()
        val out = mutableListOf<Booking>()
        providerIds.chunked(10).forEach { chunk ->
            out += db.collection("bookings").whereIn("providerId", chunk).get().await().toObjects<Booking>()
        }
        return out.sortedByDescending { it.scheduledAt }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus) =
        db.collection("bookings").document(bookingId).update("status", status.name).await()

    // ---- Service requests (job requests) ----
    suspend fun createServiceRequest(customerUid: String, providerId: String, title: String, details: String): String {
        val req = ServiceRequest(
            customerUid = customerUid, providerId = providerId,
            title = title, details = details, status = RequestStatus.NEW
        )
        return db.collection("serviceRequests").add(req).await().id
    }

    suspend fun serviceRequestsForCustomer(customerUid: String): List<ServiceRequest> =
        db.collection("serviceRequests").whereEqualTo("customerUid", customerUid)
            .get().await().toObjects<ServiceRequest>().sortedByDescending { it.createdAt }

    /** All service requests across every service this seller owns (newest first). */
    suspend fun serviceRequestsForOwner(ownerUid: String): List<ServiceRequest> {
        val providerIds = db.collection("services").whereEqualTo("ownerUid", ownerUid)
            .get().await().documents.map { it.id }
        if (providerIds.isEmpty()) return emptyList()
        val out = mutableListOf<ServiceRequest>()
        providerIds.chunked(10).forEach { chunk ->
            out += db.collection("serviceRequests").whereIn("providerId", chunk).get().await().toObjects<ServiceRequest>()
        }
        return out.sortedByDescending { it.createdAt }
    }

    suspend fun updateRequestStatus(requestId: String, status: RequestStatus) =
        db.collection("serviceRequests").document(requestId).update("status", status.name).await()

    // ---- Appointments ----
    suspend fun createAppointment(customerUid: String, storeOrProviderId: String, purpose: String, scheduledAt: com.google.firebase.Timestamp): String {
        val appt = Appointment(
            customerUid = customerUid, storeOrProviderId = storeOrProviderId,
            purpose = purpose, scheduledAt = scheduledAt, status = AppointmentStatus.NEW
        )
        return db.collection("appointments").add(appt).await().id
    }

    suspend fun appointmentsForCustomer(customerUid: String): List<Appointment> =
        db.collection("appointments").whereEqualTo("customerUid", customerUid)
            .get().await().toObjects<Appointment>().sortedByDescending { it.scheduledAt }

    /** Appointments across every store owned by this seller (newest first). */
    suspend fun appointmentsForOwner(ownerUid: String): List<Appointment> {
        val ids = db.collection("stores").whereEqualTo("ownerUid", ownerUid)
            .get().await().documents.map { it.id }
        if (ids.isEmpty()) return emptyList()
        val out = mutableListOf<Appointment>()
        ids.chunked(10).forEach { chunk ->
            out += db.collection("appointments").whereIn("storeOrProviderId", chunk).get().await().toObjects<Appointment>()
        }
        return out.sortedByDescending { it.scheduledAt }
    }

    suspend fun updateAppointmentStatus(id: String, status: AppointmentStatus) =
        db.collection("appointments").document(id).update("status", status.name).await()

    // ---- FCM token ----
    suspend fun saveFcmToken(uid: String, token: String) =
        db.collection("users").document(uid).update("fcmToken", token).await()

    // ---- Photos ----
    /** Sets the photo on the user doc and all stores/services they own. */
    suspend fun updateOwnerPhoto(uid: String, url: String) {
        runCatching { db.collection("users").document(uid).update("photoUrl", url).await() }
        storesForOwner(uid).forEach {
            runCatching { db.collection("stores").document(it.id).update("photoUrl", url).await() }
        }
        servicesForOwner(uid).forEach {
            runCatching { db.collection("services").document(it.id).update("photoUrl", url).await() }
        }
    }

    // ---- Reviews ----
    suspend fun addReview(review: Review) {
        db.collection("reviews").add(review).await()
        recomputeRating(review.targetType, review.targetId)
    }

    suspend fun reviewsFor(targetType: String, targetId: String): List<Review> =
        db.collection("reviews")
            .whereEqualTo("targetType", targetType)
            .whereEqualTo("targetId", targetId)
            .get().await().toObjects<Review>().sortedByDescending { it.createdAt }

    private suspend fun recomputeRating(targetType: String, targetId: String) {
        val reviews = db.collection("reviews")
            .whereEqualTo("targetType", targetType).whereEqualTo("targetId", targetId)
            .get().await().toObjects<Review>()
        if (reviews.isEmpty()) return
        val avg = reviews.map { it.rating }.average()
        val collection = if (targetType == "service") "services" else "stores"
        runCatching {
            db.collection(collection).document(targetId)
                .update(mapOf("rating" to avg, "ratingCount" to reviews.size)).await()
        }
    }

    // ---- Chat ----
    /** Deterministic thread id so both sides resolve the same conversation. */
    private fun threadId(customerUid: String, sellerUid: String) = "${customerUid}_${sellerUid}"

    suspend fun openThread(
        customerUid: String, customerName: String,
        sellerUid: String, sellerName: String, storeId: String
    ): String {
        val id = threadId(customerUid, sellerUid)
        val ref = db.collection("chatThreads").document(id)
        if (!ref.get().await().exists()) {
            ref.set(
                ChatThread(
                    customerUid = customerUid, customerName = customerName,
                    sellerUid = sellerUid, sellerName = sellerName, storeId = storeId
                )
            ).await()
        }
        return id
    }

    suspend fun sendMessage(threadId: String, senderUid: String, text: String) {
        val msg = ChatMessage(senderUid = senderUid, text = text)
        db.collection("chatThreads").document(threadId).collection("messages").add(msg).await()
        db.collection("chatThreads").document(threadId)
            .update(mapOf("lastMessage" to text, "updatedAt" to com.google.firebase.Timestamp.now())).await()
    }

    /** Realtime stream of messages in a thread (oldest first). */
    fun messagesFlow(threadId: String): Flow<List<ChatMessage>> = callbackFlow {
        val reg = db.collection("chatThreads").document(threadId).collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun threadsForSeller(sellerUid: String): List<ChatThread> =
        db.collection("chatThreads").whereEqualTo("sellerUid", sellerUid)
            .get().await().toObjects<ChatThread>().sortedByDescending { it.updatedAt }

    suspend fun threadsForCustomer(customerUid: String): List<ChatThread> =
        db.collection("chatThreads").whereEqualTo("customerUid", customerUid)
            .get().await().toObjects<ChatThread>().sortedByDescending { it.updatedAt }

    // ---- Seller side ----
    suspend fun ordersForStore(storeId: String): List<Order> =
        db.collection("orders").whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await().toObjects()

    /** All orders across every store owned by this seller (sorted newest first, client-side). */
    suspend fun ordersForOwner(ownerUid: String): List<Order> {
        val storeIds = db.collection("stores").whereEqualTo("ownerUid", ownerUid)
            .get().await().documents.map { it.id }
        if (storeIds.isEmpty()) return emptyList()
        val out = mutableListOf<Order>()
        storeIds.chunked(10).forEach { chunk ->
            out += db.collection("orders").whereIn("storeId", chunk).get().await().toObjects<Order>()
        }
        return out.sortedByDescending { it.createdAt }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus) =
        db.collection("orders").document(orderId).update("status", status.name).await()

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
