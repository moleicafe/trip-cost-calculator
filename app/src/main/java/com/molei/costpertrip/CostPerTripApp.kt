package com.molei.costpertrip

import android.app.Application
import com.molei.costpertrip.data.AppDatabase
import com.molei.costpertrip.data.SettingsRepository
import com.molei.costpertrip.data.createAppDatabase
import com.molei.costpertrip.data.createSettingsRepository

class CostPerTripApp : Application() {
    val database: AppDatabase by lazy { createAppDatabase(this) }
    val settings: SettingsRepository by lazy { createSettingsRepository(this) }
}
