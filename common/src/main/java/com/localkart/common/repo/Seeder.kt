package com.localkart.common.repo

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Product
import com.localkart.common.model.ServiceProvider
import com.localkart.common.model.Store
import kotlinx.coroutines.tasks.await

/**
 * One-tap demo data for development. Creates a handful of stores, service providers
 * and products owned by the current user. Listings are written with approved = false
 * (the only thing client security rules allow); the dev queries pass onlyApproved=false
 * so they still appear. Remove/disable before production.
 */
object Seeder {

    suspend fun seed(): Result<Int> = runCatching {
        val uid = AuthManager.currentUid ?: error("Sign in first")
        val db = Firebase.firestore

        val stores = listOf(
            Store(ownerUid = uid, name = "Ravikumar Grocery", category = "groceries",
                description = "Daily needs & fresh produce", rating = 4.6, ratingCount = 120,
                address = "Madhapur, Hyderabad", isOpen = true, approved = false),
            Store(ownerUid = uid, name = "City Mobile Care", category = "mobile_repairing",
                description = "Screen & battery repairs", rating = 4.3, ratingCount = 58,
                address = "Kondapur", isOpen = true, approved = false),
            Store(ownerUid = uid, name = "Sri Fancy World", category = "fancy",
                description = "Gifts, cosmetics & accessories", rating = 4.1, ratingCount = 41,
                address = "Gachibowli", isOpen = true, approved = false),
            Store(ownerUid = uid, name = "Net Zone Cyber", category = "net_center",
                description = "Printouts, scanning, online forms", rating = 4.4, ratingCount = 30,
                address = "Miyapur", isOpen = true, approved = false)
        )
        val storeRefs = stores.map { db.collection("stores").add(it).await() }

        val products = listOf(
            Product(storeId = storeRefs[0].id, name = "Rice 5kg", category = "groceries",
                price = 320.0, mrp = 360.0, unit = "5 kg", inStock = true),
            Product(storeId = storeRefs[0].id, name = "Sunflower Oil 1L", category = "groceries",
                price = 145.0, mrp = 160.0, unit = "1 L", inStock = true),
            Product(storeId = storeRefs[0].id, name = "Toor Dal 1kg", category = "groceries",
                price = 130.0, mrp = 150.0, unit = "1 kg", inStock = true)
        )
        products.forEach { db.collection("products").add(it).await() }

        val services = listOf(
            ServiceProvider(ownerUid = uid, name = "Anil Plumbing", category = "plumber",
                description = "Leaks, fittings, bathroom work", rating = 4.5, ratingCount = 64,
                available = true, address = "Madhapur", pricePerVisit = 199.0, approved = false),
            ServiceProvider(ownerUid = uid, name = "Ravi Electricals", category = "electrician",
                description = "Wiring, fans, switches, repairs", rating = 4.8, ratingCount = 90,
                available = true, address = "Kondapur", pricePerVisit = 249.0, approved = false),
            ServiceProvider(ownerUid = uid, name = "Kumar Carpentry", category = "carpenter",
                description = "Furniture, doors, fittings", rating = 4.2, ratingCount = 37,
                available = true, address = "Gachibowli", pricePerVisit = 299.0, approved = false),
            ServiceProvider(ownerUid = uid, name = "GreenThumb Garden", category = "gardener",
                description = "Garden setup & maintenance", rating = 4.4, ratingCount = 22,
                available = true, address = "Miyapur", pricePerVisit = 349.0, approved = false)
        )
        services.forEach { db.collection("services").add(it).await() }

        stores.size + products.size + services.size
    }
}
