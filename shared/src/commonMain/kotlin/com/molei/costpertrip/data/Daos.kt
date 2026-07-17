package com.molei.costpertrip.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY name")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun byId(id: Long): Flow<Vehicle?>

    @Upsert
    suspend fun upsert(vehicle: Vehicle): Long

    @Delete
    suspend fun delete(vehicle: Vehicle)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY dateEpochMillis DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun byId(id: Long): Flow<Trip?>

    @Insert
    suspend fun insert(trip: Trip): Long

    @Delete
    suspend fun delete(trip: Trip)
}
