package com.molei.costpertrip.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/**
 * App settings. The Directions API key lives ONLY here (user-entered, stored locally) —
 * never hardcoded in source, per spec. Construct via the platform `createSettingsRepository`.
 */
class SettingsRepository internal constructor(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val API_KEY = stringPreferencesKey("directions_api_key")
        val LAST_VEHICLE_ID = longPreferencesKey("last_vehicle_id")
    }

    val apiKey: Flow<String> = dataStore.data.map { it[Keys.API_KEY] ?: "" }

    val lastVehicleId: Flow<Long?> = dataStore.data.map { it[Keys.LAST_VEHICLE_ID] }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[Keys.API_KEY] = key.trim() }
    }

    suspend fun setLastVehicleId(id: Long) {
        dataStore.edit { it[Keys.LAST_VEHICLE_ID] = id }
    }
}

internal fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
