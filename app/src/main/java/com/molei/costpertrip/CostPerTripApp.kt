package com.molei.costpertrip

import android.app.Application
import com.molei.costpertrip.data.AppDatabase
import com.molei.costpertrip.data.SettingsRepository

class CostPerTripApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
