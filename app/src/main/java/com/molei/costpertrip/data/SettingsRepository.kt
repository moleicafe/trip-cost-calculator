package com.molei.costpertrip.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * App settings. The Directions API key lives ONLY here (user-entered, stored locally) —
 * never hardcoded in source, per spec.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("directions_api_key")
        val LAST_VEHICLE_ID = longPreferencesKey("last_vehicle_id")
    }

    val apiKey: Flow<String> = context.settingsDataStore.data.map { it[Keys.API_KEY] ?: "" }

    val lastVehicleId: Flow<Long?> = context.settingsDataStore.data.map { it[Keys.LAST_VEHICLE_ID] }

    suspend fun setApiKey(key: String) {
        context.settingsDataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun setLastVehicleId(id: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_VEHICLE_ID] = id }
    }
}
