package br.edu.ifsp.ifrota.data.tracking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverTrackerTest {

    private fun location(
        at: Long,
        lat: Double = -21.78,
        phase: TrackingPhase = TrackingPhase.NAVIGATING,
        deliveryId: String? = "d1"
    ) = LiveLocation(
        driverId = "u1", plate = "ABC1D23", latitude = lat, longitude = -48.2, heading = 0f,
        speedKmh = 45, phase = phase, deliveryId = deliveryId, updatedAt = at
    )

    @Test
    fun firstPositionIsAlwaysSent() {
        assertTrue(DriverTracker.shouldPublish(location(0), null))
    }

    @Test
    fun movingIsThrottled() {
        val last = location(at = 0)
        assertFalse(DriverTracker.shouldPublish(location(at = 1_500, lat = -21.781), last))
        assertTrue(DriverTracker.shouldPublish(location(at = 5_000, lat = -21.781), last))
    }

    @Test
    fun milestonesAreSentImmediately() {
        val last = location(at = 0)
        assertTrue(DriverTracker.shouldPublish(location(at = 100, phase = TrackingPhase.ARRIVED), last))
        assertTrue(DriverTracker.shouldPublish(location(at = 100, deliveryId = "d2"), last))
    }

    @Test
    fun stoppedOnlySendsHeartbeat() {
        val last = location(at = 0, phase = TrackingPhase.PLANNING)
        assertFalse(DriverTracker.shouldPublish(location(at = 30_000, phase = TrackingPhase.PLANNING), last))
        assertTrue(DriverTracker.shouldPublish(location(at = 60_000, phase = TrackingPhase.PLANNING), last))
    }
}
