package com.molei.costpertrip.data

import androidx.room.Room
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createAppDatabase(): AppDatabase =
    buildAppDatabase(
        Room.databaseBuilder<AppDatabase>(name = documentDirectory() + "/costpertrip.db")
    )

fun createSettingsRepository(): SettingsRepository =
    SettingsRepository(
        createPreferencesDataStore { documentDirectory() + "/settings.preferences_pb" }
    )

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) { "Could not resolve iOS documents directory" }
}
