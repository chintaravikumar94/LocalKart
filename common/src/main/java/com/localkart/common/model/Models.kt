package com.localkart.common.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

/**
 * Firestore data model for LocalKart.
 * Collections: users, stores, services, products, orders, serviceRequests,
 * bookings, appointments, banners, categories, growItems, notifications.
 * All classes have a no-arg default so Firestore can deserialize them.
 */

enum class UserRole { CUSTOMER, STORE_OWNER, SERVICE_PROVIDER, STORE_AND_PROVIDER, ADMIN }

data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val location: GeoPoint? = null,
    val address: String = "",
    val fcmToken: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/** A local store (groceries, fancy, net center, meeseva, etc.) */
data class Store(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val category: String = "",        // groceries | mobile_repairing | fancy | net_center | meeseva | household
    val description: String = "",
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val location: GeoPoint? = null,
    val address: String = "",
    val isOpen: Boolean = true,
    val approved: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

/** A service provider entry (plumber, electrician, carpenter, etc.) */
data class ServiceProvider(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val category: String = "",        // plumber | electrician | carpenter | gardener | mechanic | housekeeping | cook
    val description: String = "",
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val available: Boolean = true,
    val approved: Boolean = false,
    val location: GeoPoint? = null,
    val address: String = "",
    val pricePerVisit: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now()
)

data class Product(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val mrp: Double = 0.0,
    val unit: String = "",
    val inStock: Boolean = true
)

data class CartItem(
    val productId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val qty: Int = 1
)

enum class OrderStatus { PENDING, ACTIVE, COMPLETED, CANCELLED }

data class Order(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val storeId: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now()
)

enum class RequestStatus { NEW, IN_PROGRESS, DONE, REJECTED }

data class ServiceRequest(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val providerId: String = "",
    val title: String = "",
    val details: String = "",
    val status: RequestStatus = RequestStatus.NEW,
    val createdAt: Timestamp = Timestamp.now()
)

enum class BookingStatus { NEW, CONFIRMED, DONE, CANCELLED }

data class Booking(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val providerId: String = "",
    val service: String = "",
    val scheduledAt: Timestamp = Timestamp.now(),
    val status: BookingStatus = BookingStatus.NEW,
    val createdAt: Timestamp = Timestamp.now()
)

enum class AppointmentStatus { NEW, CONFIRMED, DONE, CANCELLED }

data class Appointment(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val storeOrProviderId: String = "",
    val purpose: String = "",
    val scheduledAt: Timestamp = Timestamp.now(),
    val status: AppointmentStatus = AppointmentStatus.NEW
)

data class Banner(
    @DocumentId val id: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val target: String = "",          // deep link / category
    val active: Boolean = true,
    val order: Int = 0
)

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val type: String = "store"        // store | service
)

/** Admin-added items shown on "Grow your business" page */
data class GrowItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val ctaText: String = "",
    val targetRole: String = "all"    // store_owner | service_provider | all
)

data class AppNotification(
    @DocumentId val id: String = "",
    val toUid: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

data class Review(
    @DocumentId val id: String = "",
    val targetType: String = "store",   // store | service
    val targetId: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/** A 1:1 conversation between a customer and a seller (deterministic id). */
data class ChatThread(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val storeId: String = "",
    val lastMessage: String = "",
    val updatedAt: Timestamp = Timestamp.now()
)

data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
