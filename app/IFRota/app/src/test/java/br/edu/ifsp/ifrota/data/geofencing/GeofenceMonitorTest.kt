package br.edu.ifsp.ifrota.data.geofencing

import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.simulation.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceMonitorTest {

    private val cd = DeliveryGeofences.distributionCenter

    /** Posição a [meters] ao norte do CD. */
    private fun north(meters: Double, id: String = "v1") =
        Geo.offset(DistributionCenter.LATITUDE, DistributionCenter.LONGITUDE, 0f, meters)
            .let { (lat, lon) -> VehiclePosition(id, "ABC1D23", lat, lon) }

    private fun delivery(order: Int, metersNorth: Double): Delivery {
        val (lat, lon) = Geo.offset(DistributionCenter.LATITUDE, DistributionCenter.LONGITUDE, 0f, metersNorth)
        return Delivery(
            id = "d$order", order = order, recipient = "Cliente $order", address = "Rua $order",
            neighborhood = "Centro", city = "Araraquara", packageType = "Caixa",
            latitude = lat, longitude = lon
        )
    }

    @Test
    fun primeiraPosicaoSoRegistraOEstado() {
        val monitor = GeofenceMonitor(listOf(cd))
        assertTrue(monitor.update(north(0.0)).isEmpty())
        assertEquals(setOf(DeliveryGeofences.CD_ID), monitor.insideOf("v1"))
    }

    @Test
    fun saidaDoCdEChegadaNaEntrega() {
        val deliveries = listOf(delivery(1, 1_000.0))
        val monitor = GeofenceMonitor(DeliveryGeofences.forRoute(deliveries, current = 0))

        monitor.update(north(0.0))
        val leftCd = monitor.update(north(300.0))
        assertEquals(listOf(GeofenceTransition.EXIT), leftCd.map { it.transition })
        assertEquals(DeliveryGeofences.CD_ID, leftCd.single().geofence.id)
        assertEquals("ABC1D23 saiu do perímetro: ${cd.name}", leftCd.single().message)

        assertTrue(monitor.update(north(600.0)).isEmpty())

        val arrived = monitor.update(north(900.0))
        assertEquals(listOf(GeofenceTransition.ENTER), arrived.map { it.transition })
        assertEquals(DeliveryGeofences.idOf(deliveries[0]), arrived.single().geofence.id)
    }

    @Test
    fun histereseEvitaEventosRepetidosNaBorda() {
        val monitor = GeofenceMonitor(listOf(cd), hysteresisMeters = 20.0)
        monitor.update(north(190.0))
        assertTrue(monitor.update(north(210.0)).isEmpty())
        assertTrue(monitor.update(north(195.0)).isEmpty())
        assertEquals(1, monitor.update(north(230.0)).size)
    }

    @Test
    fun perimetroDeOutroVeiculoEIgnorado() {
        val onlyV2 = cd.copy(vehicleIds = setOf("v2"))
        val monitor = GeofenceMonitor(listOf(onlyV2))
        monitor.update(north(1_000.0, id = "v1"))
        assertTrue(monitor.update(north(0.0, id = "v1")).isEmpty())

        monitor.update(north(1_000.0, id = "v2"))
        assertEquals(1, monitor.update(north(0.0, id = "v2")).size)
    }

    @Test
    fun resetFazAProximaPosicaoValerComoPrimeira() {
        val monitor = GeofenceMonitor(listOf(cd))
        monitor.update(north(1_000.0))
        monitor.reset("v1")
        assertTrue(monitor.update(north(0.0)).isEmpty())
    }

    @Test
    fun rotaMonitoraCdParadaAnteriorEEntregaAtual() {
        val deliveries = listOf(delivery(1, 500.0), delivery(2, 1_000.0), delivery(3, 1_500.0))

        assertEquals(
            listOf(DeliveryGeofences.CD_ID, "entrega-d1"),
            DeliveryGeofences.forRoute(deliveries, current = 0).map { it.id }
        )
        assertEquals(
            listOf(DeliveryGeofences.CD_ID, "entrega-d1", "entrega-d2"),
            DeliveryGeofences.forRoute(deliveries, current = 1).map { it.id }
        )
        // Tudo entregue: sobra a última parada (de onde o veículo sai) e o CD.
        assertEquals(
            listOf(DeliveryGeofences.CD_ID, "entrega-d3"),
            DeliveryGeofences.forRoute(deliveries, current = 3).map { it.id }
        )
    }

    @Test
    fun trocarPerimetrosNaoGeraEventoDosRemovidos() {
        val deliveries = listOf(delivery(1, 500.0), delivery(2, 1_000.0), delivery(3, 1_500.0))
        val monitor = GeofenceMonitor(DeliveryGeofences.forRoute(deliveries, current = 1))
        monitor.update(north(500.0)) // na entrega 1
        monitor.setGeofences(DeliveryGeofences.forRoute(deliveries, current = 2))
        // A entrega 1 deixou de ser monitorada: sair dela não notifica.
        assertTrue(monitor.update(north(750.0)).isEmpty())
    }
}
