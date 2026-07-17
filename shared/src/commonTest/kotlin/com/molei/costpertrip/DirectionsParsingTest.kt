package com.molei.costpertrip

import com.molei.costpertrip.data.DirectionsClient
import com.molei.costpertrip.data.DirectionsDistance
import com.molei.costpertrip.data.DirectionsLeg
import com.molei.costpertrip.data.DirectionsResponse
import com.molei.costpertrip.data.DirectionsRoute
import com.molei.costpertrip.data.DistanceLookupResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DirectionsParsingTest {

    private fun response(status: String, vararg legMeters: Long) = DirectionsResponse(
        status = status,
        routes = if (legMeters.isEmpty()) emptyList() else listOf(
            DirectionsRoute(legs = legMeters.map { DirectionsLeg(DirectionsDistance(value = it)) })
        ),
        errorMessage = null,
    )

    @Test
    fun sumsLegsOfFirstRouteAndConvertsMetersToKm() {
        val result = DirectionsClient.extractDistanceKm(response("OK", 15_000L, 6_000L))
        assertIs<DistanceLookupResult.Success>(result)
        assertEquals(21.0, result.distanceKm, 1e-9)
    }

    @Test
    fun singleLegRoadDistance() {
        val result = DirectionsClient.extractDistanceKm(response("OK", 21_337L))
        assertIs<DistanceLookupResult.Success>(result)
        assertEquals(21.337, result.distanceKm, 1e-9)
    }

    @Test
    fun zeroResultsIsAReadableFailureNotACrash() {
        val result = DirectionsClient.extractDistanceKm(response("ZERO_RESULTS"))
        assertIs<DistanceLookupResult.Failure>(result)
        assertTrue(result.message.contains("route", ignoreCase = true))
    }

    @Test
    fun requestDeniedSurfacesApiError() {
        val result = DirectionsClient.extractDistanceKm(
            DirectionsResponse(status = "REQUEST_DENIED", routes = emptyList(), errorMessage = "The provided API key is invalid.")
        )
        assertIs<DistanceLookupResult.Failure>(result)
        assertTrue(result.message.contains("invalid", ignoreCase = true))
    }

    @Test
    fun okStatusWithEmptyRoutesIsAFailure() {
        val result = DirectionsClient.extractDistanceKm(response("OK"))
        assertIs<DistanceLookupResult.Failure>(result)
    }
}
