package br.edu.ifsp.ifrota.poc_admin_entregas.data

import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.PolylineCodec
import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.RouteMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMetricsTest {

    @Test
    fun `mede o comprimento do trecho gravado`() {
        // 0,01 grau de latitude são cerca de 1,1 km.
        val encoded = PolylineCodec.encode(listOf(-21.78 to -48.20, -21.79 to -48.20))

        assertEquals(1113.2, RouteMetrics.pathMeters(encoded), 1.0)
    }

    @Test
    fun `trecho vazio ou de um ponto tem comprimento zero`() {
        assertEquals(0.0, RouteMetrics.pathMeters(""), 0.0)
        assertEquals(0.0, RouteMetrics.pathMeters(PolylineCodec.encode(listOf(-21.78 to -48.20))), 0.0)
    }

    @Test
    fun `formata metros e quilometros`() {
        assertEquals("850 m", RouteMetrics.formatKm(850.0))
        assertEquals("5,1 km", RouteMetrics.formatKm(5086.0))
    }
}
