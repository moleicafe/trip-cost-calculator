package com.molei.fuelcost.ui.history

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
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TripRow(val trip: Trip, val vehicle: Vehicle?)

data class MonthTotal(val tripCount: Int, val cost: Double, val currencySymbol: String)

class TripHistoryViewModel(tripDao: TripDao, vehicleDao: VehicleDao) : ViewModel() {

    val rows: StateFlow<List<TripRow>> =
        combine(tripDao.observeAllNewestFirst(), vehicleDao.observeAll()) { trips, vehicles ->
            val byId = vehicles.associateBy { it.id }
            trips.map { TripRow(it, byId[it.vehicleId]) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Running total for the current calendar month (system time zone). */
    val monthTotal: StateFlow<MonthTotal> = rows.map { all ->
        val zone = ZoneId.systemDefault()
        val thisMonth = YearMonth.now(zone)
        val inMonth = all.filter {
            YearMonth.from(Instant.ofEpochMilli(it.trip.dateEpochMillis).atZone(zone)) == thisMonth
        }
        MonthTotal(
            tripCount = inMonth.size,
            cost = inMonth.sumOf { it.trip.cost },
            currencySymbol = (inMonth.firstOrNull() ?: all.firstOrNull())?.vehicle?.currencySymbol ?: "$",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthTotal(0, 0.0, "$"))

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FuelCostApp
                TripHistoryViewModel(app.database.tripDao(), app.database.vehicleDao())
            }
        }
    }
}
