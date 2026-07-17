package com.molei.fuelcost

import android.app.Application
import com.molei.fuelcost.data.AppDatabase
import com.molei.fuelcost.data.SettingsRepository

class FuelCostApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
