package com.molei.costpertrip.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- DTOs for the Google Maps Directions API JSON response ---

@Serializable
data class DirectionsResponse(
    val status: String? = null,
    val routes: List<DirectionsRoute>? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
data class DirectionsRoute(val legs: List<DirectionsLeg>? = null)

@Serializable
data class DirectionsLeg(val distance: DirectionsDistance? = null)

@Serializable
data class DirectionsDistance(
    /** Distance in meters. */
    val value: Long? = null,
)

sealed interface DistanceLookupResult {
    data class Success(val distanceKm: Double) : DistanceLookupResult
    data class Failure(val message: String) : DistanceLookupResult
}

/**
 * Road-distance lookup via the Google Maps Directions API. Only ever called when the
 * user has configured an API key in Settings; there is deliberately NO straight-line/
 * haversine fallback (it under-estimates road distance — see spec).
 */
object DirectionsClient {

    private val http: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    suspend fun lookupDistanceKm(origin: String, destination: String, apiKey: String): DistanceLookupResult {
        if (apiKey.isBlank()) return DistanceLookupResult.Failure("No API key configured in Settings.")
        return try {
            val response: DirectionsResponse = http.get("https://maps.googleapis.com/maps/api/directions/json") {
                parameter("origin", origin)
                parameter("destination", destination)
                parameter("key", apiKey)
            }.body()
            extractDistanceKm(response)
        } catch (e: Exception) {
            DistanceLookupResult.Failure("Network error: ${e.message ?: e::class.simpleName ?: "unknown"}")
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
