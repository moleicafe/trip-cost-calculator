package com.molei.costpertrip.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.molei.costpertrip.domain.FuelPriceMode
import com.molei.costpertrip.domain.TripMode

enum class DistanceSource { Manual, DirectionsAPI }

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** L/100km, normal mode */
    val consumptionNormal: Double,
    /** L/100km, eco mode (auto-derived from ecoSavingPercent unless set manually) */
    val consumptionEco: Double,
    val ecoSavingPercent: Double = 9.0,
    /** Whether consumptionEco is auto-derived (drives the setup-screen toggle state). */
    val ecoAutoDerived: Boolean = true,
    val tankCapacityLitres: Double,
    val fuelPriceMode: FuelPriceMode,
    val fuelPricePerTank: Double = 0.0,
    val fuelPricePerLitre: Double = 0.0,
    val currencySymbol: String = "$",
)

@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("vehicleId")],
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateEpochMillis: Long,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val distanceKm: Double,
    val distanceSource: DistanceSource,
    val mode: TripMode,
    /** Calculated at save time and stored, per spec. */
    val fuelUsedLitres: Double,
    val cost: Double,
)

class EnumConverters {
    @TypeConverter fun fuelPriceModeToString(v: FuelPriceMode): String = v.name
    @TypeConverter fun stringToFuelPriceMode(v: String): FuelPriceMode = FuelPriceMode.valueOf(v)
    @TypeConverter fun tripModeToString(v: TripMode): String = v.name
    @TypeConverter fun stringToTripMode(v: String): TripMode = TripMode.valueOf(v)
    @TypeConverter fun distanceSourceToString(v: DistanceSource): String = v.name
    @TypeConverter fun stringToDistanceSource(v: String): DistanceSource = DistanceSource.valueOf(v)
}
