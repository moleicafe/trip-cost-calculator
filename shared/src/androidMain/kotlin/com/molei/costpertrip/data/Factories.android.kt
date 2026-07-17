package com.molei.costpertrip.data

import android.content.Context
import androidx.room.Room

fun createAppDatabase(context: Context): AppDatabase {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("costpertrip.db")
    return buildAppDatabase(
        Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
    )
}

fun createSettingsRepository(context: Context): SettingsRepository {
    val appContext = context.applicationContext
    return SettingsRepository(
        createPreferencesDataStore {
            appContext.filesDir.resolve("settings.preferences_pb").absolutePath
        }
    )
}
