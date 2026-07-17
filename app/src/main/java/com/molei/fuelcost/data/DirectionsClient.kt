package com.molei.fuelcost.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- DTOs for the Google Maps Directions API JSON response ---

data class DirectionsResponse(
    val status: String?,
    val routes: List<DirectionsRoute>?,
    @SerializedName("error_message") val errorMessage: String?,
)

data class DirectionsRoute(val legs: List<DirectionsLeg>?)

data class DirectionsLeg(val distance: DirectionsDistance?)

data class DirectionsDistance(
    /** Distance in meters. */
    val value: Long?,
)

sealed interface DistanceLookupResult {
    data class Success(val distanceKm: Double) : DistanceLookupResult
    data class Failure(val message: String) : DistanceLookupResult
}

private interface DirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
    ): DirectionsResponse
}

/**
 * Road-distance lookup via the Google Maps Directions API. Only ever called when the
 * user has configured an API key in Settings; there is deliberately NO straight-line/
 * haversine fallback (it under-estimates road distance — see spec).
 */
object DirectionsClient {

    private val api: DirectionsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsApi::class.java)
    }

    suspend fun lookupDistanceKm(origin: String, destination: String, apiKey: String): DistanceLookupResult {
        if (apiKey.isBlank()) return DistanceLookupResult.Failure("No API key configured in Settings.")
        return try {
            extractDistanceKm(api.directions(origin, destination, apiKey))
        } catch (e: Exception) {
            DistanceLookupResult.Failure("Network error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Pure parsing step, unit-tested: sum the first route's leg distances (meters) → km. */
    fun extractDistanceKm(response: DirectionsResponse): DistanceLookupResult {
        when (response.status) {
            "OK" -> Unit
            "ZERO_RESULTS" -> return DistanceLookupResult.Failure("No route found between those addresses.")
            else -> return DistanceLookupResult.Failure(
                response.errorMessage ?: "Directions API error: ${response.status ?: "empty response"}"
            )
        }
        val legs = response.routes?.firstOrNull()?.legs.orEmpty()
        val meters = legs.sumOf { it.distance?.value ?: 0L }
        if (legs.isEmpty() || meters <= 0L) {
            return DistanceLookupResult.Failure("Directions API returned no usable distance.")
        }
        return DistanceLookupResult.Success(meters / 1000.0)
    }
}
