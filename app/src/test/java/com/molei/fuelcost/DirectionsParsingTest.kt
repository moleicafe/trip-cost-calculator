package com.molei.fuelcost

import com.molei.fuelcost.data.DirectionsClient
import com.molei.fuelcost.data.DirectionsDistance
import com.molei.fuelcost.data.DirectionsLeg
import com.molei.fuelcost.data.DirectionsResponse
import com.molei.fuelcost.data.DirectionsRoute
import com.molei.fuelcost.data.DistanceLookupResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsParsingTest {

    private fun response(status: String, vararg legMeters: Long) = DirectionsResponse(
        status = status,
        routes = if (legMeters.isEmpty()) emptyList() else listOf(
            DirectionsRoute(legs = legMeters.map { DirectionsLeg(DirectionsDistance(value = it)) })
        ),
        errorMessage = null,
    )

    @Test
    fun `sums legs of first route and converts meters to km`() {
        val result = DirectionsClient.extractDistanceKm(response("OK", 15_000L, 6_000L))
        assertTrue(result is DistanceLookupResult.Success)
        assertEquals(21.0, (result as DistanceLookupResult.Success).distanceKm, 1e-9)
    }

    @Test
    fun `single leg road distance`() {
        val result = DirectionsClient.extractDistanceKm(response("OK", 21_337L))
        assertEquals(21.337, (result as DistanceLookupResult.Success).distanceKm, 1e-9)
    }

    @Test
    fun `zero results is a readable failure, not a crash`() {
        val result = DirectionsClient.extractDistanceKm(response("ZERO_RESULTS"))
        assertTrue(result is DistanceLookupResult.Failure)
        assertTrue((result as DistanceLookupResult.Failure).message.contains("route", ignoreCase = true))
    }

    @Test
    fun `request denied surfaces API error`() {
        val result = DirectionsClient.extractDistanceKm(
            DirectionsResponse(status = "REQUEST_DENIED", routes = emptyList(), errorMessage = "The provided API key is invalid.")
        )
        assertTrue(result is DistanceLookupResult.Failure)
        assertTrue((result as DistanceLookupResult.Failure).message.contains("invalid", ignoreCase = true))
    }

    @Test
    fun `ok status with empty routes is a failure`() {
        val result = DirectionsClient.extractDistanceKm(response("OK"))
        assertTrue(result is DistanceLookupResult.Failure)
    }
}
