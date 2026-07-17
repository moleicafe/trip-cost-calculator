package com.molei.costpertrip.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.molei.costpertrip.CostPerTripApp
import com.molei.costpertrip.data.DirectionsClient
import com.molei.costpertrip.data.DistanceLookupResult
import com.molei.costpertrip.data.DistanceSource
import com.molei.costpertrip.data.SettingsRepository
import com.molei.costpertrip.data.Trip
import com.molei.costpertrip.data.TripDao
import com.molei.costpertrip.data.Vehicle
import com.molei.costpertrip.data.VehicleDao
import com.molei.costpertrip.domain.FuelCalculator
import com.molei.costpertrip.domain.TripCalculation
import com.molei.costpertrip.domain.TripMode
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripFormState(
    val vehicleId: Long? = null,
    val distanceText: String = "",
    val mode: TripMode = TripMode.Normal,
    val startLocation: String = "",
    val endLocation: String = "",
    val distanceSource: DistanceSource = DistanceSource.Manual,
    val lookupInProgress: Boolean = false,
    val lookupError: String? = null,
)

class NewTripViewModel(
    private val vehicleDao: VehicleDao,
    private val tripDao: TripDao,
    private val settings: SettingsRepository,
) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _form = MutableStateFlow(TripFormState())
    val form: StateFlow<TripFormState> = _form.asStateFlow()

    val hasApiKey: StateFlow<Boolean> = settings.apiKey.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Default the vehicle dropdown to the last-used vehicle.
        viewModelScope.launch {
            val last = settings.lastVehicleId.first()
            if (last != null) _form.update { f -> if (f.vehicleId == null) f.copy(vehicleId = last) else f }
        }
    }

    val selectedVehicle: StateFlow<Vehicle?> = combine(vehicles, _form) { list, f ->
        list.find { it.id == f.vehicleId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Recomputes on every keystroke — live result card requirement. */
    val calculation: StateFlow<TripCalculation?> = combine(selectedVehicle, _form) { v, f ->
        val distance = f.distanceText.toDoubleOrNull()
        if (v == null || distance == null || distance <= 0.0) null
        else FuelCalculator.calculate(
            mode = f.mode,
            distanceKm = distance,
            consumptionNormal = v.consumptionNormal,
            consumptionEco = v.consumptionEco,
            priceMode = v.fuelPriceMode,
            fuelPricePerTank = v.fuelPricePerTank,
            tankCapacityLitres = v.tankCapacityLitres,
            fuelPricePerLitre = v.fuelPricePerLitre,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onVehicleSelected(id: Long) = _form.update { it.copy(vehicleId = id) }

    fun onModeChanged(mode: TripMode) = _form.update { it.copy(mode = mode) }

    fun onStartChanged(text: String) = _form.update { it.copy(startLocation = text, lookupError = null) }

    fun onEndChanged(text: String) = _form.update { it.copy(endLocation = text, lookupError = null) }

    /** Manual edits always reset the source to Manual. */
    fun onDistanceChanged(text: String) =
        _form.update { it.copy(distanceText = text, distanceSource = DistanceSource.Manual, lookupError = null) }

    fun lookupDistance() {
        val f = _form.value
        if (f.startLocation.isBlank() || f.endLocation.isBlank()) {
            _form.update { it.copy(lookupError = "Enter both start and end addresses first.") }
            return
        }
        viewModelScope.launch {
            val key = settings.apiKey.first()
            if (key.isBlank()) {
                // Spec: no API key -> manual entry only; never a silent straight-line estimate.
                _form.update {
                    it.copy(lookupError = "No Google Maps API key configured. Add one in Settings, or enter the distance manually.")
                }
                return@launch
            }
            _form.update { it.copy(lookupInProgress = true, lookupError = null) }
            when (val result = DirectionsClient.lookupDistanceKm(f.startLocation, f.endLocation, key)) {
                is DistanceLookupResult.Success -> _form.update {
                    it.copy(
                        distanceText = result.distanceKm.asDistanceInput(),
                        distanceSource = DistanceSource.DirectionsAPI,
                        lookupInProgress = false,
                    )
                }
                is DistanceLookupResult.Failure -> _form.update {
                    it.copy(lookupError = result.message, lookupInProgress = false)
                }
            }
        }
    }

    fun saveTrip(onSaved: () -> Unit) {
        val vehicle = selectedVehicle.value ?: return
        val calc = calculation.value ?: return
        val f = _form.value
        val distance = f.distanceText.toDoubleOrNull() ?: return
        viewModelScope.launch {
            tripDao.insert(
                Trip(
                    vehicleId = vehicle.id,
                    dateEpochMillis = System.currentTimeMillis(),
                    startLocation = f.startLocation.ifBlank { null },
                    endLocation = f.endLocation.ifBlank { null },
                    distanceKm = distance,
                    distanceSource = f.distanceSource,
                    mode = f.mode,
                    fuelUsedLitres = calc.fuelUsedLitres,
                    cost = calc.cost,
                )
            )
            settings.setLastVehicleId(vehicle.id)
            _form.update {
                it.copy(
                    distanceText = "", startLocation = "", endLocation = "",
                    distanceSource = DistanceSource.Manual, lookupError = null,
                )
            }
            onSaved()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CostPerTripApp
                NewTripViewModel(app.database.vehicleDao(), app.database.tripDao(), app.settings)
            }
        }
    }
}

/** The looked-up km value shown in (and used from) the distance field: 2 dp, trailing zeros trimmed. */
private fun Double.asDistanceInput(): String =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
