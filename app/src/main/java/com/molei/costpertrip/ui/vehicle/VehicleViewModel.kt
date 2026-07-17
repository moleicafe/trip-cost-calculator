package com.molei.costpertrip.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.molei.costpertrip.CostPerTripApp
import com.molei.costpertrip.data.SettingsRepository
import com.molei.costpertrip.data.Vehicle
import com.molei.costpertrip.data.VehicleDao
import com.molei.costpertrip.domain.FuelCalculator
import com.molei.costpertrip.domain.FuelPriceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehicleListViewModel(private val vehicleDao: VehicleDao) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch { vehicleDao.delete(vehicle) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CostPerTripApp
                VehicleListViewModel(app.database.vehicleDao())
            }
        }
    }
}

data class VehicleFormState(
    val id: Long = 0,
    val name: String = "",
    val consumptionNormal: String = "",
    val consumptionEco: String = "",
    val ecoSavingPercent: String = "9",
    val ecoAutoDerived: Boolean = true,
    val tankCapacityLitres: String = "",
    val fuelPriceMode: FuelPriceMode = FuelPriceMode.PerTank,
    val fuelPricePerTank: String = "",
    val fuelPricePerLitre: String = "",
    val currencySymbol: String = "$",
    val loaded: Boolean = false,
    val saveError: String? = null,
) {
    /** Live-derived eco consumption while the auto toggle is on (formula 5). */
    val derivedEco: Double?
        get() {
            val normal = consumptionNormal.toDoubleOrNull() ?: return null
            val pct = ecoSavingPercent.toDoubleOrNull() ?: return null
            return FuelCalculator.deriveEcoConsumption(normal, pct)
        }

    /** Live-derived price per litre in PerTank mode (formula 1). */
    val derivedPricePerLitre: Double?
        get() {
            if (fuelPriceMode != FuelPriceMode.PerTank) return fuelPricePerLitre.toDoubleOrNull()
            val perTank = fuelPricePerTank.toDoubleOrNull() ?: return null
            val tank = tankCapacityLitres.toDoubleOrNull() ?: return null
            if (tank <= 0.0) return null
            return FuelCalculator.pricePerLitre(FuelPriceMode.PerTank, perTank, tank, 0.0)
        }
}

class VehicleEditViewModel(
    private val vehicleDao: VehicleDao,
    private val settings: SettingsRepository,
    private val vehicleId: Long?,
) : ViewModel() {

    private val _form = MutableStateFlow(VehicleFormState(loaded = vehicleId == null))
    val form: StateFlow<VehicleFormState> = _form.asStateFlow()

    init {
        if (vehicleId != null) {
            viewModelScope.launch {
                vehicleDao.byId(vehicleId).first()?.let { v ->
                    _form.value = VehicleFormState(
                        id = v.id,
                        name = v.name,
                        consumptionNormal = v.consumptionNormal.toInput(),
                        consumptionEco = v.consumptionEco.toInput(),
                        ecoSavingPercent = v.ecoSavingPercent.toInput(),
                        ecoAutoDerived = v.ecoAutoDerived,
                        tankCapacityLitres = v.tankCapacityLitres.toInput(),
                        fuelPriceMode = v.fuelPriceMode,
                        fuelPricePerTank = if (v.fuelPricePerTank > 0) v.fuelPricePerTank.toInput() else "",
                        fuelPricePerLitre = if (v.fuelPricePerLitre > 0) v.fuelPricePerLitre.toInput() else "",
                        currencySymbol = v.currencySymbol,
                        loaded = true,
                    )
                } ?: _form.update { it.copy(loaded = true) }
            }
        }
    }

    fun update(transform: (VehicleFormState) -> VehicleFormState) =
        _form.update { transform(it).copy(saveError = null) }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val normal = f.consumptionNormal.toDoubleOrNull()
        val tank = f.tankCapacityLitres.toDoubleOrNull()
        val ecoPct = f.ecoSavingPercent.toDoubleOrNull()
        val eco = if (f.ecoAutoDerived) f.derivedEco else f.consumptionEco.toDoubleOrNull()
        val perTank = f.fuelPricePerTank.toDoubleOrNull()
        val perLitre = f.fuelPricePerLitre.toDoubleOrNull()

        val error = when {
            f.name.isBlank() -> "Give the vehicle a name."
            normal == null || normal <= 0 -> "Normal consumption must be a number above 0."
            f.ecoAutoDerived && (ecoPct == null || ecoPct < 0 || ecoPct >= 100) ->
                "Eco saving % must be between 0 and 100."
            eco == null || eco <= 0 -> "Eco consumption must be a number above 0."
            tank == null || tank <= 0 -> "Tank capacity must be a number above 0."
            f.fuelPriceMode == FuelPriceMode.PerTank && (perTank == null || perTank <= 0) ->
                "Full-tank price must be a number above 0."
            f.fuelPriceMode == FuelPriceMode.PerLitre && (perLitre == null || perLitre <= 0) ->
                "Price per litre must be a number above 0."
            f.currencySymbol.isBlank() -> "Currency symbol can't be empty."
            else -> null
        }
        if (error != null) {
            _form.update { it.copy(saveError = error) }
            return
        }

        viewModelScope.launch {
            val id = vehicleDao.upsert(
                Vehicle(
                    id = f.id,
                    name = f.name.trim(),
                    consumptionNormal = normal!!,
                    consumptionEco = eco!!,
                    ecoSavingPercent = ecoPct ?: 9.0,
                    ecoAutoDerived = f.ecoAutoDerived,
                    tankCapacityLitres = tank!!,
                    fuelPriceMode = f.fuelPriceMode,
                    fuelPricePerTank = perTank ?: 0.0,
                    fuelPricePerLitre = perLitre ?: 0.0,
                    currencySymbol = f.currencySymbol.trim(),
                )
            )
            // Make the first vehicle the default selection on the New Trip screen.
            if (settings.lastVehicleId.first() == null) {
                settings.setLastVehicleId(if (f.id != 0L) f.id else id)
            }
            onSaved()
        }
    }

    companion object {
        fun factory(vehicleId: Long?) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CostPerTripApp
                VehicleEditViewModel(app.database.vehicleDao(), app.settings, vehicleId)
            }
        }
    }
}

private fun Double.toInput(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()
