package com.localkart.common.repo

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.CatalogItem
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

        // Locations spread around Hyderabad (Madhapur ~17.448, 78.391) at increasing distance.
        val stores = listOf(
            Store(ownerUid = uid, name = "Ravikumar Grocery", category = "groceries",
                description = "Daily needs & fresh produce", rating = 4.6, ratingCount = 120,
                address = "Madhapur, Hyderabad", isOpen = true, approved = false,
                location = GeoPoint(17.4480, 78.3915)),
            Store(ownerUid = uid, name = "City Mobile Care", category = "mobile_repairing",
                description = "Screen & battery repairs", rating = 4.3, ratingCount = 58,
                address = "Kondapur", isOpen = true, approved = false,
                location = GeoPoint(17.4615, 78.3690)),
            Store(ownerUid = uid, name = "Sri Fancy World", category = "fancy",
                description = "Gifts, cosmetics & accessories", rating = 4.1, ratingCount = 41,
                address = "Gachibowli", isOpen = true, approved = false,
                location = GeoPoint(17.4400, 78.3489)),
            Store(ownerUid = uid, name = "Net Zone Cyber", category = "net_center",
                description = "Printouts, scanning, online forms", rating = 4.4, ratingCount = 30,
                address = "Miyapur", isOpen = true, approved = false,
                location = GeoPoint(17.4948, 78.3578))
        )
        val storeRefs = stores.map { db.collection("stores").add(it).await() }

        val products = listOf(
            Product(storeId = storeRefs[0].id, name = "Rice 5kg", category = "groceries",
                price = 320.0, mrp = 360.0, unit = "5 kg", inStock = true, approved = true),
            Product(storeId = storeRefs[0].id, name = "Sunflower Oil 1L", category = "groceries",
                price = 145.0, mrp = 160.0, unit = "1 L", inStock = true, approved = true),
            Product(storeId = storeRefs[0].id, name = "Toor Dal 1kg", category = "groceries",
                price = 130.0, mrp = 150.0, unit = "1 kg", inStock = true, approved = true)
        )
        products.forEach { db.collection("products").add(it).await() }

        // Seed a small master catalog (company-defined) for sellers to pick from.
        val catalog = listOf(
            CatalogItem(name = "Rice 5kg", type = "product", category = "groceries", suggestedMrp = 360.0, unit = "5 kg"),
            CatalogItem(name = "Sunflower Oil 1L", type = "product", category = "groceries", suggestedMrp = 160.0, unit = "1 L"),
            CatalogItem(name = "Toor Dal 1kg", type = "product", category = "groceries", suggestedMrp = 150.0, unit = "1 kg"),
            CatalogItem(name = "Wheat Atta 5kg", type = "product", category = "groceries", suggestedMrp = 280.0, unit = "5 kg"),
            CatalogItem(name = "Screen Replacement", type = "service", category = "mobile_repairing", suggestedMrp = 1500.0),
            CatalogItem(name = "Tap Repair", type = "service", category = "plumber", suggestedMrp = 250.0),
            CatalogItem(name = "Fan Installation", type = "service", category = "electrician", suggestedMrp = 300.0)
        )
        catalog.forEach { db.collection("catalog").add(it).await() }

        val services = listOf(
            ServiceProvider(ownerUid = uid, name = "Anil Plumbing", category = "plumber",
                description = "Leaks, fittings, bathroom work", rating = 4.5, ratingCount = 64,
                available = true, address = "Madhapur", pricePerVisit = 199.0, approved = false,
                location = GeoPoint(17.4495, 78.3930)),
            ServiceProvider(ownerUid = uid, name = "Ravi Electricals", category = "electrician",
                description = "Wiring, fans, switches, repairs", rating = 4.8, ratingCount = 90,
                available = true, address = "Kondapur", pricePerVisit = 249.0, approved = false,
                location = GeoPoint(17.4630, 78.3700)),
            ServiceProvider(ownerUid = uid, name = "Kumar Carpentry", category = "carpenter",
                description = "Furniture, doors, fittings", rating = 4.2, ratingCount = 37,
                available = true, address = "Gachibowli", pricePerVisit = 299.0, approved = false,
                location = GeoPoint(17.4410, 78.3500)),
            ServiceProvider(ownerUid = uid, name = "GreenThumb Garden", category = "gardener",
                description = "Garden setup & maintenance", rating = 4.4, ratingCount = 22,
                available = true, address = "Miyapur", pricePerVisit = 349.0, approved = false,
                location = GeoPoint(17.4960, 78.3590))
        )
        services.forEach { db.collection("services").add(it).await() }

        stores.size + products.size + services.size
    }
}
