package com.molei.fuelcost.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.molei.fuelcost.FuelCostApp
import com.molei.fuelcost.data.Trip
import com.molei.fuelcost.data.TripDao
import com.molei.fuelcost.data.Vehicle
import com.molei.fuelcost.data.VehicleDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TripDetail(val trip: Trip, val vehicle: Vehicle?)

class TripDetailViewModel(tripDao: TripDao, vehicleDao: VehicleDao, tripId: Long) : ViewModel() {

    val detail: StateFlow<TripDetail?> =
        combine(tripDao.byId(tripId), vehicleDao.observeAll()) { trip, vehicles ->
            trip?.let { TripDetail(it, vehicles.find { v -> v.id == it.vehicleId }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(tripId: Long) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FuelCostApp
                TripDetailViewModel(app.database.tripDao(), app.database.vehicleDao(), tripId)
            }
        }
    }
}
