package com.localkart.seller.ui.provider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.Booking
import com.localkart.common.model.BookingStatus
import com.localkart.common.repo.FirestoreRepo
import kotlinx.coroutines.launch

class ProviderBookingsViewModel : ViewModel() {
    private val repo = FirestoreRepo()

    var bookings by mutableStateOf<List<Booking>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            loading = true; error = null
            val uid = AuthManager.currentUid
            if (uid == null) { error = "Not signed in"; loading = false; return@launch }
            runCatching { repo.bookingsForOwner(uid) }
                .onSuccess { bookings = it }
                .onFailure { error = it.message ?: "Failed to load bookings" }
            loading = false
        }
    }

    /** NEW → CONFIRMED → DONE */
    fun advance(b: Booking) {
        val next = when (b.status) {
            BookingStatus.NEW -> BookingStatus.CONFIRMED
            BookingStatus.CONFIRMED -> BookingStatus.DONE
            else -> return
        }
        viewModelScope.launch { runCatching { repo.updateBookingStatus(b.id, next) }.onSuccess { load() } }
    }

    fun cancel(b: Booking) {
        viewModelScope.launch {
            runCatching { repo.updateBookingStatus(b.id, BookingStatus.CANCELLED) }.onSuccess { load() }
        }
    }
}
