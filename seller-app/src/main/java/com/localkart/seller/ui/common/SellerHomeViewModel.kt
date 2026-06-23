package com.localkart.seller.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.BookingStatus
import com.localkart.common.model.RequestStatus
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch
import java.util.Calendar

/** Loads the seller's real profile + today's metrics for the home dashboards. */
class SellerHomeViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var name by mutableStateOf(AuthManager.currentName); private set
    var storeName by mutableStateOf("Your store"); private set
    var storeCategory by mutableStateOf("—"); private set
    var storeId by mutableStateOf<String?>(null); private set
    var storeOpen by mutableStateOf(true); private set
    var serviceName by mutableStateOf("Your service"); private set
    var serviceCategory by mutableStateOf("—"); private set
    var serviceId by mutableStateOf<String?>(null); private set
    var available by mutableStateOf(true); private set
    var ordersToday by mutableStateOf(0); private set
    var salesToday by mutableStateOf(0.0); private set
    var newRequests by mutableStateOf(0); private set
    var upcomingBookings by mutableStateOf(0); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            val uid = AuthManager.currentUid ?: return@launch
            runCatching {
                AuthManager.currentUser()?.let { if (it.name.isNotBlank()) name = it.name }

                repo.storesForOwner(uid).firstOrNull()?.let {
                    storeName = it.name; storeCategory = it.category.replace('_', ' ')
                    storeId = it.id; storeOpen = it.isOpen
                }
                repo.servicesForOwner(uid).firstOrNull()?.let {
                    serviceName = it.name; serviceCategory = it.category.replaceFirstChar { c -> c.uppercase() }
                    serviceId = it.id; available = it.available
                }

                val orders = repo.ordersForOwner(uid)
                val dayStart = startOfToday()
                val today = orders.filter { it.createdAt.toDate().time >= dayStart }
                ordersToday = today.size
                salesToday = today.sumOf { it.total }

                newRequests = repo.serviceRequestsForOwner(uid).count { it.status == RequestStatus.NEW }
                upcomingBookings = repo.bookingsForOwner(uid)
                    .count { it.status == BookingStatus.NEW || it.status == BookingStatus.CONFIRMED }
            }
        }
    }

    fun toggleStoreOpen(open: Boolean) {
        storeOpen = open
        storeId?.let { id -> viewModelScope.launch { runCatching { repo.setStoreOpen(id, open) } } }
    }

    fun toggleAvailable(a: Boolean) {
        available = a
        serviceId?.let { id -> viewModelScope.launch { runCatching { repo.setProviderAvailable(id, a) } } }
    }

    private fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
