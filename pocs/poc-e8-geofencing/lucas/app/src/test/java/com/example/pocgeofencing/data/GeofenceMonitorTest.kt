package com.example.pocgeofencing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceMonitorTest {

    private val center = -21.7980 to -48.1830
    private val fence = Geofence("cd", "CD", center.first, center.second, radiusMeters = 100.0)

    /** Posição a [meters] ao norte do centro do perímetro. */
    private fun at(meters: Double, id: String = "v1") =
        Geo.offset(center.first, center.second, 0f, meters).let { (lat, lon) -> VehiclePosition(id, "ABC1D23", lat, lon) }

    @Test
    fun primeiraPosicaoNaoGeraEvento() {
        val monitor = GeofenceMonitor(listOf(fence))
        assertTrue(monitor.update(at(0.0)).isEmpty())
        assertEquals(setOf("cd"), monitor.insideOf("v1"))
    }

    @Test
    fun saidaEEntradaGeramEventos() {
        val monitor = GeofenceMonitor(listOf(fence), hysteresisMeters = 0.0)
        monitor.update(at(0.0))

        val exit = monitor.update(at(150.0))
        assertEquals(listOf(GeofenceTransition.EXIT), exit.map { it.transition })
        assertEquals("ABC1D23 saiu de CD", exit.single().message)

        assertTrue(monitor.update(at(200.0)).isEmpty())

        val enter = monitor.update(at(50.0))
        assertEquals(listOf(GeofenceTransition.ENTER), enter.map { it.transition })
    }

    @Test
    fun histereseEvitaEventosNaBorda() {
        val monitor = GeofenceMonitor(listOf(fence), hysteresisMeters = 20.0)
        monitor.update(at(90.0))
        // Passou do raio, mas não da histerese: continua dentro.
        assertTrue(monitor.update(at(110.0)).isEmpty())
        assertTrue(monitor.update(at(95.0)).isEmpty())
        assertEquals(1, monitor.update(at(130.0)).size)
    }

    @Test
    fun perimetroDeOutroVeiculoEIgnorado() {
        val onlyV2 = fence.copy(vehicleIds = setOf("v2"))
        val monitor = GeofenceMonitor(listOf(onlyV2))
        monitor.update(at(500.0, id = "v1"))
        assertTrue(monitor.update(at(0.0, id = "v1")).isEmpty())

        monitor.update(at(500.0, id = "v2"))
        assertEquals(1, monitor.update(at(0.0, id = "v2")).size)
    }
}
